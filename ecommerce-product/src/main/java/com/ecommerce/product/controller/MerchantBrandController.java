package com.ecommerce.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.common.result.Result;
import com.ecommerce.common.tenant.MerchantTenantSupport;
import com.ecommerce.product.common.ProductErrorCode;
import com.ecommerce.product.entity.Brand;
import com.ecommerce.product.service.BrandService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/merchant/brands")
public class MerchantBrandController {

    private final BrandService brandService;

    public MerchantBrandController(BrandService brandService) {
        this.brandService = brandService;
    }

    @GetMapping
    public Result<Page<Brand>> list(@RequestHeader("X-Merchant-Id") Long merchantId,
                                    @RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "10") int size) {
        return Result.ok(brandService.pageForMerchant(merchantId, page, size));
    }

    @GetMapping("/{id}")
    public Result<Brand> detail(@RequestHeader("X-Merchant-Id") Long merchantId,
                                @PathVariable Long id) {
        Brand brand = brandService.getById(id);
        if (!"merchant".equals(brand.getSourceType())) {
            throw new BusinessException(ProductErrorCode.BRAND_FORBIDDEN);
        }
        MerchantTenantSupport.requireMerchantScope("merchant", merchantId, brand.getMerchantId(), ProductErrorCode.BRAND_FORBIDDEN);
        return Result.ok(brand);
    }

    @PostMapping
    public Result<Brand> create(@RequestHeader("X-Merchant-Id") Long merchantId,
                                @Valid @RequestBody Brand brand) {
        return Result.ok(brandService.createMerchantBrand(merchantId, brand));
    }

    @PutMapping("/{id}")
    public Result<Brand> update(@RequestHeader("X-Merchant-Id") Long merchantId,
                                @PathVariable Long id,
                                @Valid @RequestBody Brand brand) {
        return Result.ok(brandService.updateMerchantBrand(merchantId, id, brand));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@RequestHeader("X-Merchant-Id") Long merchantId,
                               @PathVariable Long id) {
        brandService.deleteMerchantBrand(merchantId, id);
        return Result.ok();
    }
}
