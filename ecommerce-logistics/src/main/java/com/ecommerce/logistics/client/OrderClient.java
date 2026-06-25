package com.ecommerce.logistics.client;

import com.ecommerce.common.dto.OrderInternalVO;
import com.ecommerce.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "ecommerce-order")
public interface OrderClient {

    @GetMapping("/api/v1/internal/orders/{id}/shipping-snapshot")
    Result<OrderInternalVO> getShippingSnapshot(@PathVariable("id") Long orderId);
}
