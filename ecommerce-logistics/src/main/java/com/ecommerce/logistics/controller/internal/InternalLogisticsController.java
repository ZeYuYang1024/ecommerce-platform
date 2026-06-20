package com.ecommerce.logistics.controller.internal;

import com.ecommerce.common.result.Result;
import com.ecommerce.logistics.dto.response.FulfillmentSummaryVO;
import com.ecommerce.logistics.dto.response.ShippingOrderVO;
import com.ecommerce.logistics.service.ShippingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/internal/logistics")
@RequiredArgsConstructor
public class InternalLogisticsController {

    private final ShippingService shippingService;

    @GetMapping("/shipping/by-order/{orderId}")
    public Result<ShippingOrderVO> getByOrderId(@PathVariable Long orderId) {
        return Result.ok(shippingService.getShippingByOrderId(orderId));
    }

    @PostMapping("/fulfillment-summary")
    public Result<List<FulfillmentSummaryVO>> getFulfillmentSummary(@RequestBody List<Long> orderIds) {
        return Result.ok(shippingService.getFulfillmentSummary(orderIds));
    }
}
