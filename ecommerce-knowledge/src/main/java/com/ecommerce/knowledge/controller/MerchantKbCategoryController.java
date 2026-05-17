package com.ecommerce.knowledge.controller;

import com.ecommerce.knowledge.common.Result;
import com.ecommerce.knowledge.dto.request.CreateCategoryRequest;
import com.ecommerce.knowledge.dto.response.CategoryVO;
import com.ecommerce.knowledge.service.KbCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/merchant/knowledge/categories")
@RequiredArgsConstructor
public class MerchantKbCategoryController {

    private final KbCategoryService categoryService;

    @PostMapping
    public Result<CategoryVO> create(@RequestHeader("X-Merchant-Id") Long merchantId,
                                     @Valid @RequestBody CreateCategoryRequest request) {
        return Result.ok(categoryService.createForMerchant(merchantId, request));
    }

    @GetMapping
    public Result<List<CategoryVO>> listAll(@RequestHeader("X-Merchant-Id") Long merchantId) {
        return Result.ok(categoryService.listForMerchant(merchantId));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@RequestHeader("X-Merchant-Id") Long merchantId,
                               @PathVariable Long id) {
        categoryService.deleteForMerchant(merchantId, id);
        return Result.ok();
    }
}
