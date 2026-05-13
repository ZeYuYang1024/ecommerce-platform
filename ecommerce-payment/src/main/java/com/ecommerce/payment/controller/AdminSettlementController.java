package com.ecommerce.payment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.Result;
import com.ecommerce.payment.dto.response.SettlementVO;
import com.ecommerce.payment.service.SettlementService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminSettlementController {

    private final SettlementService settlementService;

    public AdminSettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @PostMapping("/settlements")
    public Result<SettlementVO> generate(@RequestParam(required = false) String date) {
        return Result.ok(settlementService.generateSettlement(date));
    }

    @GetMapping("/settlements")
    public Result<Page<SettlementVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(settlementService.listSettlements(page, size));
    }
}
