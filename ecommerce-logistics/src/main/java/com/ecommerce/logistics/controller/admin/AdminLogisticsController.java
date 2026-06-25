package com.ecommerce.logistics.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ecommerce.common.result.Result;
import com.ecommerce.common.tenant.MerchantTenantSupport;
import com.ecommerce.logistics.common.LogisticsErrorCode;
import com.ecommerce.logistics.dto.request.BatchShipRequest;
import com.ecommerce.logistics.dto.request.CreateShippingRequest;
import com.ecommerce.logistics.dto.request.CreateShippingTemplateRequest;
import com.ecommerce.logistics.dto.request.UpdateShippingTemplateRequest;
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
    public Result<LogisticsProviderVO> createProvider(@Valid @RequestBody LogisticsProviderVO vo) {
        return Result.ok(providerService.createProvider(vo));
    }

    @PutMapping("/providers/{id}")
    public Result<LogisticsProviderVO> updateProvider(@PathVariable Long id, @Valid @RequestBody LogisticsProviderVO vo) {
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
            @RequestParam(required = false) Long merchantId,
            @RequestHeader(value = "X-User-Type", defaultValue = "super_admin") String userType,
            @RequestHeader(value = "X-Merchant-Id", required = false) Long headerMerchantId) {
        Long scopedMerchantId = MerchantTenantSupport.resolveScopedMerchantId(
                userType, headerMerchantId, merchantId, LogisticsErrorCode.TEMPLATE_FORBIDDEN);
        return Result.ok(templateService.listTemplates(page, size, scopedMerchantId));
    }

    @GetMapping("/templates/{id}")
    public Result<ShippingTemplateVO> getTemplate(@PathVariable Long id,
                                                  @RequestHeader(value = "X-User-Type", defaultValue = "super_admin") String userType,
                                                  @RequestHeader(value = "X-Merchant-Id", required = false) Long headerMerchantId) {
        return Result.ok(templateService.getTemplate(
                id, MerchantTenantSupport.resolveRequestMerchantId(
                        userType, headerMerchantId, LogisticsErrorCode.TEMPLATE_FORBIDDEN)));
    }

    @PostMapping("/templates")
    public Result<ShippingTemplateVO> createTemplate(@Valid @RequestBody CreateShippingTemplateRequest request,
                                                     @RequestHeader(value = "X-User-Type", defaultValue = "super_admin") String userType,
                                                     @RequestHeader(value = "X-Merchant-Id", required = false) Long headerMerchantId) {
        Long scopedMerchantId = MerchantTenantSupport.resolveRequestMerchantId(
                userType, headerMerchantId, LogisticsErrorCode.TEMPLATE_FORBIDDEN);
        if (scopedMerchantId != null) {
            request.setMerchantId(scopedMerchantId);
        }
        return Result.ok(templateService.createTemplate(request));
    }

    @PutMapping("/templates/{id}")
    public Result<ShippingTemplateVO> updateTemplate(@PathVariable Long id,
                                                     @RequestBody UpdateShippingTemplateRequest request,
                                                     @RequestHeader(value = "X-User-Type", defaultValue = "super_admin") String userType,
                                                     @RequestHeader(value = "X-Merchant-Id", required = false) Long headerMerchantId) {
        Long scopedMerchantId = MerchantTenantSupport.resolveRequestMerchantId(
                userType, headerMerchantId, LogisticsErrorCode.TEMPLATE_FORBIDDEN);
        if (scopedMerchantId != null) {
            request.setMerchantId(scopedMerchantId);
        }
        return Result.ok(templateService.updateTemplate(id, request, scopedMerchantId));
    }

    @DeleteMapping("/templates/{id}")
    public Result<Void> deleteTemplate(@PathVariable Long id,
                                       @RequestHeader(value = "X-User-Type", defaultValue = "super_admin") String userType,
                                       @RequestHeader(value = "X-Merchant-Id", required = false) Long headerMerchantId) {
        templateService.deleteTemplate(
                id, MerchantTenantSupport.resolveRequestMerchantId(
                        userType, headerMerchantId, LogisticsErrorCode.TEMPLATE_FORBIDDEN));
        return Result.ok();
    }

    // ===== 发货管理 =====

    @PostMapping("/shipping")
    public Result<ShippingOrderVO> createShipping(@Valid @RequestBody CreateShippingRequest request,
                                                  @RequestHeader(value = "X-User-Type", defaultValue = "super_admin") String userType,
                                                  @RequestHeader(value = "X-Merchant-Id", required = false) Long headerMerchantId) {
        return Result.ok(shippingService.createShipping(
                request,
                userType,
                MerchantTenantSupport.resolveRequestMerchantId(
                        userType, headerMerchantId, LogisticsErrorCode.SHIPPING_FORBIDDEN)));
    }

    @GetMapping("/shipping")
    public Result<IPage<ShippingOrderVO>> listShipping(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Integer shippingStatus,
            @RequestParam(required = false) Long merchantId,
            @RequestHeader(value = "X-User-Type", defaultValue = "super_admin") String userType,
            @RequestHeader(value = "X-Merchant-Id", required = false) Long headerMerchantId) {
        Long scopedMerchantId = MerchantTenantSupport.resolveScopedMerchantId(
                userType, headerMerchantId, merchantId, LogisticsErrorCode.SHIPPING_FORBIDDEN);
        return Result.ok(shippingService.listShipping(page, size, orderNo, shippingStatus, scopedMerchantId));
    }

    @GetMapping("/shipping/{id}")
    public Result<ShippingOrderVO> getShipping(@PathVariable Long id,
                                               @RequestHeader(value = "X-User-Type", defaultValue = "super_admin") String userType,
                                               @RequestHeader(value = "X-Merchant-Id", required = false) Long headerMerchantId) {
        return Result.ok(shippingService.getShipping(
                id,
                userType,
                MerchantTenantSupport.resolveRequestMerchantId(
                        userType, headerMerchantId, LogisticsErrorCode.SHIPPING_FORBIDDEN)));
    }

    @PostMapping("/shipping/{id}/waybill")
    public Result<String> generateWaybill(@PathVariable Long id,
                                          @RequestHeader(value = "X-User-Type", defaultValue = "super_admin") String userType,
                                          @RequestHeader(value = "X-Merchant-Id", required = false) Long headerMerchantId) {
        return Result.ok(shippingService.generateWaybill(
                id, MerchantTenantSupport.resolveRequestMerchantId(
                        userType, headerMerchantId, LogisticsErrorCode.SHIPPING_FORBIDDEN)));
    }

    @PostMapping("/shipping/batch")
    public Result<List<ShippingOrderVO>> batchShip(@RequestBody BatchShipRequest request,
                                                   @RequestHeader(value = "X-User-Type", defaultValue = "super_admin") String userType,
                                                   @RequestHeader(value = "X-Merchant-Id", required = false) Long headerMerchantId) {
        return Result.ok(shippingService.batchShip(
                request,
                userType,
                MerchantTenantSupport.resolveRequestMerchantId(
                        userType, headerMerchantId, LogisticsErrorCode.SHIPPING_FORBIDDEN)));
    }

    // ===== 排障用轨迹查询 =====

    @GetMapping("/tracking/no/{trackingNo}")
    public Result<TrackingVO> getTrackingByNo(@PathVariable String trackingNo,
                                              @RequestParam String providerCode,
                                              @RequestHeader(value = "X-User-Type", defaultValue = "super_admin") String userType,
                                              @RequestHeader(value = "X-Merchant-Id", required = false) Long headerMerchantId) {
        return Result.ok(shippingService.getTrackingByTrackingNo(
                trackingNo,
                providerCode,
                MerchantTenantSupport.resolveRequestMerchantId(
                        userType, headerMerchantId, LogisticsErrorCode.SHIPPING_FORBIDDEN)));
    }
}
