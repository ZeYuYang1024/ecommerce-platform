package com.ecommerce.payment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.outbox.OutboxQuery;
import com.ecommerce.common.outbox.OutboxSummary;
import com.ecommerce.common.result.Result;
import com.ecommerce.payment.dto.request.OutboxRetryRequest;
import com.ecommerce.payment.dto.request.RefundRequest;
import com.ecommerce.payment.dto.response.OutboxMessageVO;
import com.ecommerce.payment.dto.response.PaymentVO;
import com.ecommerce.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminPaymentController {

    private final PaymentService paymentService;

    public AdminPaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/payment")
    public Result<Page<PaymentVO>> listAll(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(paymentService.listAll(status, page, size));
    }

    @GetMapping("/payment/outbox")
    public Result<Page<OutboxMessageVO>> listOutbox(@RequestParam(required = false) Integer status,
                                                    @RequestParam(required = false) String topic,
                                                    @RequestParam(required = false) String aggregateId,
                                                    @RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "10") int size) {
        return Result.ok(paymentService.listOutbox(new OutboxQuery("payment", topic, status, aggregateId), page, size));
    }

    @GetMapping("/payment/outbox/summary")
    public Result<OutboxSummary> getOutboxSummary(@RequestParam(required = false) Integer status,
                                                  @RequestParam(required = false) String topic,
                                                  @RequestParam(required = false) String aggregateId) {
        return Result.ok(paymentService.getOutboxSummary(new OutboxQuery("payment", topic, status, aggregateId)));
    }

    @PostMapping("/payment/outbox/retry")
    public Result<Integer> retryOutbox(@Valid @RequestBody OutboxRetryRequest request) {
        if (request.getMessageId() == null) {
            throw new IllegalArgumentException("messageId is required");
        }
        return Result.ok(paymentService.retryOutboxMessage(request.getMessageId()));
    }

    @PostMapping("/payment/outbox/retry-batch")
    public Result<Integer> retryOutboxBatch(@Valid @RequestBody OutboxRetryRequest request) {
        if (request.getLimit() == null) {
            throw new IllegalArgumentException("limit is required");
        }
        if (request.getStatus() == null && isBlank(request.getTopic()) && isBlank(request.getAggregateId())) {
            throw new IllegalArgumentException("retry batch requires a filter");
        }
        return Result.ok(paymentService.retryOutboxBatch(
                new OutboxQuery("payment", request.getTopic(), request.getStatus(), request.getAggregateId()),
                request.getLimit()));
    }

    @PostMapping("/payment/{orderNo}/refund")
    public Result<PaymentVO> refund(@PathVariable String orderNo,
                                     @Valid @RequestBody RefundRequest request) {
        return Result.ok(paymentService.refund(orderNo, request));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
