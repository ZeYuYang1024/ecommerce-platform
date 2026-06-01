package com.ecommerce.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.common.util.SnowflakeUtils;
import com.ecommerce.member.common.MemberErrorCode;
import com.ecommerce.member.dto.response.GrowthTransactionVO;
import com.ecommerce.member.entity.GrowthTransaction;
import com.ecommerce.member.entity.MemberLevel;
import com.ecommerce.member.entity.MemberProfile;
import com.ecommerce.member.mapper.MemberLevelMapper;
import com.ecommerce.member.mapper.MemberProfileMapper;
import com.ecommerce.member.mapper.GrowthTransactionMapper;
import com.ecommerce.member.service.GrowthService;
import com.ecommerce.member.service.LevelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GrowthServiceImpl implements GrowthService {

    private final GrowthTransactionMapper growthTransactionMapper;
    private final MemberProfileMapper memberProfileMapper;
    private final MemberLevelMapper memberLevelMapper;
    private final LevelService levelService;

    @Override
    @Transactional
    public void add(Long userId, Integer amount, String sourceType, String sourceId,
                    String bizKey, String remark) {
        if (amount <= 0) {
            return;
        }

        // 幂等检查
        Long exists = growthTransactionMapper.selectCount(
                new LambdaQueryWrapper<GrowthTransaction>()
                        .eq(GrowthTransaction::getBizKey, bizKey));
        if (exists > 0) {
            log.info("Duplicate growth bizKey ignored: {}", bizKey);
            return;
        }

        // 获取或创建档案
        MemberProfile profile = getOrCreateProfile(userId);

        // 乐观锁更新成长值
        int rows = memberProfileMapper.update(null,
                new LambdaUpdateWrapper<MemberProfile>()
                        .eq(MemberProfile::getId, profile.getId())
                        .eq(MemberProfile::getVersion, profile.getVersion())
                        .setSql("growth_value = growth_value + " + amount)
                        .setSql("total_growth_value = total_growth_value + " + amount)
                        .setSql("version = version + 1"));
        if (rows == 0) {
            throw new BusinessException(MemberErrorCode.CONCURRENT_UPDATE_FAILED);
        }

        // 获取更新后的余额
        MemberProfile updated = memberProfileMapper.selectById(profile.getId());

        // 插入成长值流水
        GrowthTransaction tx = new GrowthTransaction();
        tx.setId(SnowflakeUtils.nextId());
        tx.setUserId(userId);
        tx.setAmount(amount);
        tx.setBalanceAfter(updated.getGrowthValue());
        tx.setSourceType(sourceType);
        tx.setSourceId(sourceId);
        tx.setBizKey(bizKey);
        tx.setRemark(remark);
        growthTransactionMapper.insert(tx);

        log.info("Growth added: userId={}, amount={}, bizKey={}, balanceAfter={}",
                userId, amount, bizKey, tx.getBalanceAfter());

        // 检查升级
        levelService.checkUpgrade(userId, updated.getGrowthValue());
    }

    @Override
    public IPage<GrowthTransactionVO> getTransactions(Long userId, int page, int size) {
        Page<GrowthTransaction> p = new Page<>(page, size);
        LambdaQueryWrapper<GrowthTransaction> wrapper = new LambdaQueryWrapper<GrowthTransaction>()
                .eq(GrowthTransaction::getUserId, userId)
                .orderByDesc(GrowthTransaction::getCreatedAt);
        IPage<GrowthTransaction> result = growthTransactionMapper.selectPage(p, wrapper);

        return result.convert(tx -> {
            GrowthTransactionVO vo = new GrowthTransactionVO();
            vo.setId(tx.getId());
            vo.setAmount(tx.getAmount());
            vo.setBalanceAfter(tx.getBalanceAfter());
            vo.setSourceType(tx.getSourceType());
            vo.setSourceId(tx.getSourceId());
            vo.setRemark(tx.getRemark());
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

        MemberLevel defaultLevel = memberLevelMapper.selectOne(
                new LambdaQueryWrapper<MemberLevel>()
                        .eq(MemberLevel::getLevelCode, "REGULAR"));
        if (defaultLevel == null) {
            throw new BusinessException(MemberErrorCode.LEVEL_NOT_FOUND);
        }

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
}
