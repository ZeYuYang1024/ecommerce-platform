package com.ecommerce.member.consumer;

import com.ecommerce.common.dto.OrderRefundedMessage;
import com.ecommerce.member.dto.request.internal.RefundCompensationRequest;
import com.ecommerce.member.dto.response.internal.RefundCompensationResult;
import com.ecommerce.member.service.RefundCompensationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderRefundedConsumerTest {

    @Mock
    private RefundCompensationService refundCompensationService;

    @InjectMocks
    private OrderRefundedConsumer consumer;

    @Test
    void shouldMapRefundEventToCompensationRequest() {
        RefundCompensationResult result = new RefundCompensationResult();
        result.setRestoredPoints(120);
        result.setReversedGrowth(88);
        when(refundCompensationService.compensate(org.mockito.ArgumentMatchers.any())).thenReturn(result);

        OrderRefundedMessage message = new OrderRefundedMessage(
                "REF-1",
                "ORD-1",
                10001L,
                new BigDecimal("88.00"),
                "PARTIAL",
                "SUCCESS",
                LocalDateTime.of(2026, 6, 2, 13, 30),
                "payment-refund:ORD-1:REF-1");

        consumer.onMessage(message);

        ArgumentCaptor<RefundCompensationRequest> captor = ArgumentCaptor.forClass(RefundCompensationRequest.class);
        verify(refundCompensationService).compensate(captor.capture());
        RefundCompensationRequest request = captor.getValue();
        assertThat(request.getRefundNo()).isEqualTo("REF-1");
        assertThat(request.getOrderNo()).isEqualTo("ORD-1");
        assertThat(request.getUserId()).isEqualTo(10001L);
        assertThat(request.getRefundAmount()).isEqualByComparingTo("88.00");
        assertThat(request.getRefundType()).isEqualTo("PARTIAL");
        assertThat(request.getIdempotencyKey()).isEqualTo("payment-refund:ORD-1:REF-1");
    }
}
