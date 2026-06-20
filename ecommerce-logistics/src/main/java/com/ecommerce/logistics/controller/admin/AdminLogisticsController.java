package com.ecommerce.logistics.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ecommerce.common.result.Result;
import com.ecommerce.logistics.dto.request.CreateShippingRequest;
import com.ecommerce.logistics.dto.request.CreateShippingTemplateRequest;
import com.ecommerce.logistics.dto.response.LogisticsProviderVO;
import com.ecommerce.logistics.dto.response.ShippingOrderVO;
import com.ecommerce.logistics.dto.response.ShippingTemplateVO;
import com.ecommerce.logistics.dto.response.TrackingVO;
import com.ecommerce.logistics.service.LogisticsProviderService;
import com.ecommerce.logistics.service.ShippingService;
import com.ecommerce.logistics.service.ShippingTemplateService;
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
    private final ShippingTemplateService templateService;

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

    // ===== 运费模板管理 =====

    @GetMapping("/templates")
    public Result<IPage<ShippingTemplateVO>> listTemplates(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long merchantId) {
        return Result.ok(templateService.listTemplates(page, size, merchantId));
    }

    @GetMapping("/templates/{id}")
    public Result<ShippingTemplateVO> getTemplate(@PathVariable Long id) {
        return Result.ok(templateService.getTemplate(id));
    }

    @PostMapping("/templates")
    public Result<ShippingTemplateVO> createTemplate(@Valid @RequestBody CreateShippingTemplateRequest request) {
        return Result.ok(templateService.createTemplate(request));
    }

    @PutMapping("/templates/{id}")
    public Result<ShippingTemplateVO> updateTemplate(@PathVariable Long id, @Valid @RequestBody CreateShippingTemplateRequest request) {
        return Result.ok(templateService.updateTemplate(id, request));
    }

    @DeleteMapping("/templates/{id}")
    public Result<Void> deleteTemplate(@PathVariable Long id) {
        templateService.deleteTemplate(id);
        return Result.ok();
    }

    // ===== 发货管理 =====

    @PostMapping("/shipping")
    public Result<ShippingOrderVO> createShipping(@Valid @RequestBody CreateShippingRequest request,
                                                  @RequestHeader(value = "X-Merchant-Id", required = false) Long headerMerchantId) {
        return Result.ok(shippingService.createShipping(request, "admin", headerMerchantId));
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
    public Result<ShippingOrderVO> getShipping(@PathVariable Long id,
                                                @RequestHeader(value = "X-Merchant-Id", required = false) Long headerMerchantId) {
        return Result.ok(shippingService.getShipping(id, "admin", headerMerchantId));
    }

    // ===== 排障用轨迹查询 =====

    @GetMapping("/tracking/no/{trackingNo}")
    public Result<TrackingVO> getTrackingByNo(@PathVariable String trackingNo,
                                               @RequestParam String providerCode,
                                               @RequestHeader(value = "X-Merchant-Id", required = false) Long headerMerchantId) {
        return Result.ok(shippingService.getTrackingByTrackingNo(trackingNo, providerCode, headerMerchantId));
    }
}
