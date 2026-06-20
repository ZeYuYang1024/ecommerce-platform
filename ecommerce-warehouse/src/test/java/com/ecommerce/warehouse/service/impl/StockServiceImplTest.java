package com.ecommerce.warehouse.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.common.outbox.OutboxService;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.warehouse.common.WarehouseErrorCode;
import com.ecommerce.warehouse.dto.response.PhysicalStockVO;
import com.ecommerce.warehouse.entity.PhysicalStock;
import com.ecommerce.warehouse.entity.Warehouse;
import com.ecommerce.warehouse.mapper.PhysicalStockMapper;
import com.ecommerce.warehouse.mapper.WarehouseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockServiceImplTest {

    @Mock
    private PhysicalStockMapper physicalStockMapper;

    @Mock
    private WarehouseMapper warehouseMapper;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private StockServiceImpl service;

    private Warehouse managedWarehouse;
    private PhysicalStock stock;

    @BeforeEach
    void setUp() {
        managedWarehouse = new Warehouse();
        managedWarehouse.setId(1L);
        managedWarehouse.setWarehouseCode("GZ001");
        managedWarehouse.setStockMode("MANAGED");
        managedWarehouse.setStatus(1);

        stock = new PhysicalStock();
        stock.setId(100L);
        stock.setWarehouseId(1L);
        stock.setSkuId(200L);
        stock.setBinId(10L);
        stock.setQuantity(50);
        stock.setLockedQty(0);
        stock.setAvailableQty(50);
        stock.setSafetyStock(10);
    }

    // ======================== Query ========================

    @Nested
    class QueryStockTests {

        @Test
        void shouldQueryStockByWarehouseAndSku() {
            when(physicalStockMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(stock));

            List<PhysicalStockVO> result = service.queryStock(1L, 200L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSkuId()).isEqualTo(200L);
            assertThat(result.get(0).getQuantity()).isEqualTo(50);
            assertThat(result.get(0).getAvailableQty()).isEqualTo(50);
        }

        @Test
        void shouldReturnEmptyListWhenNoStock() {
            when(physicalStockMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());

            List<PhysicalStockVO> result = service.queryStock(1L, 999L);

            assertThat(result).isEmpty();
        }
    }

    // ======================== addStock ========================

    @Nested
    class AddStockTests {

        @Test
        void shouldAddStockNewRecord() {
            when(warehouseMapper.selectById(1L)).thenReturn(managedWarehouse);
            when(physicalStockMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(physicalStockMapper.insert(any(PhysicalStock.class))).thenReturn(1);

            service.addStock(1L, 300L, 10L, 30);

            verify(physicalStockMapper).insert(any(PhysicalStock.class));
            verify(outboxService).enqueue(
                    eq("physical_stock"), anyString(),
                    eq("warehouse-stock-changed"), any());
        }

        @Test
        void shouldAddStockExistingRecord() {
            when(warehouseMapper.selectById(1L)).thenReturn(managedWarehouse);
            when(physicalStockMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(stock);
            when(physicalStockMapper.updateById(any(PhysicalStock.class))).thenReturn(1);

            service.addStock(1L, 200L, 10L, 20);

            assertThat(stock.getQuantity()).isEqualTo(70);
            assertThat(stock.getAvailableQty()).isEqualTo(70);
            verify(physicalStockMapper).updateById(stock);
            verify(physicalStockMapper, never()).insert(any(PhysicalStock.class));
            verify(outboxService).enqueue(
                    eq("physical_stock"), anyString(),
                    eq("warehouse-stock-changed"), any());
        }

        @Test
        void shouldRejectAddStockForNonManagedWarehouse() {
            managedWarehouse.setStockMode("SELF");
            when(warehouseMapper.selectById(1L)).thenReturn(managedWarehouse);

            assertThatThrownBy(() -> service.addStock(1L, 200L, 10L, 10))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(WarehouseErrorCode.NOT_MANAGED_WAREHOUSE.getCode());

            verify(physicalStockMapper, never()).insert(any(PhysicalStock.class));
            verify(physicalStockMapper, never()).updateById(any(PhysicalStock.class));
        }

        @Test
        void shouldRejectAddStockForDisabledWarehouse() {
            managedWarehouse.setStatus(0);
            when(warehouseMapper.selectById(1L)).thenReturn(managedWarehouse);

            assertThatThrownBy(() -> service.addStock(1L, 200L, 10L, 10))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(WarehouseErrorCode.WAREHOUSE_DISABLED.getCode());
        }
    }

    // ======================== lockStock ========================

    @Nested
    class LockStockTests {

        @Test
        void shouldLockStockSufficient() {
            when(warehouseMapper.selectById(1L)).thenReturn(managedWarehouse);
            when(physicalStockMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(stock));
            when(physicalStockMapper.updateById(any(PhysicalStock.class))).thenReturn(1);

            service.lockStock(1L, 200L, 20);

            assertThat(stock.getLockedQty()).isEqualTo(20);
            assertThat(stock.getAvailableQty()).isEqualTo(30);
            verify(physicalStockMapper).updateById(stock);
            verify(outboxService).enqueue(
                    eq("physical_stock"), anyString(),
                    eq("warehouse-stock-changed"), any());
        }

        @Test
        void shouldLockStockFromMultipleBins() {
            PhysicalStock stock2 = new PhysicalStock();
            stock2.setId(101L);
            stock2.setWarehouseId(1L);
            stock2.setSkuId(200L);
            stock2.setBinId(11L);
            stock2.setQuantity(10);
            stock2.setLockedQty(0);
            stock2.setAvailableQty(10);
            stock2.setSafetyStock(0);

            when(warehouseMapper.selectById(1L)).thenReturn(managedWarehouse);
            when(physicalStockMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(stock, stock2));
            when(physicalStockMapper.updateById(any(PhysicalStock.class))).thenReturn(1);

            // Request 55 units: 50 from first bin, 5 from second
            service.lockStock(1L, 200L, 55);

            assertThat(stock.getLockedQty()).isEqualTo(50);
            assertThat(stock.getAvailableQty()).isEqualTo(0);
            assertThat(stock2.getLockedQty()).isEqualTo(5);
            assertThat(stock2.getAvailableQty()).isEqualTo(5);
            verify(physicalStockMapper, times(2)).updateById(any(PhysicalStock.class));
            verify(outboxService, times(2)).enqueue(
                    eq("physical_stock"), anyString(),
                    eq("warehouse-stock-changed"), any());
        }

        @Test
        void shouldThrowWhenInsufficientStock() {
            when(warehouseMapper.selectById(1L)).thenReturn(managedWarehouse);
            when(physicalStockMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(stock));

            assertThatThrownBy(() -> service.lockStock(1L, 200L, 100))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(WarehouseErrorCode.INSUFFICIENT_STOCK.getCode());

            verify(physicalStockMapper, never()).updateById(any(PhysicalStock.class));
        }
    }

    // ======================== deductStock ========================

    @Nested
    class DeductStockTests {

        @Test
        void shouldDeductStock() {
            stock.setLockedQty(30);
            stock.setQuantity(50);
            when(physicalStockMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(stock));
            when(physicalStockMapper.updateById(any(PhysicalStock.class))).thenReturn(1);

            service.deductStock(1L, 200L, 20);

            assertThat(stock.getQuantity()).isEqualTo(30);
            assertThat(stock.getLockedQty()).isEqualTo(10);
            // availableQty unchanged
            verify(physicalStockMapper).updateById(stock);
            verify(outboxService).enqueue(
                    eq("physical_stock"), anyString(),
                    eq("warehouse-stock-changed"), any());
        }

        @Test
        void shouldThrowWhenInsufficientLockedStock() {
            stock.setLockedQty(5);
            when(physicalStockMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(stock));

            assertThatThrownBy(() -> service.deductStock(1L, 200L, 20))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(WarehouseErrorCode.INSUFFICIENT_STOCK.getCode());

            verify(physicalStockMapper, never()).updateById(any(PhysicalStock.class));
        }
    }

    // ======================== releaseStock ========================

    @Nested
    class ReleaseStockTests {

        @Test
        void shouldReleaseStock() {
            stock.setLockedQty(20);
            stock.setAvailableQty(30);
            when(physicalStockMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(stock));
            when(physicalStockMapper.updateById(any(PhysicalStock.class))).thenReturn(1);

            service.releaseStock(1L, 200L, 10);

            assertThat(stock.getLockedQty()).isEqualTo(10);
            assertThat(stock.getAvailableQty()).isEqualTo(40);
            verify(physicalStockMapper).updateById(stock);
            verify(outboxService).enqueue(
                    eq("physical_stock"), anyString(),
                    eq("warehouse-stock-changed"), any());
        }

        @Test
        void shouldThrowWhenInsufficientLockedForRelease() {
            stock.setLockedQty(5);
            when(physicalStockMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(stock));

            assertThatThrownBy(() -> service.releaseStock(1L, 200L, 20))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(WarehouseErrorCode.INSUFFICIENT_STOCK.getCode());
        }
    }

    // ======================== getLowStockAlerts ========================

    @Nested
    class LowStockAlertsTests {

        @Test
        void shouldReturnLowStockAlerts() {
            PhysicalStock lowStock = new PhysicalStock();
            lowStock.setId(101L);
            lowStock.setWarehouseId(1L);
            lowStock.setSkuId(300L);
            lowStock.setBinId(10L);
            lowStock.setQuantity(5);
            lowStock.setLockedQty(0);
            lowStock.setAvailableQty(5);
            lowStock.setSafetyStock(10); // availableQty (5) <= safetyStock (10) = LOW

            when(physicalStockMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(lowStock));

            List<PhysicalStockVO> result = service.getLowStockAlerts(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSkuId()).isEqualTo(300L);
            assertThat(result.get(0).getAvailableQty()).isEqualTo(5);
        }

        @Test
        void shouldExcludeStockAboveSafetyLevel() {
            // availableQty (50) > safetyStock (10) = NOT low
            when(physicalStockMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(stock));

            List<PhysicalStockVO> result = service.getLowStockAlerts(1L);

            assertThat(result).isEmpty();
        }

        @Test
        void shouldReturnEmptyWhenNoStockHasSafetyStock() {
            when(physicalStockMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());

            List<PhysicalStockVO> result = service.getLowStockAlerts(1L);

            assertThat(result).isEmpty();
        }

        @Test
        void shouldVerifyAvailableQtyConsistency() {
            // availableQty = quantity - lockedQty
            stock.setQuantity(100);
            stock.setLockedQty(30);
            stock.setAvailableQty(70);
            when(physicalStockMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(stock));

            List<PhysicalStockVO> result = service.queryStock(1L, 200L);

            assertThat(result).hasSize(1);
            PhysicalStockVO vo = result.get(0);
            assertThat(vo.getAvailableQty()).isEqualTo(vo.getQuantity() - vo.getLockedQty());
        }
    }
}
