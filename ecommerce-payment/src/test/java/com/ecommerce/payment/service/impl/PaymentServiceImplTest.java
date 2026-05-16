package com.ecommerce.payment.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.payment.client.OrderClient;
import com.ecommerce.payment.common.PaymentErrorCode;
import com.ecommerce.payment.dto.request.PayRequest;
import com.ecommerce.payment.dto.request.RefundRequest;
import com.ecommerce.payment.dto.response.PaymentVO;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.Refund;
import com.ecommerce.payment.mapper.PaymentMapper;
import com.ecommerce.payment.mapper.RefundMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock private PaymentMapper paymentMapper;
    @Mock private RefundMapper refundMapper;
    @Mock private OrderClient orderClient;
    @Mock private RocketMQTemplate rocketMQTemplate;
    @InjectMocks private PaymentServiceImpl service;

    private Payment payment;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Payment.class);

        payment = new Payment();
        payment.setId(1L);
        payment.setPaymentNo("PAY202605091200000001");
        payment.setOrderNo("202605091200000001");
        payment.setOrderId(1L);
        payment.setUserId(1L);
        payment.setAmount(new BigDecimal("6999.00"));
        payment.setStatus(1);
        payment.setPayMethod("wx_jsapi");
    }

    @Nested
    class PayTests {
        @Test
        void shouldCreatePayment() {
            when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(paymentMapper.insert(any(Payment.class))).thenReturn(1);

            PayRequest req = new PayRequest();
            req.setOrderNo("202605091200000001"); req.setOrderId(1L); req.setAmount(new BigDecimal("6999.00"));

            PaymentVO vo = service.pay(1L, req);
            assertThat(vo.getPaymentNo()).isNotNull();
            assertThat(vo.getStatus()).isEqualTo(1);
            assertThat(vo.getStatusText()).isEqualTo("已支付");
        }

        @Test
        void shouldRejectDuplicatePay() {
            when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(payment);
            PayRequest req = new PayRequest();
            req.setOrderNo("202605091200000001"); req.setOrderId(1L); req.setAmount(new BigDecimal("6999.00"));

            assertThatThrownBy(() -> service.pay(1L, req))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(PaymentErrorCode.PAYMENT_ALREADY_PAID.getCode());
        }
    }

    @Nested
    class QueryTests {
        @Test
        void shouldQueryByOrderNo() {
            when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(payment);
            PaymentVO vo = service.queryByOrderNo("202605091200000001");
            assertThat(vo.getPaymentNo()).isEqualTo("PAY202605091200000001");
        }

        @Test
        void shouldQueryByOrderNoForUser() {
            when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(payment);

            PaymentVO vo = service.queryByOrderNoForUser(1L, "202605091200000001");

            verify(paymentMapper).selectOne(any(LambdaQueryWrapper.class));
            assertThat(vo.getPaymentNo()).isEqualTo("PAY202605091200000001");
        }

        @Test
        void shouldThrowWhenNotFound() {
            when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            assertThatThrownBy(() -> service.queryByOrderNo("xxx"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        void shouldThrowWhenUserScopedPaymentNotFound() {
            when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            assertThatThrownBy(() -> service.queryByOrderNoForUser(1L, "xxx"))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(PaymentErrorCode.PAYMENT_NOT_FOUND.getCode());
        }

        @Test
        void shouldListAll() {
            when(paymentMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenAnswer(invocation -> {
                        Page<Payment> page = invocation.getArgument(0);
                        page.setRecords(Collections.singletonList(payment));
                        page.setTotal(1);
                        return page;
                    });

            Page<PaymentVO> page = service.listAll(null, 1, 10);

            assertThat(page.getRecords()).hasSize(1);
        }
    }

    @Nested
    class RefundTests {
        @Test
        void shouldRefundFullAmount() {
            when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(payment);
            when(refundMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(refundMapper.insert(any(Refund.class))).thenReturn(1);
            when(paymentMapper.updateById(any(Payment.class))).thenReturn(1);

            RefundRequest req = new RefundRequest();
            req.setReason("用户申请退款");

            PaymentVO vo = service.refund("202605091200000001", req);
            assertThat(vo.getStatus()).isEqualTo(3);
        }

        @Test
        void shouldRejectRefundNotPaid() {
            payment.setStatus(0);
            when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(payment);

            RefundRequest req = new RefundRequest();
            req.setReason("退款");

            assertThatThrownBy(() -> service.refund("x", req))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(PaymentErrorCode.PAYMENT_NOT_PAID.getCode());
        }

        @Test
        void shouldRejectDuplicateRefund() {
            when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(payment);
            when(refundMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            RefundRequest req = new RefundRequest();
            req.setReason("退款");

            assertThatThrownBy(() -> service.refund("x", req))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(PaymentErrorCode.REFUND_ALREADY_EXISTS.getCode());
        }

        @Test
        void shouldRefundPartialAmount() {
            when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(payment);
            when(refundMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(refundMapper.insert(any(Refund.class))).thenReturn(1);
            when(paymentMapper.updateById(any(Payment.class))).thenReturn(1);

            RefundRequest req = new RefundRequest();
            req.setReason("部分退款"); req.setAmount(new BigDecimal("1000.00"));

            PaymentVO vo = service.refund("202605091200000001", req);
            assertThat(vo.getStatus()).isEqualTo(1);
        }
    }

    @Nested
    class BoundaryTests {
        @Test
        void shouldPayWithMinimumAmount() {
            when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(paymentMapper.insert(any(Payment.class))).thenReturn(1);
            PayRequest req = new PayRequest();
            req.setOrderNo("min"); req.setOrderId(1L); req.setAmount(new BigDecimal("0.01"));
            PaymentVO vo = service.pay(1L, req);
            assertThat(vo.getStatus()).isEqualTo(1);
        }

        @Test
        void shouldPayWithLargeAmount() {
            when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(paymentMapper.insert(any(Payment.class))).thenReturn(1);
            PayRequest req = new PayRequest();
            req.setOrderNo("max"); req.setOrderId(1L); req.setAmount(new BigDecimal("99999999.99"));
            PaymentVO vo = service.pay(1L, req);
            assertThat(vo.getStatus()).isEqualTo(1);
        }

        @Test
        void shouldShowCorrectStatusTextForAllStatuses() {
            payment.setStatus(0);
            when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(payment);
            assertThat(service.queryByOrderNo("any").getStatusText()).isEqualTo("待支付");

            payment.setStatus(1);
            when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(payment);
            assertThat(service.queryByOrderNo("any").getStatusText()).isEqualTo("已支付");

            payment.setStatus(3);
            when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(payment);
            assertThat(service.queryByOrderNo("any").getStatusText()).isEqualTo("已退款");

            payment.setStatus(4);
            when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(payment);
            assertThat(service.queryByOrderNo("any").getStatusText()).isEqualTo("已关闭");
        }

        @Test
        void shouldListPaymentsByStatus() {
            payment.setStatus(3);
            when(paymentMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenAnswer(invocation -> {
                        Page<Payment> page = invocation.getArgument(0);
                        page.setRecords(Collections.singletonList(payment));
                        page.setTotal(1);
                        return page;
                    });

            Page<PaymentVO> page = service.listAll(3, 1, 10);

            assertThat(page.getRecords()).hasSize(1);
            assertThat(page.getRecords().get(0).getStatusText()).isEqualTo("已退款");
        }

        @Test
        void shouldListAllPaymentsWhenNoFilter() {
            when(paymentMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenAnswer(invocation -> {
                        Page<Payment> page = invocation.getArgument(0);
                        page.setRecords(Collections.emptyList());
                        page.setTotal(0);
                        return page;
                    });

            Page<PaymentVO> page = service.listAll(null, 1, 10);

            assertThat(page.getRecords()).isEmpty();
        }

        @Test
        void shouldQueryByNonexistentOrderNo() {
            when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            assertThatThrownBy(() -> service.queryByOrderNo("DOES_NOT_EXIST"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        void shouldPaymentNumberMatchPattern() {
            when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(paymentMapper.insert(any(Payment.class))).thenReturn(1);
            PayRequest req = new PayRequest();
            req.setOrderNo("test"); req.setOrderId(1L); req.setAmount(new BigDecimal("1.00"));
            PaymentVO vo = service.pay(1L, req);
            assertThat(vo.getPaymentNo()).startsWith("PAY");
            assertThat(vo.getPaymentNo()).hasSize(21);
        }

        @Test
        void shouldRefundPaymentThatDoesNotExist() {
            when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            RefundRequest req = new RefundRequest();
            req.setReason("x");
            assertThatThrownBy(() -> service.refund("NOPE", req))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    class MoreBoundaryTests {
        @Test
        void shouldRefundWithNullAmountDefaultsToFull() {
            when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(payment);
            when(refundMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(refundMapper.insert(any(Refund.class))).thenReturn(1);
            when(paymentMapper.updateById(any(Payment.class))).thenReturn(1);

            RefundRequest req = new RefundRequest();
            req.setReason("全额退"); req.setAmount(null);
            PaymentVO vo = service.refund("202605091200000001", req);
            assertThat(vo.getStatus()).isEqualTo(3);
        }

        @Test
        void shouldPayWithFraccionPennyAmount() {
            when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(paymentMapper.insert(any(Payment.class))).thenReturn(1);
            PayRequest req = new PayRequest();
            req.setOrderNo("frac"); req.setOrderId(1L); req.setAmount(new BigDecimal("0.01"));
            PaymentVO vo = service.pay(1L, req);
            assertThat(vo.getStatus()).isEqualTo(1);
        }

        @Test
        void shouldHandleNullPayMethod() {
            when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(paymentMapper.insert(any(Payment.class))).thenReturn(1);
            PayRequest req = new PayRequest();
            req.setOrderNo("nullpay"); req.setOrderId(1L); req.setAmount(new BigDecimal("1.00"));
            req.setPayMethod(null);
            PaymentVO vo = service.pay(1L, req);
            assertThat(vo.getStatus()).isEqualTo(1);
        }

        @Test
        void shouldPayWithDifferentPayMethods() {
            when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(paymentMapper.insert(any(Payment.class))).thenReturn(1);
            PayRequest req = new PayRequest();
            req.setOrderNo("alipay"); req.setOrderId(1L); req.setAmount(new BigDecimal("100.00"));
            req.setPayMethod("alipay");
            PaymentVO vo = service.pay(1L, req);
            assertThat(vo.getPayMethod()).isEqualTo("alipay");
        }

        @Test
        void shouldListAllPaymentsWhenEmpty() {
            when(paymentMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenAnswer(invocation -> {
                        Page<Payment> page = invocation.getArgument(0);
                        page.setRecords(Collections.emptyList());
                        page.setTotal(0);
                        return page;
                    });

            assertThat(service.listAll(null, 1, 10).getRecords()).isEmpty();
        }

        @Test
        void shouldRefundWithVeryLargeAmount() {
            payment.setAmount(new BigDecimal("99999999.99"));
            when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(payment);
            when(refundMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(refundMapper.insert(any(Refund.class))).thenReturn(1);
            when(paymentMapper.updateById(any(Payment.class))).thenReturn(1);

            RefundRequest req = new RefundRequest();
            req.setReason("大额退款"); req.setAmount(new BigDecimal("99999999.99"));
            PaymentVO vo = service.refund("202605091200000001", req);
            assertThat(vo.getStatus()).isEqualTo(3);
        }

        @Test
        void shouldRefundWithVerySmallAmount() {
            when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(payment);
            when(refundMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(refundMapper.insert(any(Refund.class))).thenReturn(1);
            when(paymentMapper.updateById(any(Payment.class))).thenReturn(1);

            RefundRequest req = new RefundRequest();
            req.setReason("一分退款"); req.setAmount(new BigDecimal("0.01"));
            PaymentVO vo = service.refund("202605091200000001", req);
            assertThat(vo.getStatus()).isEqualTo(1);
        }
    }
}
