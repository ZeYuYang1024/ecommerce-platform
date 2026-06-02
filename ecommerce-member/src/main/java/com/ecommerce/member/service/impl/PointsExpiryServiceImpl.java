package com.ecommerce.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ecommerce.common.util.SnowflakeUtils;
import com.ecommerce.member.entity.MemberProfile;
import com.ecommerce.member.entity.PointsTransaction;
import com.ecommerce.member.mapper.MemberProfileMapper;
import com.ecommerce.member.mapper.PointsTransactionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PointsExpiryServiceImpl {

    private final PointsTransactionMapper pointsTransactionMapper;
    private final MemberProfileMapper memberProfileMapper;

    private static final int BATCH_SIZE = 500;

    /**
     * 每日凌晨 2:00 扫描过期积分
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void expirePoints() {
        log.info("Points expiry scan started");

        int totalExpired = 0;
        LocalDateTime now = LocalDateTime.now();

        while (true) {
            // 查询未过期、有剩余可用量的 EARN 记录
            List<PointsTransaction> expiredList = pointsTransactionMapper.selectList(
                    new LambdaQueryWrapper<PointsTransaction>()
                            .eq(PointsTransaction::getDirection, "EARN")
                            .lt(PointsTransaction::getExpireAt, now)
                            .apply("amount > consumed_amount")
                            .last("LIMIT " + BATCH_SIZE));

            if (expiredList.isEmpty()) {
                break;
            }

            for (PointsTransaction tx : expiredList) {
                int remaining = tx.getAmount() - tx.getConsumedAmount();
                if (remaining <= 0) continue;

                // 乐观锁更新 profile 积分余额
                MemberProfile profile = memberProfileMapper.selectOne(
                        new LambdaQueryWrapper<MemberProfile>()
                                .eq(MemberProfile::getUserId, tx.getUserId()));
                if (profile == null) continue;

                int rows = memberProfileMapper.update(null,
                        new LambdaUpdateWrapper<MemberProfile>()
                                .eq(MemberProfile::getId, profile.getId())
                                .eq(MemberProfile::getVersion, profile.getVersion())
                                .setSql("available_points = available_points - " + remaining)
                                .setSql("version = version + 1"));
                if (rows == 0) {
                    log.warn("Concurrent update failed on expiry for userId={}", tx.getUserId());
                    continue;
                }

                // 标记原记录已全部消耗
                tx.setConsumedAmount(tx.getAmount());
                pointsTransactionMapper.updateById(tx);

                // 插入 EXPIRE 流水
                MemberProfile updated = memberProfileMapper.selectById(profile.getId());
                PointsTransaction expireTx = new PointsTransaction();
                expireTx.setId(SnowflakeUtils.nextId());
                expireTx.setUserId(tx.getUserId());
                expireTx.setDirection("EXPIRE");
                expireTx.setAmount(remaining);
                expireTx.setBalanceAfter(updated.getAvailablePoints());
                expireTx.setSourceType("EXPIRE");
                expireTx.setSourceId("expire-" + tx.getId());
                expireTx.setBizKey("EXPIRE:" + tx.getId());
                expireTx.setRemark("积分过期: " + tx.getSourceType() + " " + tx.getSourceId());
                pointsTransactionMapper.insert(expireTx);

                totalExpired += remaining;
            }
        }

        log.info("Points expiry scan completed, total expired: {}", totalExpired);
    }
}
