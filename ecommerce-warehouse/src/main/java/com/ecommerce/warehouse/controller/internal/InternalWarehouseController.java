package com.ecommerce.warehouse.controller.internal;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.common.result.Result;
import com.ecommerce.warehouse.common.OutboundStatus;
import com.ecommerce.warehouse.dto.request.CreateOutboundRequest;
import com.ecommerce.warehouse.dto.response.OutboundOrderVO;
import com.ecommerce.warehouse.dto.response.PhysicalStockVO;
import com.ecommerce.warehouse.dto.response.WarehouseVO;
import com.ecommerce.warehouse.entity.OutboundOrder;
import com.ecommerce.warehouse.mapper.OutboundOrderMapper;
import com.ecommerce.warehouse.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/internal/warehouse")
@RequiredArgsConstructor
public class InternalWarehouseController {

    private final WarehouseService warehouseService;
    private final InboundService inboundService;
    private final OutboundService outboundService;
    private final StockService stockService;
    private final CheckService checkService;
    private final OutboundOrderMapper outboundOrders;

    @GetMapping("/{id}")
    public Result<WarehouseVO> getWarehouse(@PathVariable Long id) {
        return Result.ok(warehouseService.getWarehouse(id));
    }

    @PostMapping("/stock/query")
    public Result<List<PhysicalStockVO>> queryStock(@RequestBody Map<String, Object> body) {
        Long warehouseId = ((Number) body.get("warehouseId")).longValue();
        @SuppressWarnings("unchecked")
        List<Integer> skuIdInts = (List<Integer>) body.get("skuIds");
        List<Long> skuIds = skuIdInts.stream().map(Integer::longValue).toList();

        List<PhysicalStockVO> result = new ArrayList<>();
        for (Long skuId : skuIds) {
            result.addAll(stockService.queryStock(warehouseId, skuId));
        }
        return Result.ok(result);
    }

    @PostMapping("/outbounds")
    public Result<OutboundOrderVO> createOutbound(@Valid @RequestBody CreateOutboundRequest request) {
        return Result.ok(outboundService.createOutbound(request));
    }

    @GetMapping("/outbounds/shipping/{shippingId}/exists")
    public Result<Boolean> hasOutbound(@PathVariable Long shippingId) {
        return Result.ok(outboundOrders.exists(new LambdaQueryWrapper<OutboundOrder>()
                .eq(OutboundOrder::getShippingId, shippingId).ne(OutboundOrder::getStatus, OutboundStatus.CANCELLED)));
    }
    @PutMapping("/outbounds/shipping/{shippingId}/cancel-unshipped")
    public Result<Boolean> cancelUnshipped(@PathVariable Long shippingId) {
        return Result.ok(outboundService.cancelUnshipped(shippingId));
    }
}
