package com.ecommerce.merchant.controller;

import com.ecommerce.common.result.Result;
import com.ecommerce.merchant.dto.request.MerchantRegisterRequest;
import com.ecommerce.merchant.dto.response.MerchantVO;
import com.ecommerce.merchant.service.MerchantService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class MerchantController {

    private final MerchantService merchantService;

    public MerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @PostMapping("/merchants/register")
    public Result<MerchantVO> register(@Valid @RequestBody MerchantRegisterRequest request) {
        return Result.ok(merchantService.register(request));
    }
}
