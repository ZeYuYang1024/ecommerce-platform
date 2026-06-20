package com.ecommerce.logistics.controller.internal;

import com.ecommerce.common.result.Result;
import com.ecommerce.logistics.dto.request.ShippingFeeRequest;
import com.ecommerce.logistics.dto.response.FulfillmentSummaryVO;
import com.ecommerce.logistics.dto.response.ShippingFeeResponse;
import com.ecommerce.logistics.dto.response.ShippingOrderVO;
import com.ecommerce.logistics.service.ShippingService;
import com.ecommerce.logistics.service.ShippingTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/internal/logistics")
@RequiredArgsConstructor
public class InternalLogisticsController {

    private final ShippingService shippingService;
    private final ShippingTemplateService shippingTemplateService;

    @GetMapping("/shipping/by-order/{orderId}")
    public Result<ShippingOrderVO> getByOrderId(@PathVariable Long orderId) {
        return Result.ok(shippingService.getShippingByOrderId(orderId));
    }

    @PostMapping("/fulfillment-summary")
    public Result<List<FulfillmentSummaryVO>> getFulfillmentSummary(@RequestBody List<Long> orderIds) {
        return Result.ok(shippingService.getFulfillmentSummary(orderIds));
    }

    @PostMapping("/shipping-fee")
    public Result<ShippingFeeResponse> calculateFee(@RequestBody ShippingFeeRequest request) {
        BigDecimal fee = shippingTemplateService.calculateFee(
                request.getTemplateId(), request.getQuantity(),
                request.getWeight(), request.getVolume(), request.getProvinceCode());
        ShippingFeeResponse resp = new ShippingFeeResponse();
        resp.setShippingFee(fee);
        return Result.ok(resp);
    }
}
