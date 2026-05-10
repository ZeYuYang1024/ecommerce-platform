package com.ecommerce.payment.controller;

import com.ecommerce.common.result.Result;
import com.ecommerce.payment.dto.response.ReconciliationVO;
import com.ecommerce.payment.service.ReconciliationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminReconciliationController {

    private final ReconciliationService reconciliationService;

    public AdminReconciliationController(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @PostMapping("/reconciliation/run")
    public Result<ReconciliationVO> run() {
        return Result.ok(reconciliationService.runReconciliation());
    }

    @GetMapping("/reconciliation")
    public Result<List<ReconciliationVO>> list() {
        return Result.ok(reconciliationService.listReconciliations());
    }

    @GetMapping("/reconciliation/{id}")
    public Result<ReconciliationVO> detail(@PathVariable Long id) {
        return Result.ok(reconciliationService.getReconciliationDetail(id));
    }
}
