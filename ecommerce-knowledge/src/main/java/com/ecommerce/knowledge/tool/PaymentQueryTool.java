package com.ecommerce.knowledge.tool;

import com.ecommerce.knowledge.agent.AgentUserContextHolder;
import com.ecommerce.knowledge.client.PaymentClient;
import com.ecommerce.knowledge.client.dto.PaymentVO;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentQueryTool {

    private final PaymentClient paymentClient;

    @Tool("根据订单号查询当前登录用户的支付状态和支付信息。")
    public PaymentVO queryCurrentUserPaymentByOrderNo(@P("订单号") String orderNo) {
        Long userId = currentUserId();
        if (userId == null) {
            return null;
        }
        try {
            var result = paymentClient.getPaymentByOrderNo(userId, orderNo);
            if (result != null) {
                return result.getData();
            }
        } catch (Exception e) {
            log.warn("Failed to query payment for user {} and order {}", userId, orderNo, e);
        }
        return null;
    }

    private Long currentUserId() {
        var context = AgentUserContextHolder.get();
        return context != null ? context.userId() : null;
    }
}
