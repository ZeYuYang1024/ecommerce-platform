package com.ecommerce.warehouse.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.common.outbox.OutboxService;
import com.ecommerce.warehouse.common.OutboundStatus;
import com.ecommerce.warehouse.common.WarehouseErrorCode;
import com.ecommerce.warehouse.dto.request.CreateOutboundRequest;
import com.ecommerce.warehouse.dto.response.OutboundOrderVO;
import com.ecommerce.warehouse.entity.OutboundOrder;
import com.ecommerce.warehouse.entity.OutboundOrderItem;
import com.ecommerce.warehouse.entity.Warehouse;
import com.ecommerce.warehouse.mapper.OutboundOrderItemMapper;
import com.ecommerce.warehouse.mapper.OutboundOrderMapper;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboundServiceImplTest {

    @Mock
    private OutboundOrderMapper outboundOrderMapper;

    @Mock
    private OutboundOrderItemMapper outboundOrderItemMapper;

    @Mock
    private WarehouseMapper warehouseMapper;

    @Mock
    private StockService stockService;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private OutboundServiceImpl service;

    private Warehouse warehouse;
    private OutboundOrder outboundOrder;
    private OutboundOrderItem outboundItem;

    @BeforeEach
    void setUp() {
        warehouse = new Warehouse();
        warehouse.setId(1L);
        warehouse.setWarehouseCode("GZ001");
        warehouse.setStockMode("MANAGED");
        warehouse.setStatus(1);

        outboundOrder = new OutboundOrder();
        outboundOrder.setId(20L);
        outboundOrder.setOutboundNo("OUT202606200001");
        outboundOrder.setWarehouseId(1L);
        outboundOrder.setOutboundType("SALES");
        outboundOrder.setShippingId(50L);
        outboundOrder.setStatus(OutboundStatus.PENDING);
        outboundOrder.setMerchantId(100L);
        outboundOrder.setRemark("测试出库单");

        outboundItem = new OutboundOrderItem();
        outboundItem.setId(200L);
        outboundItem.setOutboundId(20L);
        outboundItem.setSkuId(300L);
        outboundItem.setQuantity(10);
        outboundItem.setPickedQty(0);
        outboundItem.setShippedQty(0);
        outboundItem.setBinId(10L);
    }

    // ======================== Query ========================

    @Nested
    class ListOutboundsTests {

        @Test
        void shouldListOutboundsWithPagination() {
            Page<OutboundOrder> mockPage = new Page<>(1, 10, 1);
            mockPage.setRecords(Collections.singletonList(outboundOrder));
            when(outboundOrderMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(mockPage);
            when(outboundOrderItemMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(outboundItem));

            IPage<OutboundOrderVO> result = service.listOutbounds(1, 10, null, null);

            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getTotal()).isEqualTo(1);
            assertThat(result.getRecords().get(0).getOutboundNo()).isEqualTo("OUT202606200001");
            assertThat(result.getRecords().get(0).getStatusText()).isEqualTo("待拣货");
        }

        @Test
        void shouldReturnEmptyPage() {
            Page<OutboundOrder> mockPage = new Page<>(1, 10, 0);
            mockPage.setRecords(Collections.emptyList());
            when(outboundOrderMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(mockPage);

            IPage<OutboundOrderVO> result = service.listOutbounds(1, 10, null, null);

            assertThat(result.getRecords()).isEmpty();
            assertThat(result.getTotal()).isZero();
        }
    }

    @Nested
    class GetOutboundTests {

        @Test
        void shouldGetOutboundById() {
            when(outboundOrderMapper.selectById(20L)).thenReturn(outboundOrder);
            when(outboundOrderItemMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(outboundItem));

            OutboundOrderVO result = service.getOutbound(20L);

            assertThat(result.getId()).isEqualTo(20L);
            assertThat(result.getOutboundNo()).isEqualTo("OUT202606200001");
            assertThat(result.getItems()).hasSize(1);
        }

        @Test
        void shouldThrowWhenOutboundNotFound() {
            when(outboundOrderMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> service.getOutbound(999L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(WarehouseErrorCode.OUTBOUND_NOT_FOUND.getCode());
        }
    }

    // ======================== createOutbound ========================

    @Nested
    class CreateOutboundTests {

        @Test
        void shouldCreateOutboundAndLockStock() {
            when(warehouseMapper.selectById(1L)).thenReturn(warehouse);
            when(outboundOrderMapper.insert(any(OutboundOrder.class))).thenReturn(1);
            when(outboundOrderItemMapper.insert(any(OutboundOrderItem.class))).thenReturn(1);
            when(outboundOrderItemMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of());

            CreateOutboundRequest req = buildCreateRequest();

            OutboundOrderVO result = service.createOutbound(req);

            assertThat(result.getOutboundNo()).isNotNull();
            assertThat(result.getOutboundNo()).startsWith("OUT");
            assertThat(result.getStatus()).isEqualTo(OutboundStatus.PENDING);
            verify(stockService, times(1)).lockStock(eq(1L), eq(300L), eq(10));
            verify(outboundOrderMapper).insert(any(OutboundOrder.class));
            verify(outboundOrderItemMapper).insert(any(OutboundOrderItem.class));
        }

        @Test
        void shouldRejectWhenWarehouseNotFound() {
            when(warehouseMapper.selectById(999L)).thenReturn(null);

            CreateOutboundRequest req = buildCreateRequest();
            req.setWarehouseId(999L);

            assertThatThrownBy(() -> service.createOutbound(req))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(WarehouseErrorCode.WAREHOUSE_NOT_FOUND.getCode());

            verify(stockService, never()).lockStock(anyLong(), anyLong(), anyInt());
        }

        @Test
        void shouldRejectWhenNotManagedWarehouse() {
            warehouse.setStockMode("SELF");
            when(warehouseMapper.selectById(1L)).thenReturn(warehouse);

            CreateOutboundRequest req = buildCreateRequest();

            assertThatThrownBy(() -> service.createOutbound(req))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(WarehouseErrorCode.NOT_MANAGED_WAREHOUSE.getCode());

            verify(stockService, never()).lockStock(anyLong(), anyLong(), anyInt());
        }
    }

    // ======================== startPicking ========================

    @Nested
    class StartPickingTests {

        @Test
        void shouldStartPicking() {
            when(outboundOrderMapper.selectById(20L)).thenReturn(outboundOrder);
            when(outboundOrderMapper.updateById(any(OutboundOrder.class))).thenReturn(1);

            service.startPicking(20L);

            assertThat(outboundOrder.getStatus()).isEqualTo(OutboundStatus.PICKING);
            verify(outboundOrderMapper).updateById(outboundOrder);
        }

        @Test
        void shouldRejectStartPickingWhenNotPending() {
            outboundOrder.setStatus(OutboundStatus.SHIPPED);
            when(outboundOrderMapper.selectById(20L)).thenReturn(outboundOrder);

            assertThatThrownBy(() -> service.startPicking(20L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(WarehouseErrorCode.INVALID_STATUS_TRANSITION.getCode());

            verify(outboundOrderMapper, never()).updateById(any(OutboundOrder.class));
        }
    }

    // ======================== confirmShipped ========================

    @Nested
    class ConfirmShippedTests {

        @Test
        void shouldConfirmShipped() {
            outboundOrder.setStatus(OutboundStatus.PICKING);
            when(outboundOrderMapper.selectById(20L)).thenReturn(outboundOrder);
            when(outboundOrderMapper.updateById(any(OutboundOrder.class))).thenReturn(1);
            when(outboundOrderItemMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(outboundItem));
            when(outboundOrderItemMapper.updateById(any(OutboundOrderItem.class))).thenReturn(1);

            service.confirmShipped(20L);

            assertThat(outboundOrder.getStatus()).isEqualTo(OutboundStatus.SHIPPED);
            assertThat(outboundItem.getShippedQty()).isEqualTo(10);
            verify(stockService, times(1)).deductStock(eq(1L), eq(300L), eq(10));
            verify(outboundOrderMapper).updateById(outboundOrder);
            verify(outboxService).enqueue(
                    eq("outbound_order"), anyString(),
                    eq("outbound-shipped"), any());
        }

        @Test
        void shouldRejectConfirmShippedWhenNotPicking() {
            outboundOrder.setStatus(OutboundStatus.PENDING);
            when(outboundOrderMapper.selectById(20L)).thenReturn(outboundOrder);

            assertThatThrownBy(() -> service.confirmShipped(20L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(WarehouseErrorCode.INVALID_STATUS_TRANSITION.getCode());

            verify(stockService, never()).deductStock(anyLong(), anyLong(), anyInt());
            verify(outboxService, never()).enqueue(anyString(), anyString(), anyString(), any());
        }
    }

    // ======================== Invalid Status Transitions ========================

    @Nested
    class InvalidTransitionsTests {

        @Test
        void shouldRejectStartPickingWhenAlreadyPicking() {
            outboundOrder.setStatus(OutboundStatus.PICKING);
            when(outboundOrderMapper.selectById(20L)).thenReturn(outboundOrder);

            assertThatThrownBy(() -> service.startPicking(20L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(WarehouseErrorCode.INVALID_STATUS_TRANSITION.getCode());
        }

        @Test
        void shouldRejectConfirmShippedWhenAlreadyShipped() {
            outboundOrder.setStatus(OutboundStatus.SHIPPED);
            when(outboundOrderMapper.selectById(20L)).thenReturn(outboundOrder);

            assertThatThrownBy(() -> service.confirmShipped(20L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(WarehouseErrorCode.INVALID_STATUS_TRANSITION.getCode());
        }
    }

    // --- helpers ---

    private CreateOutboundRequest buildCreateRequest() {
        CreateOutboundRequest req = new CreateOutboundRequest();
        req.setWarehouseId(1L);
        req.setOutboundType("SALES");
        req.setShippingId(50L);
        req.setMerchantId(100L);
        req.setRemark("测试出库单");

        CreateOutboundRequest.OutboundItem item = new CreateOutboundRequest.OutboundItem();
        item.setSkuId(300L);
        item.setQuantity(10);
        item.setBinId(10L);
        req.setItems(List.of(item));
        return req;
    }
}
