package com.ecommerce.inventory.controller;

import com.ecommerce.common.result.Result;
import com.ecommerce.inventory.dto.request.StockOperateRequest;
import com.ecommerce.inventory.dto.request.StockSetRequest;
import com.ecommerce.inventory.dto.response.StockVO;
import com.ecommerce.inventory.entity.Stock;
import com.ecommerce.inventory.service.StockService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping("/{skuId}")
    public Result<StockVO> get(@PathVariable Long skuId) {
        return Result.ok(toVO(stockService.getBySkuId(skuId)));
    }

    @PostMapping("/batch-query")
    public Result<List<StockVO>> batchQuery(@RequestBody List<Long> skuIds) {
        List<Stock> stocks = stockService.batchQuery(skuIds);
        List<StockVO> vos = new ArrayList<>();
        for (Stock s : stocks) {
            vos.add(toVO(s));
        }
        return Result.ok(vos);
    }

    @PostMapping("/deduct")
    public Result<Void> deduct(@Valid @RequestBody StockOperateRequest request) {
        stockService.deduct(request.getSkuId(), request.getQuantity());
        return Result.ok();
    }

    @PostMapping("/release")
    public Result<Void> release(@Valid @RequestBody StockOperateRequest request) {
        stockService.release(request.getSkuId(), request.getQuantity());
        return Result.ok();
    }

    @PostMapping("/admin/{skuId}")
    public Result<Void> setStock(@PathVariable Long skuId, @Valid @RequestBody StockSetRequest request) {
        stockService.setStock(skuId, request.getTotalStock());
        return Result.ok();
    }

    private StockVO toVO(Stock s) {
        StockVO vo = new StockVO();
        vo.setId(s.getId());
        vo.setSkuId(s.getSkuId());
        vo.setTotalStock(s.getTotalStock());
        vo.setLockedStock(s.getLockedStock());
        vo.setAvailableStock(s.getAvailableStock());
        return vo;
    }
}
