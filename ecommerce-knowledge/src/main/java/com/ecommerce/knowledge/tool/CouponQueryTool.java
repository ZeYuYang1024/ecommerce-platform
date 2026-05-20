package com.ecommerce.knowledge.tool;

import com.ecommerce.knowledge.agent.AgentUserContextHolder;
import com.ecommerce.knowledge.client.CouponClient;
import com.ecommerce.knowledge.client.dto.CouponVO;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponQueryTool {

    private final CouponClient couponClient;

    @Tool("查询当前可领取或可浏览的优惠券列表，返回优惠券名称、门槛、优惠力度和有效期。")
    public List<CouponVO> queryAvailableCoupons() {
        try {
            var result = couponClient.listAvailable(1, 10);
            if (result != null && result.getData() != null) {
                return result.getData().getRecords();
            }
        } catch (Exception e) {
            log.warn("Failed to query available coupons", e);
        }
        return Collections.emptyList();
    }

    @Tool("查询当前登录用户已领取的优惠券，适用于回答我有哪些券、券是否可用。")
    public List<CouponVO> queryCurrentUserCoupons() {
        Long userId = currentUserId();
        if (userId == null) {
            return Collections.emptyList();
        }
        try {
            var result = couponClient.listMySummaries(userId);
            if (result != null && result.getData() != null) {
                return result.getData();
            }
        } catch (Exception e) {
            log.warn("Failed to query coupons for user {}", userId, e);
        }
        return Collections.emptyList();
    }

    @Tool("根据优惠券名称关键词过滤当前可用优惠券列表。")
    public List<CouponVO> searchCoupons(@P("优惠券名称关键词") String name) {
        try {
            var result = couponClient.listAvailable(1, 20);
            if (result != null && result.getData() != null) {
                return result.getData().getRecords().stream()
                        .filter(coupon -> coupon.getName() != null && coupon.getName().contains(name))
                        .toList();
            }
        } catch (Exception e) {
            log.warn("Failed to search coupons with name '{}'", name, e);
        }
        return Collections.emptyList();
    }

    private Long currentUserId() {
        var context = AgentUserContextHolder.get();
        return context != null ? context.userId() : null;
    }
}
