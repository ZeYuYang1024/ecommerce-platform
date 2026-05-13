package com.ecommerce.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.common.util.SnowflakeUtils;
import com.ecommerce.payment.common.PaymentErrorCode;
import com.ecommerce.payment.dto.request.PayRequest;
import com.ecommerce.payment.dto.request.RefundRequest;
import com.ecommerce.payment.dto.response.PaymentVO;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.Refund;
import com.ecommerce.payment.mapper.PaymentMapper;
import com.ecommerce.payment.mapper.RefundMapper;
import com.ecommerce.payment.client.OrderClient;
import com.ecommerce.common.dto.OrderPaidMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import com.ecommerce.payment.service.PaymentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentMapper paymentMapper;
    private final RefundMapper refundMapper;
    private final OrderClient orderClient;
    private final RocketMQTemplate rocketMQTemplate;

    public PaymentServiceImpl(PaymentMapper paymentMapper, RefundMapper refundMapper, OrderClient orderClient, RocketMQTemplate rocketMQTemplate) {
        this.paymentMapper = paymentMapper;
        this.refundMapper = refundMapper;
        this.orderClient = orderClient;
        this.rocketMQTemplate = rocketMQTemplate;
    }

    @Override
    @Transactional
    public PaymentVO pay(Long userId, PayRequest request) {
        // Check if already paid
        Payment existing = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>().eq(Payment::getOrderNo, request.getOrderNo()));
        if (existing != null && existing.getStatus() == 1) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_ALREADY_PAID);
        }

        // Use orderNo to get real orderId (avoids JS Long precision loss)
        Long realOrderId = request.getOrderId();
        try {
            var orderRes = orderClient.getOrderByOrderNo(request.getOrderNo(), userId);
            if (orderRes.getData() != null) {
                realOrderId = orderRes.getData().get("id") != null
                    ? Long.valueOf(orderRes.getData().get("id").toString()) : request.getOrderId();
            }
        } catch (Exception ignored) {}

        Payment payment = new Payment();
        payment.setId(SnowflakeUtils.nextId());
        payment.setPaymentNo(generatePaymentNo());
        payment.setOrderNo(request.getOrderNo());
        payment.setOrderId(realOrderId);
        payment.setUserId(userId);
        payment.setAmount(request.getAmount());
        payment.setPayMethod(request.getPayMethod());
        payment.setStatus(1);
        payment.setPaidAt(LocalDateTime.now());
        paymentMapper.insert(payment);

        // 发送MQ消息通知订单服务
        OrderPaidMessage msg = new OrderPaidMessage(payment.getOrderNo(), 1, payment.getPaidAt());
        try {
            rocketMQTemplate.syncSend("order-paid", msg);
            log.info("MQ sent: order-paid, orderNo={}", payment.getOrderNo());
        } catch (Exception e) {
            log.error("MQ send failed: orderNo={}", payment.getOrderNo(), e);
        }

        return toVO(payment);
    }

    @Override
    public PaymentVO queryByOrderNo(String orderNo) {
        Payment payment = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>().eq(Payment::getOrderNo, orderNo));
        if (payment == null) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND);
        }
        return toVO(payment);
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

        // Check existing refund
        Long count = refundMapper.selectCount(
                new LambdaQueryWrapper<Refund>().eq(Refund::getOrderNo, orderNo));
        if (count > 0) {
            throw new BusinessException(PaymentErrorCode.REFUND_ALREADY_EXISTS);
        }

        BigDecimal refundAmount = request.getAmount() != null ? request.getAmount() : payment.getAmount();

        // Create refund
        Refund refund = new Refund();
        refund.setId(SnowflakeUtils.nextId());
        refund.setRefundNo(generateRefundNo());
        refund.setPaymentId(payment.getId());
        refund.setOrderNo(orderNo);
        refund.setAmount(refundAmount);
        refund.setReason(request.getReason());
        refund.setStatus(1); // auto-complete
        refundMapper.insert(refund);

        // Update payment status
        boolean isPartial = refundAmount.compareTo(payment.getAmount()) < 0;
        payment.setStatus(isPartial ? 1 : 3);
        paymentMapper.updateById(payment);

        OrderPaidMessage msg2 = new OrderPaidMessage(payment.getOrderNo(), payment.getStatus(), LocalDateTime.now());
        try { rocketMQTemplate.syncSend("order-paid", msg2); } catch (Exception e) { log.error("MQ send failed", e); }

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
        return new Page<PaymentVO>(pageReq.getCurrent(), pageReq.getSize(), pageReq.getTotal())
                .setRecords(pageReq.getRecords().stream()
                        .map(this::toVO).collect(Collectors.toList()));
    }

    private PaymentVO toVO(Payment p) {
        PaymentVO vo = new PaymentVO();
        vo.setId(p.getId());
        vo.setPaymentNo(p.getPaymentNo());
        vo.setOrderNo(p.getOrderNo());
        vo.setUserId(p.getUserId());
        vo.setAmount(p.getAmount());
        vo.setStatus(p.getStatus());
        vo.setStatusText(statusText(p.getStatus()));
        vo.setPayMethod(p.getPayMethod());
        vo.setPaidAt(p.getPaidAt());
        vo.setCreatedAt(p.getCreatedAt());
        return vo;
    }

    private String generatePaymentNo() {
        return "PAY" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", SnowflakeUtils.nextId() % 10000);
    }

    private String generateRefundNo() {
        return "REF" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", SnowflakeUtils.nextId() % 10000);
    }

    private String statusText(Integer s) {
        if (s == null) return "未知";
        if (s == 0) return "待支付";
        if (s == 1) return "已支付";
        if (s == 2) return "退款中";
        if (s == 3) return "已退款";
        if (s == 4) return "已关闭";
        return "未知";
    }
}
