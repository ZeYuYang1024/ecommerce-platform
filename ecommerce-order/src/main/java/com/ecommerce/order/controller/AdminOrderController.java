package com.ecommerce.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.outbox.OutboxQuery;
import com.ecommerce.common.outbox.OutboxSummary;
import com.ecommerce.common.result.Result;
import com.ecommerce.order.dto.request.UpdateOrderStatusRequest;
import com.ecommerce.common.dto.ReconOrderVO;
import com.ecommerce.order.dto.request.OutboxRetryRequest;
import com.ecommerce.order.dto.response.OutboxMessageVO;
import com.ecommerce.order.dto.response.OrderVO;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/orders")
    public Result<Page<OrderVO>> listAll(@RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "10") int size,
                                          @RequestParam(required = false) Integer status) {
        return Result.ok(orderService.listAll(page, size, status));
    }

    @GetMapping("/orders/outbox")
    public Result<Page<OutboxMessageVO>> listOutbox(@RequestParam(required = false) Integer status,
                                                    @RequestParam(required = false) String topic,
                                                    @RequestParam(required = false) String aggregateId,
                                                    @RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "10") int size) {
        return Result.ok(orderService.listOutbox(new OutboxQuery("order", topic, status, aggregateId), page, size));
    }

    @GetMapping("/orders/outbox/summary")
    public Result<OutboxSummary> getOutboxSummary(@RequestParam(required = false) Integer status,
                                                  @RequestParam(required = false) String topic,
                                                  @RequestParam(required = false) String aggregateId) {
        return Result.ok(orderService.getOutboxSummary(new OutboxQuery("order", topic, status, aggregateId)));
    }

    @PostMapping("/orders/outbox/retry")
    public Result<Integer> retryOutbox(@Valid @RequestBody OutboxRetryRequest request) {
        if (request.getMessageId() == null) {
            throw new IllegalArgumentException("messageId is required");
        }
        return Result.ok(orderService.retryOutboxMessage(request.getMessageId()));
    }

    @PostMapping("/orders/outbox/retry-batch")
    public Result<Integer> retryOutboxBatch(@Valid @RequestBody OutboxRetryRequest request) {
        if (request.getLimit() == null) {
            throw new IllegalArgumentException("limit is required");
        }
        if (request.getStatus() == null && isBlank(request.getTopic()) && isBlank(request.getAggregateId())) {
            throw new IllegalArgumentException("retry batch requires a filter");
        }
        return Result.ok(orderService.retryOutboxBatch(
                new OutboxQuery("order", request.getTopic(), request.getStatus(), request.getAggregateId()),
                request.getLimit()));
    }

    @PutMapping("/orders/{id}/ship")
    public Result<Void> ship(@PathVariable Long id,
                             @RequestHeader(value = "X-User-Type", defaultValue = "super_admin") String userType,
                             @RequestHeader(value = "X-Merchant-Id", required = false) Long merchantId) {
        orderService.markShipped(id, userType, merchantId);
        return Result.ok();
    }

    @GetMapping("/merchant/orders")
    public Result<Page<OrderVO>> listByMerchant(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int size,
                                                  @RequestParam(required = false) Integer status,
                                                  @RequestHeader("X-Merchant-Id") Long merchantId) {
        return Result.ok(orderService.listByMerchant(merchantId, page, size, status));
    }

    @PutMapping("/orders/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @Valid @RequestBody UpdateOrderStatusRequest request,
                                     @RequestHeader(value = "X-User-Type", defaultValue = "super_admin") String userType,
                                     @RequestHeader(value = "X-Merchant-Id", required = false) Long merchantId) {
        orderService.updateStatus(id, request.getStatus(), userType, merchantId);
        return Result.ok();
    }

    @GetMapping("/orders/recon")
    public Result<List<ReconOrderVO>> getOrdersForRecon(@RequestParam("start") String start,
                                                         @RequestParam("end") String end) {
        LocalDateTime startTime = null, endTime = null;
        try {
            if (start != null && !start.isEmpty()) startTime = LocalDateTime.parse(start);
            if (end != null && !end.isEmpty()) endTime = LocalDateTime.parse(end);
        } catch (Exception e) {
            return Result.fail(400, "日期格式错误");
        }
        List<Order> orders = orderService.listForRecon(startTime, endTime);
        List<ReconOrderVO> result = orders.stream().map(o -> {
            ReconOrderVO vo = new ReconOrderVO();
            vo.setOrderNo(o.getOrderNo());
            vo.setAmount(o.getTotalAmount());
            vo.setStatus(o.getStatus());
            return vo;
        }).collect(Collectors.toList());
        return Result.ok(result);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
