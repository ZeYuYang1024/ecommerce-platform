package com.ecommerce.product.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.common.result.Result;
import com.ecommerce.common.util.SnowflakeUtils;
import com.ecommerce.product.entity.Brand;
import com.ecommerce.product.mapper.BrandMapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
public class BrandController {

    private final BrandMapper brandMapper;

    public BrandController(BrandMapper brandMapper) {
        this.brandMapper = brandMapper;
    }

    @GetMapping("/brands")
    public Result<List<Brand>> list() {
        return Result.ok(brandMapper.selectList(
                new LambdaQueryWrapper<Brand>().orderByAsc(Brand::getName)));
    }

    @GetMapping("/brands/{id}")
    public Result<Brand> detail(@PathVariable Long id) {
        return Result.ok(brandMapper.selectById(id));
    }

    @PostMapping("/brands")
    public Result<Brand> create(@RequestBody Brand brand) {
        brand.setId(SnowflakeUtils.nextId());
        brandMapper.insert(brand);
        return Result.ok(brand);
    }

    @PutMapping("/brands/{id}")
    public Result<Brand> update(@PathVariable Long id, @RequestBody Brand brand) {
        brand.setId(id);
        brandMapper.updateById(brand);
        return Result.ok(brandMapper.selectById(id));
    }

    @DeleteMapping("/brands/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        brandMapper.deleteById(id);
        return Result.ok();
    }
}
