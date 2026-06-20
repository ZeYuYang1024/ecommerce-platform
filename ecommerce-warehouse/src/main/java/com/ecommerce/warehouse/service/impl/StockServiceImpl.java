package com.ecommerce.warehouse.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.ecommerce.common.dto.WarehouseStockChangedMessage;
import com.ecommerce.common.outbox.OutboxService;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.warehouse.common.WarehouseErrorCode;
import com.ecommerce.warehouse.dto.response.PhysicalStockVO;
import com.ecommerce.warehouse.entity.PhysicalStock;
import com.ecommerce.warehouse.entity.Warehouse;
import com.ecommerce.warehouse.mapper.PhysicalStockMapper;
import com.ecommerce.warehouse.mapper.WarehouseMapper;
import com.ecommerce.warehouse.service.StockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class StockServiceImpl implements StockService {

    private final PhysicalStockMapper physicalStockMapper;
    private final WarehouseMapper warehouseMapper;
    private final OutboxService outboxService;

    public StockServiceImpl(PhysicalStockMapper physicalStockMapper,
                            WarehouseMapper warehouseMapper,
                            OutboxService outboxService) {
        this.physicalStockMapper = physicalStockMapper;
        this.warehouseMapper = warehouseMapper;
        this.outboxService = outboxService;
    }

    // ======================== Query ========================

    @Override
    public List<PhysicalStockVO> queryStock(Long warehouseId, Long skuId) {
        LambdaQueryWrapper<PhysicalStock> wrapper = new LambdaQueryWrapper<PhysicalStock>()
                .eq(PhysicalStock::getWarehouseId, warehouseId)
                .eq(PhysicalStock::getSkuId, skuId);
        List<PhysicalStock> list = physicalStockMapper.selectList(wrapper);
        return list.stream().map(this::toVO).toList();
    }

    @Override
    public List<PhysicalStockVO> getLowStockAlerts(Long warehouseId) {
        // Fetch all stock with safetyStock > 0 and filter in Java (column-to-column
        // comparison not supported by MyBatis-Plus LambdaQueryWrapper).
        List<PhysicalStock> all = physicalStockMapper.selectList(
                new LambdaQueryWrapper<PhysicalStock>()
                        .eq(PhysicalStock::getWarehouseId, warehouseId)
                        .gt(PhysicalStock::getSafetyStock, 0));
        List<PhysicalStock> alerts = new ArrayList<>();
        for (PhysicalStock stock : all) {
            if (stock.getAvailableQty() != null
                    && stock.getSafetyStock() != null
                    && stock.getAvailableQty() <= stock.getSafetyStock()) {
                alerts.add(stock);
            }
        }
        return alerts.stream().map(this::toVO).toList();
    }

    // ======================== Mutations ========================

    @Override
    @Transactional
    public void addStock(Long warehouseId, Long skuId, Long binId, int quantity) {
        validateManagedWarehouse(warehouseId);

        // Find existing stock record for this warehouse+sku+bin
        PhysicalStock stock = physicalStockMapper.selectOne(
                new LambdaQueryWrapper<PhysicalStock>()
                        .eq(PhysicalStock::getWarehouseId, warehouseId)
                        .eq(PhysicalStock::getSkuId, skuId)
                        .eq(PhysicalStock::getBinId, binId));

        if (stock != null) {
            // Atomic UPDATE: add quantity directly at DB level
            UpdateWrapper<PhysicalStock> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", stock.getId())
                    .setSql("quantity = quantity + " + quantity)
                    .setSql("available_qty = available_qty + " + quantity);

            int rows = physicalStockMapper.update(null, updateWrapper);
            if (rows == 0) {
                throw new BusinessException(WarehouseErrorCode.STOCK_CONCURRENT_UPDATE);
            }
            // Re-select for accurate outbox message
            stock = physicalStockMapper.selectById(stock.getId());
        } else {
            // Create new record
            stock = new PhysicalStock();
            stock.setWarehouseId(warehouseId);
            stock.setSkuId(skuId);
            stock.setBinId(binId);
            stock.setQuantity(quantity);
            stock.setLockedQty(0);
            stock.setAvailableQty(quantity);
            stock.setSafetyStock(0);
            physicalStockMapper.insert(stock);
        }

        publishStockChanged(stock, "ADD", quantity);
    }

    @Override
    @Transactional
    public void lockStock(Long warehouseId, Long skuId, int quantity) {
        validateManagedWarehouse(warehouseId);

        List<PhysicalStock> stocks = physicalStockMapper.selectList(
                new LambdaQueryWrapper<PhysicalStock>()
                        .eq(PhysicalStock::getWarehouseId, warehouseId)
                        .eq(PhysicalStock::getSkuId, skuId)
                        .gt(PhysicalStock::getAvailableQty, 0));

        // Fast-fail pre-check: sum available across all bins
        int totalAvailable = stocks.stream()
                .mapToInt(s -> s.getAvailableQty() != null ? s.getAvailableQty() : 0)
                .sum();

        if (totalAvailable < quantity) {
            throw new BusinessException(WarehouseErrorCode.INSUFFICIENT_STOCK);
        }

        int remaining = quantity;
        for (PhysicalStock stock : stocks) {
            if (remaining <= 0) break;
            int available = stock.getAvailableQty() != null ? stock.getAvailableQty() : 0;
            int take = Math.min(available, remaining);
            if (take <= 0) continue;

            // Atomic UPDATE: check available_qty >= take at DB level, then lock
            UpdateWrapper<PhysicalStock> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", stock.getId())
                    .ge("available_qty", take)
                    .setSql("locked_qty = locked_qty + " + take)
                    .setSql("available_qty = available_qty - " + take);

            int rows = physicalStockMapper.update(null, updateWrapper);
            if (rows == 0) {
                // Either concurrent modification or stock was taken by another tx.
                // @Transactional ensures previous bin updates in this tx roll back.
                throw new BusinessException(WarehouseErrorCode.STOCK_CONCURRENT_UPDATE);
            }

            // Re-select for accurate outbox message
            PhysicalStock updated = physicalStockMapper.selectById(stock.getId());
            publishStockChanged(updated, "LOCK", take);
            remaining -= take;
        }

        if (remaining > 0) {
            throw new BusinessException(WarehouseErrorCode.STOCK_LOCK_FAILED);
        }
    }

    @Override
    @Transactional
    public void deductStock(Long warehouseId, Long skuId, int quantity) {
        List<PhysicalStock> stocks = physicalStockMapper.selectList(
                new LambdaQueryWrapper<PhysicalStock>()
                        .eq(PhysicalStock::getWarehouseId, warehouseId)
                        .eq(PhysicalStock::getSkuId, skuId)
                        .gt(PhysicalStock::getLockedQty, 0));

        int totalLocked = stocks.stream()
                .mapToInt(s -> s.getLockedQty() != null ? s.getLockedQty() : 0)
                .sum();

        if (totalLocked < quantity) {
            throw new BusinessException(WarehouseErrorCode.INSUFFICIENT_STOCK);
        }

        int remaining = quantity;
        for (PhysicalStock stock : stocks) {
            if (remaining <= 0) break;
            int locked = stock.getLockedQty() != null ? stock.getLockedQty() : 0;
            int take = Math.min(locked, remaining);
            if (take <= 0) continue;

            // Atomic UPDATE: check locked_qty >= take at DB level, then deduct
            UpdateWrapper<PhysicalStock> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", stock.getId())
                    .ge("locked_qty", take)
                    .setSql("quantity = quantity - " + take)
                    .setSql("locked_qty = locked_qty - " + take);

            int rows = physicalStockMapper.update(null, updateWrapper);
            if (rows == 0) {
                throw new BusinessException(WarehouseErrorCode.STOCK_CONCURRENT_UPDATE);
            }

            PhysicalStock updated = physicalStockMapper.selectById(stock.getId());
            publishStockChanged(updated, "DEDUCT", take);
            remaining -= take;
        }

        if (remaining > 0) {
            throw new BusinessException(WarehouseErrorCode.STOCK_LOCK_FAILED);
        }
    }

    @Override
    @Transactional
    public void releaseStock(Long warehouseId, Long skuId, int quantity) {
        List<PhysicalStock> stocks = physicalStockMapper.selectList(
                new LambdaQueryWrapper<PhysicalStock>()
                        .eq(PhysicalStock::getWarehouseId, warehouseId)
                        .eq(PhysicalStock::getSkuId, skuId)
                        .gt(PhysicalStock::getLockedQty, 0));

        int totalLocked = stocks.stream()
                .mapToInt(s -> s.getLockedQty() != null ? s.getLockedQty() : 0)
                .sum();

        if (totalLocked < quantity) {
            throw new BusinessException(WarehouseErrorCode.INSUFFICIENT_STOCK);
        }

        int remaining = quantity;
        for (PhysicalStock stock : stocks) {
            if (remaining <= 0) break;
            int locked = stock.getLockedQty() != null ? stock.getLockedQty() : 0;
            int take = Math.min(locked, remaining);
            if (take <= 0) continue;

            // Atomic UPDATE: check locked_qty >= take at DB level, then release
            UpdateWrapper<PhysicalStock> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", stock.getId())
                    .ge("locked_qty", take)
                    .setSql("locked_qty = locked_qty - " + take)
                    .setSql("available_qty = available_qty + " + take);

            int rows = physicalStockMapper.update(null, updateWrapper);
            if (rows == 0) {
                throw new BusinessException(WarehouseErrorCode.STOCK_CONCURRENT_UPDATE);
            }

            PhysicalStock updated = physicalStockMapper.selectById(stock.getId());
            publishStockChanged(updated, "RELEASE", take);
            remaining -= take;
        }

        if (remaining > 0) {
            throw new BusinessException(WarehouseErrorCode.STOCK_LOCK_FAILED);
        }
    }

    // ======================== Private helpers ========================

    private void validateManagedWarehouse(Long warehouseId) {
        Warehouse warehouse = warehouseMapper.selectById(warehouseId);
        if (warehouse == null) {
            throw new BusinessException(WarehouseErrorCode.WAREHOUSE_NOT_FOUND);
        }
        if (warehouse.getStatus() != null && warehouse.getStatus() != 1) {
            throw new BusinessException(WarehouseErrorCode.WAREHOUSE_DISABLED);
        }
        if (!"MANAGED".equals(warehouse.getStockMode())) {
            throw new BusinessException(WarehouseErrorCode.NOT_MANAGED_WAREHOUSE);
        }
    }

    private void publishStockChanged(PhysicalStock stock, String changeType, int changeQty) {
        WarehouseStockChangedMessage message = new WarehouseStockChangedMessage();
        message.setSkuId(stock.getSkuId());
        message.setWarehouseId(stock.getWarehouseId());
        message.setPhysicalQty(stock.getQuantity());
        message.setAvailableQty(stock.getAvailableQty());
        message.setLockedQty(stock.getLockedQty());
        message.setChangeType(changeType);
        message.setChangeQty(changeQty);
        message.setOccurredAt(LocalDateTime.now());

        outboxService.enqueue("physical_stock", String.valueOf(stock.getId()),
                "warehouse-stock-changed", message);
    }

    private PhysicalStockVO toVO(PhysicalStock entity) {
        PhysicalStockVO vo = new PhysicalStockVO();
        vo.setId(entity.getId());
        vo.setWarehouseId(entity.getWarehouseId());
        vo.setSkuId(entity.getSkuId());
        vo.setBinId(entity.getBinId());
        vo.setQuantity(entity.getQuantity());
        vo.setLockedQty(entity.getLockedQty());
        vo.setAvailableQty(entity.getAvailableQty());
        vo.setSafetyStock(entity.getSafetyStock());
        return vo;
    }
}
