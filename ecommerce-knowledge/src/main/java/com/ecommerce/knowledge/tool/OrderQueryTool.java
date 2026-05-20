package com.ecommerce.knowledge.tool;

import com.ecommerce.knowledge.agent.AgentUserContextHolder;
import com.ecommerce.knowledge.client.OrderClient;
import com.ecommerce.knowledge.client.dto.OrderSummaryVO;
import com.ecommerce.knowledge.client.dto.OrderVO;
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
public class OrderQueryTool {

    private final OrderClient orderClient;

    @Tool("查询当前登录用户最近的订单列表，返回订单号、状态、金额和下单时间。")
    public List<OrderVO> queryCurrentUserOrders() {
        Long userId = currentUserId();
        if (userId == null) {
            return Collections.emptyList();
        }
        try {
            var result = orderClient.listByUser(userId, 1, 10);
            if (result != null && result.getData() != null) {
                return result.getData().getRecords();
            }
        } catch (Exception e) {
            log.warn("Failed to query orders for user {}", userId, e);
        }
        return Collections.emptyList();
    }

    @Tool("查询当前登录用户最近订单摘要，返回订单号、状态、金额、商品摘要和下单时间。")
    public List<OrderSummaryVO> queryCurrentUserOrderSummaries(@P("返回条数上限") int limit) {
        Long userId = currentUserId();
        if (userId == null) {
            return Collections.emptyList();
        }
        try {
            var result = orderClient.listSummaries(userId, limit);
            if (result != null && result.getData() != null) {
                return result.getData();
            }
        } catch (Exception e) {
            log.warn("Failed to query order summaries for user {}", userId, e);
        }
        return Collections.emptyList();
    }

    @Tool("根据订单号查询当前登录用户的订单详情，适用于查询订单状态、商品明细和金额。")
    public OrderVO queryOrderByNo(@P("订单号") String orderNo) {
        Long userId = currentUserId();
        if (userId == null) {
            return null;
        }
        try {
            var result = orderClient.getByOrderNo(userId, orderNo);
            if (result != null && result.getData() != null) {
                return result.getData();
            }
        } catch (Exception e) {
            log.warn("Failed to query order by no {}", orderNo, e);
        }
        return null;
    }

    private Long currentUserId() {
        var context = AgentUserContextHolder.get();
        return context != null ? context.userId() : null;
    }
}
