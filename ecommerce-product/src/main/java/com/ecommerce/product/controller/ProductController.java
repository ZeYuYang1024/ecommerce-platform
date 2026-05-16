package com.ecommerce.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.common.result.Result;
import com.ecommerce.product.common.ProductErrorCode;
import com.ecommerce.product.dto.request.CreateProductRequest;
import com.ecommerce.product.dto.request.UpdateStatusRequest;
import com.ecommerce.product.dto.response.ProductDetailVO;
import com.ecommerce.common.dto.ProductStatsVO;
import com.ecommerce.common.dto.SkuBatchVO;
import com.ecommerce.product.dto.response.SpuVO;
import com.ecommerce.product.entity.Sku;
import com.ecommerce.product.entity.Spu;
import com.ecommerce.product.service.BrandService;
import com.ecommerce.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
public class ProductController {

    private final ProductService productService;
    private final BrandService brandService;

    public ProductController(ProductService productService, BrandService brandService) {
        this.productService = productService;
        this.brandService = brandService;
    }

    @GetMapping("/products")
    public Result<Page<SpuVO>> list(@RequestParam(name = "page", defaultValue = "1") int page,
                                     @RequestParam(name = "size", defaultValue = "10") int size,
                                     @RequestParam(name = "categoryId", required = false) Long categoryId,
                                     @RequestParam(name = "keyword", required = false) String keyword) {
        Page<Spu> result = productService.spuPage(page, size, categoryId, 1, keyword);
        List<SpuVO> vos = result.getRecords().stream().map(productService::toSpuVO).collect(Collectors.toList());
        Page<SpuVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(vos);
        return Result.ok(voPage);
    }

    @GetMapping("/admin/products")
    public Result<Page<SpuVO>> adminList(@RequestParam(name = "page", defaultValue = "1") int page,
                                          @RequestParam(name = "size", defaultValue = "10") int size,
                                          @RequestParam(name = "categoryId", required = false) Long categoryId,
                                          @RequestParam(name = "status", required = false) Integer status,
                                          @RequestParam(name = "keyword", required = false) String keyword,
                                          @RequestHeader(value = "X-User-Type", defaultValue = "super_admin") String userType,
                                          @RequestHeader(value = "X-Merchant-Id", required = false) Long merchantId) {
        Page<Spu> result;
        if ("merchant".equals(userType) && merchantId != null) {
            result = productService.spuPageByMerchant(page, size, categoryId, status, keyword, merchantId);
        } else {
            result = productService.spuPage(page, size, categoryId, status, keyword);
        }
        List<SpuVO> vos = result.getRecords().stream().map(productService::toSpuVO).collect(Collectors.toList());
        Page<SpuVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(vos);
        return Result.ok(voPage);
    }

    @GetMapping("/products/{id}")
    public Result<ProductDetailVO> detail(@PathVariable Long id) {
        return Result.ok(productService.getProductDetail(id));
    }

    @PostMapping("/admin/products")
    public Result<Spu> create(@Valid @RequestBody CreateProductRequest request,
                               @RequestHeader(value = "X-Merchant-Id", required = false) Long merchantId,
                               @RequestHeader(value = "X-User-Type", defaultValue = "super_admin") String userType) {
        if ("merchant".equals(userType) && request.getSpu().getBrandId() != null) {
            requireMerchantId(merchantId);
            brandService.validateMerchantBrandSelectable(merchantId, request.getSpu().getBrandId());
        }
        Spu spu = productService.createProduct(request);
        if ("merchant".equals(userType)) {
            requireMerchantId(merchantId);
            spu.setMerchantId(merchantId);
            spu = productService.updateSpu(spu);
        }
        return Result.ok(spu);
    }

    @PutMapping("/admin/products/{id}")
    public Result<Spu> update(@PathVariable Long id,
                              @RequestBody Spu spu,
                              @RequestHeader(value = "X-User-Type", defaultValue = "super_admin") String userType,
                              @RequestHeader(value = "X-Merchant-Id", required = false) Long merchantId) {
        if ("merchant".equals(userType)) {
            Spu existing = requireMerchantOwnedSpu(id, merchantId);
            spu.setMerchantId(existing.getMerchantId());
        }
        spu.setId(id);
        return Result.ok(productService.updateSpu(spu));
    }

    @PutMapping("/admin/products/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @Valid @RequestBody UpdateStatusRequest request,
                                     @RequestHeader(value = "X-User-Type", defaultValue = "super_admin") String userType,
                                     @RequestHeader(value = "X-Merchant-Id", required = false) Long merchantId) {
        if ("merchant".equals(userType)) {
            requireMerchantOwnedSpu(id, merchantId);
        }
        productService.updateStatus(id, request.getStatus());
        return Result.ok();
    }

    @DeleteMapping("/admin/products/{id}")
    public Result<Void> delete(@PathVariable Long id,
                               @RequestHeader(value = "X-User-Type", defaultValue = "super_admin") String userType,
                               @RequestHeader(value = "X-Merchant-Id", required = false) Long merchantId) {
        if ("merchant".equals(userType)) {
            requireMerchantOwnedSpu(id, merchantId);
        }
        productService.deleteSpu(id);
        return Result.ok();
    }

    @GetMapping("/internal/spu-ids")
    public Result<List<Long>> getSpuIdsByMerchant(@RequestParam("merchantId") Long merchantId) {
        return Result.ok(productService.getSpuIdsByMerchant(merchantId));
    }

    @GetMapping("/products/skus/batch")
    public Result<List<SkuBatchVO>> batchQuerySkus(@RequestParam("ids") List<Long> ids) {
        List<Sku> skus = productService.getSkusByIds(ids);
        if (skus.isEmpty()) return Result.ok(List.of());

        List<Long> spuIds = skus.stream().map(Sku::getSpuId).distinct().collect(Collectors.toList());
        Map<Long, Spu> spuMap = spuIds.isEmpty() ? Map.of() :
                productService.getSpusByIds(spuIds).stream().collect(Collectors.toMap(Spu::getId, Function.identity()));

        List<SkuBatchVO> result = new ArrayList<>();
        for (Sku sku : skus) {
            SkuBatchVO vo = new SkuBatchVO();
            vo.setSkuId(sku.getId());
            vo.setSkuName(sku.getName());
            vo.setSpuId(sku.getSpuId());
            vo.setPrice(sku.getPrice());
            Spu spu = spuMap.get(sku.getSpuId());
            if (spu != null) vo.setSpuName(spu.getName());
            result.add(vo);
        }
        return Result.ok(result);
    }

    @GetMapping("/admin/products/stats")
    public Result<ProductStatsVO> stats() {
        ProductStatsVO stats = new ProductStatsVO();
        stats.setProductCount(productService.countAll());
        return Result.ok(stats);
    }

    private void requireMerchantId(Long merchantId) {
        if (merchantId == null) {
            throw new BusinessException(ProductErrorCode.PRODUCT_FORBIDDEN);
        }
    }

    private Spu requireMerchantOwnedSpu(Long spuId, Long merchantId) {
        requireMerchantId(merchantId);
        Spu spu = productService.getSpuById(spuId);
        if (!merchantId.equals(spu.getMerchantId())) {
            throw new BusinessException(ProductErrorCode.PRODUCT_FORBIDDEN);
        }
        return spu;
    }
}
