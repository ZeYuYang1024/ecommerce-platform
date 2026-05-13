package com.ecommerce.payment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.Result;
import com.ecommerce.payment.dto.response.ReconciliationVO;
import com.ecommerce.payment.service.ReconciliationService;
import org.springframework.web.bind.annotation.*;

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
    public Result<Page<ReconciliationVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(reconciliationService.listReconciliations(page, size));
    }

    @GetMapping("/reconciliation/{id}")
    public Result<ReconciliationVO> detail(@PathVariable Long id) {
        return Result.ok(reconciliationService.getReconciliationDetail(id));
    }
}
