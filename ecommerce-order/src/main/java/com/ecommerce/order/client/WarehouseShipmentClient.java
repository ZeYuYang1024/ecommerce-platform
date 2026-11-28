package com.ecommerce.order.client;

import com.ecommerce.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "ecommerce-warehouse")
public interface WarehouseShipmentClient {
    @GetMapping("/api/v1/internal/warehouse/outbounds/shipping/{shippingId}/exists")
    Result<Boolean> hasOutbound(@PathVariable("shippingId") Long shippingId);
}
