package com.ecommerce.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.common.dto.AfterSaleRefundRequest;
import com.ecommerce.common.dto.CompensationRefundRequest;
import com.ecommerce.common.dto.ManualRefundCommand;
import com.ecommerce.common.dto.OrderRefundedMessage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.dto.OrderPaidMessage;
import com.ecommerce.common.outbox.OutboxMessage;
import com.ecommerce.common.outbox.OutboxQuery;
import com.ecommerce.common.outbox.OutboxSummary;
import com.ecommerce.common.outbox.OutboxService;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.common.result.ErrorCode;
import com.ecommerce.common.transaction.DistributedTransactionContext;
import com.ecommerce.common.util.SnowflakeUtils;
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
import com.ecommerce.payment.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentMapper paymentMapper;
    private final RefundMapper refundMapper;
    private final OrderClient orderClient;
    private final OutboxService outboxService;

    public PaymentServiceImpl(PaymentMapper paymentMapper, RefundMapper refundMapper, OrderClient orderClient,
                              OutboxService outboxService) {
        this.paymentMapper = paymentMapper;
        this.refundMapper = refundMapper;
        this.orderClient = orderClient;
        this.outboxService = outboxService;
    }

    @Override
    @Transactional
    public PaymentVO pay(Long userId, PayRequest request) {
        Payment existing = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>().eq(Payment::getOrderNo, request.getOrderNo()));
        if (existing != null && existing.getStatus() == 1) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_ALREADY_PAID);
        }

        var orderRes = orderClient.getOrderByOrderNo(request.getOrderNo(), userId);
        if (orderRes == null || orderRes.getData() == null || orderRes.getData().getId() == null) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND);
        }
        var order = orderRes.getData();
        if (order.getStatus() == null || order.getStatus() != 0) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_NOT_PAID);
        }
        if (order.getPaymentExpiresAt() != null && !LocalDateTime.now().isBefore(order.getPaymentExpiresAt())) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_ORDER_EXPIRED);
        }
        if (order.getTotalAmount() == null || request.getAmount() == null
                || order.getTotalAmount().signum() < 0 || order.getTotalAmount().compareTo(request.getAmount()) != 0) {
            throw new BusinessException(PaymentErrorCode.REFUND_AMOUNT_INVALID);
        }

        Payment payment = new Payment();
        payment.setId(SnowflakeUtils.nextId());
        payment.setPaymentNo(generatePaymentNo());
        payment.setOrderNo(request.getOrderNo());
        payment.setOrderId(order.getId());
        payment.setUserId(userId);
        payment.setAmount(request.getAmount());
        payment.setPayMethod(request.getPayMethod());
        payment.setStatus(1);
        payment.setPaidAt(LocalDateTime.now());
        try {
            paymentMapper.insert(payment);
        } catch (DuplicateKeyException ex) {
            throw alreadyPaid(ex);
        }

        DistributedTransactionContext transaction = startTransaction(
                payment.getOrderNo(),
                "payment-paid:" + payment.getOrderNo());
        outboxService.enqueue("payment", payment.getOrderNo(), "order-paid",
                new OrderPaidMessage(
                        payment.getOrderNo(),
                        1,
                        payment.getPaidAt(),
                        transaction.getTransactionId(),
                        transaction.getIdempotencyKey(),
                        null));

        return toVO(payment);
    }

    @Override
    public PaymentVO queryByOrderNo(String orderNo) {
        return toVO(requirePayment(new LambdaQueryWrapper<Payment>().eq(Payment::getOrderNo, orderNo)));
    }

    @Override
    public PaymentVO queryByOrderNoForUser(Long userId, String orderNo) {
        return toVO(requirePayment(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getUserId, userId)
                .eq(Payment::getOrderNo, orderNo)));
    }

    @Override
    public PaymentVO refund(String orderNo, RefundRequest request) {
        return requestManualRefund(orderNo, request, false);
    }

    @Override
    public PaymentVO refundAfterShipment(String orderNo, RefundRequest request) {
        return requestManualRefund(orderNo, request, true);
    }

    private record RefundRejection(int getCode, String getMessage) implements ErrorCode {}

    private PaymentVO requestManualRefund(String orderNo, RefundRequest request, boolean afterShipment) {
        Payment payment = requirePayment(new LambdaQueryWrapper<Payment>().eq(Payment::getOrderNo, orderNo));
        if (!Integer.valueOf(1).equals(payment.getStatus()) && !Integer.valueOf(3).equals(payment.getStatus())) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_NOT_PAID);
        }
        BigDecimal amount = request.getAmount();
        if (amount != null && (amount.signum() <= 0 || !hasCentPrecision(amount)
                || payment.getAmount() == null || amount.compareTo(payment.getAmount()) > 0)) {
            throw new BusinessException(PaymentErrorCode.REFUND_AMOUNT_INVALID);
        }

        String requestId = request.getClientRequestId() == null ? UUID.randomUUID().toString() : request.getClientRequestId();
        var reply = orderClient.requestManualRefund(orderNo,
                new ManualRefundCommand(requestId, request.getReason().trim(), amount, afterShipment));
        if (reply == null) throw new BusinessException(PaymentErrorCode.REFUND_REQUEST_UNAVAILABLE);
        if (reply.getCode() != 200) throw new BusinessException(new RefundRejection(reply.getCode(), reply.getMessage()));
        return toVO(payment);
    }

    @Override
    @Transactional
    public void refundAfterSale(AfterSaleRefundRequest request) {
        Payment payment = lockPayment(request.orderNo());
        String aggregate = request.manual() ? "manual-refund" : "after-sale";
        if (!outboxService.queryMessages(new OutboxQuery(aggregate, "order-refunded", null, request.outcomeKey()),
                1, 1).getRecords().isEmpty()) return;

        if (payment == null || payment.getAmount() == null || !Objects.equals(payment.getUserId(), request.userId())
                || !Objects.equals(payment.getOrderId(), request.orderId())) {
            publishRefundFailure(request, aggregate, "未找到对应的支付记录");
            return;
        }

        List<Refund> history = successfulRefunds(payment);
        BigDecimal refunded = refundedAmount(history);
        BigDecimal remaining = payment.getAmount().subtract(refunded);
        if (!request.manual() && Integer.valueOf(3).equals(payment.getStatus())
                && remaining.signum() == 0 && !history.isEmpty()) {
            publishRefundResult(payment, history.getFirst(), refunded, request, aggregate);
            return;
        }
        if (!Integer.valueOf(1).equals(payment.getStatus()) || remaining.signum() < 0) {
            publishRefundFailure(request, aggregate, "支付状态不支持此次退款");
            return;
        }

        BigDecimal amount = request.amount() == null ? remaining : request.amount();
        if (amount.signum() < 0 || !hasCentPrecision(amount) || amount.compareTo(remaining) > 0
                || (amount.signum() == 0 && (payment.getAmount().signum() != 0 || !history.isEmpty()))
                || (!request.manual() && amount.compareTo(remaining) != 0)) {
            log.error("Invalid refund amount: orderNo={}, request={}, requested={}, remaining={}",
                    request.orderNo(), request.outcomeKey(), amount, remaining);
            publishRefundFailure(request, aggregate, "申请金额与当前剩余可退金额不符");
            return;
        }

        Refund refund = insertRefund(payment, amount, request.reason());
        BigDecimal cumulative = refunded.add(amount);
        if (cumulative.compareTo(payment.getAmount()) == 0) {
            payment.setStatus(3);
            paymentMapper.updateById(payment);
        }
        publishRefundResult(payment, refund, cumulative, request, aggregate);
    }

    private boolean hasCentPrecision(BigDecimal amount) {
        return amount.stripTrailingZeros().scale() <= 2;
    }

    private List<Refund> successfulRefunds(Payment payment) {
        return refundMapper.selectList(new LambdaQueryWrapper<Refund>()
                .eq(Refund::getPaymentId, payment.getId()).eq(Refund::getStatus, 1).orderByDesc(Refund::getId));
    }

    private BigDecimal refundedAmount(List<Refund> refunds) {
        return refunds.stream().map(Refund::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Transactional
    public void refundCompensation(CompensationRefundRequest request) {
        Payment payment = lockPayment(request.orderNo());
        if (payment == null || payment.getAmount() == null || payment.getAmount().signum() < 0) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND);
        }
        if (!request.replay() && !outboxService.queryMessages(new OutboxQuery("compensation-refund", "order-refunded", null,
                request.orderNo()), 1, 1).getRecords().isEmpty()) return;

        List<Refund> previousRefunds = successfulRefunds(payment);
        BigDecimal remaining = payment.getAmount().subtract(refundedAmount(previousRefunds));
        Refund refund;
        if (Integer.valueOf(3).equals(payment.getStatus()) && remaining.signum() == 0 && !previousRefunds.isEmpty()) {
            refund = previousRefunds.getFirst();
        } else {
            if (!Integer.valueOf(1).equals(payment.getStatus()) || remaining.signum() < 0) {
                throw new BusinessException(PaymentErrorCode.PAYMENT_NOT_PAID);
            }
            refund = insertRefund(payment, remaining, request.reason());
            payment.setStatus(3);
            paymentMapper.updateById(payment);
        }
        var result = new OrderRefundedMessage(refund.getRefundNo(), payment.getOrderNo(), payment.getUserId(),
                refund.getAmount(), "FULL", "SUCCESS", LocalDateTime.now(), "compensation-refund:" + payment.getOrderNo());
        result.setCumulativeRefundAmount(payment.getAmount());
        outboxService.enqueue("compensation-refund", payment.getOrderNo(), "order-refunded", result);
    }

    private Payment lockPayment(String orderNo) {
        return paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getOrderNo, orderNo).last("FOR UPDATE"));
    }

    private Refund insertRefund(Payment payment, BigDecimal refundAmount, String reason) {
        Refund refund = new Refund();
        refund.setId(SnowflakeUtils.nextId());
        refund.setRefundNo("REF" + refund.getId());
        refund.setPaymentId(payment.getId());
        refund.setOrderNo(payment.getOrderNo());
        refund.setAmount(refundAmount);
        refund.setReason(reason);
        refund.setStatus(1);
        refundMapper.insert(refund);
        return refund;
    }

    private void publishRefundResult(Payment payment, Refund refund, BigDecimal cumulative,
                                     AfterSaleRefundRequest request, String aggregate) {
        String key = "payment-refund:" + payment.getOrderNo() + ":" + refund.getRefundNo();
        var event = new OrderRefundedMessage(refund.getRefundNo(), payment.getOrderNo(),
                payment.getUserId(), refund.getAmount(), cumulative.compareTo(payment.getAmount()) == 0 ? "FULL" : "PARTIAL",
                "SUCCESS", LocalDateTime.now(), key, request.afterSaleId(), null,
                request.refundAttempt(), cumulative, request.manualRefundKey());
        outboxService.enqueue(aggregate, request.outcomeKey(), "order-refunded", event);
    }

    private void publishRefundFailure(AfterSaleRefundRequest request, String aggregate, String reason) {
        var event = new OrderRefundedMessage(null, request.orderNo(), request.userId(), null, "FULL",
                "FAILED", LocalDateTime.now(), null, request.afterSaleId(), reason, request.refundAttempt(),
                null, request.manualRefundKey());
        outboxService.enqueue(aggregate, request.outcomeKey(), "order-refunded", event);
    }

    @Override
    public Page<PaymentVO> listAll(Integer status, int page, int size) {
        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Payment::getStatus, status);
        }
        wrapper.orderByDesc(Payment::getCreatedAt);
        Page<Payment> pageReq = new Page<>(page, size);
        paymentMapper.selectPage(pageReq, wrapper);
        return toPage(pageReq);
    }

    @Override
    public Page<PaymentVO> listByMerchant(Long merchantId, Integer status, int page, int size) {
        List<String> orderNos = loadMerchantOrderNos(merchantId);
        if (orderNos.isEmpty()) {
            return new Page<>(page, size, 0);
        }
        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<Payment>()
                .in(Payment::getOrderNo, orderNos)
                .eq(status != null, Payment::getStatus, status)
                .orderByDesc(Payment::getCreatedAt);
        Page<Payment> pageReq = new Page<>(page, size);
        paymentMapper.selectPage(pageReq, wrapper);
        return toPage(pageReq);
    }

    @Override
    public PaymentVO refundByMerchant(Long merchantId, String orderNo, RefundRequest request) {
        var ownership = orderClient.merchantOwnsEntireOrder(merchantId, orderNo);
        if (ownership == null || ownership.getCode() != 200 || !Boolean.TRUE.equals(ownership.getData())) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND);
        }
        return refund(orderNo, request);
    }

    @Override
    public Page<OutboxMessageVO> listOutbox(OutboxQuery query, int page, int size) {
        Page<OutboxMessage> result = outboxService.queryMessages(query, page, size);
        Page<OutboxMessageVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toOutboxVO).toList());
        return voPage;
    }

    @Override
    public OutboxSummary getOutboxSummary(OutboxQuery query) {
        return outboxService.summarize(query);
    }

    @Override
    public int retryOutboxMessage(Long messageId) {
        return outboxService.retryMessage(messageId) ? 1 : 0;
    }

    @Override
    public int retryOutboxBatch(OutboxQuery query, int limit) {
        return outboxService.retryBatch(query, limit);
    }

    private PaymentVO toVO(Payment payment) {
        PaymentVO vo = new PaymentVO();
        vo.setId(payment.getId());
        vo.setPaymentNo(payment.getPaymentNo());
        vo.setOrderNo(payment.getOrderNo());
        vo.setUserId(payment.getUserId());
        vo.setAmount(payment.getAmount());
        vo.setStatus(payment.getStatus());
        vo.setStatusText(statusText(payment.getStatus()));
        vo.setPayMethod(payment.getPayMethod());
        vo.setPaidAt(payment.getPaidAt());
        vo.setCreatedAt(payment.getCreatedAt());
        return vo;
    }

    private Payment requirePayment(LambdaQueryWrapper<Payment> wrapper) {
        Payment payment = paymentMapper.selectOne(wrapper);
        if (payment == null) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND);
        }
        return payment;
    }

    private Page<PaymentVO> toPage(Page<Payment> pageReq) {
        return new Page<PaymentVO>(pageReq.getCurrent(), pageReq.getSize(), pageReq.getTotal())
                .setRecords(pageReq.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
    }

    private OutboxMessageVO toOutboxVO(OutboxMessage message) {
        OutboxMessageVO vo = new OutboxMessageVO();
        vo.setId(message.getId());
        vo.setAggregateId(message.getAggregateId());
        vo.setTopic(message.getTopic());
        vo.setStatus(message.getStatus());
        vo.setRetryCount(message.getRetryCount());
        vo.setLastError(message.getLastError());
        vo.setNextRetryAt(message.getNextRetryAt());
        vo.setCreatedAt(message.getCreatedAt());
        return vo;
    }

    private List<String> loadMerchantOrderNos(Long merchantId) {
        try {
            var response = orderClient.listOrderNosByMerchant(merchantId);
            return response.getData() != null ? response.getData() : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private String generatePaymentNo() {
        return "PAY" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", SnowflakeUtils.nextId() % 10000);
    }

    private DistributedTransactionContext startTransaction(String businessNo, String idempotencyKey) {
        return DistributedTransactionContext.start(
                SnowflakeUtils.nextIdStr(),
                businessNo,
                idempotencyKey);
    }

    private BusinessException alreadyPaid(DuplicateKeyException ex) {
        BusinessException businessException = new BusinessException(PaymentErrorCode.PAYMENT_ALREADY_PAID);
        businessException.initCause(ex);
        return businessException;
    }

    private String statusText(Integer status) {
        if (status == null) return "鏈煡";
        if (status == 0) return "寰呮敮浠?";
        if (status == 1) return "宸叉敮浠?";
        if (status == 2) return "閫€娆句腑";
        if (status == 3) return "宸查€€娆?";
        if (status == 4) return "宸插叧闂?";
        return "鏈煡";
    }
}
