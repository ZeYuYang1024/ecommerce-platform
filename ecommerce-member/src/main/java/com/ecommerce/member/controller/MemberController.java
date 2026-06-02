package com.ecommerce.member.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ecommerce.common.result.Result;
import com.ecommerce.member.dto.response.CheckInStatusVO;
import com.ecommerce.member.dto.response.GrowthTransactionVO;
import com.ecommerce.member.dto.response.MemberLevelVO;
import com.ecommerce.member.dto.response.MemberProfileVO;
import com.ecommerce.member.dto.response.PointsTransactionVO;
import com.ecommerce.member.entity.MemberLevel;
import com.ecommerce.member.entity.MemberProfile;
import com.ecommerce.member.mapper.MemberLevelMapper;
import com.ecommerce.member.mapper.MemberProfileMapper;
import com.ecommerce.member.service.CheckInService;
import com.ecommerce.member.service.GrowthService;
import com.ecommerce.member.service.LevelService;
import com.ecommerce.member.service.PointsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/member")
public class MemberController {

    private final PointsService pointsService;
    private final GrowthService growthService;
    private final LevelService levelService;
    private final CheckInService checkInService;
    private final MemberProfileMapper memberProfileMapper;
    private final MemberLevelMapper memberLevelMapper;

    public MemberController(PointsService pointsService, GrowthService growthService,
                           LevelService levelService, CheckInService checkInService,
                           MemberProfileMapper memberProfileMapper, MemberLevelMapper memberLevelMapper) {
        this.pointsService = pointsService;
        this.growthService = growthService;
        this.levelService = levelService;
        this.checkInService = checkInService;
        this.memberProfileMapper = memberProfileMapper;
        this.memberLevelMapper = memberLevelMapper;
    }

    @GetMapping("/profile")
    public Result<MemberProfileVO> profile(@RequestHeader("X-User-Id") Long userId) {
        MemberProfile profile = memberProfileMapper.selectOne(
                new LambdaQueryWrapper<MemberProfile>().eq(MemberProfile::getUserId, userId));

        MemberProfileVO vo = new MemberProfileVO();
        vo.setUserId(userId);

        if (profile == null) {
            // 返回默认 REGULAR 视图
            MemberLevel defaultLevel = levelService.getDefaultLevel();
            vo.setLevel(toLevelVO(defaultLevel));
            vo.setGrowthValue(0L);
            vo.setTotalGrowthValue(0L);
            vo.setAvailablePoints(0L);
            vo.setTotalEarnedPoints(0L);
            vo.setTotalSpentPoints(0L);
            vo.setNextLevelGrowth(getNextLevelThreshold(defaultLevel));
            return Result.ok(vo);
        }

        MemberLevel currentLevel = memberLevelMapper.selectById(profile.getLevelId());
        if (currentLevel == null) {
            currentLevel = levelService.getDefaultLevel();
        }

        vo.setLevel(toLevelVO(currentLevel));
        vo.setGrowthValue(profile.getGrowthValue());
        vo.setTotalGrowthValue(profile.getTotalGrowthValue());
        vo.setAvailablePoints(profile.getAvailablePoints());
        vo.setTotalEarnedPoints(profile.getTotalEarnedPoints());
        vo.setTotalSpentPoints(profile.getTotalSpentPoints());
        vo.setNextLevelGrowth(getNextLevelThreshold(currentLevel));
        return Result.ok(vo);
    }

    @GetMapping("/points/transactions")
    public Result<IPage<PointsTransactionVO>> pointsTransactions(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(pointsService.getTransactions(userId, page, size));
    }

    @GetMapping("/growth/transactions")
    public Result<IPage<GrowthTransactionVO>> growthTransactions(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(growthService.getTransactions(userId, page, size));
    }

    @PostMapping("/check-in")
    public Result<CheckInStatusVO> checkIn(@RequestHeader("X-User-Id") Long userId) {
        return Result.ok(checkInService.checkIn(userId));
    }

    @GetMapping("/check-in/status")
    public Result<CheckInStatusVO> checkInStatus(@RequestHeader("X-User-Id") Long userId) {
        return Result.ok(checkInService.getStatus(userId));
    }

    private MemberLevelVO toLevelVO(MemberLevel level) {
        if (level == null) return null;
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

    private Long getNextLevelThreshold(MemberLevel currentLevel) {
        MemberLevel nextLevel = memberLevelMapper.selectList(
                new LambdaQueryWrapper<MemberLevel>()
                        .gt(MemberLevel::getSortOrder, currentLevel.getSortOrder())
                        .orderByAsc(MemberLevel::getSortOrder)
                        .last("LIMIT 1"))
                .stream().findFirst().orElse(null);
        return nextLevel != null ? nextLevel.getGrowthThreshold() : null;
    }
}
