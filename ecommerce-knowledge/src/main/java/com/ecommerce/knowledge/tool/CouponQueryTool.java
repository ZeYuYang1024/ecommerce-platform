package com.ecommerce.knowledge.tool;

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
@RequiredArgsConstructor
public class CouponQueryTool {

    private final CouponClient couponClient;

    @Tool("查询当前可领取的优惠券列表，返回优惠券名称、类型、折扣力度、有效期、剩余数量等信息。")
    public List<CouponVO> queryAvailableCoupons() {
        try {
            var result = couponClient.listAvailable();
            if (result != null && result.getData() != null) {
                return result.getData();
            }
        } catch (Exception e) {
            log.warn("Failed to query available coupons", e);
        }
        return Collections.emptyList();
    }

    @Tool("根据优惠券名称搜索优惠券，返回匹配的优惠券详情和使用规则。")
    public List<CouponVO> searchCoupons(@P("优惠券名称关键词") String name) {
        try {
            var result = couponClient.listAvailable();
            if (result != null && result.getData() != null) {
                return result.getData().stream()
                        .filter(c -> c.getName() != null && c.getName().contains(name))
                        .toList();
            }
        } catch (Exception e) {
            log.warn("Failed to search coupons with name '{}'", name, e);
        }
        return Collections.emptyList();
    }
}
