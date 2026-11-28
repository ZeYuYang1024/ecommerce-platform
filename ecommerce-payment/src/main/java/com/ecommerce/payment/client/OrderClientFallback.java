package com.ecommerce.payment.client;

import com.ecommerce.common.dto.OrderInternalVO;
import com.ecommerce.common.dto.ManualRefundCommand;
import com.ecommerce.common.result.Result;
import com.ecommerce.payment.dto.request.StatusRequest;
import com.ecommerce.common.dto.ReconOrderVO;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

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
            public Result<OrderInternalVO> getOrderByOrderNo(String orderNo, Long userId) {
                return Result.fail(500, "order service unavailable");
            }
            @Override
            public Result<List<String>> listOrderNosByMerchant(Long merchantId) {
                return Result.ok(Collections.emptyList());
            }
            @Override
            public Result<Boolean> merchantOwnsEntireOrder(Long merchantId, String orderNo) {
                return Result.fail(503, "order service unavailable");
            }
            @Override
            public Result<Void> requestManualRefund(String orderNo, ManualRefundCommand command) {
                return Result.fail(503, "订单服务暂不可用，请重试");
            }
            @Override
            public Result<List<ReconOrderVO>> getOrdersByNumberForRecon(List<String> orderNumbers) {
                return Result.fail(503, "order service unavailable");
            }
            @Override
            public Result<List<ReconOrderVO>> getOrdersForRecon(String start, String end) {
                return Result.fail(503, "order service unavailable");
            }
        };
    }
}
