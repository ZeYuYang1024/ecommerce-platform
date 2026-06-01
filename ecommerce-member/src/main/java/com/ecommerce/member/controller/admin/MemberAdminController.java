package com.ecommerce.member.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.Result;
import com.ecommerce.member.dto.request.LevelUpdateRequest;
import com.ecommerce.member.dto.request.PointsGrantRequest;
import com.ecommerce.member.dto.response.MemberLevelVO;
import com.ecommerce.member.dto.response.MemberProfileVO;
import com.ecommerce.member.dto.response.PointsTransactionVO;
import com.ecommerce.member.entity.MemberLevel;
import com.ecommerce.member.entity.MemberProfile;
import com.ecommerce.member.entity.PointsTransaction;
import com.ecommerce.member.mapper.MemberLevelMapper;
import com.ecommerce.member.mapper.MemberProfileMapper;
import com.ecommerce.member.mapper.PointsTransactionMapper;
import com.ecommerce.member.service.LevelService;
import com.ecommerce.member.service.PointsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/member")
public class MemberAdminController {

    private final LevelService levelService;
    private final PointsService pointsService;
    private final MemberProfileMapper memberProfileMapper;
    private final MemberLevelMapper memberLevelMapper;
    private final PointsTransactionMapper pointsTransactionMapper;

    public MemberAdminController(LevelService levelService, PointsService pointsService,
                                 MemberProfileMapper memberProfileMapper,
                                 MemberLevelMapper memberLevelMapper,
                                 PointsTransactionMapper pointsTransactionMapper) {
        this.levelService = levelService;
        this.pointsService = pointsService;
        this.memberProfileMapper = memberProfileMapper;
        this.memberLevelMapper = memberLevelMapper;
        this.pointsTransactionMapper = pointsTransactionMapper;
    }

    @GetMapping("/levels")
    public Result<List<MemberLevelVO>> listLevels() {
        return Result.ok(levelService.getAllLevels());
    }

    @PutMapping("/levels/{id}")
    public Result<Void> updateLevel(@PathVariable Long id, @Valid @RequestBody LevelUpdateRequest request) {
        MemberLevelVO vo = new MemberLevelVO();
        vo.setName(request.getName());
        vo.setGrowthThreshold(request.getGrowthThreshold());
        vo.setPointsMultiplier(request.getPointsMultiplier());
        vo.setBirthdayGiftPoints(request.getBirthdayGiftPoints());
        vo.setDiscountRate(request.getDiscountRate());
        vo.setFreeShipping(request.getFreeShipping());
        vo.setPrioritySupport(request.getPrioritySupport());
        vo.setEarlyAccess(request.getEarlyAccess());
        vo.setIconUrl(request.getIconUrl());
        vo.setDescription(request.getDescription());
        levelService.updateLevel(id, vo);
        return Result.ok();
    }

