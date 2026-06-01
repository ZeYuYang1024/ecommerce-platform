package com.ecommerce.member.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.Result;
import com.ecommerce.member.dto.response.MemberLevelVO;
import com.ecommerce.member.dto.response.MemberProfileVO;
import com.ecommerce.member.entity.MemberLevel;
import com.ecommerce.member.entity.MemberProfile;
import com.ecommerce.member.mapper.MemberLevelMapper;
import com.ecommerce.member.mapper.MemberProfileMapper;
import com.ecommerce.member.mapper.PointsTransactionMapper;
import com.ecommerce.member.service.LevelService;
import com.ecommerce.member.service.PointsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberAdminControllerTest {

    @Mock
    private LevelService levelService;

    @Mock
    private PointsService pointsService;

    @Mock
    private MemberProfileMapper memberProfileMapper;

    @Mock
    private MemberLevelMapper memberLevelMapper;

    @Mock
    private PointsTransactionMapper pointsTransactionMapper;

    @InjectMocks
    private MemberAdminController controller;

    @Test
    void getProfileShouldReturnCompleteLevelBenefits() {
        MemberProfile profile = new MemberProfile();
        profile.setUserId(2001L);
        profile.setLevelId(3001L);
        profile.setGrowthValue(100L);
        profile.setTotalGrowthValue(100L);
        profile.setAvailablePoints(20L);
        profile.setTotalEarnedPoints(20L);
        profile.setTotalSpentPoints(0L);

        MemberLevel level = buildLevel();

        when(memberProfileMapper.selectOne(any())).thenReturn(profile);
        when(memberLevelMapper.selectById(3001L)).thenReturn(level);

        Result<MemberProfileVO> result = controller.getProfile(2001L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getLevel().getBirthdayGiftPoints()).isEqualTo(50);
        assertThat(result.getData().getLevel().getDiscountRate()).isEqualByComparingTo("0.98");
        assertThat(result.getData().getLevel().getFreeShipping()).isEqualTo(1);
        assertThat(result.getData().getLevel().getPrioritySupport()).isEqualTo(1);
        assertThat(result.getData().getLevel().getEarlyAccess()).isEqualTo(1);
        assertThat(result.getData().getLevel().getDescription()).isEqualTo("silver-desc");
    }

    @Test
    void listProfilesShouldReturnCompleteLevelBenefits() {
        MemberProfile profile = new MemberProfile();
        profile.setUserId(2002L);
        profile.setLevelId(3001L);
        profile.setGrowthValue(300L);
        profile.setTotalGrowthValue(300L);
        profile.setAvailablePoints(40L);
        profile.setTotalEarnedPoints(40L);
        profile.setTotalSpentPoints(0L);

        Page<MemberProfile> page = new Page<>(1, 20, 1);
        page.setRecords(List.of(profile));

        MemberLevel currentLevel = buildLevel();
        MemberLevel nextLevel = new MemberLevel();
        nextLevel.setGrowthThreshold(5000L);

        when(memberProfileMapper.selectPage(any(Page.class), any())).thenReturn(page);
        when(memberLevelMapper.selectById(3001L)).thenReturn(currentLevel);
        when(memberLevelMapper.selectList(any())).thenReturn(List.of(nextLevel));

        Result<IPage<MemberProfileVO>> result = controller.listProfiles(1, 20, null, null);

        assertThat(result.getCode()).isEqualTo(200);
        MemberLevelVO level = result.getData().getRecords().getFirst().getLevel();
        assertThat(level.getBirthdayGiftPoints()).isEqualTo(50);
        assertThat(level.getDiscountRate()).isEqualByComparingTo("0.98");
        assertThat(level.getFreeShipping()).isEqualTo(1);
        assertThat(level.getPrioritySupport()).isEqualTo(1);
        assertThat(level.getEarlyAccess()).isEqualTo(1);
    }

    private MemberLevel buildLevel() {
        MemberLevel level = new MemberLevel();
        level.setId(3001L);
        level.setName("Silver");
        level.setLevelCode("SILVER");
        level.setSortOrder(2);
        level.setGrowthThreshold(1000L);
        level.setPointsMultiplier(new BigDecimal("1.20"));
        level.setBirthdayGiftPoints(50);
        level.setDiscountRate(new BigDecimal("0.98"));
        level.setFreeShipping(1);
        level.setPrioritySupport(1);
        level.setEarlyAccess(1);
        level.setDescription("silver-desc");
        return level;
    }
}
