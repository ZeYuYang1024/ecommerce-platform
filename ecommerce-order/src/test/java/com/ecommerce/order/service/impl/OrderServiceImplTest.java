package com.ecommerce.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.common.dto.OrderPaidMessage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.dto.OrderInventoryMessage;
import com.ecommerce.common.dto.SkuBatchVO;
import com.ecommerce.common.outbox.OutboxQuery;
import com.ecommerce.common.outbox.OutboxSummary;
import com.ecommerce.common.outbox.OutboxService;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.common.result.Result;
import com.ecommerce.order.client.CartClient;
import com.ecommerce.order.client.ProductSpuClient;
import com.ecommerce.order.common.OrderErrorCode;
import com.ecommerce.order.dto.request.CreateOrderRequest;
import com.ecommerce.order.dto.response.OrderSummaryVO;
import com.ecommerce.order.dto.response.OutboxMessageVO;
import com.ecommerce.order.dto.response.OrderVO;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.mapper.OrderItemMapper;
import com.ecommerce.order.mapper.OrderMapper;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderItemMapper itemMapper;

    @Mock
    private ProductSpuClient productSpuClient;

    @Mock
    private CartClient cartClient;

    @Mock
    private OutboxService outboxService;

    @Mock
    private RocketMQTemplate rocketMQTemplate;

    @InjectMocks
    private OrderServiceImpl service;

    private Order order;

    @BeforeEach
    void setUp() {
        order = new Order();
        order.setId(1L);
        order.setOrderNo("202605091200000001");
        order.setUserId(1L);
        order.setTotalAmount(new BigDecimal("6999.00"));
        order.setStatus(0);
        order.setReceiverName("receiver");
        order.setReceiverPhone("13800001111");
        order.setReceiverAddress("address");
    }

    @Nested
    class CreateTests {
        @Test
        void shouldCreateOrder() {
            when(orderMapper.insert(any(Order.class))).thenReturn(1);
            when(itemMapper.insert(any(OrderItem.class))).thenReturn(1);
            when(productSpuClient.batchQuerySkus(anyList()))
                    .thenReturn(Result.ok(List.of(buildSku(1L, 1L, "Product", "img-1", "99.00"))));

            CreateOrderRequest req = buildRequest(List.of(buildRequestItem(1L, 2, "99.00")));

            OrderVO vo = service.createOrder(1L, req);

            assertThat(vo.getOrderNo()).isNotBlank();
            assertThat(vo.getStatus()).isEqualTo(0);
            assertThat(vo.getTotalAmount()).isEqualByComparingTo("198.00");
            verify(outboxService).enqueue(eq("order"), anyString(), eq("order-created"), any(OrderInventoryMessage.class));
        }

        @Test
        void shouldRejectEmptyItems() {
            CreateOrderRequest req = new CreateOrderRequest();
            req.setItems(Collections.emptyList());

            assertThatThrownBy(() -> service.createOrder(1L, req))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(OrderErrorCode.ORDER_ITEMS_EMPTY.getCode());
        }

        @Test
        void shouldUseTrustedSkuSnapshotWhenCreatingOrder() {
            when(orderMapper.insert(any(Order.class))).thenReturn(1);
            when(itemMapper.insert(any(OrderItem.class))).thenReturn(1);
            when(productSpuClient.batchQuerySkus(anyList()))
                    .thenReturn(Result.ok(List.of(buildSku(1L, 99L, "Trusted SKU", "trusted.png", "129.00"))));

            CreateOrderRequest req = buildRequest(List.of(buildRequestItem(1L, 2, "0.01")));
            req.getItems().getFirst().setSpuId(1L);
            req.getItems().getFirst().setName("Fake");
            req.getItems().getFirst().setImage("fake.png");

            OrderVO vo = service.createOrder(1L, req);

            assertThat(vo.getTotalAmount()).isEqualByComparingTo("258.00");
            verify(itemMapper).insert(argThat((OrderItem saved) ->
                    Long.valueOf(1L).equals(saved.getSkuId())
                            && Long.valueOf(99L).equals(saved.getSpuId())
                            && "Trusted SKU".equals(saved.getName())
                            && "trusted.png".equals(saved.getImage())
                            && saved.getPrice().compareTo(new BigDecimal("129.00")) == 0));
        }

        @Test
        void shouldFailOrderCreationWhenOutboxEnqueueFails() {
            when(orderMapper.insert(any(Order.class))).thenReturn(1);
            when(itemMapper.insert(any(OrderItem.class))).thenReturn(1);
            when(productSpuClient.batchQuerySkus(anyList()))
                    .thenReturn(Result.ok(List.of(buildSku(1L, 1L, "Product", "img-1", "99.00"))));
            doThrow(new RuntimeException("persist outbox failed")).when(outboxService)
                    .enqueue(eq("order"), anyString(), eq("order-created"), any(OrderInventoryMessage.class));

            CreateOrderRequest req = buildRequest(List.of(buildRequestItem(1L, 1, "99.00")));

            assertThatThrownBy(() -> service.createOrder(1L, req))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("persist outbox failed");
        }

        @Test
        void shouldEnqueueSingleInventoryMessageForWholeOrder() {
            when(orderMapper.insert(any(Order.class))).thenReturn(1);
            when(itemMapper.insert(any(OrderItem.class))).thenReturn(1);
            when(productSpuClient.batchQuerySkus(anyList())).thenReturn(Result.ok(List.of(
                    buildSku(1L, 11L, "A", "a.png", "10.00"),
                    buildSku(2L, 22L, "B", "b.png", "20.00"))));

            CreateOrderRequest req = buildRequest(List.of(
                    buildRequestItem(1L, 2, "0.01"),
                    buildRequestItem(2L, 1, "0.01")));

            service.createOrder(1L, req);

            verify(outboxService, times(1)).enqueue(eq("order"), anyString(), eq("order-created"),
                    argThat((OrderInventoryMessage message) ->
                            message != null
                                    && message.getItems() != null
                                    && message.getItems().size() == 2
                                    && message.getItems().stream().anyMatch(i ->
                                    Long.valueOf(1L).equals(i.getSkuId()) && Integer.valueOf(2).equals(i.getQuantity()))
                                    && message.getItems().stream().anyMatch(i ->
                                    Long.valueOf(2L).equals(i.getSkuId()) && Integer.valueOf(1).equals(i.getQuantity()))));
        }

        @Test
        void shouldAttachTransactionMetadataToInventoryMessage() {
            when(orderMapper.insert(any(Order.class))).thenReturn(1);
            when(itemMapper.insert(any(OrderItem.class))).thenReturn(1);
            when(productSpuClient.batchQuerySkus(anyList()))
                    .thenReturn(Result.ok(List.of(buildSku(1L, 11L, "A", "a.png", "10.00"))));

            CreateOrderRequest req = buildRequest(List.of(buildRequestItem(1L, 2, "0.01")));

            service.createOrder(1L, req);

            verify(outboxService).enqueue(eq("order"), anyString(), eq("order-created"),
                    argThat((OrderInventoryMessage message) ->
                            message != null
                                    && message.getTransactionId() != null
                                    && !message.getTransactionId().isBlank()
                                    && message.getIdempotencyKey() != null
                                    && !message.getIdempotencyKey().isBlank()));
        }

        @Test
        void shouldNotSendMqDirectlyFromCreateOrder() {
            when(orderMapper.insert(any(Order.class))).thenReturn(1);
            when(itemMapper.insert(any(OrderItem.class))).thenReturn(1);
            when(productSpuClient.batchQuerySkus(anyList()))
                    .thenReturn(Result.ok(List.of(buildSku(1L, 1L, "Product", "img-1", "99.00"))));

            CreateOrderRequest req = buildRequest(List.of(buildRequestItem(1L, 1, "99.00")));

            service.createOrder(1L, req);

            verify(outboxService).enqueue(eq("order"), anyString(), eq("order-created"), any(OrderInventoryMessage.class));
            verify(rocketMQTemplate, never()).syncSend(eq("order-created"), any(OrderInventoryMessage.class));
        }
    }

    @Nested
    class QueryTests {
        @Test
        void shouldGetOrderWithItems() {
            when(orderMapper.selectById(1L)).thenReturn(order);
            when(itemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

            OrderVO vo = service.getOrder(1L, 1L);

            assertThat(vo.getOrderNo()).isEqualTo("202605091200000001");
            assertThat(vo.getStatusText()).isNotBlank();
        }

        @Test
        void shouldRejectOrderAccessForDifferentUser() {
            order.setUserId(9L);
            when(orderMapper.selectById(1L)).thenReturn(order);

            assertThatThrownBy(() -> service.getOrder(1L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(OrderErrorCode.ORDER_FORBIDDEN);
        }

        @Test
        void shouldThrowWhenOrderNotFound() {
            when(orderMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> service.getOrder(1L, 999L))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        void shouldListUserOrdersWithPagination() {
            Page<Order> mockPage = new Page<>(1, 10, 1);
            mockPage.setRecords(Collections.singletonList(order));
            when(orderMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

            Page<OrderVO> result = service.listByUser(1L, 1, 10);

            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getTotal()).isEqualTo(1);
        }

        @Test
        void shouldListUserOrderSummariesWithCompactItemPreview() {
            order.setCreatedAt(LocalDateTime.of(2026, 5, 20, 10, 15));
            Page<Order> mockPage = new Page<>(1, 2, 1);
            mockPage.setRecords(Collections.singletonList(order));
            OrderItem firstItem = new OrderItem();
            firstItem.setOrderId(1L);
            firstItem.setName("Phone");
            firstItem.setQuantity(1);
            OrderItem secondItem = new OrderItem();
            secondItem.setOrderId(1L);
            secondItem.setName("Case");
            secondItem.setQuantity(2);
            when(orderMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);
            when(itemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(firstItem, secondItem));

            List<OrderSummaryVO> result = service.listSummariesByUser(1L, 2);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getOrderNo()).isEqualTo("202605091200000001");
            assertThat(result.getFirst().getStatusText()).isNotBlank();
            assertThat(result.getFirst().getFirstItemName()).isEqualTo("Phone");
            assertThat(result.getFirst().getItemCount()).isEqualTo(3);
        }

        @Test
        void shouldSkipItemLookupWhenListingEmptyUserOrderSummaries() {
            Page<Order> mockPage = new Page<>(1, 5, 0);
            mockPage.setRecords(Collections.emptyList());
            when(orderMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

            List<OrderSummaryVO> result = service.listSummariesByUser(1L, 5);

            assertThat(result).isEmpty();
            verify(itemMapper, never()).selectList(any(LambdaQueryWrapper.class));
        }
    }

    @Nested
    class CancelTests {
        @Test
        void shouldCancelPendingOrderAndEnqueueReleaseMessage() {
            when(orderMapper.selectById(1L)).thenReturn(order);
            when(orderMapper.updateById(any(Order.class))).thenReturn(1);
            when(itemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

            service.cancelOrder(1L, 1L);

            verify(orderMapper).updateById(any(Order.class));
            verify(outboxService).enqueue(eq("order"), eq("202605091200000001"), eq("order-cancelled"),
                    any(OrderInventoryMessage.class));
        }

        @Test
        void shouldRejectCancelNonPending() {
            order.setStatus(1);
            when(orderMapper.selectById(1L)).thenReturn(order);

            assertThatThrownBy(() -> service.cancelOrder(1L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(OrderErrorCode.ORDER_NOT_PENDING);
        }

        @Test
        void shouldRejectCancelForDifferentUser() {
            order.setUserId(9L);
            when(orderMapper.selectById(1L)).thenReturn(order);

            assertThatThrownBy(() -> service.cancelOrder(1L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(OrderErrorCode.ORDER_FORBIDDEN);
        }
    }

    @Nested
    class TransactionTests {
        @Test
        void shouldCancelPendingOrderWhenInventoryCompensationMessageArrives() {
            order.setStatus(0);
            when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);
            when(orderMapper.updateById(any(Order.class))).thenReturn(1);

            service.applyInventoryCompensation(new OrderPaidMessage("202605091200000001", 4, LocalDateTime.now()));

            assertThat(order.getStatus()).isEqualTo(4);
            verify(orderMapper).updateById(order);
        }

        @Test
        void shouldIgnoreInventoryCompensationWhenOrderAlreadyPaid() {
            order.setStatus(1);
            when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);

            service.applyInventoryCompensation(new OrderPaidMessage("202605091200000001", 4, LocalDateTime.now()));

            verify(orderMapper, never()).updateById(order);
        }
    }

    @Nested
    class ShipTests {
        @Test
        void shouldMarkShippedWhenPaid() {
            order.setStatus(1);
            when(orderMapper.selectById(1L)).thenReturn(order);
            when(orderMapper.updateById(any(Order.class))).thenReturn(1);

            service.markShipped(1L, "super_admin", null);

            verify(orderMapper).updateById(any(Order.class));
        }

        @Test
        void shouldRejectShipWhenNotPaid() {
            order.setStatus(0);
            when(orderMapper.selectById(1L)).thenReturn(order);

            assertThatThrownBy(() -> service.markShipped(1L, "super_admin", null))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(OrderErrorCode.ORDER_NOT_PAID.getCode());
        }

        @Test
        void shouldRejectMerchantShipForOrderOutsideMerchantScope() {
            order.setStatus(1);
            when(orderMapper.selectById(1L)).thenReturn(order);
            when(productSpuClient.getSpuIdsByMerchant(100L)).thenReturn(Result.ok(List.of(10L)));
            OrderItem foreignItem = new OrderItem();
            foreignItem.setOrderId(1L);
            foreignItem.setSpuId(99L);
            when(itemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(foreignItem));

            assertThatThrownBy(() -> service.markShipped(1L, "merchant", 100L))
                    .isInstanceOf(BusinessException.class);

            verify(orderMapper, never()).updateById(any(Order.class));
        }
    }

    @Nested
    class AdminAndBoundaryTests {
        @Test
        void shouldListAllAdminOrdersWithPagination() {
            Page<Order> mockPage = new Page<>(1, 10, 1);
            mockPage.setRecords(Collections.singletonList(order));
            when(orderMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

            Page<OrderVO> result = service.listAll(1, 10, 0);

            assertThat(result.getRecords()).hasSize(1);
        }

        @Test
        void shouldListForRecon() {
            order.setCreatedAt(LocalDateTime.now());
            when(orderMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.singletonList(order));

            List<Order> orders = service.listForRecon(null, null);

            assertThat(orders).hasSize(1);
        }

        @Test
        void shouldCreateOrderWithMultipleItems() {
            when(orderMapper.insert(any(Order.class))).thenReturn(1);
            when(itemMapper.insert(any(OrderItem.class))).thenReturn(1);
            when(productSpuClient.batchQuerySkus(anyList())).thenReturn(Result.ok(List.of(
                    buildSku(1L, 1L, "A", "a.png", "10.00"),
                    buildSku(2L, 1L, "B", "b.png", "20.00"),
                    buildSku(3L, 2L, "C", "c.png", "0.01"))));

            CreateOrderRequest req = buildRequest(List.of(
                    buildRequestItem(1L, 1, "10.00"),
                    buildRequestItem(2L, 3, "20.00"),
                    buildRequestItem(3L, 100, "0.01")));

            OrderVO vo = service.createOrder(1L, req);

            assertThat(vo.getTotalAmount()).isEqualByComparingTo("71.00");
        }

        @Test
        void shouldCreateOrderWithSingleLargeQuantity() {
            when(orderMapper.insert(any(Order.class))).thenReturn(1);
            when(itemMapper.insert(any(OrderItem.class))).thenReturn(1);
            when(productSpuClient.batchQuerySkus(anyList()))
                    .thenReturn(Result.ok(List.of(buildSku(1L, 1L, "Bulk", "bulk.png", "1.00"))));

            CreateOrderRequest req = buildRequest(List.of(buildRequestItem(1L, 9999, "1.00")));

            assertThat(service.createOrder(1L, req).getTotalAmount()).isEqualByComparingTo("9999.00");
        }

        @Test
        void shouldShowRefundedStatusText() {
            order.setStatus(5);
            when(orderMapper.selectById(1L)).thenReturn(order);
            when(itemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

            assertThat(service.getOrder(1L, 1L).getStatusText()).isNotBlank();
        }

        @Test
        void shouldUpdateStatusToValidTransition() {
            order.setStatus(2);
            when(orderMapper.selectById(1L)).thenReturn(order);
            when(orderMapper.updateById(any(Order.class))).thenReturn(1);

            service.updateStatus(1L, 3, "super_admin", null);

            assertThat(order.getStatus()).isEqualTo(3);
        }

        @Test
        void shouldRejectInvalidStatusTransitionFromPendingToCompleted() {
            order.setStatus(0);
            when(orderMapper.selectById(1L)).thenReturn(order);

            assertThatThrownBy(() -> service.updateStatus(1L, 3, "super_admin", null))
                    .isInstanceOf(BusinessException.class);

            verify(orderMapper, never()).updateById(any(Order.class));
        }

        @Test
        void shouldRejectMerchantStatusUpdateForOrderOutsideMerchantScope() {
            when(orderMapper.selectById(1L)).thenReturn(order);
            when(productSpuClient.getSpuIdsByMerchant(100L)).thenReturn(Result.ok(List.of(10L)));
            OrderItem foreignItem = new OrderItem();
            foreignItem.setOrderId(1L);
            foreignItem.setSpuId(99L);
            when(itemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(foreignItem));

            assertThatThrownBy(() -> service.updateStatus(1L, 3, "merchant", 100L))
                    .isInstanceOf(BusinessException.class);

            verify(orderMapper, never()).updateById(any(Order.class));
        }

        @Test
        void shouldListOrderOutboxMessagesFromSharedOutbox() {
            Page<com.ecommerce.common.outbox.OutboxMessage> page = new Page<>(1, 10, 1);
            com.ecommerce.common.outbox.OutboxMessage message = new com.ecommerce.common.outbox.OutboxMessage();
            message.setId(1001L);
            message.setAggregateId("ORD-1");
            message.setTopic("order-created");
            message.setStatus(3);
            message.setRetryCount(2);
            message.setCreatedAt(LocalDateTime.of(2026, 5, 28, 13, 0));
            page.setRecords(List.of(message));
            when(outboxService.queryMessages(any(OutboxQuery.class), eq(1), eq(10))).thenReturn(page);

            Page<OutboxMessageVO> result = service.listOutbox(new OutboxQuery("order", "order-created", 3, "ORD-1"), 1, 10);

            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getRecords().getFirst().getTopic()).isEqualTo("order-created");
            verify(outboxService).queryMessages(argThat(query ->
                    "order".equals(query.getAggregateType())
                            && "order-created".equals(query.getTopic())
                            && Integer.valueOf(3).equals(query.getStatus())
                            && "ORD-1".equals(query.getAggregateId())), eq(1), eq(10));
        }

        @Test
        void shouldRetryOrderOutboxMessage() {
            when(outboxService.retryMessage(1001L)).thenReturn(true);

            int affected = service.retryOutboxMessage(1001L);

            assertThat(affected).isEqualTo(1);
            verify(outboxService).retryMessage(1001L);
        }

        @Test
        void shouldReturnZeroWhenOrderOutboxMessageRetryDidNothing() {
            when(outboxService.retryMessage(1002L)).thenReturn(false);

            int affected = service.retryOutboxMessage(1002L);

            assertThat(affected).isZero();
            verify(outboxService).retryMessage(1002L);
        }

        @Test
        void shouldRetryOrderOutboxBatch() {
            when(outboxService.retryBatch(any(OutboxQuery.class), eq(20))).thenReturn(2);

            int affected = service.retryOutboxBatch(new OutboxQuery("order", "order-created", 3, "ORD-2"), 20);

            assertThat(affected).isEqualTo(2);
            verify(outboxService).retryBatch(argThat(query ->
                    "order".equals(query.getAggregateType())
                            && "order-created".equals(query.getTopic())
                            && Integer.valueOf(3).equals(query.getStatus())
                            && "ORD-2".equals(query.getAggregateId())), eq(20));
        }

        @Test
        void shouldReturnOrderOutboxSummary() {
            when(outboxService.summarize(any(OutboxQuery.class))).thenReturn(new OutboxSummary(1, 0, 2, 3));

            OutboxSummary summary = service.getOutboxSummary(new OutboxQuery("order", "order-created", 3, "ORD-3"));

            assertThat(summary.getPendingCount()).isEqualTo(1);
            assertThat(summary.getFailedCount()).isEqualTo(3);
            verify(outboxService).summarize(argThat(query ->
                    "order".equals(query.getAggregateType())
                            && "order-created".equals(query.getTopic())
                            && Integer.valueOf(3).equals(query.getStatus())
                            && "ORD-3".equals(query.getAggregateId())));
        }
    }

    private CreateOrderRequest buildRequest(List<CreateOrderRequest.OrderItemRequest> items) {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setReceiverName("receiver");
        req.setReceiverPhone("13800001111");
        req.setReceiverAddress("address");
        req.setItems(items);
        return req;
    }

    private CreateOrderRequest.OrderItemRequest buildRequestItem(Long skuId, int quantity, String price) {
        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
        item.setSkuId(skuId);
        item.setSpuId(1L);
        item.setName("request-item");
        item.setPrice(price);
        item.setQuantity(quantity);
        return item;
    }

    private SkuBatchVO buildSku(Long skuId, Long spuId, String name, String image, String price) {
        SkuBatchVO sku = new SkuBatchVO();
        sku.setSkuId(skuId);
        sku.setSpuId(spuId);
        sku.setSkuName(name);
        sku.setImage(image);
        sku.setPrice(new BigDecimal(price));
        return sku;
    }
}
