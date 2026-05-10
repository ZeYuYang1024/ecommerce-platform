package com.ecommerce.payment.controller;

import com.ecommerce.common.result.Result;
import com.ecommerce.payment.dto.response.SettlementVO;
import com.ecommerce.payment.service.SettlementService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public Result<List<SettlementVO>> list() {
        return Result.ok(settlementService.listSettlements());
    }
}
