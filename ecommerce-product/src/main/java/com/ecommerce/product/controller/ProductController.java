package com.ecommerce.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.Result;
import com.ecommerce.product.dto.request.CreateProductRequest;
import com.ecommerce.product.dto.request.UpdateStatusRequest;
import com.ecommerce.product.dto.response.ProductDetailVO;
import com.ecommerce.product.dto.response.SpuVO;
import com.ecommerce.product.entity.Spu;
import com.ecommerce.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/products")
    public Result<Page<SpuVO>> list(@RequestParam(name = "page", defaultValue = "1") int page,
                                     @RequestParam(name = "size", defaultValue = "10") int size,
                                     @RequestParam(name = "categoryId", required = false) Long categoryId,
                                     @RequestParam(name = "keyword", required = false) String keyword) {
        Page<Spu> result = productService.spuPage(page, size, categoryId, 1, keyword);
        List<SpuVO> vos = result.getRecords().stream().map(this::toSpuVO).collect(Collectors.toList());
        Page<SpuVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(vos);
        return Result.ok(voPage);
    }

    @GetMapping("/admin/products")
    public Result<Page<SpuVO>> adminList(@RequestParam(name = "page", defaultValue = "1") int page,
                                          @RequestParam(name = "size", defaultValue = "10") int size,
                                          @RequestParam(name = "categoryId", required = false) Long categoryId,
                                          @RequestParam(name = "status", required = false) Integer status,
                                          @RequestParam(name = "keyword", required = false) String keyword) {
        Page<Spu> result = productService.spuPage(page, size, categoryId, status, keyword);
        List<SpuVO> vos = result.getRecords().stream().map(this::toSpuVO).collect(Collectors.toList());
        Page<SpuVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(vos);
        return Result.ok(voPage);
    }

    @GetMapping("/products/{id}")
    public Result<ProductDetailVO> detail(@PathVariable Long id) {
        return Result.ok(productService.getProductDetail(id));
    }

    @PostMapping("/admin/products")
    public Result<Spu> create(@Valid @RequestBody CreateProductRequest request) {
        return Result.ok(productService.createProduct(request));
    }

    @PutMapping("/admin/products/{id}")
    public Result<Spu> update(@PathVariable Long id, @RequestBody Spu spu) {
        spu.setId(id);
        return Result.ok(productService.updateSpu(spu));
    }

    @PutMapping("/admin/products/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest request) {
        productService.updateStatus(id, request.getStatus());
        return Result.ok();
    }

    @DeleteMapping("/admin/products/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        productService.deleteSpu(id);
        return Result.ok();
    }

    private SpuVO toSpuVO(Spu spu) {
        SpuVO vo = new SpuVO();
        vo.setId(spu.getId());
        vo.setName(spu.getName());
        vo.setCategoryId(spu.getCategoryId());
        vo.setBrandId(spu.getBrandId());
        vo.setDescription(spu.getDescription());
        vo.setMainImage(spu.getMainImage());
        vo.setImages(spu.getImages());
        vo.setDetail(spu.getDetail());
        vo.setStatus(spu.getStatus());
        vo.setAvgRating(spu.getAvgRating());
        vo.setReviewCount(spu.getReviewCount());
        vo.setCreatedAt(spu.getCreatedAt());
        return vo;
    }
}
