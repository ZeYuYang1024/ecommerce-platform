package com.ecommerce.warehouse.service;

import com.ecommerce.warehouse.dto.response.PhysicalStockVO;
import com.ecommerce.warehouse.mapper.OutboundStockAllocationMapper.Allocation;

import java.util.List;

public interface StockService {

    /**
     * Query physical stock records for a given warehouse and SKU across all bins.
     */
    List<PhysicalStockVO> queryStock(Long warehouseId, Long skuId);

    /**
     * Add stock to a specific bin. Creates or updates the physical_stock record.
     * quantity += qty, available_qty += qty.
     */
    void addStock(Long warehouseId, Long skuId, Long binId, int quantity);

    void reconcileCount(Long warehouseId, Long skuId, Long binId, int expected, int actual);

    void allocateOutboundStock(Long warehouseId, Long itemId, Long skuId, Long binId, int quantity);

    void settleOutboundStock(Long itemId, int quantity, boolean shipped);

    List<Allocation> outboundAllocations(Long itemId);

    /**
     * Get stock records where available_qty is at or below safety stock level.
     */
    List<PhysicalStockVO> getLowStockAlerts(Long warehouseId);
}
