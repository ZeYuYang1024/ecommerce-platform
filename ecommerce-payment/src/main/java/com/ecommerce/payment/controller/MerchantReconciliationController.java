package com.ecommerce.payment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.common.result.Result;
import com.ecommerce.payment.common.PaymentErrorCode;
import com.ecommerce.payment.dto.response.ReconciliationVO;
import com.ecommerce.payment.service.ReconciliationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/merchant")
public class MerchantReconciliationController {

    private final ReconciliationService reconciliationService;

    public MerchantReconciliationController(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @GetMapping("/reconciliation")
    public Result<Page<ReconciliationVO>> listByMerchant(@RequestHeader("X-Merchant-Id") Long merchantId,
                                                         @RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "10") int size) {
        return Result.ok(reconciliationService.listByMerchant(merchantId, page, size));
    }

    @GetMapping("/reconciliation/{id}")
    public Result<ReconciliationVO> detailByMerchant(@RequestHeader("X-Merchant-Id") Long merchantId,
                                                     @PathVariable Long id) {
        ReconciliationVO detail = reconciliationService.getReconciliationDetailByMerchant(merchantId, id);
        if (detail == null) {
            throw new BusinessException(PaymentErrorCode.RECONCILIATION_NOT_FOUND);
        }
        return Result.ok(detail);
    }
}
