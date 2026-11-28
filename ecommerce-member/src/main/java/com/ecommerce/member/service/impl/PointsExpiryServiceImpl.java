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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PointsExpiryServiceImpl {

    private final PointsTransactionMapper pointsTransactionMapper;
    private final MemberProfileMapper memberProfileMapper;
    private final PlatformTransactionManager transactions;

    private static final int BATCH_SIZE = 500;

    /**
     * 每日凌晨 2:00 扫描过期积分
     */
    @Scheduled(cron = "${member.points.expiry-cron:0 0 2 * * ?}")
    public void expirePoints() {
        var transaction = new TransactionTemplate(transactions);
        LocalDateTime now = LocalDateTime.now();
        long cursor = 0;
        while (true) {
            List<PointsTransaction> lots = pointsTransactionMapper.selectList(new LambdaQueryWrapper<PointsTransaction>()
                    .gt(PointsTransaction::getId, cursor)
                    .eq(PointsTransaction::getDirection, "EARN")
                    .lt(PointsTransaction::getExpireAt, now)
                    .apply("amount > consumed_amount")
                    .orderByAsc(PointsTransaction::getId).last("LIMIT " + BATCH_SIZE));
            if (lots.isEmpty()) return;
            for (PointsTransaction lot : lots) {
                try {
                    transaction.executeWithoutResult(status -> expireLot(lot.getId(), lot.getUserId()));
                } catch (RuntimeException failure) {
                    log.warn("Could not expire points lot {}", lot.getId(), failure);
                }
            }
            cursor = lots.getLast().getId();
        }
    }

    private void expireLot(Long id, Long userId) {
        MemberProfile profile = memberProfileMapper.selectOne(new LambdaQueryWrapper<MemberProfile>()
                .eq(MemberProfile::getUserId, userId).last("FOR UPDATE"));
        if (profile == null) return;
        PointsTransaction lot = pointsTransactionMapper.selectOne(new LambdaQueryWrapper<PointsTransaction>()
                .eq(PointsTransaction::getId, id).last("FOR UPDATE"));
        if (lot == null || lot.getExpireAt() == null || lot.getExpireAt().isAfter(LocalDateTime.now())) return;
        int remaining = lot.getAmount() - lot.getConsumedAmount();
        if (remaining <= 0) return;

        memberProfileMapper.update(null, new LambdaUpdateWrapper<MemberProfile>()
                .eq(MemberProfile::getId, profile.getId())
                .setSql("available_points = available_points - " + remaining)
                .setSql("version = version + 1"));
        lot.setConsumedAmount(lot.getAmount());
        pointsTransactionMapper.updateById(lot);

        PointsTransaction expired = new PointsTransaction();
        expired.setId(SnowflakeUtils.nextId());
        expired.setUserId(userId);
        expired.setDirection("EXPIRE");
        expired.setAmount(remaining);
        expired.setBalanceAfter(profile.getAvailablePoints() - remaining);
        expired.setSourceType("EXPIRE");
        expired.setSourceId("expire-" + lot.getId());
        expired.setBizKey("EXPIRE:" + lot.getId());
        expired.setRemark("积分过期");
        pointsTransactionMapper.insert(expired);
    }
}
