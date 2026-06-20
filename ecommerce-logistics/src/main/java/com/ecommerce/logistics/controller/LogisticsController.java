package com.ecommerce.logistics.controller;

import com.ecommerce.common.result.Result;
import com.ecommerce.logistics.dto.response.TrackingVO;
import com.ecommerce.logistics.service.ShippingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/logistics")
@RequiredArgsConstructor
public class LogisticsController {

    private final ShippingService shippingService;

    @GetMapping("/tracking/shipping/{shippingId}")
    public Result<TrackingVO> getTracking(@PathVariable Long shippingId) {
        return Result.ok(shippingService.getTracking(shippingId));
    }
}
