package com.ecommerce.warehouse.controller.internal;

import com.ecommerce.common.result.Result;
import com.ecommerce.warehouse.dto.request.CreateOutboundRequest;
import com.ecommerce.warehouse.dto.response.OutboundOrderVO;
import com.ecommerce.warehouse.dto.response.PhysicalStockVO;
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
}