    @GetMapping("/profiles")
    public Result<IPage<MemberProfileVO>> listProfiles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String levelCode,
            @RequestParam(required = false) String keyword) {

        Page<MemberProfile> p = new Page<>(page, size);
        LambdaQueryWrapper<MemberProfile> wrapper = new LambdaQueryWrapper<>();

        // 按等级筛选
        if (levelCode != null && !levelCode.isEmpty()) {
            MemberLevel level = memberLevelMapper.selectOne(
                    new LambdaQueryWrapper<MemberLevel>().eq(MemberLevel::getLevelCode, levelCode));
            if (level != null) {
                wrapper.eq(MemberProfile::getLevelId, level.getId());
            }
        }

        IPage<MemberProfile> result = memberProfileMapper.selectPage(p, wrapper);

        return Result.ok(result.convert(profile -> {
            MemberProfileVO vo = new MemberProfileVO();
            vo.setUserId(profile.getUserId());

            MemberLevel level = memberLevelMapper.selectById(profile.getLevelId());
            if (level != null) {
                vo.setLevel(toLevelVO(level));

                // 计算下一级门槛
                MemberLevel nextLevel = memberLevelMapper.selectList(
                        new LambdaQueryWrapper<MemberLevel>()
                                .gt(MemberLevel::getSortOrder, level.getSortOrder())
                                .orderByAsc(MemberLevel::getSortOrder)
                                .last("LIMIT 1"))
                        .stream().findFirst().orElse(null);
                vo.setNextLevelGrowth(nextLevel != null ? nextLevel.getGrowthThreshold() : null);
            }

            vo.setGrowthValue(profile.getGrowthValue());
            vo.setTotalGrowthValue(profile.getTotalGrowthValue());
            vo.setAvailablePoints(profile.getAvailablePoints());
            vo.setTotalEarnedPoints(profile.getTotalEarnedPoints());
            vo.setTotalSpentPoints(profile.getTotalSpentPoints());
            return vo;
        }));
    }

    @GetMapping("/profiles/{userId}")
    public Result<MemberProfileVO> getProfile(@PathVariable Long userId) {
        MemberProfile profile = memberProfileMapper.selectOne(
                new LambdaQueryWrapper<MemberProfile>().eq(MemberProfile::getUserId, userId));
        if (profile == null) {
            return Result.fail(404, "会员档案不存在");
        }

        MemberProfileVO vo = new MemberProfileVO();
        vo.setUserId(profile.getUserId());

        MemberLevel level = memberLevelMapper.selectById(profile.getLevelId());
        if (level != null) {
            vo.setLevel(toLevelVO(level));
        }

        vo.setGrowthValue(profile.getGrowthValue());
        vo.setTotalGrowthValue(profile.getTotalGrowthValue());
        vo.setAvailablePoints(profile.getAvailablePoints());
        vo.setTotalEarnedPoints(profile.getTotalEarnedPoints());
        vo.setTotalSpentPoints(profile.getTotalSpentPoints());
        return Result.ok(vo);
    }

    @GetMapping("/points/transactions")
    public Result<IPage<PointsTransactionVO>> listPointsTransactions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String sourceType) {

        Page<PointsTransaction> p = new Page<>(page, size);
        LambdaQueryWrapper<PointsTransaction> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(PointsTransaction::getUserId, userId);
        }
        if (sourceType != null && !sourceType.isEmpty()) {
            wrapper.eq(PointsTransaction::getSourceType, sourceType);
        }
        wrapper.orderByDesc(PointsTransaction::getCreatedAt);

        IPage<PointsTransaction> result = pointsTransactionMapper.selectPage(p, wrapper);
        return Result.ok(result.convert(tx -> {
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
        }));
    }

    @PostMapping("/points/grant")
    public Result<Void> grantPoints(@Valid @RequestBody PointsGrantRequest request) {
        pointsService.earn(request.getUserId(), request.getAmount(), "CAMPAIGN",
                request.getSourceId(),
                "CAMPAIGN:" + request.getSourceId() + ":EARN",
                request.getRemark() != null ? request.getRemark() : "后台发放积分");
        return Result.ok();
    }
    private MemberLevelVO toLevelVO(MemberLevel level) {
        MemberLevelVO levelVO = new MemberLevelVO();
        levelVO.setId(level.getId());
        levelVO.setName(level.getName());
        levelVO.setLevelCode(level.getLevelCode());
        levelVO.setSortOrder(level.getSortOrder());
        levelVO.setGrowthThreshold(level.getGrowthThreshold());
        levelVO.setPointsMultiplier(level.getPointsMultiplier());
        levelVO.setBirthdayGiftPoints(level.getBirthdayGiftPoints());
        levelVO.setDiscountRate(level.getDiscountRate());
        levelVO.setFreeShipping(level.getFreeShipping());
        levelVO.setPrioritySupport(level.getPrioritySupport());
        levelVO.setEarlyAccess(level.getEarlyAccess());
        levelVO.setIconUrl(level.getIconUrl());
        levelVO.setDescription(level.getDescription());
        return levelVO;
    }
}
