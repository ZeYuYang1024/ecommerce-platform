package com.ecommerce.merchant.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.Result;
import com.ecommerce.merchant.dto.request.MerchantAuditRequest;
import com.ecommerce.common.dto.MerchantStatsVO;
import com.ecommerce.merchant.dto.response.MerchantVO;
import com.ecommerce.merchant.service.MerchantService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminMerchantController {

    private final MerchantService merchantService;

    public AdminMerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @GetMapping("/merchants")
    public Result<Page<MerchantVO>> list(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(merchantService.list(status, page, size));
    }

    @GetMapping("/merchants/{id}")
    public Result<MerchantVO> detail(@PathVariable Long id) {
        return Result.ok(merchantService.getById(id));
    }

    @PutMapping("/merchants/{id}/audit")
    public Result<MerchantVO> audit(@PathVariable Long id,
                                     @Valid @RequestBody MerchantAuditRequest request,
                                     @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return Result.ok(merchantService.audit(id, request, userId));
    }

    @GetMapping("/merchants/stats")
    public Result<MerchantStatsVO> stats() {
        List<MerchantVO> all = merchantService.listAll(null);
        long approved = all.stream().filter(m -> m.getStatus() != null && m.getStatus() == 1).count();
        long pending = all.stream().filter(m -> m.getStatus() != null && m.getStatus() == 0).count();
        MerchantStatsVO stats = new MerchantStatsVO();
        stats.setMerchantCount(approved);
        stats.setPendingAuditCount(pending);
        return Result.ok(stats);
    }
}
