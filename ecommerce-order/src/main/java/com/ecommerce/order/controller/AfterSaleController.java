package com.ecommerce.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.common.result.Result;
import com.ecommerce.common.tenant.MerchantTenantSupport;
import com.ecommerce.order.common.OrderErrorCode;
import com.ecommerce.order.entity.AfterSale;
import com.ecommerce.order.service.AfterSaleService;
import com.ecommerce.order.service.ShippingClaimReaper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AfterSaleController {
    private final AfterSaleService service;
    private final ShippingClaimReaper claimReaper;

    record Application(@NotBlank @Size(max = 256) String reason) {}
    record Decision(@NotNull Boolean approved, @Size(max = 256) String note) {}

    @GetMapping("/orders/no/{orderNo}/after-sale")
    public Result<AfterSaleService.BuyerStatus> get(@RequestHeader("X-User-Id") Long userId,
                                                    @RequestHeader("X-User-Type") String userType,
                                                    @PathVariable String orderNo) {
        requireBuyerRole(userType);
        return Result.ok(service.buyerStatus(userId, orderNo));
    }

    @PostMapping("/orders/no/{orderNo}/after-sale")
    public Result<AfterSale> apply(@RequestHeader("X-User-Id") Long userId,
                                   @RequestHeader("X-User-Type") String userType, @PathVariable String orderNo,
                                   @Valid @RequestBody Application application) {
        requireBuyerRole(userType);
        return Result.ok(service.apply(userId, orderNo, application.reason()));
    }

    @GetMapping("/admin/orders/after-sales")
    public Result<Page<AfterSale>> platformList(@RequestParam(required = false) AfterSale.Status status,
                                                 @RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int size) {
        return Result.ok(service.list(null, status, page, Math.max(1, Math.min(size, 100))));
    }

    @GetMapping("/admin/merchant/orders/after-sales")
    public Result<Page<AfterSale>> merchantList(@RequestHeader("X-Merchant-Id") Long merchantId,
                                                   @RequestParam(required = false) AfterSale.Status status,
                                                   @RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        Long scope = MerchantTenantSupport.requireMerchantId(merchantId, OrderErrorCode.ORDER_FORBIDDEN);
        return Result.ok(service.list(scope, status, page, Math.max(1, Math.min(size, 100))));
    }

    @PutMapping("/admin/orders/after-sales/{id}/review")
    public Result<AfterSale> platformReview(@PathVariable Long id, @Valid @RequestBody Decision decision) {
        return Result.ok(service.review(null, id, decision.approved(), decision.note()));
    }

    @PutMapping("/admin/merchant/orders/after-sales/{id}/review")
    public Result<AfterSale> merchantReview(@RequestHeader("X-Merchant-Id") Long merchantId,
                                             @PathVariable Long id, @Valid @RequestBody Decision decision) {
        Long scope = MerchantTenantSupport.requireMerchantId(merchantId, OrderErrorCode.ORDER_FORBIDDEN);
        return Result.ok(service.review(scope, id, decision.approved(), decision.note()));
    }

    @PostMapping("/admin/orders/after-sales/{id}/retry")
    public Result<AfterSale> retryRefund(@PathVariable Long id, @RequestHeader("X-User-Type") String userType) {
        requirePlatformRole(userType);
        return Result.ok(service.retryRefund(id));
    }

    @PostMapping("/admin/orders/{id}/shipping-claims/release-expired")
    public Result<Integer> releaseExpiredClaims(@PathVariable Long id, @RequestHeader("X-User-Type") String userType) {
        requirePlatformRole(userType);
        return Result.ok(claimReaper.releaseExpired(id));
    }

    @PutMapping("/internal/orders/{id}/shipping-claim")
    public Result<Void> claimShipping(@PathVariable Long id, @RequestParam String shippingNo,
                                      @RequestParam(required = false) Long shippingId,
                                      @RequestParam(defaultValue = "false") boolean managed) {
        service.claimShipping(id, shippingNo, shippingId, managed);
        return Result.ok();
    }

    @PutMapping("/internal/orders/{id}/shipping-claim/confirm")
    public Result<Void> confirmShipping(@PathVariable Long id, @RequestParam String shippingNo) {
        service.confirmShipping(id, shippingNo);
        return Result.ok();
    }

    @DeleteMapping("/internal/orders/{id}/shipping-claim")
    public Result<Void> releaseShipping(@PathVariable Long id, @RequestParam String shippingNo) {
        service.releaseShipping(id, shippingNo);
        return Result.ok();
    }

    private void requireBuyerRole(String userType) {
        if (!"user".equals(userType)) throw new BusinessException(OrderErrorCode.ORDER_FORBIDDEN);
    }

    private void requirePlatformRole(String userType) {
        if (!"super_admin".equals(userType) && !"platform_admin".equals(userType)) {
            throw new BusinessException(OrderErrorCode.ORDER_FORBIDDEN);
        }
    }
}
