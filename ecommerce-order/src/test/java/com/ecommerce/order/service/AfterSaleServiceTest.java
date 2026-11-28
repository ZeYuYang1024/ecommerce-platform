package com.ecommerce.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.common.constant.OrderStatus;
import com.ecommerce.common.dto.AfterSaleRefundRequest;
import com.ecommerce.common.dto.OrderInventoryMessage;
import com.ecommerce.common.dto.OrderRefundedMessage;
import com.ecommerce.common.outbox.OutboxService;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.order.client.ProductSpuClient;
import com.ecommerce.order.common.OrderErrorCode;
import com.ecommerce.order.entity.AfterSale;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.mapper.AfterSaleMapper;
import com.ecommerce.order.mapper.OrderItemMapper;
import com.ecommerce.order.mapper.OrderMapper;
import com.ecommerce.order.mapper.ShippingClaimMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AfterSaleServiceTest {
    @Mock private OrderMapper orders;
    @Mock private OrderItemMapper items;
    @Mock private AfterSaleMapper cases;
    @Mock private ShippingClaimMapper shippingClaims;
    @Mock private ShippingClaimReaper claimReaper;
    @Mock private ProductSpuClient catalog;
    @Mock private OutboxService outbox;
    @InjectMocks private AfterSaleService service;

    @Test
    void rejectsBuyerApplicationAfterShippingWasClaimed() {
        when(orders.selectOne(any(LambdaQueryWrapper.class))).thenReturn(paidOrder());
        when(claimReaper.hasShipping(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.apply(9L, "ORDER-1", "购买有误"))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(OrderErrorCode.AFTER_SALE_NOT_ELIGIBLE);
        verify(cases, never()).insert(any(AfterSale.class));
    }

    @Test
    void blocksShippingWhileTheRefundIsBeingProcessed() {
        when(orders.selectOne(any(LambdaQueryWrapper.class))).thenReturn(paidOrder());
        when(cases.selectOne(any(LambdaQueryWrapper.class))).thenReturn(application(AfterSale.Status.REFUNDING));

        assertThatThrownBy(() -> service.claimShipping(1L, "SHIP-1", 10L, false))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(OrderErrorCode.AFTER_SALE_BLOCKS_SHIPPING);
        verify(shippingClaims, never()).claim(any(), any(), any(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    void rejectsReviewByADifferentMerchant() {
        when(cases.selectOne(any(LambdaQueryWrapper.class))).thenReturn(application(AfterSale.Status.PENDING));

        assertThatThrownBy(() -> service.review(777L, 11L, true, null))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(OrderErrorCode.AFTER_SALE_NOT_FOUND);
        verify(cases, never()).updateById(any(AfterSale.class));
    }

    @Test
    void aRepeatedRefundOutcomeReleasesStockOnlyOnce() {
        AfterSale application = application(AfterSale.Status.REFUNDING);
        when(orders.selectOne(any(LambdaQueryWrapper.class))).thenReturn(paidOrder());
        when(cases.selectOne(any(LambdaQueryWrapper.class))).thenReturn(application);
        when(orders.transitionStatus(1L, OrderStatus.PAID, OrderStatus.REFUNDED)).thenReturn(true);
        OrderItem item = new OrderItem();
        item.setSkuId(3L);
        item.setQuantity(2);
        when(items.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(item));
        var outcome = new OrderRefundedMessage("REF-1", "ORDER-1", 9L, new BigDecimal("10.00"),
                "FULL", "SUCCESS", LocalDateTime.now(), "refund-result", 11L, null, 0);

        service.finish(outcome);
        service.finish(outcome);

        assertThat(application.getStatus()).isEqualTo(AfterSale.Status.REFUNDED);
        assertThat(application.getRefundNo()).isEqualTo("REF-1");
        verify(outbox, times(1)).enqueue(eq("after-sale"), eq("ORDER-1"), eq(AfterSaleService.RELEASE_TOPIC),
                any(OrderInventoryMessage.class));
    }

    @Test
    void retryUsesNewAttemptAndLateFailureDoesNotReplaceIt() {
        AfterSale application = application(AfterSale.Status.FAILED);
        application.setRefundAttempt(1);
        application.setFailureReason("旧错误");
        when(cases.selectById(11L)).thenReturn(application);
        when(orders.selectOne(any(LambdaQueryWrapper.class))).thenReturn(paidOrder());
        when(cases.selectOne(any(LambdaQueryWrapper.class))).thenReturn(application);

        service.retryRefund(11L);
        service.finish(new OrderRefundedMessage(null, "ORDER-1", 9L, null, "FULL", "FAILED",
                LocalDateTime.now(), null, 11L, "旧错误", 1));

        assertThat(application.getRefundAttempt()).isEqualTo(2);
        assertThat(application.getStatus()).isEqualTo(AfterSale.Status.REFUNDING);
        assertThat(application.getFailureReason()).isNull();
        verify(outbox).enqueue(eq("after-sale"), eq("ORDER-1"), eq(AfterSaleRefundRequest.TOPIC),
                argThat((AfterSaleRefundRequest request) -> request.refundAttempt() == 2));
    }

    @Test
    void recordsPaymentSuccessWithoutStockReleaseWhenOrderHasShipped() {
        Order shipped = paidOrder();
        shipped.setStatus(OrderStatus.SHIPPED);
        AfterSale application = application(AfterSale.Status.REFUNDING);
        when(orders.selectOne(any(LambdaQueryWrapper.class))).thenReturn(shipped);
        when(cases.selectOne(any(LambdaQueryWrapper.class))).thenReturn(application);

        service.finish(outcome("FULL", "10.00", 11L));

        assertThat(application.getStatus()).isEqualTo(AfterSale.Status.REFUNDED);
        assertThat(application.getFailureReason()).contains("订单状态迁移失败");
        verify(orders, never()).transitionStatus(any(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt());
        verify(outbox, never()).enqueue(any(), any(), any(), any());
    }

    @Test
    void stopsOnInvalidRefundAmountAndRecordsFailure() {
        AfterSale application = application(AfterSale.Status.REFUNDING);
        when(orders.selectOne(any(LambdaQueryWrapper.class))).thenReturn(paidOrder());
        when(cases.selectOne(any(LambdaQueryWrapper.class))).thenReturn(application);

        service.finish(outcome("FULL", "9.00", 11L));

        assertThat(application.getStatus()).isEqualTo(AfterSale.Status.FAILED);
        assertThat(application.getFailureReason()).contains("金额");
        verify(outbox, never()).enqueue(any(), any(), any(), any());
    }

    @Test
    void manualPartialLeavesOrderPaidButManualFullTransitions() {
        when(orders.selectOne(any(LambdaQueryWrapper.class))).thenReturn(paidOrder());
        when(orders.transitionStatus(1L, OrderStatus.PAID, OrderStatus.REFUNDED)).thenReturn(true);
        when(items.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        service.finish(outcome("PARTIAL", "9.00", null));
        service.finish(outcome("FULL", "10.00", null));

        verify(orders).transitionStatus(1L, OrderStatus.PAID, OrderStatus.REFUNDED);
        verify(outbox).enqueue(eq("after-sale"), eq("ORDER-1"), eq(AfterSaleService.RELEASE_TOPIC),
                any(OrderInventoryMessage.class));
    }

    private OrderRefundedMessage outcome(String type, String amount, Long caseId) {
        return new OrderRefundedMessage("REF-1", "ORDER-1", 9L, new BigDecimal(amount),
                type, "SUCCESS", LocalDateTime.now(), "refund-result", caseId, null, 0);
    }

    private Order paidOrder() {
        Order order = new Order();
        order.setId(1L);
        order.setUserId(9L);
        order.setOrderNo("ORDER-1");
        order.setStatus(OrderStatus.PAID);
        order.setInventoryReserved(true);
        order.setTotalAmount(new BigDecimal("10.00"));
        return order;
    }

    private AfterSale application(AfterSale.Status status) {
        AfterSale application = new AfterSale();
        application.setId(11L);
        application.setOrderId(1L);
        application.setOrderNo("ORDER-1");
        application.setMerchantId(7L);
        application.setUserId(9L);
        application.setAmount(new BigDecimal("10.00"));
        application.setStatus(status);
        return application;
    }
}
