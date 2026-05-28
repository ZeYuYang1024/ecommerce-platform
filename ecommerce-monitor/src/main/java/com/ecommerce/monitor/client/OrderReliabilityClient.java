package com.ecommerce.monitor.client;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.outbox.OutboxSummary;
import com.ecommerce.common.result.Result;
import com.ecommerce.monitor.dto.request.OutboxRetryRequest;
import com.ecommerce.monitor.dto.response.OutboxMessageVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "ecommerce-order")
public interface OrderReliabilityClient {

    @GetMapping("/api/v1/admin/orders/outbox")
    Result<Page<OutboxMessageVO>> listOutbox(@RequestParam(required = false) Integer status,
                                             @RequestParam(required = false) String topic,
                                             @RequestParam(required = false) String aggregateId,
                                             @RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size);

    @GetMapping("/api/v1/admin/orders/outbox/summary")
    Result<OutboxSummary> getOutboxSummary(@RequestParam(required = false) Integer status,
                                           @RequestParam(required = false) String topic,
                                           @RequestParam(required = false) String aggregateId);

    @PostMapping("/api/v1/admin/orders/outbox/retry")
    Result<Integer> retryOutboxMessage(@RequestBody OutboxRetryRequest request);

    @PostMapping("/api/v1/admin/orders/outbox/retry-batch")
    Result<Integer> retryOutboxBatch(@RequestBody OutboxRetryRequest request);
}
