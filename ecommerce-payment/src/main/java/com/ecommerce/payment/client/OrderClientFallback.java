package com.ecommerce.payment.client;

import com.ecommerce.common.result.Result;
import com.ecommerce.payment.dto.request.StatusRequest;
import com.ecommerce.common.dto.ReconOrderVO;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OrderClientFallback implements FallbackFactory<OrderClient> {
    @Override
    public OrderClient create(Throwable cause) {
        return new OrderClient() {
            @Override
            public Result<Void> updateStatus(Long id, StatusRequest request) {
                return Result.ok();
            }
            @Override
            public Result<Map<String, Object>> getOrderByOrderNo(String orderNo, Long userId) {
                return Result.fail(500, "order service unavailable");
            }
            @Override
            public Result<List<ReconOrderVO>> getOrdersForRecon(String start, String end) {
                return Result.ok(Collections.emptyList());
            }
        };
    }
}
