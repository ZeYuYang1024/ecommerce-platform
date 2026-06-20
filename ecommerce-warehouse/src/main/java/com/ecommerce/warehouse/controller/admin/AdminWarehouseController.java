package com.ecommerce.warehouse.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ecommerce.common.result.Result;
import com.ecommerce.warehouse.dto.request.CreateCheckRequest;
import com.ecommerce.warehouse.dto.request.CreateInboundRequest;
import com.ecommerce.warehouse.dto.request.CreateOutboundRequest;
import com.ecommerce.warehouse.dto.request.CreateWarehouseRequest;
import com.ecommerce.warehouse.dto.response.*;
import com.ecommerce.warehouse.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminWarehouseController {

    private final WarehouseService warehouseService;
    private final InboundService inboundService;
    private final OutboundService outboundService;
    private final StockService stockService;
    private final CheckService checkService;

    // ===== 仓库管理 =====

    @GetMapping("/warehouses")
    public Result<IPage<WarehouseVO>> listWarehouses(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long merchantId) {
        return Result.ok(warehouseService.listWarehouses(page, size, merchantId));
    }

    @GetMapping("/warehouses/{id}")
    public Result<WarehouseVO> getWarehouse(@PathVariable Long id) {
        return Result.ok(warehouseService.getWarehouse(id));
    }

    @PostMapping("/warehouses")
    public Result<WarehouseVO> createWarehouse(@Valid @RequestBody CreateWarehouseRequest request) {
        return Result.ok(warehouseService.createWarehouse(request));
    }

    @PutMapping("/warehouses/{id}")
    public Result<WarehouseVO> updateWarehouse(@PathVariable Long id,
                                               @Valid @RequestBody CreateWarehouseRequest request) {
        return Result.ok(warehouseService.updateWarehouse(id, request));
    }

    @DeleteMapping("/warehouses/{id}")
    public Result<Void> deleteWarehouse(@PathVariable Long id) {
        warehouseService.deleteWarehouse(id);
        return Result.ok();
    }

    @PutMapping("/warehouses/{id}/status")
    public Result<Void> toggleStatus(@PathVariable Long id, @RequestParam Integer status) {
        warehouseService.toggleStatus(id, status);
        return Result.ok();
    }

    // ===== 库区管理 =====

    @GetMapping("/warehouses/{id}/zones")
    public Result<List<WarehouseZoneVO>> listZones(@PathVariable("id") Long warehouseId) {
        return Result.ok(warehouseService.listZonesByWarehouse(warehouseId));
    }

    @PostMapping("/warehouses/{id}/zones")
    public Result<WarehouseZoneVO> createZone(@PathVariable("id") Long warehouseId,
                                              @RequestBody WarehouseZoneVO vo) {
        vo.setWarehouseId(warehouseId);
        return Result.ok(warehouseService.createZone(vo));
    }

    @PutMapping("/warehouses/{id}/zones/{zid}")
    public Result<WarehouseZoneVO> updateZone(@PathVariable("id") Long warehouseId,
                                              @PathVariable Long zid,
                                              @RequestBody WarehouseZoneVO vo) {
        vo.setWarehouseId(warehouseId);
        return Result.ok(warehouseService.updateZone(zid, vo));
    }

    @DeleteMapping("/warehouses/{id}/zones/{zid}")
    public Result<Void> deleteZone(@PathVariable("id") Long warehouseId,
                                   @PathVariable Long zid) {
        warehouseService.deleteZone(zid);
        return Result.ok();
    }

    // ===== 库位管理 =====

    @GetMapping("/warehouses/{id}/bins")
    public Result<List<WarehouseBinVO>> listBins(@PathVariable("id") Long warehouseId) {
        return Result.ok(warehouseService.listBinsByWarehouse(warehouseId));
    }

    @PostMapping("/warehouses/{id}/bins")
    public Result<WarehouseBinVO> createBin(@PathVariable("id") Long warehouseId,
                                            @RequestBody WarehouseBinVO vo) {
        vo.setWarehouseId(warehouseId);
        return Result.ok(warehouseService.createBin(vo));
    }

    @PutMapping("/warehouses/{id}/bins/{bid}")
    public Result<WarehouseBinVO> updateBin(@PathVariable("id") Long warehouseId,
                                            @PathVariable Long bid,
                                            @RequestBody WarehouseBinVO vo) {
        vo.setWarehouseId(warehouseId);
        return Result.ok(warehouseService.updateBin(bid, vo));
    }

    @DeleteMapping("/warehouses/{id}/bins/{bid}")
    public Result<Void> deleteBin(@PathVariable("id") Long warehouseId,
                                  @PathVariable Long bid) {
        warehouseService.deleteBin(bid);
        return Result.ok();
    }

    // ===== 入库管理 =====

    @GetMapping("/warehouse/inbounds")
    public Result<IPage<InboundOrderVO>> listInbounds(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long warehouseId,
            @RequestHeader(value = "X-Merchant-Id", required = false) Long merchantId) {
        return Result.ok(inboundService.listInbounds(page, size, warehouseId, merchantId));
    }

    @GetMapping("/warehouse/inbounds/{id}")
    public Result<InboundOrderVO> getInbound(@PathVariable Long id) {
        return Result.ok(inboundService.getInbound(id));
    }

    @PostMapping("/warehouse/inbounds")
    public Result<InboundOrderVO> createInbound(@Valid @RequestBody CreateInboundRequest request) {
        return Result.ok(inboundService.createInbound(request));
    }

    @PutMapping("/warehouse/inbounds/{id}/receive")
    public Result<Void> confirmReceived(@PathVariable Long id) {
        inboundService.confirmReceived(id);
        return Result.ok();
    }

    @PutMapping("/warehouse/inbounds/{id}/shelve")
    public Result<Void> confirmShelved(@PathVariable Long id) {
        inboundService.confirmShelved(id);
        return Result.ok();
    }

    // ===== 出库管理 =====

    @GetMapping("/warehouse/outbounds")
    public Result<IPage<OutboundOrderVO>> listOutbounds(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long warehouseId,
            @RequestHeader(value = "X-Merchant-Id", required = false) Long merchantId) {
        return Result.ok(outboundService.listOutbounds(page, size, warehouseId, merchantId));
    }

    @GetMapping("/warehouse/outbounds/{id}")
    public Result<OutboundOrderVO> getOutbound(@PathVariable Long id) {
        return Result.ok(outboundService.getOutbound(id));
    }

    @PostMapping("/warehouse/outbounds")
    public Result<OutboundOrderVO> createOutbound(@Valid @RequestBody CreateOutboundRequest request) {
        return Result.ok(outboundService.createOutbound(request));
    }

    @PutMapping("/warehouse/outbounds/{id}/pick")
    public Result<Void> startPicking(@PathVariable Long id) {
        outboundService.startPicking(id);
        return Result.ok();
    }

    @PutMapping("/warehouse/outbounds/{id}/ship")
    public Result<Void> confirmShipped(@PathVariable Long id) {
        outboundService.confirmShipped(id);
        return Result.ok();
    }

    // ===== 库存管理 =====

    @GetMapping("/warehouse/stock")
    public Result<List<PhysicalStockVO>> queryStock(
            @RequestParam Long warehouseId,
            @RequestParam Long skuId) {
        return Result.ok(stockService.queryStock(warehouseId, skuId));
    }

    @GetMapping("/warehouse/stock/alerts")
    public Result<List<PhysicalStockVO>> getLowStockAlerts(
            @RequestParam Long warehouseId) {
        return Result.ok(stockService.getLowStockAlerts(warehouseId));
    }

    // ===== 盘点管理 =====

    @GetMapping("/warehouse/checks")
    public Result<IPage<StockCheckVO>> listChecks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long warehouseId,
            @RequestHeader(value = "X-Merchant-Id", required = false) Long merchantId) {
        return Result.ok(checkService.listChecks(page, size, warehouseId, merchantId));
    }

    @GetMapping("/warehouse/checks/{id}")
    public Result<StockCheckVO> getCheck(@PathVariable Long id) {
        return Result.ok(checkService.getCheck(id));
    }

    @PostMapping("/warehouse/checks")
    public Result<StockCheckVO> createCheck(@Valid @RequestBody CreateCheckRequest request) {
        return Result.ok(checkService.createCheck(request));
    }

    @PutMapping("/warehouse/checks/{id}/items/{itemId}")
    public Result<Void> recordCheckItem(@PathVariable("id") Long checkId,
                                        @PathVariable Long itemId,
                                        @RequestParam int actualQty) {
        checkService.recordCheckItem(checkId, itemId, actualQty);
        return Result.ok();
    }

    @PutMapping("/warehouse/checks/{id}/complete")
    public Result<Void> completeCheck(@PathVariable Long id) {
        checkService.completeCheck(id);
        return Result.ok();
    }
}
