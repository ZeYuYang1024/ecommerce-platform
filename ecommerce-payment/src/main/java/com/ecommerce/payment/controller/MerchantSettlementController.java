package com.ecommerce.payment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.Result;
import com.ecommerce.payment.dto.response.SettlementVO;
import com.ecommerce.payment.service.SettlementService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/merchant")
public class MerchantSettlementController {

    private final SettlementService settlementService;

    public MerchantSettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @GetMapping("/settlement")
    public Result<Page<SettlementVO>> listByMerchant(@RequestHeader("X-Merchant-Id") Long merchantId,
                                                     @RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "10") int size) {
        return Result.ok(settlementService.listByMerchant(merchantId, page, size));
    }
}
