package com.ecommerce.warehouse.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.constant.WarehouseStockMode;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.warehouse.common.InboundStatus;
import com.ecommerce.warehouse.common.InboundType;
import com.ecommerce.warehouse.common.WarehouseErrorCode;
import com.ecommerce.warehouse.dto.request.CreateInboundRequest;
import com.ecommerce.warehouse.dto.response.InboundOrderVO;
import com.ecommerce.warehouse.entity.InboundOrder;
import com.ecommerce.warehouse.entity.InboundOrderItem;
import com.ecommerce.warehouse.entity.Warehouse;
import com.ecommerce.warehouse.mapper.InboundOrderItemMapper;
import com.ecommerce.warehouse.mapper.InboundOrderMapper;
import com.ecommerce.warehouse.mapper.WarehouseMapper;
import com.ecommerce.warehouse.service.StockService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InboundServiceImplTest {

    @Mock
    private InboundOrderMapper inboundOrderMapper;
    @Mock
    private InboundOrderItemMapper inboundOrderItemMapper;
    @Mock
    private WarehouseMapper warehouseMapper;
    @Mock
    private StockService stockService;

    @InjectMocks
    private InboundServiceImpl service;

    private Warehouse warehouse;
    private InboundOrder inboundOrder;
    private InboundOrderItem inboundItem;

    @BeforeEach
    void setUp() {
        warehouse = new Warehouse();
        warehouse.setId(1L);
        warehouse.setWarehouseCode("GZ001");
        warehouse.setStockMode(WarehouseStockMode.MANAGED);
        warehouse.setStatus(1);

        inboundOrder = new InboundOrder();
        inboundOrder.setId(10L);
        inboundOrder.setInboundNo("IN202606200001");
        inboundOrder.setWarehouseId(1L);
        inboundOrder.setInboundType(InboundType.PURCHASE);
        inboundOrder.setSourceOrderNo("PO20260601");
        inboundOrder.setStatus(InboundStatus.PENDING);
        inboundOrder.setMerchantId(100L);

        inboundItem = new InboundOrderItem();
        inboundItem.setId(100L);
        inboundItem.setInboundId(10L);
        inboundItem.setSkuId(200L);
        inboundItem.setQuantity(30);
        inboundItem.setReceivedQty(0);
        inboundItem.setBinId(5L);
    }

    @Test
    void shouldListInboundsWithPagination() {
        Page<InboundOrder> mockPage = new Page<>(1, 10, 1);
        mockPage.setRecords(Collections.singletonList(inboundOrder));
        when(inboundOrderMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);
        when(inboundOrderItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(inboundItem));

        IPage<InboundOrderVO> result = service.listInbounds(1, 10, null, null);

        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getInboundType()).isEqualTo(InboundType.PURCHASE);
        assertThat(result.getRecords().get(0).getInboundTypeText()).isEqualTo("采购入库");
    }

    @Test
    void shouldCreateInbound() {
        when(warehouseMapper.selectById(1L)).thenReturn(warehouse);
        when(inboundOrderMapper.insert(any(InboundOrder.class))).thenReturn(1);
        when(inboundOrderItemMapper.insert(any(InboundOrderItem.class))).thenReturn(1);
        when(inboundOrderItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        InboundOrderVO result = service.createInbound(buildCreateRequest());

        assertThat(result.getStatus()).isEqualTo(InboundStatus.PENDING);
    }

    @Test
    void shouldConfirmShelved() {
        inboundOrder.setStatus(InboundStatus.RECEIVED);
        when(inboundOrderMapper.selectById(10L)).thenReturn(inboundOrder);
        when(inboundOrderItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(inboundItem));
        when(inboundOrderMapper.updateById(any(InboundOrder.class))).thenReturn(1);

        service.confirmShelved(10L);

        assertThat(inboundOrder.getStatus()).isEqualTo(InboundStatus.SHELVED);
        verify(stockService, times(1)).addStock(eq(1L), eq(200L), eq(5L), eq(30));
    }

    @Test
    void shouldRejectWhenWarehouseNotFound() {
        when(warehouseMapper.selectById(999L)).thenReturn(null);

        CreateInboundRequest req = buildCreateRequest();
        req.setWarehouseId(999L);

        assertThatThrownBy(() -> service.createInbound(req))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                .isEqualTo(WarehouseErrorCode.WAREHOUSE_NOT_FOUND.getCode());
    }

    private CreateInboundRequest buildCreateRequest() {
        CreateInboundRequest req = new CreateInboundRequest();
        req.setWarehouseId(1L);
        req.setInboundType(InboundType.PURCHASE);
        req.setSourceOrderNo("PO20260601");
        req.setMerchantId(100L);

        CreateInboundRequest.InboundItem item = new CreateInboundRequest.InboundItem();
        item.setSkuId(200L);
        item.setQuantity(30);
        req.setItems(List.of(item));
        return req;
    }
}
