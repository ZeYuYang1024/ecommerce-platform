package com.ecommerce.merchant.controller;

import com.ecommerce.common.result.Result;
import com.ecommerce.merchant.dto.request.MerchantAuditRequest;
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
    public Result<List<MerchantVO>> list(@RequestParam(required = false) Integer status) {
        return Result.ok(merchantService.list(status));
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
}
