package com.ecommerce.member.common;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.common.util.SnowflakeUtils;
import com.ecommerce.member.entity.MemberLevel;
import com.ecommerce.member.mapper.MemberLevelMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemberDataInitializer {

    private final MemberLevelMapper memberLevelMapper;

    @EventListener(ApplicationReadyEvent.class)
    public void initLevels() {
        Long count = memberLevelMapper.selectCount(new LambdaQueryWrapper<>());
        if (count > 0) {
            log.info("Member levels already initialized, count={}", count);
            return;
        }

        log.info("Initializing default member levels...");

        insertLevel("普通会员", "REGULAR", 1, 0L,
                new BigDecimal("1.00"), 0, new BigDecimal("1.00"), 0, 0, 0, "累计 0 成长值");

        insertLevel("银卡会员", "SILVER", 2, 1000L,
                new BigDecimal("1.20"), 50, new BigDecimal("0.98"), 0, 0, 0, "累计 1000 成长值升级");

        insertLevel("金卡会员", "GOLD", 3, 5000L,
                new BigDecimal("1.50"), 100, new BigDecimal("0.95"), 1, 1, 0, "累计 5000 成长值升级");

        insertLevel("钻石会员", "DIAMOND", 4, 20000L,
                new BigDecimal("2.00"), 200, new BigDecimal("0.90"), 1, 1, 1, "累计 20000 成长值升级");

        log.info("Default member levels initialized");
    }

    private void insertLevel(String name, String code, int sort, long threshold,
                             BigDecimal multiplier, int birthdayGift, BigDecimal discount,
                             int freeShipping, int prioritySupport, int earlyAccess, String desc) {
        MemberLevel level = new MemberLevel();
        level.setId(SnowflakeUtils.nextId());
        level.setName(name);
        level.setLevelCode(code);
        level.setSortOrder(sort);
        level.setGrowthThreshold(threshold);
        level.setPointsMultiplier(multiplier);
        level.setBirthdayGiftPoints(birthdayGift);
        level.setDiscountRate(discount);
        level.setFreeShipping(freeShipping);
        level.setPrioritySupport(prioritySupport);
        level.setEarlyAccess(earlyAccess);
        level.setDescription(desc);
        memberLevelMapper.insert(level);
    }
}
