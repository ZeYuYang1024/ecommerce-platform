package com.ecommerce.knowledge.tool;

import com.ecommerce.knowledge.client.OrderClient;
import com.ecommerce.knowledge.client.dto.OrderVO;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class OrderQueryTool {

    private final OrderClient orderClient;

    @Tool("查询用户的订单列表，返回订单号、状态、金额等摘要信息。需要提供用户ID。")
    public List<OrderVO> queryUserOrders(@P("用户ID") Long userId) {
        try {
            var result = orderClient.listByUserId(userId, 1, 10);
            if (result != null && result.getData() != null) {
                return result.getData();
            }
        } catch (Exception e) {
            log.warn("Failed to query orders for user {}", userId, e);
        }
        return Collections.emptyList();
    }

    @Tool("根据订单号查询单个订单的详细信息。需要提供订单号。")
    public OrderVO queryOrderByNo(@P("订单号") String orderNo) {
        try {
            var result = orderClient.getByOrderNo(orderNo);
            if (result != null && result.getData() != null) {
                return result.getData();
            }
        } catch (Exception e) {
            log.warn("Failed to query order by no {}", orderNo, e);
        }
        return null;
    }
}
