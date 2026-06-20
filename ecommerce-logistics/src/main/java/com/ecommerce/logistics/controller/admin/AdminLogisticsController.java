package com.ecommerce.logistics.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ecommerce.common.result.Result;
import com.ecommerce.logistics.dto.request.CreateShippingRequest;
import com.ecommerce.logistics.dto.response.LogisticsProviderVO;
import com.ecommerce.logistics.dto.response.ShippingOrderVO;
import com.ecommerce.logistics.dto.response.TrackingVO;
import com.ecommerce.logistics.service.LogisticsProviderService;
import com.ecommerce.logistics.service.ShippingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/logistics")
@RequiredArgsConstructor
public class AdminLogisticsController {

    private final LogisticsProviderService providerService;
    private final ShippingService shippingService;

    // ===== 物流公司管理 =====

    @GetMapping("/providers")
    public Result<IPage<LogisticsProviderVO>> listProviders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(providerService.listProviders(page, size));
    }

    @GetMapping("/providers/all")
    public Result<List<LogisticsProviderVO>> listAllEnabled() {
        return Result.ok(providerService.listAllEnabled());
    }

    @GetMapping("/providers/{id}")
    public Result<LogisticsProviderVO> getProvider(@PathVariable Long id) {
        return Result.ok(providerService.getProvider(id));
    }

    @PostMapping("/providers")
    public Result<LogisticsProviderVO> createProvider(@RequestBody LogisticsProviderVO vo) {
        return Result.ok(providerService.createProvider(vo));
    }

    @PutMapping("/providers/{id}")
    public Result<LogisticsProviderVO> updateProvider(@PathVariable Long id, @RequestBody LogisticsProviderVO vo) {
        return Result.ok(providerService.updateProvider(id, vo));
    }

    @DeleteMapping("/providers/{id}")
    public Result<Void> deleteProvider(@PathVariable Long id) {
        providerService.deleteProvider(id);
        return Result.ok();
    }

    @PutMapping("/providers/{id}/status")
    public Result<Void> toggleStatus(@PathVariable Long id, @RequestParam Integer status) {
        providerService.toggleStatus(id, status);
        return Result.ok();
    }

    // ===== 发货管理 =====

    @PostMapping("/shipping")
    public Result<ShippingOrderVO> createShipping(@Valid @RequestBody CreateShippingRequest request) {
        return Result.ok(shippingService.createShipping(request, "admin", null));
    }

    @GetMapping("/shipping")
    public Result<IPage<ShippingOrderVO>> listShipping(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Integer shippingStatus,
            @RequestParam(required = false) Long merchantId) {
        return Result.ok(shippingService.listShipping(page, size, orderNo, shippingStatus, merchantId));
    }

    @GetMapping("/shipping/{id}")
    public Result<ShippingOrderVO> getShipping(@PathVariable Long id) {
        return Result.ok(shippingService.getShipping(id, "admin", null));
    }

    // ===== 排障用轨迹查询 =====

    @GetMapping("/tracking/no/{trackingNo}")
    public Result<TrackingVO> getTrackingByNo(@PathVariable String trackingNo, @RequestParam String providerCode) {
        return Result.ok(shippingService.getTrackingByTrackingNo(trackingNo, providerCode));
    }
}
