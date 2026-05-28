package com.ecommerce.monitor.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.outbox.OutboxSummary;
import com.ecommerce.common.result.Result;
import com.ecommerce.monitor.client.InventoryReliabilityClient;
import com.ecommerce.monitor.client.OrderReliabilityClient;
import com.ecommerce.monitor.client.PaymentReliabilityClient;
import com.ecommerce.monitor.dto.request.OutboxRetryRequest;
import com.ecommerce.monitor.dto.response.InventoryEventLogVO;
import com.ecommerce.monitor.dto.response.InventoryEventSummaryVO;
import com.ecommerce.monitor.dto.response.OutboxMessageVO;
import com.ecommerce.monitor.dto.response.ReliabilityOverviewVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReliabilityServiceImplTest {

    @Mock
    private OrderReliabilityClient orderReliabilityClient;

    @Mock
    private PaymentReliabilityClient paymentReliabilityClient;

    @Mock
    private InventoryReliabilityClient inventoryReliabilityClient;

    @Test
    void overviewShouldMergeOrderPaymentAndInventoryReliabilityData() {
        ReliabilityServiceImpl service = new ReliabilityServiceImpl(
                orderReliabilityClient,
                paymentReliabilityClient,
                inventoryReliabilityClient,
                10,
                1,
                30,
                Clock.fixed(Instant.parse("2026-05-28T12:00:00Z"), ZoneId.of("Asia/Shanghai")));
        when(orderReliabilityClient.getOutboxSummary(null, null, null))
                .thenReturn(Result.ok(new OutboxSummary(1, 0, 2, 4, 0, LocalDateTime.of(2026, 5, 28, 19, 40))));
        when(paymentReliabilityClient.getOutboxSummary(null, null, null))
                .thenReturn(Result.ok(new OutboxSummary(2, 0, 1, 2, 1, LocalDateTime.of(2026, 5, 28, 19, 50))));
        when(inventoryReliabilityClient.getEventSummary(null, null, null))
                .thenReturn(Result.ok(new InventoryEventSummaryVO(3, 18)));

        ReliabilityOverviewVO overview = service.getOverview();

        assertThat(overview.getFailedOrderOutboxCount()).isEqualTo(4);
        assertThat(overview.getFailedPaymentOutboxCount()).isEqualTo(2);
        assertThat(overview.getExhaustedRetryCount()).isEqualTo(1);
        assertThat(overview.getInventoryProcessingCount()).isEqualTo(3);
        assertThat(overview.getInventoryProcessedCount()).isEqualTo(18);
    }

    @Test
    void overviewShouldEmitWarningsWhenThresholdsExceeded() {
        ReliabilityServiceImpl service = new ReliabilityServiceImpl(
                orderReliabilityClient,
                paymentReliabilityClient,
                inventoryReliabilityClient,
                3,
                0,
                10,
                Clock.fixed(Instant.parse("2026-05-28T12:00:00Z"), ZoneId.of("Asia/Shanghai")));
        when(orderReliabilityClient.getOutboxSummary(null, null, null))
                .thenReturn(Result.ok(new OutboxSummary(1, 0, 2, 4, 1, LocalDateTime.of(2026, 5, 28, 19, 30))));
        when(paymentReliabilityClient.getOutboxSummary(null, null, null))
                .thenReturn(Result.ok(new OutboxSummary(0, 0, 1, 0, 0, null)));
        when(inventoryReliabilityClient.getEventSummary(null, null, null))
                .thenReturn(Result.ok(new InventoryEventSummaryVO(0, 10)));

        ReliabilityOverviewVO overview = service.getOverview();

        assertThat(overview.getWarnings()).extracting("code")
                .contains("FAILED_BACKLOG", "EXHAUSTED_RETRIES", "OLDEST_RETRYABLE_AGE");
    }

    @Test
    void overviewShouldTolerateServiceFailure() {
        ReliabilityServiceImpl service = new ReliabilityServiceImpl(
                orderReliabilityClient,
                paymentReliabilityClient,
                inventoryReliabilityClient,
                10,
                1,
                30,
                Clock.fixed(Instant.parse("2026-05-28T12:00:00Z"), ZoneId.of("Asia/Shanghai")));
        when(orderReliabilityClient.getOutboxSummary(null, null, null)).thenThrow(new RuntimeException("order down"));
        when(paymentReliabilityClient.getOutboxSummary(null, null, null))
                .thenReturn(Result.ok(new OutboxSummary(0, 0, 1, 2, 0, null)));
        when(inventoryReliabilityClient.getEventSummary(null, null, null))
                .thenReturn(Result.ok(new InventoryEventSummaryVO(1, 9)));

        ReliabilityOverviewVO overview = service.getOverview();

        assertThat(overview.getFailedPaymentOutboxCount()).isEqualTo(2);
        assertThat(overview.getDegradedSections()).contains("order");
        assertThat(overview.getWarnings()).extracting("code").contains("DEGRADED");
    }

    @Test
    void retryOrderOutboxShouldForwardToOrderService() {
        ReliabilityServiceImpl service = new ReliabilityServiceImpl(
                orderReliabilityClient,
                paymentReliabilityClient,
                inventoryReliabilityClient,
                10,
                1,
                30,
                Clock.systemDefaultZone());
        when(orderReliabilityClient.retryOutboxMessage(any())).thenReturn(Result.ok(1));

        int affected = service.retryOutboxMessage("order", 1001L);

        assertThat(affected).isEqualTo(1);
        verify(orderReliabilityClient).retryOutboxMessage(any(OutboxRetryRequest.class));
    }

    @Test
    void listPaymentOutboxShouldDelegateToPaymentClient() {
        ReliabilityServiceImpl service = new ReliabilityServiceImpl(
                orderReliabilityClient,
                paymentReliabilityClient,
                inventoryReliabilityClient,
                10,
                1,
                30,
                Clock.systemDefaultZone());
        when(paymentReliabilityClient.listOutbox(3, "order-paid", "ORD-9", 1, 10))
                .thenReturn(Result.ok(new Page<OutboxMessageVO>()));

        service.listOutbox("payment", 3, "order-paid", "ORD-9", 1, 10);

        verify(paymentReliabilityClient).listOutbox(3, "order-paid", "ORD-9", 1, 10);
    }

    @Test
    void inventoryEventQueriesShouldDelegateToInventoryClient() {
        ReliabilityServiceImpl service = new ReliabilityServiceImpl(
                orderReliabilityClient,
                paymentReliabilityClient,
                inventoryReliabilityClient,
                10,
                1,
                30,
                Clock.systemDefaultZone());
        when(inventoryReliabilityClient.listEvents("order-created", "ORD-1", 1, 1, 10))
                .thenReturn(Result.ok(new Page<InventoryEventLogVO>()));

        service.listInventoryEvents("order-created", "ORD-1", 1, 1, 10);

        verify(inventoryReliabilityClient).listEvents("order-created", "ORD-1", 1, 1, 10);
    }
}
