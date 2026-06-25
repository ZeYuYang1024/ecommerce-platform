package com.ecommerce.warehouse.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.ecommerce.common.constant.WarehouseStockMode;
import com.ecommerce.common.outbox.OutboxService;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.warehouse.common.WarehouseErrorCode;
import com.ecommerce.warehouse.dto.response.PhysicalStockVO;
import com.ecommerce.warehouse.entity.PhysicalStock;
import com.ecommerce.warehouse.entity.Warehouse;
import com.ecommerce.warehouse.mapper.PhysicalStockMapper;
import com.ecommerce.warehouse.mapper.WarehouseMapper;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
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
        managedWarehouse.setStockMode(WarehouseStockMode.MANAGED);
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

    @Test
    void shouldAddStockExistingRecord() {
        when(warehouseMapper.selectById(1L)).thenReturn(managedWarehouse);
        when(physicalStockMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(stock);
        when(physicalStockMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);
        when(physicalStockMapper.selectById(100L)).thenReturn(stock);

        service.addStock(1L, 200L, 10L, 20);

        verify(physicalStockMapper).update(isNull(), any(UpdateWrapper.class));
        verify(outboxService).enqueue(eq("physical_stock"), anyString(), eq("warehouse-stock-changed"), any());
    }

    @Test
    void shouldRejectAddStockForNonManagedWarehouse() {
        managedWarehouse.setStockMode(WarehouseStockMode.LIGHT);
        when(warehouseMapper.selectById(1L)).thenReturn(managedWarehouse);

        assertThatThrownBy(() -> service.addStock(1L, 200L, 10L, 10))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                .isEqualTo(WarehouseErrorCode.NOT_MANAGED_WAREHOUSE.getCode());
    }

    @Test
    void shouldQueryStockByWarehouseAndSku() {
        when(physicalStockMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(stock));

        List<PhysicalStockVO> result = service.queryStock(1L, 200L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAvailableQty()).isEqualTo(50);
    }

    @Test
    void shouldReturnEmptyListWhenNoStock() {
        when(physicalStockMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        assertThat(service.queryStock(1L, 999L)).isEmpty();
    }
}
