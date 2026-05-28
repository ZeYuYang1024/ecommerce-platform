package com.ecommerce.inventory.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.Result;
import com.ecommerce.inventory.dto.request.StockOperateRequest;
import com.ecommerce.inventory.dto.request.StockSetRequest;
import com.ecommerce.inventory.dto.response.InventoryEventLogVO;
import com.ecommerce.inventory.dto.response.InventoryEventSummaryVO;
import com.ecommerce.inventory.dto.response.StockVO;
import com.ecommerce.inventory.service.InventoryEventAdminService;
import com.ecommerce.inventory.service.StockService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class StockController {

    private final StockService stockService;
    private final InventoryEventAdminService inventoryEventAdminService;

    public StockController(StockService stockService, InventoryEventAdminService inventoryEventAdminService) {
        this.stockService = stockService;
        this.inventoryEventAdminService = inventoryEventAdminService;
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

    @GetMapping("/api/v1/admin/inventory")
    public Result<Page<StockVO>> list(@RequestParam(name = "skuId", required = false) Long skuId,
                                      @RequestParam(name = "stockStatus", required = false) Integer stockStatus,
                                      @RequestParam(name = "page", defaultValue = "1") int page,
                                      @RequestParam(name = "size", defaultValue = "10") int size) {
        return Result.ok(stockService.list(skuId, stockStatus, page, size));
    }

    @GetMapping("/api/v1/admin/merchant/inventory")
    public Result<Page<StockVO>> merchantList(@RequestHeader("X-Merchant-Id") Long merchantId,
                                              @RequestParam(name = "skuId", required = false) Long skuId,
                                              @RequestParam(name = "stockStatus", required = false) Integer stockStatus,
                                              @RequestParam(name = "page", defaultValue = "1") int page,
                                              @RequestParam(name = "size", defaultValue = "10") int size) {
        return Result.ok(stockService.listForMerchant(merchantId, skuId, stockStatus, page, size));
    }

    @GetMapping("/api/v1/admin/inventory/events")
    public Result<Page<InventoryEventLogVO>> listEvents(@RequestParam(required = false) String topic,
                                                        @RequestParam(required = false) String orderNo,
                                                        @RequestParam(required = false) Integer status,
                                                        @RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "10") int size) {
        return Result.ok(inventoryEventAdminService.listEvents(topic, orderNo, status, page, size));
    }

    @GetMapping("/api/v1/admin/inventory/events/summary")
    public Result<InventoryEventSummaryVO> summarizeEvents(@RequestParam(required = false) String topic,
                                                           @RequestParam(required = false) String orderNo,
                                                           @RequestParam(required = false) Integer status) {
        return Result.ok(inventoryEventAdminService.summarize(topic, orderNo, status));
    }

    @PostMapping("/api/v1/internal/inventory/deduct")
    public Result<Void> deduct(@Valid @RequestBody StockOperateRequest request) {
        stockService.deduct(request.getSkuId(), request.getQuantity());
        return Result.ok();
    }

    @PostMapping("/api/v1/internal/inventory/release")
    public Result<Void> release(@Valid @RequestBody StockOperateRequest request) {
        stockService.release(request.getSkuId(), request.getQuantity());
        return Result.ok();
    }

    @PostMapping("/api/v1/admin/inventory/{skuId}")
    public Result<Void> setStock(@PathVariable Long skuId, @Valid @RequestBody StockSetRequest request) {
        stockService.setStock(skuId, request.getTotalStock());
        return Result.ok();
    }

    @PostMapping("/api/v1/admin/merchant/inventory/{skuId}")
    public Result<Void> merchantSetStock(@RequestHeader("X-Merchant-Id") Long merchantId,
                                         @PathVariable Long skuId,
                                         @Valid @RequestBody StockSetRequest request) {
        stockService.setStockForMerchant(merchantId, skuId, request.getTotalStock());
        return Result.ok();
    }

    @PostMapping("/api/v1/internal/inventory/{skuId}")
    public Result<Void> initStock(@PathVariable Long skuId, @Valid @RequestBody StockSetRequest request) {
        stockService.setStock(skuId, request.getTotalStock());
        return Result.ok();
    }
}
