package com.ecommerce.payment.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.dto.OrderInternalVO;
import com.ecommerce.common.dto.OrderPaidMessage;
import com.ecommerce.common.outbox.OutboxMessage;
import com.ecommerce.common.outbox.OutboxQuery;
import com.ecommerce.common.outbox.OutboxSummary;
import com.ecommerce.common.outbox.OutboxService;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.common.result.Result;
import com.ecommerce.payment.client.OrderClient;
import com.ecommerce.payment.common.PaymentErrorCode;
import com.ecommerce.payment.dto.request.PayRequest;
import com.ecommerce.payment.dto.request.RefundRequest;
import com.ecommerce.payment.dto.response.OutboxMessageVO;
import com.ecommerce.payment.dto.response.PaymentVO;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.Refund;
import com.ecommerce.payment.mapper.PaymentMapper;
import com.ecommerce.payment.mapper.RefundMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private RefundMapper refundMapper;

    @Mock
    private OrderClient orderClient;

    @Mock
    private OutboxService outboxService;

    @Mock
    private RocketMQTemplate rocketMQTemplate;

    @InjectMocks
    private PaymentServiceImpl service;

    private Payment payment;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Payment.class);

        payment = new Payment();
        payment.setId(1L);
        payment.setPaymentNo("PAY202605091200000001");
        payment.setOrderNo("202605091200000001");
        payment.setOrderId(1L);
        payment.setUserId(1L);
        payment.setAmount(new BigDecimal("6999.00"));
        payment.setStatus(1);
        payment.setPayMethod("wx_jsapi");
    }

    @Nested
    class PayTests {
        @Test
        void shouldCreatePaymentAndEnqueueOrderPaidMessage() {
            when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(paymentMapper.insert(any(Payment.class))).thenReturn(1);
            when(orderClient.getOrderByOrderNo("202605091200000001", 1L))
                    .thenReturn(Result.ok(buildOrder("202605091200000001", 1L, new BigDecimal("6999.00"), 0)));

            PayRequest req = new PayRequest();
            req.setOrderNo("202605091200000001");
            req.setOrderId(1L);
            req.setAmount(new BigDecimal("6999.00"));

            PaymentVO vo = service.pay(1L, req);

            assertThat(vo.getPaymentNo()).isNotNull();
            assertThat(vo.getStatus()).isEqualTo(1);
            verify(outboxService).enqueue(
                    eq("payment"),
                    eq("202605091200000001"),
                    eq("order-paid"),
                    argThat((OrderPaidMessage message) ->
                            message != null
                                    && "202605091200000001".equals(message.getOrderNo())
                                    && Integer.valueOf(1).equals(message.getStatus())
                                    && message.getPaidAt() != null));
        }

        @Test
        void shouldRejectDuplicatePay() {
            when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(payment);
            PayRequest req = new PayRequest();
            req.setOrderNo("202605091200000001");
            req.setOrderId(1L);
            req.setAmount(new BigDecimal("6999.00"));

            assertThatThrownBy(() -> service.pay(1L, req))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(PaymentErrorCode.PAYMENT_ALREADY_PAID.getCode());
        }

        @Test
        void shouldRejectPaymentWhenOrderLookupFails() {
            when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(orderClient.getOrderByOrderNo("202605091200000001", 1L)).thenReturn(Result.ok(null));

            PayRequest req = new PayRequest();
            req.setOrderNo("202605091200000001");
            req.setAmount(new BigDecimal("6999.00"));

            assertThatThrownBy(() -> service.pay(1L, req))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        void shouldRejectPaymentWhenAmountDoesNotMatchOrderTotal() {
            when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(orderClient.getOrderByOrderNo("202605091200000001", 1L))
                    .thenReturn(Result.ok(buildOrder("202605091200000001", 1L, new BigDecimal("6999.00"), 0)));

            PayRequest req = new PayRequest();
            req.setOrderNo("202605091200000001");
            req.setAmount(new BigDecimal("0.01"));

            assertThatThrownBy(() -> service.pay(1L, req))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        void shouldRejectPaymentWhenOrderIsNotPending() {
            when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(orderClient.getOrderByOrderNo("202605091200000001", 1L))
                    .thenReturn(Result.ok(buildOrder("202605091200000001", 1L, new BigDecimal("6999.00"), 4)));

            PayRequest req = new PayRequest();
            req.setOrderNo("202605091200000001");
            req.setAmount(new BigDecimal("6999.00"));

            assertThatThrownBy(() -> service.pay(1L, req))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        void shouldFailPaymentWhenOutboxEnqueueFails() {
            when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(paymentMapper.insert(any(Payment.class))).thenReturn(1);
            when(orderClient.getOrderByOrderNo("202605091200000001", 1L))
                    .thenReturn(Result.ok(buildOrder("202605091200000001", 1L, new BigDecimal("6999.00"), 0)));
            doThrow(new RuntimeException("outbox down")).when(outboxService)
                    .enqueue(eq("payment"), eq("202605091200000001"), eq("order-paid"), any(OrderPaidMessage.class));

            PayRequest req = new PayRequest();
            req.setOrderNo("202605091200000001");
            req.setAmount(new BigDecimal("6999.00"));

            assertThatThrownBy(() -> service.pay(1L, req))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("outbox down");
        }
    }

    @Nested
    class QueryTests {
        @Test
        void shouldQueryByOrderNo() {
            when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(payment);

            PaymentVO vo = service.queryByOrderNo("202605091200000001");

            assertThat(vo.getPaymentNo()).isEqualTo("PAY202605091200000001");
            assertThat(vo.getOrderNo()).isEqualTo("202605091200000001");
        }

        @Test
        void shouldThrowWhenUserScopedPaymentNotFound() {
            when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            assertThatThrownBy(() -> service.queryByOrderNoForUser(1L, "xxx"))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(PaymentErrorCode.PAYMENT_NOT_FOUND.getCode());
        }

        @Test
        void shouldListPaymentsByMerchant() {
            when(orderClient.listOrderNosByMerchant(2001L))
                    .thenReturn(Result.ok(List.of("202605091200000001")));
            when(paymentMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenAnswer(invocation -> {
                        Page<Payment> page = invocation.getArgument(0);
                        page.setRecords(Collections.singletonList(payment));
                        page.setTotal(1);
                        return page;
                    });

            Page<PaymentVO> page = service.listByMerchant(2001L, null, 1, 10);

            assertThat(page.getRecords()).hasSize(1);
            assertThat(page.getRecords().get(0).getOrderNo()).isEqualTo("202605091200000001");
        }
    }

    @Nested
    class RefundTests {
        @Test
        void shouldRefundFullAmountAndEnqueueRefundedStatus() {
            when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(payment);
            when(refundMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(refundMapper.insert(any(Refund.class))).thenReturn(1);
            when(paymentMapper.updateById(any(Payment.class))).thenReturn(1);

            RefundRequest req = new RefundRequest();
            req.setReason("full refund");

            PaymentVO vo = service.refund("202605091200000001", req);

            assertThat(vo.getStatus()).isEqualTo(3);
            verify(outboxService).enqueue(
                    eq("payment"),
                    eq("202605091200000001"),
                    eq("order-paid"),
                    argThat((OrderPaidMessage message) ->
                            message != null
                                    && "202605091200000001".equals(message.getOrderNo())
                                    && Integer.valueOf(5).equals(message.getStatus())
                                    && message.getPaidAt() != null));
        }

        @Test
        void shouldRefundPartialAmountAndKeepPaidStatus() {
            when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(payment);
            when(refundMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(refundMapper.insert(any(Refund.class))).thenReturn(1);
            when(paymentMapper.updateById(any(Payment.class))).thenReturn(1);

            RefundRequest req = new RefundRequest();
            req.setReason("partial refund");
            req.setAmount(new BigDecimal("1000.00"));

            PaymentVO vo = service.refund("202605091200000001", req);

            assertThat(vo.getStatus()).isEqualTo(1);
            verify(outboxService).enqueue(
                    eq("payment"),
                    eq("202605091200000001"),
                    eq("order-paid"),
                    argThat((OrderPaidMessage message) ->
                            message != null
                                    && "202605091200000001".equals(message.getOrderNo())
                                    && Integer.valueOf(1).equals(message.getStatus())
                                    && message.getPaidAt() != null));
        }

        @Test
        void shouldRejectDuplicateRefund() {
            when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(payment);
            when(refundMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            RefundRequest req = new RefundRequest();
            req.setReason("duplicate refund");

            assertThatThrownBy(() -> service.refund("202605091200000001", req))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(PaymentErrorCode.REFUND_ALREADY_EXISTS.getCode());
        }

        @Test
        void shouldRejectMerchantRefundForForeignOrder() {
            when(orderClient.listOrderNosByMerchant(2001L))
                    .thenReturn(Result.ok(List.of("OTHER-ORDER")));

            RefundRequest req = new RefundRequest();
            req.setReason("merchant refund");

            assertThatThrownBy(() -> service.refundByMerchant(2001L, "202605091200000001", req))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(PaymentErrorCode.PAYMENT_NOT_FOUND.getCode());
        }

        @Test
        void shouldRejectRefundAmountGreaterThanPayment() {
            when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(payment);
            when(refundMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

            RefundRequest req = new RefundRequest();
            req.setReason("over refund");
            req.setAmount(new BigDecimal("7000.00"));

            assertThatThrownBy(() -> service.refund("202605091200000001", req))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(PaymentErrorCode.REFUND_AMOUNT_INVALID.getCode());
        }
    }

    @Nested
    class AdminOutboxTests {
        @Test
        void shouldListPaymentOutboxMessagesFromSharedOutbox() {
            Page<OutboxMessage> page = new Page<>(1, 10, 1);
            OutboxMessage message = new OutboxMessage();
            message.setId(2001L);
            message.setAggregateId("ORD-9");
            message.setTopic("order-paid");
            message.setStatus(3);
            message.setRetryCount(1);
            page.setRecords(List.of(message));
            when(outboxService.queryMessages(any(OutboxQuery.class), eq(1), eq(10))).thenReturn(page);

            Page<OutboxMessageVO> result = service.listOutbox(new OutboxQuery("payment", "order-paid", 3, "ORD-9"), 1, 10);

            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getRecords().getFirst().getTopic()).isEqualTo("order-paid");
            verify(outboxService).queryMessages(argThat(query ->
                    "payment".equals(query.getAggregateType())
                            && "order-paid".equals(query.getTopic())
                            && Integer.valueOf(3).equals(query.getStatus())
                            && "ORD-9".equals(query.getAggregateId())), eq(1), eq(10));
        }

        @Test
        void shouldReturnPaymentOutboxSummary() {
            when(outboxService.summarize(any(OutboxQuery.class))).thenReturn(new OutboxSummary(1, 0, 2, 3));

            OutboxSummary summary = service.getOutboxSummary(new OutboxQuery("payment", "order-paid", 3, "ORD-9"));

            assertThat(summary.getPendingCount()).isEqualTo(1);
            assertThat(summary.getFailedCount()).isEqualTo(3);
            verify(outboxService).summarize(argThat(query ->
                    "payment".equals(query.getAggregateType())
                            && "order-paid".equals(query.getTopic())
                            && Integer.valueOf(3).equals(query.getStatus())
                            && "ORD-9".equals(query.getAggregateId())));
        }

        @Test
        void shouldRetryPaymentOutboxMessage() {
            when(outboxService.retryMessage(2001L)).thenReturn(true);

            int affected = service.retryOutboxMessage(2001L);

            assertThat(affected).isEqualTo(1);
            verify(outboxService).retryMessage(2001L);
        }

        @Test
        void shouldReturnZeroWhenPaymentOutboxMessageRetryDidNothing() {
            when(outboxService.retryMessage(2002L)).thenReturn(false);

            int affected = service.retryOutboxMessage(2002L);

            assertThat(affected).isZero();
            verify(outboxService).retryMessage(2002L);
        }

        @Test
        void shouldRetryPaymentOutboxBatch() {
            when(outboxService.retryBatch(any(OutboxQuery.class), eq(20))).thenReturn(2);

            int affected = service.retryOutboxBatch(new OutboxQuery("payment", "order-paid", 3, "ORD-9"), 20);

            assertThat(affected).isEqualTo(2);
            verify(outboxService).retryBatch(argThat(query ->
                    "payment".equals(query.getAggregateType())
                            && "order-paid".equals(query.getTopic())
                            && Integer.valueOf(3).equals(query.getStatus())
                            && "ORD-9".equals(query.getAggregateId())), eq(20));
        }
    }

    private OrderInternalVO buildOrder(String orderNo, Long id, BigDecimal totalAmount, Integer status) {
        OrderInternalVO order = new OrderInternalVO();
        order.setId(id);
        order.setOrderNo(orderNo);
        order.setTotalAmount(totalAmount);
        order.setStatus(status);
        return order;
    }
}
