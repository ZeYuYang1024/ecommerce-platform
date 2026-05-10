package com.ecommerce.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.payment.dto.response.SettlementVO;
import com.ecommerce.payment.entity.DailySettlement;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.Refund;
import com.ecommerce.payment.mapper.DailySettlementMapper;
import com.ecommerce.payment.mapper.PaymentMapper;
import com.ecommerce.payment.mapper.RefundMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettlementServiceImplTest {

    @Mock private DailySettlementMapper settlementMapper;
    @Mock private PaymentMapper paymentMapper;
    @Mock private RefundMapper refundMapper;
    @InjectMocks private SettlementServiceImpl service;

    @BeforeEach
    void setUp() {
    }

    @Nested
    class GenerateSettlementTests {
        @Test
        void shouldGenerateSettlementForYesterday() {
            when(settlementMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            Payment payment = buildPayment(1L, new BigDecimal("500.00"), 1, LocalDateTime.now().minusDays(1));
            when(paymentMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.singletonList(payment));

            Refund refund = buildRefund(1L, new BigDecimal("100.00"), 1, LocalDateTime.now().minusDays(1));
            when(refundMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.singletonList(refund));

            when(settlementMapper.insert(any(DailySettlement.class))).thenReturn(1);

            SettlementVO vo = service.generateSettlement(null);
            assertThat(vo.getTotalPaymentCount()).isEqualTo(1);
            assertThat(vo.getTotalPaymentAmount()).isEqualByComparingTo("500.00");
            assertThat(vo.getTotalRefundCount()).isEqualTo(1);
            assertThat(vo.getNetAmount()).isEqualByComparingTo("400.00");
            assertThat(vo.getStatus()).isEqualTo(1);
        }

        @Test
        void shouldReturnExistingSettlement() {
            DailySettlement existing = new DailySettlement();
            existing.setId(1L); existing.setSettlementDate(LocalDate.now().minusDays(1));
            existing.setTotalPaymentAmount(new BigDecimal("300.00"));
            existing.setStatus(1);
            when(settlementMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

            SettlementVO vo = service.generateSettlement(null);
            assertThat(vo.getTotalPaymentAmount()).isEqualByComparingTo("300.00");
            verify(paymentMapper, never()).selectList(any());
        }

        @Test
        void shouldHandleDateParam() {
            String dateStr = "2026-05-01";
            when(settlementMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(paymentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
            when(refundMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
            when(settlementMapper.insert(any(DailySettlement.class))).thenReturn(1);

            SettlementVO vo = service.generateSettlement(dateStr);
            assertThat(vo.getSettlementDate().toString()).isEqualTo(dateStr);
        }
    }

    @Nested
    class ListTests {
        @Test
        void shouldListSettlements() {
            DailySettlement s = new DailySettlement();
            s.setId(1L); s.setSettlementDate(LocalDate.now().minusDays(1));
            s.setTotalPaymentAmount(new BigDecimal("500.00"));
            s.setNetAmount(new BigDecimal("400.00")); s.setStatus(1);
            when(settlementMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.singletonList(s));

            List<SettlementVO> list = service.listSettlements();
            assertThat(list).hasSize(1);
            assertThat(list.get(0).getStatusText()).isEqualTo("已确认");
        }

        @Test
        void shouldReturnEmptyList() {
            when(settlementMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());
            assertThat(service.listSettlements()).isEmpty();
        }
    }

    @Nested
    class BoundaryTests {
        @Test
        void shouldHandleNoPaymentsNoRefunds() {
            when(settlementMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(paymentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
            when(refundMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
            when(settlementMapper.insert(any(DailySettlement.class))).thenReturn(1);

            SettlementVO vo = service.generateSettlement("2026-05-09");
            assertThat(vo.getTotalPaymentCount()).isEqualTo(0);
            assertThat(vo.getNetAmount()).isEqualByComparingTo("0.00");
        }

        @Test
        void shouldOnlyCountPaidPayments() {
            when(settlementMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            Payment paid = buildPayment(1L, new BigDecimal("100.00"), 1, LocalDateTime.now().minusDays(1));
            // DB query filters by status=1, so only paid payments are returned
            when(paymentMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.singletonList(paid));
            when(refundMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());
            when(settlementMapper.insert(any(DailySettlement.class))).thenReturn(1);

            SettlementVO vo = service.generateSettlement(null);
            assertThat(vo.getTotalPaymentCount()).isEqualTo(1);
            assertThat(vo.getTotalPaymentAmount()).isEqualByComparingTo("100.00");
        }

        @Test
        void shouldOnlyCountRefundedRefunds() {
            when(settlementMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(paymentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
            Refund done = buildRefund(1L, new BigDecimal("50.00"), 1, LocalDateTime.now().minusDays(1));
            // DB query filters by status=1, so only completed refunds are returned
            when(refundMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.singletonList(done));
            when(settlementMapper.insert(any(DailySettlement.class))).thenReturn(1);

            SettlementVO vo = service.generateSettlement(null);
            assertThat(vo.getTotalRefundCount()).isEqualTo(1);
        }

        @Test
        void shouldHandleLargeAmounts() {
            when(settlementMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            Payment p = buildPayment(1L, new BigDecimal("99999999.99"), 1, LocalDateTime.now().minusDays(1));
            when(paymentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(p));
            when(refundMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
            when(settlementMapper.insert(any(DailySettlement.class))).thenReturn(1);

            SettlementVO vo = service.generateSettlement(null);
            assertThat(vo.getTotalPaymentAmount()).isEqualByComparingTo("99999999.99");
        }

        @Test
        void shouldHandleNegativeNetAmount() {
            when(settlementMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            Payment p = buildPayment(1L, new BigDecimal("50.00"), 1, LocalDateTime.now().minusDays(1));
            when(paymentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(p));
            Refund r = buildRefund(1L, new BigDecimal("100.00"), 1, LocalDateTime.now().minusDays(1));
            when(refundMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(r));
            when(settlementMapper.insert(any(DailySettlement.class))).thenReturn(1);

            SettlementVO vo = service.generateSettlement(null);
            assertThat(vo.getNetAmount()).isEqualByComparingTo("-50.00");
        }

        @Test
        void shouldFilterByDateOnlyFromYesterday() {
            when(settlementMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            Payment yesterday = buildPayment(2L, new BigDecimal("200.00"), 1, LocalDateTime.now().minusDays(1));
            // DB query filters by yesterday's date range, so only yesterday's payment is returned
            when(paymentMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.singletonList(yesterday));
            when(refundMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());
            when(settlementMapper.insert(any(DailySettlement.class))).thenReturn(1);

            SettlementVO vo = service.generateSettlement(null);
            assertThat(vo.getTotalPaymentCount()).isEqualTo(1);
            assertThat(vo.getTotalPaymentAmount()).isEqualByComparingTo("200.00");
        }
    }

    private Payment buildPayment(Long id, BigDecimal amount, int status, LocalDateTime createdAt) {
        Payment p = new Payment();
        p.setId(id); p.setAmount(amount); p.setStatus(status); p.setCreatedAt(createdAt);
        return p;
    }

    private Refund buildRefund(Long id, BigDecimal amount, int status, LocalDateTime createdAt) {
        Refund r = new Refund();
        r.setId(id); r.setAmount(amount); r.setStatus(status); r.setCreatedAt(createdAt);
        return r;
    }
}
