package com.ecommerce.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.payment.client.OrderClient;
import com.ecommerce.payment.common.PaymentErrorCode;
import com.ecommerce.payment.dto.request.RefundRequest;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.Refund;
import com.ecommerce.payment.mapper.PaymentMapper;
import com.ecommerce.payment.mapper.RefundMapper;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceRefundValidationTest {

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private RefundMapper refundMapper;

    @Mock
    private OrderClient orderClient;

    @Mock
    private RocketMQTemplate rocketMQTemplate;

    @InjectMocks
    private PaymentServiceImpl service;

    @Test
    void refundShouldRejectAmountGreaterThanPaidAmount() {
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setOrderNo("202605091200000001");
        payment.setAmount(new BigDecimal("6999.00"));
        payment.setStatus(1);

        when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(payment);
        when(refundMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        RefundRequest request = new RefundRequest();
        request.setReason("over refund");
        request.setAmount(new BigDecimal("7000.00"));

        assertThatThrownBy(() -> service.refund("202605091200000001", request))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode().getCode())
                .isEqualTo(PaymentErrorCode.REFUND_AMOUNT_INVALID.getCode());

        verify(refundMapper, never()).insert(any(Refund.class));
        verify(paymentMapper, never()).updateById(any(Payment.class));
    }
}
