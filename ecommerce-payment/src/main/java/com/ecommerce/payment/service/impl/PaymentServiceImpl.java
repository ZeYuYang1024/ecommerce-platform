package com.ecommerce.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.dto.OrderPaidMessage;
import com.ecommerce.common.outbox.OutboxMessage;
import com.ecommerce.common.outbox.OutboxQuery;
import com.ecommerce.common.outbox.OutboxSummary;
import com.ecommerce.common.outbox.OutboxService;
import com.ecommerce.common.result.BusinessException;
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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PaymentServiceImpl implements PaymentService {

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
        if (order.getTotalAmount() == null || request.getAmount() == null
                || order.getTotalAmount().compareTo(request.getAmount()) != 0) {
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
    @Transactional
    public PaymentVO refund(String orderNo, RefundRequest request) {
        Payment payment = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>().eq(Payment::getOrderNo, orderNo));
        if (payment == null) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND);
        }
        if (payment.getStatus() != 1) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_NOT_PAID);
        }

        Long count = refundMapper.selectCount(
                new LambdaQueryWrapper<Refund>().eq(Refund::getOrderNo, orderNo));
        if (count > 0) {
            throw new BusinessException(PaymentErrorCode.REFUND_ALREADY_EXISTS);
        }

        BigDecimal refundAmount = request.getAmount() != null ? request.getAmount() : payment.getAmount();
        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0 || refundAmount.compareTo(payment.getAmount()) > 0) {
            throw new BusinessException(PaymentErrorCode.REFUND_AMOUNT_INVALID);
        }

        Refund refund = new Refund();
        refund.setId(SnowflakeUtils.nextId());
        refund.setRefundNo(generateRefundNo());
        refund.setPaymentId(payment.getId());
        refund.setOrderNo(orderNo);
        refund.setAmount(refundAmount);
        refund.setReason(request.getReason());
        refund.setStatus(1);
        refundMapper.insert(refund);

        boolean isPartial = refundAmount.compareTo(payment.getAmount()) < 0;
        payment.setStatus(isPartial ? 1 : 3);
        paymentMapper.updateById(payment);

        DistributedTransactionContext transaction = startTransaction(
                payment.getOrderNo(),
                "payment-refund:" + payment.getOrderNo() + ":" + refund.getRefundNo());
        outboxService.enqueue("payment", payment.getOrderNo(), "order-paid",
                new OrderPaidMessage(
                        payment.getOrderNo(),
                        isPartial ? 1 : 5,
                        LocalDateTime.now(),
                        transaction.getTransactionId(),
                        transaction.getIdempotencyKey(),
                        null));

        return toVO(payment);
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
    @Transactional
    public PaymentVO refundByMerchant(Long merchantId, String orderNo, RefundRequest request) {
        List<String> orderNos = loadMerchantOrderNos(merchantId);
        if (!orderNos.contains(orderNo)) {
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

    private String generateRefundNo() {
        return "REF" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
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
