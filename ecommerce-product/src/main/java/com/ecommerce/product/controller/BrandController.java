package com.ecommerce.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.Result;
import com.ecommerce.product.entity.Brand;
import com.ecommerce.product.service.BrandService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
public class BrandController {

    private final BrandService brandService;

    public BrandController(BrandService brandService) {
        this.brandService = brandService;
    }

    @GetMapping("/brands")
    public Result<Page<Brand>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(brandService.pageAll(page, size));
    }

    @GetMapping("/brands/{id}")
    public Result<Brand> detail(@PathVariable Long id) {
        return Result.ok(brandService.getById(id));
    }

    @PostMapping("/brands")
    public Result<Brand> create(@RequestBody Brand brand) {
        return Result.ok(brandService.createPlatformBrand(brand));
    }

    @PutMapping("/brands/{id}")
    public Result<Brand> update(@PathVariable Long id, @RequestBody Brand brand) {
        return Result.ok(brandService.updatePlatformBrand(id, brand));
    }

    @PutMapping("/brands/{id}/audit")
    public Result<Brand> audit(@PathVariable Long id, @RequestParam("status") String status) {
        return Result.ok(brandService.auditMerchantBrand(id, status));
    }

    @DeleteMapping("/brands/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        brandService.deletePlatformBrand(id);
        return Result.ok();
    }
}
