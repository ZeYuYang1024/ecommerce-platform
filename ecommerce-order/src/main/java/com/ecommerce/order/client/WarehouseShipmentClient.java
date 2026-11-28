package com.ecommerce.order.client;

import com.ecommerce.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

@FeignClient(name = "ecommerce-warehouse")
public interface WarehouseShipmentClient {
    @PutMapping("/api/v1/internal/warehouse/outbounds/shipping/{shippingId}/cancel-unshipped")
    Result<Boolean> cancelUnshipped(@PathVariable("shippingId") Long shippingId);
}
