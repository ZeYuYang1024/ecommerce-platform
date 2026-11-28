package com.ecommerce.warehouse.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.ecommerce.common.constant.WarehouseStockMode;
import com.ecommerce.common.dto.WarehouseStockChangedMessage;
import com.ecommerce.common.outbox.OutboxService;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.warehouse.common.WarehouseErrorCode;
import com.ecommerce.warehouse.dto.response.PhysicalStockVO;
import com.ecommerce.warehouse.entity.PhysicalStock;
import com.ecommerce.warehouse.entity.Warehouse;
import com.ecommerce.warehouse.mapper.PhysicalStockMapper;
import com.ecommerce.warehouse.mapper.OutboundStockAllocationMapper;
import com.ecommerce.warehouse.mapper.OutboundStockAllocationMapper.Allocation;
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
    private final OutboundStockAllocationMapper allocations;
    private final WarehouseMapper warehouseMapper;
    private final OutboxService outboxService;

    public StockServiceImpl(OutboundStockAllocationMapper allocations, PhysicalStockMapper physicalStockMapper,
                            WarehouseMapper warehouseMapper,
                            OutboxService outboxService) {
        this.physicalStockMapper = physicalStockMapper;
        this.allocations = allocations;
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
    public void reconcileCount(Long warehouseId, Long skuId, Long binId, int expected, int actual) {
        validateManagedWarehouse(warehouseId);
        PhysicalStock stock = physicalStockMapper.selectOne(new LambdaQueryWrapper<PhysicalStock>()
                .eq(PhysicalStock::getWarehouseId, warehouseId).eq(PhysicalStock::getSkuId, skuId)
                .eq(PhysicalStock::getBinId, binId).last("FOR UPDATE"));
        if (stock == null || stock.getQuantity() != expected || actual < stock.getLockedQty()) {
            throw new BusinessException(WarehouseErrorCode.CHECK_STOCK_CHANGED);
        }
        stock.setQuantity(actual);
        stock.setAvailableQty(actual - stock.getLockedQty());
        physicalStockMapper.updateById(stock);
        publishStockChanged(stock, "ADJUST", actual - expected);
    }

    @Override
    @Transactional
    public void allocateOutboundStock(Long warehouseId, Long itemId, Long skuId, Long binId, int quantity) {
        if (quantity <= 0) throw new BusinessException(WarehouseErrorCode.STOCK_LOCK_FAILED);
        List<PhysicalStock> stocks = physicalStockMapper.selectList(new LambdaQueryWrapper<PhysicalStock>()
                .eq(PhysicalStock::getWarehouseId, warehouseId)
                .eq(PhysicalStock::getSkuId, skuId)
                .eq(binId != null, PhysicalStock::getBinId, binId)
                .gt(PhysicalStock::getAvailableQty, 0)
                .orderByAsc(PhysicalStock::getId)
                .last("FOR UPDATE"));

        int remaining = quantity;
        for (PhysicalStock stock : stocks) {
            if (remaining == 0) break;
            int take = Math.min(stock.getAvailableQty(), remaining);
            int changed = physicalStockMapper.update(null, new UpdateWrapper<PhysicalStock>()
                    .eq("id", stock.getId()).ge("available_qty", take)
                    .setSql("locked_qty = locked_qty + " + take)
                    .setSql("available_qty = available_qty - " + take));
            if (changed != 1) throw new BusinessException(WarehouseErrorCode.STOCK_CONCURRENT_UPDATE);
            allocations.insert(itemId, stock.getId(), take);
            publishStockChanged(physicalStockMapper.selectById(stock.getId()), "LOCK", take);
            remaining -= take;
        }
        if (remaining > 0) throw new BusinessException(WarehouseErrorCode.INSUFFICIENT_STOCK);
    }

    @Override
    @Transactional
    public void settleOutboundStock(Long itemId, int quantity, boolean shipped) {
        List<Allocation> rows = outboundAllocations(itemId);
        if (rows.stream().mapToInt(Allocation::quantity).sum() != quantity) {
            throw new BusinessException(WarehouseErrorCode.OUTBOUND_ALLOCATION_INCOMPLETE);
        }
        for (Allocation allocation : rows) {
            int take = allocation.quantity();
            int changed = physicalStockMapper.update(null, new UpdateWrapper<PhysicalStock>()
                    .eq("id", allocation.stockId()).ge("locked_qty", take)
                    .ge("quantity", take)
                    .setSql("locked_qty = locked_qty - " + take)
                    .setSql(shipped ? "quantity = quantity - " + take : "available_qty = available_qty + " + take));
            if (changed != 1) throw new BusinessException(WarehouseErrorCode.STOCK_CONCURRENT_UPDATE);
            publishStockChanged(physicalStockMapper.selectById(allocation.stockId()), shipped ? "DEDUCT" : "RELEASE", take);
        }
    }

    @Override
    public List<Allocation> outboundAllocations(Long itemId) {
        return allocations.findByItem(itemId);
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
        if (!java.util.Objects.equals(warehouse.getStockMode(), WarehouseStockMode.MANAGED)) {
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
