package com.ecommerce.order.transaction;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ecommerce.common.dto.OrderInternalVO;
import com.ecommerce.common.dto.OrderInventoryMessage;
import com.ecommerce.common.dto.OrderItemMessage;
import com.ecommerce.common.dto.OrderPaidMessage;
import com.ecommerce.common.dto.SkuBatchVO;
import com.ecommerce.common.outbox.OutboxService;
import com.ecommerce.common.result.Result;
import com.ecommerce.inventory.entity.InventoryEventLog;
import com.ecommerce.inventory.mapper.InventoryEventLogMapper;
import com.ecommerce.inventory.service.StockService;
import com.ecommerce.inventory.service.impl.InventoryMessageServiceImpl;
import com.ecommerce.inventory.transaction.InventoryTransactionExecutor;
import com.ecommerce.order.client.CartClient;
import com.ecommerce.order.client.ProductSpuClient;
import com.ecommerce.order.consumer.OrderPaidConsumer;
import com.ecommerce.order.dto.request.CreateOrderRequest;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.mapper.OrderItemMapper;
import com.ecommerce.order.mapper.OrderMapper;
import com.ecommerce.order.service.OrderService;
import com.ecommerce.order.service.impl.OrderServiceImpl;
import com.ecommerce.payment.client.OrderClient;
import com.ecommerce.payment.dto.request.PayRequest;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.mapper.PaymentMapper;
import com.ecommerce.payment.mapper.RefundMapper;
import com.ecommerce.payment.service.impl.PaymentServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderSagaFlowContractTest {

    @BeforeEach
    void setUpTableMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Payment.class);
    }

    @Test
    void happyPathShouldCoverOrderCreateInventoryLockAndPaymentConfirmation() {
        OrderInventoryMessage inventoryMessage = createOrderAndCaptureInventoryMessage();

        InventoryEventLogMapper inventoryEventLogMapper = mock(InventoryEventLogMapper.class);
        StockService stockService = mock(StockService.class);
        OutboxService inventoryOutboxService = mock(OutboxService.class);
        InventoryTransactionExecutor transactionExecutor = Runnable::run;
        when(inventoryEventLogMapper.insert(any(InventoryEventLog.class))).thenReturn(1);

        InventoryMessageServiceImpl inventoryService = new InventoryMessageServiceImpl(
                inventoryEventLogMapper, stockService, inventoryOutboxService, transactionExecutor);
        inventoryService.handleDeduct(inventoryMessage);

        verify(stockService).deduct(11L, 2);
        verify(inventoryEventLogMapper).markProcessed(any(Long.class));
        verifyNoInteractions(inventoryOutboxService);

        PaymentMapper paymentMapper = mock(PaymentMapper.class);
        RefundMapper refundMapper = mock(RefundMapper.class);
        OrderClient orderClient = mock(OrderClient.class);
        OutboxService paymentOutboxService = mock(OutboxService.class);
        when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(paymentMapper.insert(any(Payment.class))).thenReturn(1);
        when(orderClient.getOrderByOrderNo(eq(inventoryMessage.getOrderNo()), eq(1L)))
                .thenReturn(Result.ok(orderForPayment(inventoryMessage.getOrderNo(), 20.00, 0)));

        PaymentServiceImpl paymentService = new PaymentServiceImpl(
                paymentMapper, refundMapper, orderClient, paymentOutboxService);
        PayRequest payRequest = new PayRequest();
        payRequest.setOrderNo(inventoryMessage.getOrderNo());
        payRequest.setAmount(new BigDecimal("20.00"));

        paymentService.pay(1L, payRequest);

        ArgumentCaptor<OrderPaidMessage> paidCaptor = ArgumentCaptor.forClass(OrderPaidMessage.class);
        verify(paymentOutboxService).enqueue(eq("payment"), eq(inventoryMessage.getOrderNo()), eq("order-paid"), paidCaptor.capture());
        OrderPaidMessage paidMessage = paidCaptor.getValue();
        assertThat(paidMessage.getStatus()).isEqualTo(1);
        assertThat(paidMessage.getTransactionId()).isNotBlank();
        assertThat(paidMessage.getIdempotencyKey()).startsWith("payment-paid:");

        OrderMapper consumerOrderMapper = mock(OrderMapper.class);
        OrderService consumerOrderService = mock(OrderService.class);
        Order pendingOrder = new Order();
        pendingOrder.setOrderNo(inventoryMessage.getOrderNo());
        pendingOrder.setStatus(0);
        when(consumerOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(pendingOrder);

        OrderPaidConsumer consumer = new OrderPaidConsumer(consumerOrderMapper, consumerOrderService);
        consumer.onMessage(paidMessage);

        verify(consumerOrderMapper).updateById(pendingOrder);
        assertThat(pendingOrder.getStatus()).isEqualTo(1);
        verifyNoInteractions(consumerOrderService);
    }

    @Test
    void inventoryFailureCompensationShouldCancelOrderAndRemainIdempotent() {
        OrderInventoryMessage inventoryMessage = createOrderAndCaptureInventoryMessage();

        InventoryEventLogMapper inventoryEventLogMapper = mock(InventoryEventLogMapper.class);
        StockService stockService = mock(StockService.class);
        OutboxService inventoryOutboxService = mock(OutboxService.class);
        InventoryTransactionExecutor transactionExecutor = Runnable::run;
        when(inventoryEventLogMapper.insert(any(InventoryEventLog.class))).thenReturn(1);
        org.mockito.Mockito.doThrow(new IllegalStateException("stock down"))
                .when(stockService).deduct(11L, 2);

        InventoryMessageServiceImpl inventoryService = new InventoryMessageServiceImpl(
                inventoryEventLogMapper, stockService, inventoryOutboxService, transactionExecutor);

        assertThatCode(() -> inventoryService.handleDeduct(inventoryMessage))
                .doesNotThrowAnyException();

        verify(inventoryEventLogMapper).markFailed(any(Long.class));
        ArgumentCaptor<OrderPaidMessage> compensationCaptor = ArgumentCaptor.forClass(OrderPaidMessage.class);
        verify(inventoryOutboxService).enqueue(eq("inventory"), eq(inventoryMessage.getOrderNo()), eq("order-paid"), compensationCaptor.capture());
        OrderPaidMessage compensationMessage = compensationCaptor.getValue();
        assertThat(compensationMessage.getStatus()).isEqualTo(4);
        assertThat(compensationMessage.getTransactionId()).isEqualTo(inventoryMessage.getTransactionId());
        assertThat(compensationMessage.getIdempotencyKey()).isEqualTo(inventoryMessage.getIdempotencyKey() + ":compensate");
        assertThat(compensationMessage.getErrorMessage()).isEqualTo("stock down");

        OrderMapper orderMapper = mock(OrderMapper.class);
        Order order = new Order();
        order.setOrderNo(inventoryMessage.getOrderNo());
        order.setStatus(0);
        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);
        when(orderMapper.updateById(any(Order.class))).thenReturn(1);

        OrderServiceImpl orderService = new OrderServiceImpl(
                orderMapper,
                mock(OrderItemMapper.class),
                mock(ProductSpuClient.class),
                mock(CartClient.class),
                mock(OutboxService.class));

        orderService.applyInventoryCompensation(compensationMessage);
        orderService.applyInventoryCompensation(compensationMessage);

        verify(orderMapper, times(1)).updateById(order);
        assertThat(order.getStatus()).isEqualTo(4);
    }

    private OrderInventoryMessage createOrderAndCaptureInventoryMessage() {
        OrderMapper orderMapper = mock(OrderMapper.class);
        OrderItemMapper itemMapper = mock(OrderItemMapper.class);
        ProductSpuClient productSpuClient = mock(ProductSpuClient.class);
        CartClient cartClient = mock(CartClient.class);
        OutboxService orderOutboxService = mock(OutboxService.class);
        when(orderMapper.insert(any(Order.class))).thenReturn(1);
        when(itemMapper.insert(any(OrderItem.class))).thenReturn(1);
        when(productSpuClient.batchQuerySkus(any()))
                .thenReturn(Result.ok(List.of(sku(11L, 101L, "Phone", "phone.png", "10.00"))));

        OrderServiceImpl orderService = new OrderServiceImpl(
                orderMapper, itemMapper, productSpuClient, cartClient, orderOutboxService);
        orderService.createOrder(1L, orderRequest());

        ArgumentCaptor<OrderInventoryMessage> inventoryCaptor = ArgumentCaptor.forClass(OrderInventoryMessage.class);
        verify(orderOutboxService).enqueue(eq("order"), anyString(), eq("order-created"), inventoryCaptor.capture());
        OrderInventoryMessage inventoryMessage = inventoryCaptor.getValue();
        assertThat(inventoryMessage.getOrderNo()).isNotBlank();
        assertThat(inventoryMessage.getTransactionId()).isNotBlank();
        assertThat(inventoryMessage.getIdempotencyKey()).startsWith("order-created:");
        assertThat(inventoryMessage.getItems()).containsExactly(new OrderItemMessage(11L, 2));
        return inventoryMessage;
    }

    private CreateOrderRequest orderRequest() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setReceiverName("receiver");
        request.setReceiverPhone("13800001111");
        request.setReceiverAddress("address");
        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
        item.setSkuId(11L);
        item.setSpuId(101L);
        item.setName("Phone");
        item.setPrice("10.00");
        item.setQuantity(2);
        request.setItems(List.of(item));
        return request;
    }

    private SkuBatchVO sku(Long skuId, Long spuId, String name, String image, String price) {
        SkuBatchVO sku = new SkuBatchVO();
        sku.setSkuId(skuId);
        sku.setSpuId(spuId);
        sku.setSkuName(name);
        sku.setImage(image);
        sku.setPrice(new BigDecimal(price));
        return sku;
    }

    private OrderInternalVO orderForPayment(String orderNo, double totalAmount, int status) {
        OrderInternalVO order = new OrderInternalVO();
        order.setId(1L);
        order.setOrderNo(orderNo);
        order.setTotalAmount(BigDecimal.valueOf(totalAmount));
        order.setStatus(status);
        return order;
    }
}
