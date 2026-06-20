package com.ecommerce.warehouse.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.warehouse.common.InboundStatus;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
        warehouse.setStockMode("MANAGED");
        warehouse.setStatus(1);

        inboundOrder = new InboundOrder();
        inboundOrder.setId(10L);
        inboundOrder.setInboundNo("IN202606200001");
        inboundOrder.setWarehouseId(1L);
        inboundOrder.setInboundType("PURCHASE");
        inboundOrder.setSourceOrderNo("PO20260601");
        inboundOrder.setStatus(InboundStatus.PENDING);
        inboundOrder.setMerchantId(100L);
        inboundOrder.setRemark("测试入库单");

        inboundItem = new InboundOrderItem();
        inboundItem.setId(100L);
        inboundItem.setInboundId(10L);
        inboundItem.setSkuId(200L);
        inboundItem.setQuantity(30);
        inboundItem.setReceivedQty(0);
        inboundItem.setBinId(5L);
    }

    // ======================== Query ========================

    @Nested
    class ListInboundsTests {

        @Test
        void shouldListInboundsWithPagination() {
            Page<InboundOrder> mockPage = new Page<>(1, 10, 1);
            mockPage.setRecords(Collections.singletonList(inboundOrder));
            when(inboundOrderMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(mockPage);
            when(inboundOrderItemMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(inboundItem));

            IPage<InboundOrderVO> result = service.listInbounds(1, 10, null, null);

            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getTotal()).isEqualTo(1);
            assertThat(result.getRecords().get(0).getInboundNo()).isEqualTo("IN202606200001");
            assertThat(result.getRecords().get(0).getStatusText()).isEqualTo("待收货");
        }

        @Test
        void shouldReturnEmptyPage() {
            Page<InboundOrder> mockPage = new Page<>(1, 10, 0);
            mockPage.setRecords(Collections.emptyList());
            when(inboundOrderMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(mockPage);

            IPage<InboundOrderVO> result = service.listInbounds(1, 10, null, null);

            assertThat(result.getRecords()).isEmpty();
            assertThat(result.getTotal()).isZero();
        }
    }

    @Nested
    class GetInboundTests {

        @Test
        void shouldGetInboundById() {
            when(inboundOrderMapper.selectById(10L)).thenReturn(inboundOrder);
            when(inboundOrderItemMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(inboundItem));

            InboundOrderVO result = service.getInbound(10L);

            assertThat(result.getId()).isEqualTo(10L);
            assertThat(result.getInboundNo()).isEqualTo("IN202606200001");
            assertThat(result.getItems()).hasSize(1);
        }

        @Test
        void shouldThrowWhenInboundNotFound() {
            when(inboundOrderMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> service.getInbound(999L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(WarehouseErrorCode.INBOUND_NOT_FOUND.getCode());
        }
    }

    // ======================== createInbound ========================

    @Nested
    class CreateInboundTests {

        @Test
        void shouldCreateInbound() {
            when(warehouseMapper.selectById(1L)).thenReturn(warehouse);
            when(inboundOrderMapper.insert(any(InboundOrder.class))).thenReturn(1);
            when(inboundOrderItemMapper.insert(any(InboundOrderItem.class))).thenReturn(1);
            when(inboundOrderItemMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of());

            CreateInboundRequest req = buildCreateRequest("PURCHASE", 30);

            InboundOrderVO result = service.createInbound(req);

            assertThat(result.getInboundNo()).isNotNull();
            assertThat(result.getInboundNo()).startsWith("IN");
            assertThat(result.getWarehouseId()).isEqualTo(1L);
            assertThat(result.getStatus()).isEqualTo(InboundStatus.PENDING);
            verify(inboundOrderMapper).insert(any(InboundOrder.class));
            verify(inboundOrderItemMapper).insert(any(InboundOrderItem.class));
        }

        @Test
        void shouldRejectWhenWarehouseNotFound() {
            when(warehouseMapper.selectById(999L)).thenReturn(null);

            CreateInboundRequest req = buildCreateRequest("PURCHASE", 30);
            req.setWarehouseId(999L);

            assertThatThrownBy(() -> service.createInbound(req))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(WarehouseErrorCode.WAREHOUSE_NOT_FOUND.getCode());
        }

        @Test
        void shouldRejectWhenWarehouseDisabled() {
            warehouse.setStatus(0);
            when(warehouseMapper.selectById(1L)).thenReturn(warehouse);

            CreateInboundRequest req = buildCreateRequest("PURCHASE", 30);

            assertThatThrownBy(() -> service.createInbound(req))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(WarehouseErrorCode.WAREHOUSE_DISABLED.getCode());
        }
    }

    // ======================== confirmReceived ========================

    @Nested
    class ConfirmReceivedTests {

        @Test
        void shouldConfirmReceived() {
            when(inboundOrderMapper.selectById(10L)).thenReturn(inboundOrder);
            when(inboundOrderMapper.updateById(any(InboundOrder.class))).thenReturn(1);
            when(inboundOrderItemMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(inboundItem));
            when(inboundOrderItemMapper.updateById(any(InboundOrderItem.class))).thenReturn(1);

            service.confirmReceived(10L);

            assertThat(inboundOrder.getStatus()).isEqualTo(InboundStatus.RECEIVED);
            assertThat(inboundItem.getReceivedQty()).isEqualTo(30);
            verify(inboundOrderMapper).updateById(inboundOrder);
            verify(inboundOrderItemMapper).updateById(inboundItem);
        }

        @Test
        void shouldRejectConfirmReceivedWhenNotPending() {
            inboundOrder.setStatus(InboundStatus.RECEIVED);
            when(inboundOrderMapper.selectById(10L)).thenReturn(inboundOrder);

            assertThatThrownBy(() -> service.confirmReceived(10L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(WarehouseErrorCode.INVALID_STATUS_TRANSITION.getCode());

            verify(inboundOrderMapper, never()).updateById(any(InboundOrder.class));
        }
    }

    // ======================== confirmShelved ========================

    @Nested
    class ConfirmShelvedTests {

        @Test
        void shouldConfirmShelved() {
            inboundOrder.setStatus(InboundStatus.RECEIVED);
            when(inboundOrderMapper.selectById(10L)).thenReturn(inboundOrder);
            when(inboundOrderMapper.updateById(any(InboundOrder.class))).thenReturn(1);
            when(inboundOrderItemMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(inboundItem));

            service.confirmShelved(10L);

            assertThat(inboundOrder.getStatus()).isEqualTo(InboundStatus.SHELVED);
            verify(stockService, times(1)).addStock(
                    eq(1L), eq(200L), eq(5L), eq(30));
            verify(inboundOrderMapper).updateById(inboundOrder);
        }

        @Test
        void shouldRejectConfirmShelvedWhenNotReceived() {
            inboundOrder.setStatus(InboundStatus.PENDING);
            when(inboundOrderMapper.selectById(10L)).thenReturn(inboundOrder);

            assertThatThrownBy(() -> service.confirmShelved(10L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(WarehouseErrorCode.INVALID_STATUS_TRANSITION.getCode());

            verify(stockService, never()).addStock(anyLong(), anyLong(), anyLong(), anyInt());
        }

        @Test
        void shouldRejectWhenItemHasNoBin() {
            inboundOrder.setStatus(InboundStatus.RECEIVED);
            inboundItem.setBinId(null);
            when(inboundOrderMapper.selectById(10L)).thenReturn(inboundOrder);
            when(inboundOrderItemMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(inboundItem));

            assertThatThrownBy(() -> service.confirmShelved(10L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(WarehouseErrorCode.BIN_NOT_FOUND.getCode());
        }
    }

    // ======================== Invalid Status Transitions ========================

    @Nested
    class InvalidTransitionsTests {

        @Test
        void shouldRejectConfirmShelvedWhenAlreadyShelved() {
            inboundOrder.setStatus(InboundStatus.SHELVED);
            when(inboundOrderMapper.selectById(10L)).thenReturn(inboundOrder);

            assertThatThrownBy(() -> service.confirmShelved(10L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(WarehouseErrorCode.INVALID_STATUS_TRANSITION.getCode());
        }
    }

    // --- helpers ---

    private CreateInboundRequest buildCreateRequest(String inboundType, int quantity) {
        CreateInboundRequest req = new CreateInboundRequest();
        req.setWarehouseId(1L);
        req.setInboundType(inboundType);
        req.setSourceOrderNo("PO20260601");
        req.setMerchantId(100L);
        req.setRemark("测试入库单");

        CreateInboundRequest.InboundItem item = new CreateInboundRequest.InboundItem();
        item.setSkuId(200L);
        item.setQuantity(quantity);
        req.setItems(List.of(item));
        return req;
    }
}
