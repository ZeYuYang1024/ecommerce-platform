package com.ecommerce.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.member.common.MemberErrorCode;
import com.ecommerce.member.dto.response.MemberLevelVO;
import com.ecommerce.member.entity.MemberLevel;
import com.ecommerce.member.entity.MemberProfile;
import com.ecommerce.member.mapper.MemberLevelMapper;
import com.ecommerce.member.mapper.MemberProfileMapper;
import com.ecommerce.member.service.LevelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LevelServiceImpl implements LevelService {

    private final MemberLevelMapper memberLevelMapper;
    private final MemberProfileMapper memberProfileMapper;

    @Override
    @Transactional
    public void checkUpgrade(Long userId, Long currentGrowthValue) {
        // 查询所有等级按阈值降序排列，找到满足条件的最高等级
        List<MemberLevel> levels = memberLevelMapper.selectList(
                new LambdaQueryWrapper<MemberLevel>()
                        .orderByDesc(MemberLevel::getSortOrder));

        MemberLevel targetLevel = null;
        for (MemberLevel level : levels) {
            if (currentGrowthValue >= level.getGrowthThreshold()) {
                targetLevel = level;
                break;
            }
        }

        if (targetLevel == null) {
            return;
        }

        MemberProfile profile = memberProfileMapper.selectOne(
                new LambdaQueryWrapper<MemberProfile>()
                        .eq(MemberProfile::getUserId, userId));
        if (profile == null) {
            return;
        }

        if (!profile.getLevelId().equals(targetLevel.getId())) {
            MemberLevel oldLevel = memberLevelMapper.selectById(profile.getLevelId());
            profile.setLevelId(targetLevel.getId());
            memberProfileMapper.updateById(profile);

            log.info("Member level upgraded: userId={}, oldLevel={}, newLevel={}",
                    userId,
                    oldLevel != null ? oldLevel.getLevelCode() : "UNKNOWN",
                    targetLevel.getLevelCode());
        }
    }

    @Override
    public MemberLevel getCurrentLevel(Long userId) {
        MemberProfile profile = memberProfileMapper.selectOne(
                new LambdaQueryWrapper<MemberProfile>()
                        .eq(MemberProfile::getUserId, userId));
        if (profile == null) {
            return getDefaultLevel();
        }

        MemberLevel level = memberLevelMapper.selectById(profile.getLevelId());
        return level != null ? level : getDefaultLevel();
    }

    @Override
    public List<MemberLevelVO> getAllLevels() {
        List<MemberLevel> levels = memberLevelMapper.selectList(
                new LambdaQueryWrapper<MemberLevel>()
                        .orderByAsc(MemberLevel::getSortOrder));
        return levels.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public MemberLevel getDefaultLevel() {
        MemberLevel level = memberLevelMapper.selectOne(
                new LambdaQueryWrapper<MemberLevel>()
                        .eq(MemberLevel::getLevelCode, "REGULAR"));
        if (level == null) {
            throw new BusinessException(MemberErrorCode.LEVEL_NOT_FOUND);
        }
        return level;
    }

    @Override
    public void updateLevel(Long levelId, MemberLevelVO vo) {
        MemberLevel level = memberLevelMapper.selectById(levelId);
        if (level == null) {
            throw new BusinessException(MemberErrorCode.LEVEL_NOT_FOUND);
        }
        level.setName(vo.getName());
        level.setGrowthThreshold(vo.getGrowthThreshold());
        level.setPointsMultiplier(vo.getPointsMultiplier());
        level.setBirthdayGiftPoints(vo.getBirthdayGiftPoints());
        level.setDiscountRate(vo.getDiscountRate());
        level.setFreeShipping(vo.getFreeShipping());
        level.setPrioritySupport(vo.getPrioritySupport());
        level.setEarlyAccess(vo.getEarlyAccess());
        level.setIconUrl(vo.getIconUrl());
        level.setDescription(vo.getDescription());
        memberLevelMapper.updateById(level);
        log.info("Member level updated: id={}, name={}", levelId, level.getName());
    }

    private MemberLevelVO toVO(MemberLevel level) {
        MemberLevelVO vo = new MemberLevelVO();
        vo.setId(level.getId());
        vo.setName(level.getName());
        vo.setLevelCode(level.getLevelCode());
        vo.setSortOrder(level.getSortOrder());
        vo.setGrowthThreshold(level.getGrowthThreshold());
        vo.setPointsMultiplier(level.getPointsMultiplier());
        vo.setBirthdayGiftPoints(level.getBirthdayGiftPoints());
        vo.setDiscountRate(level.getDiscountRate());
        vo.setFreeShipping(level.getFreeShipping());
        vo.setPrioritySupport(level.getPrioritySupport());
        vo.setEarlyAccess(level.getEarlyAccess());
        vo.setIconUrl(level.getIconUrl());
        vo.setDescription(level.getDescription());
        return vo;
    }
}
