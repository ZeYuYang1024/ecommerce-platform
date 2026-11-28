package com.ecommerce.logistics.client;

import com.ecommerce.common.dto.OrderInternalVO;
import com.ecommerce.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "ecommerce-order")
public interface OrderClient {

    @GetMapping("/api/v1/internal/orders/{id}/shipping-snapshot")
    Result<OrderInternalVO> getShippingSnapshot(@PathVariable("id") Long orderId);

    @PutMapping("/api/v1/internal/orders/{id}/shipping-claim")
    Result<Void> claimShipping(@PathVariable("id") Long orderId, @RequestParam("shippingNo") String shippingNo,
                               @RequestParam("shippingId") Long shippingId, @RequestParam("managed") boolean managed);

    @PutMapping("/api/v1/internal/orders/{id}/shipping-claim/confirm")
    Result<Void> confirmShipping(@PathVariable("id") Long orderId, @RequestParam("shippingNo") String shippingNo);

    @DeleteMapping("/api/v1/internal/orders/{id}/shipping-claim")
    Result<Void> releaseShipping(@PathVariable("id") Long orderId, @RequestParam("shippingNo") String shippingNo);
}
