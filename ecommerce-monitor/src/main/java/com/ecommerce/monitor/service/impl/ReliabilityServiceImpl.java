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
import com.ecommerce.monitor.dto.response.ReliabilityWarningVO;
import com.ecommerce.monitor.service.ReliabilityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReliabilityServiceImpl implements ReliabilityService {

    private final OrderReliabilityClient orderReliabilityClient;
    private final PaymentReliabilityClient paymentReliabilityClient;
    private final InventoryReliabilityClient inventoryReliabilityClient;
    private final int failedBacklogThreshold;
    private final int exhaustedRetriesThreshold;
    private final int oldestRetryableAgeMinutesThreshold;
    private final Clock clock;

    @Autowired
    public ReliabilityServiceImpl(OrderReliabilityClient orderReliabilityClient,
                                  PaymentReliabilityClient paymentReliabilityClient,
                                  InventoryReliabilityClient inventoryReliabilityClient,
                                  @Value("${reliability.thresholds.failed-backlog:20}") int failedBacklogThreshold,
                                  @Value("${reliability.thresholds.exhausted-retries:1}") int exhaustedRetriesThreshold,
                                  @Value("${reliability.thresholds.oldest-retryable-age-minutes:30}") int oldestRetryableAgeMinutesThreshold) {
        this(orderReliabilityClient, paymentReliabilityClient, inventoryReliabilityClient,
                failedBacklogThreshold, exhaustedRetriesThreshold, oldestRetryableAgeMinutesThreshold, Clock.systemDefaultZone());
    }

    ReliabilityServiceImpl(OrderReliabilityClient orderReliabilityClient,
                           PaymentReliabilityClient paymentReliabilityClient,
                           InventoryReliabilityClient inventoryReliabilityClient,
                           int failedBacklogThreshold,
                           int exhaustedRetriesThreshold,
                           int oldestRetryableAgeMinutesThreshold,
                           Clock clock) {
        this.orderReliabilityClient = orderReliabilityClient;
        this.paymentReliabilityClient = paymentReliabilityClient;
        this.inventoryReliabilityClient = inventoryReliabilityClient;
        this.failedBacklogThreshold = failedBacklogThreshold;
        this.exhaustedRetriesThreshold = exhaustedRetriesThreshold;
        this.oldestRetryableAgeMinutesThreshold = oldestRetryableAgeMinutesThreshold;
        this.clock = clock;
    }

    @Override
    public ReliabilityOverviewVO getOverview() {
        ReliabilityOverviewVO overview = new ReliabilityOverviewVO();
        List<String> degradedSections = new ArrayList<>();
        OutboxSummary orderSummary = safeLoadSummary("order", degradedSections);
        OutboxSummary paymentSummary = safeLoadSummary("payment", degradedSections);
        InventoryEventSummaryVO inventorySummary = safeLoadInventorySummary(degradedSections);

        overview.setFailedOrderOutboxCount(orderSummary.getFailedCount());
        overview.setFailedPaymentOutboxCount(paymentSummary.getFailedCount());
        overview.setOrderPendingCount(orderSummary.getPendingCount());
        overview.setPaymentPendingCount(paymentSummary.getPendingCount());
        overview.setExhaustedRetryCount(orderSummary.getExhaustedCount() + paymentSummary.getExhaustedCount());
        overview.setInventoryProcessingCount(inventorySummary.getProcessingCount());
        overview.setInventoryProcessedCount(inventorySummary.getProcessedCount());
        overview.setDegradedSections(degradedSections);

        Long oldestRetryableAge = calculateOldestRetryableAgeMinutes(orderSummary, paymentSummary);
        overview.setOldestRetryableAgeMinutes(oldestRetryableAge);
        addWarnings(overview, oldestRetryableAge);
        return overview;
    }

    @Override
    public Page<OutboxMessageVO> listOutbox(String service, Integer status, String topic, String aggregateId, int page, int size) {
        return switch (service) {
            case "order" -> requireData(orderReliabilityClient.listOutbox(status, topic, aggregateId, page, size));
            case "payment" -> requireData(paymentReliabilityClient.listOutbox(status, topic, aggregateId, page, size));
            default -> throw new IllegalArgumentException("unsupported service: " + service);
        };
    }

    @Override
    public int retryOutboxMessage(String service, Long messageId) {
        OutboxRetryRequest request = new OutboxRetryRequest();
        request.setMessageId(messageId);
        return switch (service) {
            case "order" -> requireData(orderReliabilityClient.retryOutboxMessage(request));
            case "payment" -> requireData(paymentReliabilityClient.retryOutboxMessage(request));
            default -> throw new IllegalArgumentException("unsupported service: " + service);
        };
    }

    @Override
    public int retryOutboxBatch(String service, Integer status, String topic, String aggregateId, int limit) {
        OutboxRetryRequest request = new OutboxRetryRequest();
        request.setStatus(status);
        request.setTopic(topic);
        request.setAggregateId(aggregateId);
        request.setLimit(limit);
        return switch (service) {
            case "order" -> requireData(orderReliabilityClient.retryOutboxBatch(request));
            case "payment" -> requireData(paymentReliabilityClient.retryOutboxBatch(request));
            default -> throw new IllegalArgumentException("unsupported service: " + service);
        };
    }

    @Override
    public Page<InventoryEventLogVO> listInventoryEvents(String topic, String orderNo, Integer status, int page, int size) {
        return requireData(inventoryReliabilityClient.listEvents(topic, orderNo, status, page, size));
    }

    @Override
    public InventoryEventSummaryVO getInventorySummary(String topic, String orderNo, Integer status) {
        return requireData(inventoryReliabilityClient.getEventSummary(topic, orderNo, status));
    }

    private OutboxSummary safeLoadSummary(String service, List<String> degradedSections) {
        try {
            return switch (service) {
                case "order" -> requireData(orderReliabilityClient.getOutboxSummary(null, null, null));
                case "payment" -> requireData(paymentReliabilityClient.getOutboxSummary(null, null, null));
                default -> throw new IllegalArgumentException("unsupported service: " + service);
            };
        } catch (Exception ex) {
            degradedSections.add(service);
            return new OutboxSummary();
        }
    }

    private InventoryEventSummaryVO safeLoadInventorySummary(List<String> degradedSections) {
        try {
            return requireData(inventoryReliabilityClient.getEventSummary(null, null, null));
        } catch (Exception ex) {
            degradedSections.add("inventory");
            return new InventoryEventSummaryVO(0, 0);
        }
    }

    private Long calculateOldestRetryableAgeMinutes(OutboxSummary orderSummary, OutboxSummary paymentSummary) {
        LocalDateTime oldest = minTime(orderSummary.getOldestRetryableCreatedAt(), paymentSummary.getOldestRetryableCreatedAt());
        if (oldest == null) {
            return null;
        }
        long minutes = Duration.between(oldest, LocalDateTime.now(clock)).toMinutes();
        return Math.max(minutes, 0);
    }

    private LocalDateTime minTime(LocalDateTime left, LocalDateTime right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.isBefore(right) ? left : right;
    }

    private void addWarnings(ReliabilityOverviewVO overview, Long oldestRetryableAgeMinutes) {
        long failedBacklog = overview.getFailedOrderOutboxCount() + overview.getFailedPaymentOutboxCount();
        if (failedBacklog > failedBacklogThreshold) {
            overview.getWarnings().add(new ReliabilityWarningVO(
                    "FAILED_BACKLOG", "warning",
                    "Failed outbox backlog exceeded threshold: " + failedBacklog));
        }
        if (overview.getExhaustedRetryCount() > exhaustedRetriesThreshold) {
            overview.getWarnings().add(new ReliabilityWarningVO(
                    "EXHAUSTED_RETRIES", "critical",
                    "Retry-exhausted outbox messages detected: " + overview.getExhaustedRetryCount()));
        }
        if (oldestRetryableAgeMinutes != null && oldestRetryableAgeMinutes > oldestRetryableAgeMinutesThreshold) {
            overview.getWarnings().add(new ReliabilityWarningVO(
                    "OLDEST_RETRYABLE_AGE", "warning",
                    "Oldest retryable outbox message age exceeded threshold: " + oldestRetryableAgeMinutes + " minutes"));
        }
        if (!overview.getDegradedSections().isEmpty()) {
            overview.getWarnings().add(new ReliabilityWarningVO(
                    "DEGRADED", "warning",
                    "Partial service degradation: " + String.join(", ", overview.getDegradedSections())));
        }
    }

    private <T> T requireData(Result<T> result) {
        if (result == null || result.getCode() != 200 || result.getData() == null) {
            throw new IllegalStateException("reliability upstream returned no data");
        }
        return result.getData();
    }
}
