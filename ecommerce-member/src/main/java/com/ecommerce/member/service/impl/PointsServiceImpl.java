package com.ecommerce.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.common.util.SnowflakeUtils;
import com.ecommerce.member.common.MemberErrorCode;
import com.ecommerce.member.dto.response.PointsTransactionVO;
import com.ecommerce.member.entity.MemberLevel;
import com.ecommerce.member.entity.MemberProfile;
import com.ecommerce.member.entity.PointsTransaction;
import com.ecommerce.member.mapper.MemberLevelMapper;
import com.ecommerce.member.mapper.MemberProfileMapper;
import com.ecommerce.member.mapper.PointsTransactionMapper;
import com.ecommerce.member.service.PointsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointsServiceImpl implements PointsService {

    private static final int POINTS_EXPIRE_MONTHS = 12;

    private final PointsTransactionMapper pointsTransactionMapper;
    private final MemberProfileMapper memberProfileMapper;
    private final MemberLevelMapper memberLevelMapper;

    @Override
    @Transactional
    public void earn(Long userId, Integer amount, String sourceType, String sourceId,
                     String bizKey, String remark) {
        earn(userId, amount, sourceType, sourceId, bizKey, remark, null, null);
    }

    @Override
    @Transactional
    public void earn(Long userId, Integer amount, String sourceType, String sourceId,
                     String bizKey, String remark, String relatedReservationNo, Long reversalOfTxId) {
        if (amount <= 0) {
            throw new BusinessException(MemberErrorCode.INVALID_POINTS_AMOUNT);
        }

        Long exists = pointsTransactionMapper.selectCount(
                new LambdaQueryWrapper<PointsTransaction>()
                        .eq(PointsTransaction::getBizKey, bizKey));
        if (exists > 0) {
            log.info("Duplicate bizKey ignored: {}", bizKey);
            return;
        }

        MemberProfile profile = getOrCreateProfile(userId);

        MemberLevel level = memberLevelMapper.selectById(profile.getLevelId());
        if (level == null) {
            level = getDefaultLevel();
        }

        int finalAmount = (int) Math.floor(amount * level.getPointsMultiplier().doubleValue());

        PointsTransaction tx = new PointsTransaction();
        tx.setId(SnowflakeUtils.nextId());
        tx.setUserId(userId);
        tx.setDirection("EARN");
        tx.setAmount(finalAmount);
        tx.setSourceType(sourceType);
        tx.setSourceId(sourceId);
        tx.setBizKey(bizKey);
        tx.setConsumedAmount(0);
        tx.setExpireAt(LocalDateTime.now().plusMonths(POINTS_EXPIRE_MONTHS));
        tx.setRemark(remark);
        tx.setRelatedReservationNo(relatedReservationNo);
        tx.setReversalOfTxId(reversalOfTxId);

        int rows = memberProfileMapper.update(null,
                new LambdaUpdateWrapper<MemberProfile>()
                        .eq(MemberProfile::getId, profile.getId())
                        .eq(MemberProfile::getVersion, profile.getVersion())
                        .setSql("available_points = available_points + " + finalAmount)
                        .setSql("total_earned_points = total_earned_points + " + finalAmount)
                        .setSql("version = version + 1"));
        if (rows == 0) {
            throw new BusinessException(MemberErrorCode.CONCURRENT_UPDATE_FAILED);
        }

        MemberProfile updated = memberProfileMapper.selectById(profile.getId());
        tx.setBalanceAfter(updated.getAvailablePoints());
        pointsTransactionMapper.insert(tx);

        log.info("Points earned: userId={}, amount={}, finalAmount={}, bizKey={}, balanceAfter={}",
                userId, amount, finalAmount, bizKey, tx.getBalanceAfter());
    }

    @Override
    public void spend(Long userId, Integer amount, String sourceType, String sourceId, String bizKey,
                      String remark, String relatedReservationNo) {
        throw new UnsupportedOperationException("Points spend flow is implemented in later Phase 2A tasks");
    }

    @Override
    public void reverseSpend(Long userId, Integer amount, String sourceType, String sourceId, String bizKey,
                             String remark, String relatedReservationNo, Long reversalOfTxId) {
        throw new UnsupportedOperationException("Points reverse spend flow is implemented in later Phase 2A tasks");
    }

    @Override
    public IPage<PointsTransactionVO> getTransactions(Long userId, int page, int size) {
        Page<PointsTransaction> pageRequest = new Page<>(page, size);
        LambdaQueryWrapper<PointsTransaction> wrapper = new LambdaQueryWrapper<PointsTransaction>()
                .eq(PointsTransaction::getUserId, userId)
                .orderByDesc(PointsTransaction::getCreatedAt);
        IPage<PointsTransaction> result = pointsTransactionMapper.selectPage(pageRequest, wrapper);

        return result.convert(tx -> {
            PointsTransactionVO vo = new PointsTransactionVO();
            vo.setId(tx.getId());
            vo.setDirection(tx.getDirection());
            vo.setAmount(tx.getAmount());
            vo.setBalanceAfter(tx.getBalanceAfter());
            vo.setSourceType(tx.getSourceType());
            vo.setSourceId(tx.getSourceId());
            vo.setRemark(tx.getRemark());
            vo.setExpireAt(tx.getExpireAt());
            vo.setCreatedAt(tx.getCreatedAt());
            return vo;
        });
    }

    private MemberProfile getOrCreateProfile(Long userId) {
        MemberProfile profile = memberProfileMapper.selectOne(
                new LambdaQueryWrapper<MemberProfile>()
                        .eq(MemberProfile::getUserId, userId));
        if (profile != null) {
            return profile;
        }

        MemberLevel defaultLevel = getDefaultLevel();

        profile = new MemberProfile();
        profile.setId(SnowflakeUtils.nextId());
        profile.setUserId(userId);
        profile.setLevelId(defaultLevel.getId());
        profile.setGrowthValue(0L);
        profile.setTotalGrowthValue(0L);
        profile.setAvailablePoints(0L);
        profile.setTotalEarnedPoints(0L);
        profile.setTotalSpentPoints(0L);
        profile.setVersion(0L);
        memberProfileMapper.insert(profile);

        log.info("Member profile created: userId={}, level={}", userId, defaultLevel.getLevelCode());
        return profile;
    }

    private MemberLevel getDefaultLevel() {
        MemberLevel level = memberLevelMapper.selectOne(
                new LambdaQueryWrapper<MemberLevel>()
                        .eq(MemberLevel::getLevelCode, "REGULAR"));
        if (level == null) {
            throw new BusinessException(MemberErrorCode.LEVEL_NOT_FOUND);
        }
        return level;
    }
}
