package com.ecommerce.inventory.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.Result;
import com.ecommerce.inventory.dto.request.StockOperateRequest;
import com.ecommerce.inventory.dto.request.StockSetRequest;
import com.ecommerce.inventory.dto.response.StockVO;
import com.ecommerce.inventory.service.StockService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping("/api/v1/inventory/{skuId}")
    public Result<StockVO> get(@PathVariable Long skuId) {
        return Result.ok(stockService.toVO(stockService.getBySkuId(skuId)));
    }

    @PostMapping("/api/v1/inventory/batch-query")
    public Result<List<StockVO>> batchQuery(@RequestBody List<Long> skuIds) {
        List<StockVO> vos = stockService.batchQuery(skuIds).stream()
                .map(stockService::toVO)
                .collect(Collectors.toList());
        return Result.ok(vos);
    }

    @GetMapping({"/api/v1/admin/inventory", "/api/v1/inventory/admin/list"})
    public Result<Page<StockVO>> list(@RequestParam(name = "skuId", required = false) Long skuId,
                                      @RequestParam(name = "stockStatus", required = false) Integer stockStatus,
                                      @RequestParam(name = "page", defaultValue = "1") int page,
                                      @RequestParam(name = "size", defaultValue = "10") int size) {
        return Result.ok(stockService.list(skuId, stockStatus, page, size));
    }

    @GetMapping({"/api/v1/admin/merchant/inventory", "/api/v1/inventory/admin/merchant/list"})
    public Result<Page<StockVO>> merchantList(@RequestHeader("X-Merchant-Id") Long merchantId,
                                              @RequestParam(name = "skuId", required = false) Long skuId,
                                              @RequestParam(name = "stockStatus", required = false) Integer stockStatus,
                                              @RequestParam(name = "page", defaultValue = "1") int page,
                                              @RequestParam(name = "size", defaultValue = "10") int size) {
        return Result.ok(stockService.listForMerchant(merchantId, skuId, stockStatus, page, size));
    }

    @PostMapping("/api/v1/inventory/deduct")
    public Result<Void> deduct(@Valid @RequestBody StockOperateRequest request) {
        stockService.deduct(request.getSkuId(), request.getQuantity());
        return Result.ok();
    }

    @PostMapping("/api/v1/inventory/release")
    public Result<Void> release(@Valid @RequestBody StockOperateRequest request) {
        stockService.release(request.getSkuId(), request.getQuantity());
        return Result.ok();
    }

    @PostMapping({"/api/v1/admin/inventory/{skuId}", "/api/v1/inventory/admin/{skuId}"})
    public Result<Void> setStock(@PathVariable Long skuId, @Valid @RequestBody StockSetRequest request) {
        stockService.setStock(skuId, request.getTotalStock());
        return Result.ok();
    }

    @PostMapping({"/api/v1/admin/merchant/inventory/{skuId}", "/api/v1/inventory/admin/merchant/{skuId}"})
    public Result<Void> merchantSetStock(@RequestHeader("X-Merchant-Id") Long merchantId,
                                         @PathVariable Long skuId,
                                         @Valid @RequestBody StockSetRequest request) {
        stockService.setStockForMerchant(merchantId, skuId, request.getTotalStock());
        return Result.ok();
    }
}
