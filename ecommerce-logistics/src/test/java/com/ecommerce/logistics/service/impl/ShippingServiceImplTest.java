package com.ecommerce.logistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.constant.WarehouseStockMode;
import com.ecommerce.common.dto.OrderDeliveredMessage;
import com.ecommerce.common.dto.OrderInternalVO;
import com.ecommerce.common.outbox.OutboxService;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.logistics.client.OrderClient;
import com.ecommerce.logistics.client.WarehouseClient;
import com.ecommerce.logistics.client.dto.WarehouseInfoVO;
import com.ecommerce.logistics.common.FulfillmentStatus;
import com.ecommerce.logistics.common.LogisticsErrorCode;
import com.ecommerce.logistics.common.ShippingStatus;
import com.ecommerce.logistics.dto.request.CreateShippingRequest;
import com.ecommerce.logistics.dto.response.FulfillmentSummaryVO;
import com.ecommerce.logistics.dto.response.ShippingOrderVO;
import com.ecommerce.logistics.dto.response.TrackingVO;
import com.ecommerce.logistics.entity.LogisticsProvider;
import com.ecommerce.logistics.entity.ShippingOrder;
import com.ecommerce.logistics.entity.ShippingOrderItem;
import com.ecommerce.logistics.entity.TrackingRecord;
import com.ecommerce.logistics.mapper.LogisticsProviderMapper;
import com.ecommerce.logistics.mapper.ShippingOrderItemMapper;
import com.ecommerce.logistics.mapper.ShippingOrderMapper;
import com.ecommerce.logistics.mapper.TrackingRecordMapper;
import com.ecommerce.logistics.provider.AggregationProvider;
import com.ecommerce.logistics.provider.dto.TrackingQueryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShippingServiceImplTest {

    @Mock
    private ShippingOrderMapper shippingOrderMapper;

    @Mock
    private ShippingOrderItemMapper shippingOrderItemMapper;

    @Mock
    private LogisticsProviderMapper providerMapper;

    @Mock
    private TrackingRecordMapper trackingRecordMapper;

    @Mock
    private OutboxService outboxService;

    @Mock
    private AggregationProvider aggregationProvider;

    @Mock
    private WarehouseClient warehouseClient;

    @Mock
    private OrderClient orderClient;

    @InjectMocks
    private ShippingServiceImpl service;

    private LogisticsProvider provider;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "trackingCacheMinutes", 30);
        ReflectionTestUtils.setField(service, "jsonMapper", JsonMapper.builder().build());
        provider = new LogisticsProvider();
        provider.setId(1L);
        provider.setProviderCode("SF");
        provider.setProviderName("顺丰速运");
        provider.setStatus(1);
    }

    @Nested
    class CreateShippingTests {

        @Test
        void shouldCreateShippingOrder() {
            CreateShippingRequest req = buildCreateRequest();
            when(shippingOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(orderClient.getShippingSnapshot(1001L)).thenReturn(com.ecommerce.common.result.Result.ok(orderSnapshot("ORD202606200001", 1, 9001L, itemSnapshot(100L, 200L, 1))));
            when(providerMapper.selectById(1L)).thenReturn(provider);
            when(shippingOrderMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(false);
            when(shippingOrderItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
            when(shippingOrderMapper.insert(any(ShippingOrder.class))).thenReturn(1);
            when(shippingOrderItemMapper.insert(any(ShippingOrderItem.class))).thenReturn(1);

            ShippingOrderVO vo = service.createShipping(req, "admin", null);

            assertThat(vo.getShippingNo()).isNotNull();
            assertThat(vo.getShippingNo()).startsWith("SH");
            assertThat(vo.getTrackingNo()).isEqualTo("SF1234567890");
            assertThat(vo.getOrderId()).isEqualTo(1001L);
            assertThat(vo.getOrderNo()).isEqualTo("ORD202606200001");
            assertThat(vo.getShippingStatus()).isEqualTo(ShippingStatus.DISPATCHED);
            verify(shippingOrderMapper).insert(any(ShippingOrder.class));
            verify(shippingOrderItemMapper).insert(any(ShippingOrderItem.class));
            verify(outboxService).enqueue(
                    eq("shipping"),
                    anyString(),
                    eq("shipping-dispatched"),
                    argThat(payload -> {
                        if (!(payload instanceof com.ecommerce.common.dto.ShippingDispatchedMessage msg)) {
                            return false;
                        }
                        return Long.valueOf(9001L).equals(msg.getUserId());
                    }));
        }

        @Test
        void shouldCreateOutboundForManagedWarehouse() {
            CreateShippingRequest req = buildCreateRequest();
            req.setWarehouseId(9L);
            when(shippingOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(orderClient.getShippingSnapshot(1001L)).thenReturn(com.ecommerce.common.result.Result.ok(orderSnapshot(
                    "ORD202606200001", 1, 9001L, itemSnapshot(100L, 200L, 1, 700L))));
            when(providerMapper.selectById(1L)).thenReturn(provider);
            when(shippingOrderMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(false);
            when(shippingOrderItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
            when(shippingOrderMapper.insert(any(ShippingOrder.class))).thenReturn(1);
            when(shippingOrderItemMapper.insert(any(ShippingOrderItem.class))).thenReturn(1);
            when(warehouseClient.getWarehouse(9L)).thenReturn(com.ecommerce.common.result.Result.ok(managedWarehouse(9L)));
            when(warehouseClient.createOutbound(any()))
                    .thenReturn(com.ecommerce.common.result.Result.ok(new com.ecommerce.logistics.client.dto.OutboundOrderVO()));

            ShippingOrderVO vo = service.createShipping(req, "admin", null);

            assertThat(vo.getShippingStatus()).isEqualTo(ShippingStatus.PENDING);
            verify(shippingOrderMapper).insert(argThat((ShippingOrder order) ->
                    Long.valueOf(700L).equals(order.getMerchantId())));
            verify(warehouseClient).createOutbound(argThat(request ->
                    Long.valueOf(700L).equals(request.getMerchantId())));
            verify(outboxService, never()).enqueue(eq("shipping"), anyString(), eq("shipping-dispatched"), any());
        }

        @Test
        void shouldDispatchImmediatelyForSelfManagedWarehouse() {
            CreateShippingRequest req = buildCreateRequest();
            req.setWarehouseId(10L);
            when(shippingOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(orderClient.getShippingSnapshot(1001L)).thenReturn(com.ecommerce.common.result.Result.ok(orderSnapshot("ORD202606200001", 1, 9001L, itemSnapshot(100L, 200L, 1))));
            when(providerMapper.selectById(1L)).thenReturn(provider);
            when(shippingOrderMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(false);
            when(shippingOrderItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
            when(shippingOrderMapper.insert(any(ShippingOrder.class))).thenReturn(1);
            when(shippingOrderItemMapper.insert(any(ShippingOrderItem.class))).thenReturn(1);
            when(warehouseClient.getWarehouse(10L)).thenReturn(com.ecommerce.common.result.Result.ok(selfWarehouse(10L)));

            ShippingOrderVO vo = service.createShipping(req, "admin", null);

            assertThat(vo.getShippingStatus()).isEqualTo(ShippingStatus.DISPATCHED);
            verify(warehouseClient, never()).createOutbound(any());
            verify(outboxService).enqueue(eq("shipping"), anyString(), eq("shipping-dispatched"), any());
        }

        @Test
        void shouldReturnExistingShippingForIdempotentRequest() {
            CreateShippingRequest req = buildCreateRequest();
            ShippingOrder existing = new ShippingOrder();
            existing.setId(10L);
            existing.setShippingNo("SH202606201200000001");
            existing.setOrderId(1001L);
            existing.setTrackingNo("SF1234567890");
            existing.setProviderId(1L);
            existing.setProviderCode("SF");
            existing.setShippingStatus(ShippingStatus.DISPATCHED);

            when(shippingOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

            ShippingOrderVO vo = service.createShipping(req, "admin", null);

            assertThat(vo.getShippingNo()).isEqualTo("SH202606201200000001");
            verify(shippingOrderMapper, never()).insert(any(ShippingOrder.class));
            verify(outboxService, never()).enqueue(anyString(), anyString(), anyString(), any());
        }

        @Test
        void shouldRejectWhenProviderNotFound() {
            CreateShippingRequest req = buildCreateRequest();
            when(shippingOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(providerMapper.selectById(1L)).thenReturn(null);

            assertThatThrownBy(() -> service.createShipping(req, "admin", null))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(LogisticsErrorCode.PROVIDER_NOT_FOUND.getCode());
        }

        @Test
        void shouldRejectWhenProviderDisabled() {
            CreateShippingRequest req = buildCreateRequest();
            provider.setStatus(0);
            when(shippingOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(providerMapper.selectById(1L)).thenReturn(provider);

            assertThatThrownBy(() -> service.createShipping(req, "admin", null))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(LogisticsErrorCode.PROVIDER_NOT_FOUND.getCode());
        }

        @Test
        void shouldRejectDuplicateTrackingNo() {
            CreateShippingRequest req = buildCreateRequest();
            when(shippingOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(providerMapper.selectById(1L)).thenReturn(provider);
            when(shippingOrderMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(true);

            assertThatThrownBy(() -> service.createShipping(req, "admin", null))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(LogisticsErrorCode.SHIPPING_DUPLICATE.getCode());
        }

        @Test
        void shouldRejectWhenItemsAlreadyShipped() {
            CreateShippingRequest req = buildCreateRequest();
            when(shippingOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(providerMapper.selectById(1L)).thenReturn(provider);
            when(shippingOrderMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(false);

            ShippingOrderItem alreadyShipped = new ShippingOrderItem();
            alreadyShipped.setOrderItemId(100L);
            when(shippingOrderItemMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(alreadyShipped));

            assertThatThrownBy(() -> service.createShipping(req, "admin", null))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(LogisticsErrorCode.QUANTITY_EXCEEDS_ORDER.getCode());

            verify(shippingOrderMapper, never()).insert(any(ShippingOrder.class));
        }

        @Test
        void shouldRejectWhenOrderIsNotPaid() {
            CreateShippingRequest req = buildCreateRequest();
            when(shippingOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(providerMapper.selectById(1L)).thenReturn(provider);
            when(shippingOrderMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(false);
            when(shippingOrderItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
            when(orderClient.getShippingSnapshot(1001L)).thenReturn(com.ecommerce.common.result.Result.ok(orderSnapshot("ORD202606200001", 0, 9001L, itemSnapshot(100L, 200L, 1))));

            assertThatThrownBy(() -> service.createShipping(req, "admin", null))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(LogisticsErrorCode.ORDER_NOT_PAID.getCode());
        }

        @Test
        void shouldCreateShippingWithMerchantId() {
            CreateShippingRequest req = buildCreateRequest();
            when(shippingOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(orderClient.getShippingSnapshot(1001L)).thenReturn(com.ecommerce.common.result.Result.ok(orderSnapshot("ORD202606200001", 1, 9001L, itemSnapshot(100L, 200L, 1))));
            when(providerMapper.selectById(1L)).thenReturn(provider);
            when(shippingOrderMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(false);
            when(shippingOrderItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
            when(shippingOrderMapper.insert(any(ShippingOrder.class))).thenReturn(1);
            when(shippingOrderItemMapper.insert(any(ShippingOrderItem.class))).thenReturn(1);

            ShippingOrderVO vo = service.createShipping(req, "merchant", 500L);

            assertThat(vo.getShippingNo()).isNotNull();
            verify(shippingOrderMapper).insert(argThat((ShippingOrder order) ->
                    Long.valueOf(500L).equals(order.getMerchantId())));
        }

        @Test
        void shouldRejectWhenWarehouseBelongsToDifferentMerchant() {
            CreateShippingRequest req = buildCreateRequest();
            req.setWarehouseId(9L);
            when(shippingOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(orderClient.getShippingSnapshot(1001L)).thenReturn(com.ecommerce.common.result.Result.ok(orderSnapshot("ORD202606200001", 1, 9001L, itemSnapshot(100L, 200L, 1))));
            when(providerMapper.selectById(1L)).thenReturn(provider);
            when(shippingOrderMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(false);
            when(shippingOrderItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
            when(warehouseClient.getWarehouse(9L)).thenReturn(com.ecommerce.common.result.Result.ok(warehouse(9L, WarehouseStockMode.MANAGED, 999L)));

            assertThatThrownBy(() -> service.createShipping(req, "merchant", 500L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(LogisticsErrorCode.WAREHOUSE_FORBIDDEN.getCode());

            verify(shippingOrderMapper, never()).insert(any(ShippingOrder.class));
        }

        @Test
        void shouldRejectWhenOrderBelongsToDifferentMerchant() {
            CreateShippingRequest req = buildCreateRequest();
            when(shippingOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(orderClient.getShippingSnapshot(1001L)).thenReturn(com.ecommerce.common.result.Result.ok(orderSnapshot(
                    "ORD202606200001", 1, 9001L, itemSnapshot(100L, 200L, 1, 700L))));
            when(providerMapper.selectById(1L)).thenReturn(provider);
            when(shippingOrderMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(false);
            when(shippingOrderItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> service.createShipping(req, "merchant", 500L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(LogisticsErrorCode.SHIPPING_FORBIDDEN.getCode());

            verify(shippingOrderMapper, never()).insert(any(ShippingOrder.class));
        }

        @Test
        void shouldCreateShippingWithoutItemsList() {
            CreateShippingRequest req = buildCreateRequest();
            req.setItems(Collections.emptyList());
            when(shippingOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(orderClient.getShippingSnapshot(1001L)).thenReturn(com.ecommerce.common.result.Result.ok(orderSnapshot("ORD202606200001", 1, 9001L, itemSnapshot(100L, 200L, 1))));
            when(providerMapper.selectById(1L)).thenReturn(provider);
            when(shippingOrderMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(false);
            when(shippingOrderItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
            when(shippingOrderMapper.insert(any(ShippingOrder.class))).thenReturn(1);

            ShippingOrderVO vo = service.createShipping(req, "admin", null);

            assertThat(vo.getShippingNo()).isNotNull();
            verify(shippingOrderItemMapper, never()).insert(any(ShippingOrderItem.class));
        }

        @Test
        void shouldDefaultPackageWeightToZeroWhenNull() {
            CreateShippingRequest req = buildCreateRequest();
            req.setPackageWeight(null);
            when(shippingOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(orderClient.getShippingSnapshot(1001L)).thenReturn(com.ecommerce.common.result.Result.ok(orderSnapshot("ORD202606200001", 1, 9001L, itemSnapshot(100L, 200L, 1))));
            when(providerMapper.selectById(1L)).thenReturn(provider);
            when(shippingOrderMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(false);
            when(shippingOrderItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
            when(shippingOrderMapper.insert(any(ShippingOrder.class))).thenReturn(1);

            ShippingOrderVO vo = service.createShipping(req, "admin", null);

            assertThat(vo.getPackageWeight()).isZero();
        }

        @Test
        void shouldBuildBatchShippingFromOrderSnapshot() {
            com.ecommerce.logistics.dto.request.BatchShipRequest request = new com.ecommerce.logistics.dto.request.BatchShipRequest();
            com.ecommerce.logistics.dto.request.BatchShipRequest.BatchShipItem item = new com.ecommerce.logistics.dto.request.BatchShipRequest.BatchShipItem();
            item.setOrderId(1001L);
            item.setProviderId(1L);
            item.setTrackingNo("SF1234567890");
            item.setPackageWeight(500);
            request.setItems(List.of(item));

            when(shippingOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(orderClient.getShippingSnapshot(1001L)).thenReturn(com.ecommerce.common.result.Result.ok(orderSnapshot(
                    "ORD202606200001",
                    1,
                    9001L,
                    itemSnapshot(100L, 200L, 1),
                    itemSnapshot(101L, 201L, 2))));
            when(providerMapper.selectById(1L)).thenReturn(provider);
            when(shippingOrderMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(false);
            when(shippingOrderItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
            when(shippingOrderMapper.insert(any(ShippingOrder.class))).thenReturn(1);
            when(shippingOrderItemMapper.insert(any(ShippingOrderItem.class))).thenReturn(1);

            List<ShippingOrderVO> result = service.batchShip(request, "admin", null);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getOrderNo()).isEqualTo("ORD202606200001");
            verify(shippingOrderItemMapper, org.mockito.Mockito.times(2)).insert(any(ShippingOrderItem.class));
        }
    }

    @Nested
    class FulfillmentSummaryTests {

        @Test
        void shouldReturnWaitingShipForOrderWithNoShipping() {
            when(shippingOrderMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());

            List<FulfillmentSummaryVO> summaries = service.getFulfillmentSummary(List.of(2001L));

            assertThat(summaries).hasSize(1);
            FulfillmentSummaryVO summary = summaries.get(0);
            assertThat(summary.getOrderId()).isEqualTo(2001L);
            assertThat(summary.getFulfillmentStatus()).isEqualTo(FulfillmentStatus.WAITING_SHIP);
            assertThat(summary.getShippingCount()).isZero();
            assertThat(summary.getDeliveredCount()).isZero();
        }

        @Test
        void shouldReturnDispatchedForShippedOrders() {
            ShippingOrder order = buildShippingOrder(1L, ShippingStatus.DISPATCHED);
            when(shippingOrderMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(order));

            List<FulfillmentSummaryVO> summaries = service.getFulfillmentSummary(List.of(1001L));

            assertThat(summaries).hasSize(1);
            assertThat(summaries.get(0).getFulfillmentStatus()).isEqualTo(FulfillmentStatus.DISPATCHED);
            assertThat(summaries.get(0).getShippingCount()).isEqualTo(1);
            assertThat(summaries.get(0).getDeliveredCount()).isZero();
        }

        @Test
        void shouldReturnDeliveredForAllSignedOrders() {
            ShippingOrder order = buildShippingOrder(1L, ShippingStatus.SIGNED);
            when(shippingOrderMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(order));

            List<FulfillmentSummaryVO> summaries = service.getFulfillmentSummary(List.of(1001L));

            assertThat(summaries.get(0).getFulfillmentStatus()).isEqualTo(FulfillmentStatus.DELIVERED);
            assertThat(summaries.get(0).getDeliveredCount()).isEqualTo(1);
        }

        @Test
        void shouldReturnExceptionWhenAnyShipmentHasException() {
            ShippingOrder normal = buildShippingOrder(1L, ShippingStatus.DISPATCHED);
            ShippingOrder exception = buildShippingOrder(2L, ShippingStatus.EXCEPTION);
            exception.setOrderId(1001L);
            when(shippingOrderMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(normal, exception));

            List<FulfillmentSummaryVO> summaries = service.getFulfillmentSummary(List.of(1001L));

            assertThat(summaries.get(0).getFulfillmentStatus()).isEqualTo(FulfillmentStatus.EXCEPTION);
            assertThat(summaries.get(0).getShippingCount()).isEqualTo(2);
        }

        @Test
        void shouldReturnPartiallyDispatchedWhenNotAllShipped() {
            ShippingOrder dispatched = buildShippingOrder(1L, ShippingStatus.DISPATCHED);
            ShippingOrder pending = buildShippingOrder(2L, ShippingStatus.PENDING);
            pending.setOrderId(1001L);
            when(shippingOrderMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(dispatched, pending));

            List<FulfillmentSummaryVO> summaries = service.getFulfillmentSummary(List.of(1001L));

            assertThat(summaries.get(0).getFulfillmentStatus()).isEqualTo(FulfillmentStatus.PARTIALLY_DISPATCHED);
        }

        @Test
        void shouldHandleMultipleOrderIds() {
            ShippingOrder order1 = buildShippingOrder(1L, ShippingStatus.DISPATCHED);
            ShippingOrder order2 = buildShippingOrder(2L, ShippingStatus.SIGNED);
            order2.setOrderId(2002L);
            when(shippingOrderMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(order1, order2));

            List<FulfillmentSummaryVO> summaries = service.getFulfillmentSummary(List.of(1001L, 2002L));

            assertThat(summaries).hasSize(2);
            assertThat(summaries.get(0).getFulfillmentStatus()).isEqualTo(FulfillmentStatus.DISPATCHED);
            assertThat(summaries.get(1).getFulfillmentStatus()).isEqualTo(FulfillmentStatus.DELIVERED);
        }

        @Test
        void shouldReturnEmptyListForNullInput() {
            List<FulfillmentSummaryVO> summaries = service.getFulfillmentSummary(null);
            assertThat(summaries).isEmpty();
        }

        @Test
        void shouldReturnEmptyListForEmptyInput() {
            List<FulfillmentSummaryVO> summaries = service.getFulfillmentSummary(Collections.emptyList());
            assertThat(summaries).isEmpty();
        }

        @Test
        void shouldIncludeLatestTraceInfo() {
            ShippingOrder order = buildShippingOrder(1L, ShippingStatus.DISPATCHED);
            order.setLastTraceDesc("已到达目的地");
            order.setLastTraceTime(LocalDateTime.of(2026, 6, 20, 14, 0));
            when(shippingOrderMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(order));

            List<FulfillmentSummaryVO> summaries = service.getFulfillmentSummary(List.of(1001L));

            assertThat(summaries.get(0).getLatestTraceDesc()).isEqualTo("已到达目的地");
            assertThat(summaries.get(0).getLatestTraceTime()).isNotNull();
        }
    }

    @Nested
    class GetTrackingTests {

        @Test
        void shouldReturnTrackingWithLocalRecords() {
            ShippingOrder order = buildShippingOrder(1L, ShippingStatus.IN_TRANSIT);
            when(shippingOrderMapper.selectById(1L)).thenReturn(order);

            TrackingRecord record = new TrackingRecord();
            record.setTraceTime(LocalDateTime.now().minusHours(1));
            record.setTraceDesc("已揽收");
            record.setTraceStatus("3");
            record.setLocation("深圳");
            when(trackingRecordMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(record));
            when(providerMapper.selectById(1L)).thenReturn(provider);

            TrackingVO vo = service.getTracking(1L, null);

            assertThat(vo.getTrackingNo()).isEqualTo("SF1234567890");
            assertThat(vo.getShippingNo()).isEqualTo("SH202606200001");
            assertThat(vo.getProviderName()).isEqualTo("顺丰速运");
            assertThat(vo.getTracks()).hasSize(1);
            assertThat(vo.getTracks().get(0).getDesc()).isEqualTo("已揽收");
        }

        @Test
        void shouldThrowWhenShippingNotFoundForTracking() {
            when(shippingOrderMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> service.getTracking(999L, null))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(LogisticsErrorCode.SHIPPING_NOT_FOUND.getCode());
        }

        @Test
        void shouldRefreshTrackingWhenLocalRecordsStale() {
            ShippingOrder order = buildShippingOrder(1L, ShippingStatus.IN_TRANSIT);
            when(shippingOrderMapper.selectById(1L)).thenReturn(order);

            TrackingRecord staleRecord = new TrackingRecord();
            staleRecord.setTraceTime(LocalDateTime.now().minusHours(2));
            staleRecord.setTraceDesc("已揽收");
            when(trackingRecordMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(staleRecord));

            when(trackingRecordMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(false);
            when(trackingRecordMapper.insert(org.mockito.ArgumentMatchers.<TrackingRecord>any())).thenReturn(1);
            when(providerMapper.selectById(1L)).thenReturn(provider);

            TrackingQueryResponse resp = new TrackingQueryResponse();
            resp.setSuccess(true);
            TrackingQueryResponse.TraceItem trace = new TrackingQueryResponse.TraceItem();
            trace.setTime(LocalDateTime.now().minusMinutes(30));
            trace.setDesc("运输中");
            trace.setStatus("5");
            trace.setLocation("广州");
            resp.setTraces(List.of(trace));
            when(aggregationProvider.queryTracking(anyString(), anyString())).thenReturn(resp);

            TrackingVO vo = service.getTracking(1L, null);

            assertThat(vo.getTracks()).isNotEmpty();
            verify(trackingRecordMapper, atLeastOnce()).insert(any(TrackingRecord.class));
        }

        @Test
        void shouldQueryTrackingWithAggregationCodeWhenAvailable() {
            ShippingOrder order = buildShippingOrder(1L, ShippingStatus.IN_TRANSIT);
            when(shippingOrderMapper.selectById(1L)).thenReturn(order);

            TrackingRecord staleRecord = new TrackingRecord();
            staleRecord.setTraceTime(LocalDateTime.now().minusHours(2));
            staleRecord.setTraceDesc("宸叉徑鏀?");
            when(trackingRecordMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(staleRecord));

            provider.setAggregationCode("SF_KDN");
            when(providerMapper.selectById(1L)).thenReturn(provider);

            TrackingQueryResponse resp = new TrackingQueryResponse();
            resp.setSuccess(true);
            resp.setTraces(Collections.emptyList());
            when(aggregationProvider.queryTracking(anyString(), anyString())).thenReturn(resp);

            service.getTracking(1L, null);

            verify(aggregationProvider).queryTracking("SF1234567890", "SF_KDN");
        }

        @Test
        void shouldRefreshTrackingWhenNoLocalRecords() {
            ShippingOrder order = buildShippingOrder(1L, ShippingStatus.IN_TRANSIT);
            when(shippingOrderMapper.selectById(1L)).thenReturn(order);
            when(trackingRecordMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());
            when(providerMapper.selectById(1L)).thenReturn(provider);

            TrackingQueryResponse resp = new TrackingQueryResponse();
            resp.setSuccess(true);
            resp.setTraces(Collections.emptyList());
            when(aggregationProvider.queryTracking(anyString(), anyString())).thenReturn(resp);

            TrackingVO vo = service.getTracking(1L, null);

            assertThat(vo.getTracks()).isEmpty();
        }

        @Test
        void shouldSkipDuplicateTraceRecords() {
            ShippingOrder order = buildShippingOrder(1L, ShippingStatus.IN_TRANSIT);
            when(shippingOrderMapper.selectById(1L)).thenReturn(order);

            TrackingRecord staleRecord = new TrackingRecord();
            staleRecord.setTraceTime(LocalDateTime.now().minusHours(2));
            staleRecord.setTraceDesc("已揽收");
            when(trackingRecordMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(staleRecord));

            when(trackingRecordMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(true);
            when(providerMapper.selectById(1L)).thenReturn(provider);

            TrackingQueryResponse resp = new TrackingQueryResponse();
            resp.setSuccess(true);
            TrackingQueryResponse.TraceItem trace = new TrackingQueryResponse.TraceItem();
            trace.setTime(LocalDateTime.now().minusMinutes(30));
            trace.setDesc("运输中");
            trace.setStatus("5");
            resp.setTraces(List.of(trace));
            when(aggregationProvider.queryTracking(anyString(), anyString())).thenReturn(resp);

            TrackingVO vo = service.getTracking(1L, null);

            verify(trackingRecordMapper, never()).insert(any(TrackingRecord.class));
        }

        @Test
        void shouldHandleProviderQueryFailureGracefully() {
            ShippingOrder order = buildShippingOrder(1L, ShippingStatus.IN_TRANSIT);
            when(shippingOrderMapper.selectById(1L)).thenReturn(order);

            TrackingRecord staleRecord = new TrackingRecord();
            staleRecord.setTraceTime(LocalDateTime.now().minusHours(2));
            staleRecord.setTraceDesc("已揽收");
            when(trackingRecordMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(staleRecord));
            when(providerMapper.selectById(1L)).thenReturn(provider);

            when(aggregationProvider.queryTracking(anyString(), anyString()))
                    .thenThrow(new RuntimeException("网络异常"));

            TrackingVO vo = service.getTracking(1L, null);

            assertThat(vo.getTracks()).hasSize(1);
        }

        @Test
        void shouldHandleNullProviderResponseGracefully() {
            ShippingOrder order = buildShippingOrder(1L, ShippingStatus.IN_TRANSIT);
            when(shippingOrderMapper.selectById(1L)).thenReturn(order);

            TrackingRecord staleRecord = new TrackingRecord();
            staleRecord.setTraceTime(LocalDateTime.now().minusHours(2));
            staleRecord.setTraceDesc("宸叉徑鏀?");
            when(trackingRecordMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(staleRecord));
            when(providerMapper.selectById(1L)).thenReturn(provider);
            when(aggregationProvider.queryTracking(anyString(), anyString())).thenReturn(null);

            TrackingVO vo = service.getTracking(1L, null);

            assertThat(vo.getTracks()).hasSize(1);
            verify(trackingRecordMapper, never()).insert(any(TrackingRecord.class));
        }

        @Test
        void shouldGetTrackingByTrackingNo() {
            ShippingOrder order = buildShippingOrder(1L, ShippingStatus.IN_TRANSIT);
            when(shippingOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);
            when(shippingOrderMapper.selectById(1L)).thenReturn(order);
            when(trackingRecordMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());
            when(providerMapper.selectById(1L)).thenReturn(provider);

            TrackingQueryResponse resp = new TrackingQueryResponse();
            resp.setSuccess(true);
            resp.setTraces(Collections.emptyList());
            when(aggregationProvider.queryTracking(anyString(), anyString())).thenReturn(resp);

            TrackingVO vo = service.getTrackingByTrackingNo("SF1234567890", "SF", null);

            assertThat(vo.getTrackingNo()).isEqualTo("SF1234567890");
        }

        @Test
        void shouldThrowWhenTrackingNoNotFound() {
            when(shippingOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            assertThatThrownBy(() -> service.getTrackingByTrackingNo("NONEXISTENT", "SF", null))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(LogisticsErrorCode.SHIPPING_NOT_FOUND.getCode());
        }

        @Test
        void shouldRejectTrackingAccessForDifferentMerchant() {
            ShippingOrder order = buildShippingOrder(1L, ShippingStatus.IN_TRANSIT);
            order.setMerchantId(999L);
            when(shippingOrderMapper.selectById(1L)).thenReturn(order);

            assertThatThrownBy(() -> service.getTracking(1L, 500L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(LogisticsErrorCode.SHIPPING_FORBIDDEN.getCode());
        }

        @Test
        void shouldRejectTrackingByTrackingNoForDifferentMerchant() {
            ShippingOrder order = buildShippingOrder(1L, ShippingStatus.IN_TRANSIT);
            order.setMerchantId(999L);
            when(shippingOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);

            assertThatThrownBy(() -> service.getTrackingByTrackingNo("SF1234567890", "SF", 500L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(LogisticsErrorCode.SHIPPING_FORBIDDEN.getCode());
        }
    }

    @Nested
    class ListShippingTests {

        @Test
        void shouldListShippingWithPagination() {
            ShippingOrder order = buildShippingOrder(1L, ShippingStatus.DISPATCHED);
            Page<ShippingOrder> mockPage = new Page<>(1, 10, 1);
            mockPage.setRecords(List.of(order));
            when(shippingOrderMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(mockPage);
            when(shippingOrderItemMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());
            when(providerMapper.selectBatchIds(any())).thenReturn(List.of(provider));

            IPage<ShippingOrderVO> result = service.listShipping(1, 10, null, null, null);

            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getTotal()).isEqualTo(1);
            assertThat(result.getRecords().get(0).getProviderName()).isEqualTo("顺丰速运");
        }

        @Test
        void shouldListShippingWithOrderNoFilter() {
            ShippingOrder order = buildShippingOrder(1L, ShippingStatus.DISPATCHED);
            Page<ShippingOrder> mockPage = new Page<>(1, 10, 1);
            mockPage.setRecords(List.of(order));
            when(shippingOrderMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(mockPage);
            when(shippingOrderItemMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());
            when(providerMapper.selectBatchIds(any())).thenReturn(List.of(provider));

            IPage<ShippingOrderVO> result = service.listShipping(1, 10, "ORD001", null, null);

            assertThat(result.getRecords()).hasSize(1);
        }

        @Test
        void shouldListEmptyPage() {
            Page<ShippingOrder> mockPage = new Page<>(1, 10, 0);
            mockPage.setRecords(Collections.emptyList());
            when(shippingOrderMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(mockPage);

            IPage<ShippingOrderVO> result = service.listShipping(1, 10, null, null, null);

            assertThat(result.getRecords()).isEmpty();
            assertThat(result.getTotal()).isZero();
        }
    }

    @Nested
    class GetShippingTests {

        @Test
        void shouldGetShippingById() {
            ShippingOrder order = buildShippingOrder(1L, ShippingStatus.DISPATCHED);
            when(shippingOrderMapper.selectById(1L)).thenReturn(order);
            when(shippingOrderItemMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());
            when(providerMapper.selectById(1L)).thenReturn(provider);

            ShippingOrderVO vo = service.getShipping(1L, "admin", null);

            assertThat(vo.getShippingNo()).isEqualTo("SH202606200001");
            assertThat(vo.getProviderName()).isEqualTo("顺丰速运");
        }

        @Test
        void shouldThrowWhenShippingByIdNotFound() {
            when(shippingOrderMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> service.getShipping(999L, "admin", null))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(LogisticsErrorCode.SHIPPING_NOT_FOUND.getCode());
        }

        @Test
        void shouldRejectGetShippingForDifferentMerchant() {
            ShippingOrder order = buildShippingOrder(1L, ShippingStatus.DISPATCHED);
            order.setMerchantId(999L);
            when(shippingOrderMapper.selectById(1L)).thenReturn(order);

            assertThatThrownBy(() -> service.getShipping(1L, "merchant", 500L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(LogisticsErrorCode.SHIPPING_FORBIDDEN.getCode());
        }

        @Test
        void shouldGetShippingByOrderId() {
            ShippingOrder order = buildShippingOrder(1L, ShippingStatus.DISPATCHED);
            when(shippingOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);
            when(shippingOrderItemMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());
            when(providerMapper.selectById(1L)).thenReturn(provider);

            ShippingOrderVO vo = service.getShippingByOrderId(1001L);

            assertThat(vo.getOrderId()).isEqualTo(1001L);
        }

        @Test
        void shouldThrowWhenShippingByOrderIdNotFound() {
            when(shippingOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            assertThatThrownBy(() -> service.getShippingByOrderId(999L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(LogisticsErrorCode.SHIPPING_NOT_FOUND.getCode());
        }
    }

    @Nested
    class CallbackTests {

        @Test
        void shouldLogAndIgnoreCallbackWithoutError() {
            when(aggregationProvider.verifyCallbackSignature("SF", "{\"status\":\"delivered\"}", "signature-abc"))
                    .thenReturn(true);

            // Phase 1 stub: processCallback should not throw
            service.processCallback("SF", "{\"status\":\"delivered\"}", "signature-abc");

            // No exception means success — no further assertions needed for stub
        }
        @Test
        void shouldMarkShippingSignedAndPublishOrderDeliveredWhenAllShipmentsSigned() {
            ShippingOrder current = buildShippingOrder(1L, ShippingStatus.DISPATCHED);
            current.setOrderId(1001L);
            current.setOrderNo("ORD202606200001");
            current.setTrackingNo("SF1234567890");

            ShippingOrder sibling = buildShippingOrder(2L, ShippingStatus.SIGNED);
            sibling.setOrderId(1001L);
            sibling.setOrderNo("ORD202606200001");

            when(shippingOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(current);
            when(trackingRecordMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(false);
            when(trackingRecordMapper.insert(org.mockito.ArgumentMatchers.<TrackingRecord>any())).thenReturn(1);
            when(shippingOrderMapper.updateById(any(ShippingOrder.class))).thenReturn(1);
            when(shippingOrderMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(current, sibling));
            when(orderClient.getShippingSnapshot(1001L))
                    .thenReturn(com.ecommerce.common.result.Result.ok(orderSnapshot("ORD202606200001", 1, 9001L)));
            when(aggregationProvider.verifyCallbackSignature(eq("SF"), anyString(), eq("signature-abc")))
                    .thenReturn(true);
            service.processCallback("SF", """
                    {
                      "trackingNo": "SF1234567890",
                      "status": "SIGNED",
                      "time": "2026-06-22 10:30:00",
                      "desc": "Package signed",
                      "location": "Shanghai"
                    }
                    """, "signature-abc");

            assertThat(current.getShippingStatus()).isEqualTo(ShippingStatus.SIGNED);
            assertThat(current.getSignedAt()).isEqualTo(LocalDateTime.of(2026, 6, 22, 10, 30, 0));
            verify(trackingRecordMapper).insert(org.mockito.ArgumentMatchers.<TrackingRecord>argThat(record ->
                    Long.valueOf(1L).equals(record.getShippingId())
                            && "SIGNED".equals(record.getEventType())
                            && "SIGNED".equals(record.getTraceStatus())));
            verify(outboxService).enqueue(
                    eq("shipping"),
                    eq("ORD202606200001"),
                    eq("order-delivered"),
                    argThat(payload -> {
                        if (!(payload instanceof OrderDeliveredMessage msg)) {
                            return false;
                        }
                        return Long.valueOf(1L).equals(msg.getShippingId())
                                && Long.valueOf(1001L).equals(msg.getOrderId())
                                && "ORD202606200001".equals(msg.getOrderNo())
                                && Long.valueOf(9001L).equals(msg.getUserId())
                                && LocalDateTime.of(2026, 6, 22, 10, 30, 0).equals(msg.getSignedAt());
                    }));
        }

        @Test
        void shouldProcessCallbackWithCarrierCodeFromPayload() {
            ShippingOrder current = buildShippingOrder(1L, ShippingStatus.DISPATCHED);
            current.setOrderId(1001L);
            current.setOrderNo("ORD202606200001");
            current.setTrackingNo("SF1234567890");
            current.setProviderCode("SF");

            ShippingOrder sibling = buildShippingOrder(2L, ShippingStatus.SIGNED);
            sibling.setOrderId(1001L);
            sibling.setOrderNo("ORD202606200001");

            when(shippingOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(current);
            when(trackingRecordMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(false);
            when(trackingRecordMapper.insert(org.mockito.ArgumentMatchers.<TrackingRecord>any())).thenReturn(1);
            when(shippingOrderMapper.updateById(any(ShippingOrder.class))).thenReturn(1);
            when(shippingOrderMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(current, sibling));
            when(orderClient.getShippingSnapshot(1001L))
                    .thenReturn(com.ecommerce.common.result.Result.ok(orderSnapshot("ORD202606200001", 1, 9001L)));
            when(aggregationProvider.verifyCallbackSignature(eq("stub"), anyString(), eq("signature-abc")))
                    .thenReturn(true);

            service.processCallback("stub", """
                    {
                      "trackingNo": "SF1234567890",
                      "expressCode": "SF",
                      "status": "SIGNED",
                      "time": "2026-06-22 10:30:00",
                      "desc": "Package signed",
                      "location": "Shanghai"
                    }
                    """, "signature-abc");

            assertThat(current.getShippingStatus()).isEqualTo(ShippingStatus.SIGNED);
            verify(trackingRecordMapper).insert(org.mockito.ArgumentMatchers.<TrackingRecord>argThat(record ->
                    Long.valueOf(1L).equals(record.getShippingId())
                            && "SF".equals(record.getProviderCode())
                            && "SIGNED".equals(record.getEventType())));
        }

        @Test
        void shouldVerifyCallbackSignatureWithAggregationProviderCode() {
            when(aggregationProvider.verifyCallbackSignature("stub", "{\"status\":\"delivered\"}", "signature-abc"))
                    .thenReturn(true);

            service.processCallback("stub", "{\"status\":\"delivered\"}", "signature-abc");

            verify(aggregationProvider).verifyCallbackSignature("stub", "{\"status\":\"delivered\"}", "signature-abc");
        }

        @Test
        void shouldNotPublishOrderDeliveredWhenNotAllShipmentsSigned() {
            ShippingOrder current = buildShippingOrder(1L, ShippingStatus.DISPATCHED);
            current.setOrderId(1001L);
            current.setOrderNo("ORD202606200001");
            current.setTrackingNo("SF1234567890");

            ShippingOrder sibling = buildShippingOrder(2L, ShippingStatus.DISPATCHED);
            sibling.setOrderId(1001L);
            sibling.setOrderNo("ORD202606200001");

            when(shippingOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(current);
            when(trackingRecordMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(false);
            when(trackingRecordMapper.insert(org.mockito.ArgumentMatchers.<TrackingRecord>any())).thenReturn(1);
            when(shippingOrderMapper.updateById(any(ShippingOrder.class))).thenReturn(1);
            when(shippingOrderMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(current, sibling));
            when(aggregationProvider.verifyCallbackSignature(eq("SF"), anyString(), eq("signature-abc")))
                    .thenReturn(true);

            service.processCallback("SF", """
                    {
                      "trackingNo": "SF1234567890",
                      "status": "SIGNED",
                      "time": "2026-06-22 10:30:00",
                      "desc": "Package signed",
                      "location": "Shanghai"
                    }
                    """, "signature-abc");

            assertThat(current.getShippingStatus()).isEqualTo(ShippingStatus.SIGNED);
            verify(outboxService, never()).enqueue(eq("shipping"), eq("ORD202606200001"), eq("order-delivered"), any());
        }

        @Test
        void shouldRejectCallbackWhenSignatureInvalid() {
            when(aggregationProvider.verifyCallbackSignature("SF", "{\"status\":\"delivered\"}", "bad-signature"))
                    .thenReturn(false);

            assertThatThrownBy(() -> service.processCallback("SF", "{\"status\":\"delivered\"}", "bad-signature"))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(LogisticsErrorCode.CALLBACK_SIGNATURE_INVALID.getCode());

            verify(shippingOrderMapper, never()).selectOne(any(LambdaQueryWrapper.class));
        }
    }

    @Nested
    class GenerateWaybillTests {

        @Test
        void shouldGenerateWaybillForScopedMerchant() {
            ShippingOrder order = buildShippingOrder(1L, ShippingStatus.DISPATCHED);
            order.setMerchantId(500L);
            when(shippingOrderMapper.selectById(1L)).thenReturn(order);
            when(shippingOrderMapper.updateById(any(ShippingOrder.class))).thenReturn(1);

            String waybillUrl = service.generateWaybill(1L, 500L);

            assertThat(waybillUrl).isEqualTo("https://waybill.example.com/SF1234567890.pdf");
            verify(shippingOrderMapper).updateById(argThat((ShippingOrder updated) ->
                    "https://waybill.example.com/SF1234567890.pdf".equals(updated.getWaybillUrl())));
        }

        @Test
        void shouldRejectWaybillGenerationForDifferentMerchant() {
            ShippingOrder order = buildShippingOrder(1L, ShippingStatus.DISPATCHED);
            order.setMerchantId(999L);
            when(shippingOrderMapper.selectById(1L)).thenReturn(order);

            assertThatThrownBy(() -> service.generateWaybill(1L, 500L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(LogisticsErrorCode.SHIPPING_FORBIDDEN.getCode());

            verify(shippingOrderMapper, never()).updateById(any(ShippingOrder.class));
        }
    }

    // --- helpers ---

    private CreateShippingRequest buildCreateRequest() {
        CreateShippingRequest req = new CreateShippingRequest();
        req.setClientRequestId("test-req-" + System.currentTimeMillis());
        req.setOrderId(1001L);
        req.setProviderId(1L);
        req.setTrackingNo("SF1234567890");
        req.setPackageWeight(500);
        req.setPackageSize("30x20x10");
        req.setSourceType(0);

        CreateShippingRequest.ShippingItemRequest item = new CreateShippingRequest.ShippingItemRequest();
        item.setOrderItemId(100L);
        item.setSkuId(200L);
        item.setQuantity(1);
        req.setItems(List.of(item));
        return req;
    }

    private ShippingOrder buildShippingOrder(Long id, int shippingStatus) {
        ShippingOrder order = new ShippingOrder();
        order.setId(id);
        order.setShippingNo("SH202606200001");
        order.setClientRequestId("test-req");
        order.setOrderId(1001L);
        order.setOrderNo("ORD202606200001");
        order.setProviderId(1L);
        order.setProviderCode("SF");
        order.setTrackingNo("SF1234567890");
        order.setShippingStatus(shippingStatus);
        order.setPackageWeight(500);
        order.setVersion(0);
        order.setMerchantId(null);
        return order;
    }

    private WarehouseInfoVO managedWarehouse(Long id) {
        return warehouse(id, WarehouseStockMode.MANAGED, null);
    }

    private WarehouseInfoVO selfWarehouse(Long id) {
        return warehouse(id, WarehouseStockMode.LIGHT, null);
    }

    private WarehouseInfoVO warehouse(Long id, Integer stockMode, Long merchantId) {
        WarehouseInfoVO warehouse = new WarehouseInfoVO();
        warehouse.setId(id);
        warehouse.setStockMode(stockMode);
        warehouse.setMerchantId(merchantId);
        return warehouse;
    }

    private OrderInternalVO orderSnapshot(String orderNo, int status, Long userId, OrderInternalVO.OrderItemSnapshot... items) {
        OrderInternalVO order = new OrderInternalVO();
        order.setId(1001L);
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setStatus(status);
        order.setItems(List.of(items));
        return order;
    }

    private OrderInternalVO.OrderItemSnapshot itemSnapshot(Long orderItemId, Long skuId, Integer quantity) {
        return itemSnapshot(orderItemId, skuId, quantity, null);
    }

    private OrderInternalVO.OrderItemSnapshot itemSnapshot(Long orderItemId, Long skuId, Integer quantity, Long merchantId) {
        OrderInternalVO.OrderItemSnapshot item = new OrderInternalVO.OrderItemSnapshot();
        item.setOrderItemId(orderItemId);
        item.setSkuId(skuId);
        item.setQuantity(quantity);
        item.setMerchantId(merchantId);
        return item;
    }
}
