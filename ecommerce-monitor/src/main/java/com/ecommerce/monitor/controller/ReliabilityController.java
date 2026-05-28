package com.ecommerce.monitor.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.Result;
import com.ecommerce.monitor.dto.request.OutboxRetryRequest;
import com.ecommerce.monitor.dto.response.InventoryEventLogVO;
import com.ecommerce.monitor.dto.response.InventoryEventSummaryVO;
import com.ecommerce.monitor.dto.response.OutboxMessageVO;
import com.ecommerce.monitor.dto.response.ReliabilityOverviewVO;
import com.ecommerce.monitor.service.ReliabilityService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/api/reliability")
public class ReliabilityController {

    private final ReliabilityService reliabilityService;

    public ReliabilityController(ReliabilityService reliabilityService) {
        this.reliabilityService = reliabilityService;
    }

    @GetMapping("/overview")
    public Result<ReliabilityOverviewVO> overview() {
        return Result.ok(reliabilityService.getOverview());
    }

    @GetMapping("/outbox/{service}/messages")
    public Result<Page<OutboxMessageVO>> listOutbox(@PathVariable String service,
                                                    @RequestParam(required = false) Integer status,
                                                    @RequestParam(required = false) String topic,
                                                    @RequestParam(required = false) String aggregateId,
                                                    @RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "10") int size) {
        return Result.ok(reliabilityService.listOutbox(service, status, topic, aggregateId, page, size));
    }

    @PostMapping("/outbox/{service}/retry")
    public Result<Integer> retryOutboxMessage(@PathVariable String service, @RequestBody OutboxRetryRequest request) {
        if (request.getMessageId() == null) {
            throw new IllegalArgumentException("messageId is required");
        }
        return Result.ok(reliabilityService.retryOutboxMessage(service, request.getMessageId()));
    }

    @PostMapping("/outbox/{service}/retry-batch")
    public Result<Integer> retryOutboxBatch(@PathVariable String service, @RequestBody OutboxRetryRequest request) {
        if (request.getLimit() == null) {
            throw new IllegalArgumentException("limit is required");
        }
        if (request.getStatus() == null && isBlank(request.getTopic()) && isBlank(request.getAggregateId())) {
            throw new IllegalArgumentException("retry batch requires a filter");
        }
        return Result.ok(reliabilityService.retryOutboxBatch(
                service, request.getStatus(), request.getTopic(), request.getAggregateId(), request.getLimit()));
    }

    @GetMapping("/inventory/events")
    public Result<Page<InventoryEventLogVO>> listInventoryEvents(@RequestParam(required = false) String topic,
                                                                 @RequestParam(required = false) String orderNo,
                                                                 @RequestParam(required = false) Integer status,
                                                                 @RequestParam(defaultValue = "1") int page,
                                                                 @RequestParam(defaultValue = "10") int size) {
        return Result.ok(reliabilityService.listInventoryEvents(topic, orderNo, status, page, size));
    }

    @GetMapping("/inventory/events/summary")
    public Result<InventoryEventSummaryVO> inventorySummary(@RequestParam(required = false) String topic,
                                                            @RequestParam(required = false) String orderNo,
                                                            @RequestParam(required = false) Integer status) {
        return Result.ok(reliabilityService.getInventorySummary(topic, orderNo, status));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
