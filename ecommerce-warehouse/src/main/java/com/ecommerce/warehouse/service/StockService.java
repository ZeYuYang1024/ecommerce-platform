package com.ecommerce.warehouse.service;

import com.ecommerce.warehouse.dto.response.PhysicalStockVO;

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

    /**
     * Lock available stock for outbound order.
     * locked_qty += quantity, available_qty -= quantity across bins.
     */
    void lockStock(Long warehouseId, Long skuId, int quantity);

    /**
     * Deduct locked stock after shipping.
     * quantity -= quantity, locked_qty -= quantity (available_qty unchanged).
     */
    void deductStock(Long warehouseId, Long skuId, int quantity);

    /**
     * Release locked stock back to available.
     * locked_qty -= quantity, available_qty += quantity.
     */
    void releaseStock(Long warehouseId, Long skuId, int quantity);

    /**
     * Get stock records where available_qty is at or below safety stock level.
     */
    List<PhysicalStockVO> getLowStockAlerts(Long warehouseId);
}
