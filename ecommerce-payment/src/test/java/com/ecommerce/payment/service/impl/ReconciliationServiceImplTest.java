package com.ecommerce.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.common.dto.ReconOrderVO;
import com.ecommerce.common.result.Result;
import com.ecommerce.payment.client.OrderClient;
import com.ecommerce.payment.dto.response.ReconciliationVO;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.Reconciliation;
import com.ecommerce.payment.entity.ReconciliationDetail;
import com.ecommerce.payment.mapper.PaymentMapper;
import com.ecommerce.payment.mapper.ReconciliationDetailMapper;
import com.ecommerce.payment.mapper.ReconciliationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceImplTest {

    @Mock private ReconciliationMapper reconciliationMapper;
    @Mock private ReconciliationDetailMapper detailMapper;
    @Mock private PaymentMapper paymentMapper;
    @Mock private OrderClient orderClient;
    @InjectMocks private ReconciliationServiceImpl service;

    private Payment payment1, payment2;

    @BeforeEach
    void setUp() {
        payment1 = new Payment();
        payment1.setId(1L); payment1.setPaymentNo("PAY001"); payment1.setOrderNo("ORD001");
        payment1.setAmount(new BigDecimal("100.00")); payment1.setStatus(1);

        payment2 = new Payment();
        payment2.setId(2L); payment2.setPaymentNo("PAY002"); payment2.setOrderNo("ORD002");
        payment2.setAmount(new BigDecimal("200.00")); payment2.setStatus(1);
    }

    @Nested
    class RunReconciliationTests {
        @Test
        void shouldMatchAllWhenAmountsEqual() {
            when(paymentMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Arrays.asList(payment1, payment2));
            List<ReconOrderVO> orders = Arrays.asList(
                    buildOrder("ORD001", new BigDecimal("100.00"), 1),
                    buildOrder("ORD002", new BigDecimal("200.00"), 1));
            when(orderClient.getOrdersForRecon(any(), any())).thenReturn(Result.ok(orders));
            when(reconciliationMapper.insert(any(Reconciliation.class))).thenReturn(1);
            when(detailMapper.insert(any(ReconciliationDetail.class))).thenReturn(1);

            ReconciliationVO vo = service.runReconciliation();
            assertThat(vo.getMatchedCount()).isEqualTo(2);
            assertThat(vo.getUnmatchedCount()).isEqualTo(0);
            assertThat(vo.getStatus()).isEqualTo(1);
        }

        @Test
        void shouldDetectAmountMismatch() {
            when(paymentMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.singletonList(payment1));
            List<ReconOrderVO> orders = Collections.singletonList(
                    buildOrder("ORD001", new BigDecimal("999.00"), 1));
            when(orderClient.getOrdersForRecon(any(), any())).thenReturn(Result.ok(orders));
            when(reconciliationMapper.insert(any(Reconciliation.class))).thenReturn(1);
            when(detailMapper.insert(any(ReconciliationDetail.class))).thenReturn(1);

            ReconciliationVO vo = service.runReconciliation();
            assertThat(vo.getUnmatchedCount()).isGreaterThan(0);
        }

        @Test
        void shouldDetectOrderOnly() {
            when(paymentMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());
            List<ReconOrderVO> orders = Collections.singletonList(
                    buildOrder("ORD_ONLY", new BigDecimal("50.00"), 1));
            when(orderClient.getOrdersForRecon(any(), any())).thenReturn(Result.ok(orders));
            when(reconciliationMapper.insert(any(Reconciliation.class))).thenReturn(1);
            when(detailMapper.insert(any(ReconciliationDetail.class))).thenReturn(1);

            ReconciliationVO vo = service.runReconciliation();
            assertThat(vo.getUnmatchedCount()).isGreaterThan(0);
        }

        @Test
        void shouldDetectPaymentOnly() {
            when(paymentMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.singletonList(payment1));
            when(orderClient.getOrdersForRecon(any(), any())).thenReturn(Result.ok(Collections.emptyList()));
            when(reconciliationMapper.insert(any(Reconciliation.class))).thenReturn(1);
            when(detailMapper.insert(any(ReconciliationDetail.class))).thenReturn(1);

            ReconciliationVO vo = service.runReconciliation();
            assertThat(vo.getUnmatchedCount()).isGreaterThan(0);
        }

        @Test
        void shouldHandleFeignFailure() {
            when(paymentMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.singletonList(payment1));
            when(orderClient.getOrdersForRecon(any(), any())).thenThrow(new RuntimeException("timeout"));
            when(reconciliationMapper.insert(any(Reconciliation.class))).thenReturn(1);
            when(detailMapper.insert(any(ReconciliationDetail.class))).thenReturn(1);

            ReconciliationVO vo = service.runReconciliation();
            assertThat(vo.getTotalPaymentCount()).isEqualTo(1);
            assertThat(vo.getTotalOrderCount()).isEqualTo(0);
        }

        @Test
        void shouldHandleEmptyPaymentAndOrder() {
            when(paymentMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());
            when(orderClient.getOrdersForRecon(any(), any())).thenReturn(Result.ok(Collections.emptyList()));
            when(reconciliationMapper.insert(any(Reconciliation.class))).thenReturn(1);

            ReconciliationVO vo = service.runReconciliation();
            assertThat(vo.getTotalOrderCount()).isEqualTo(0);
            assertThat(vo.getTotalPaymentCount()).isEqualTo(0);
            assertThat(vo.getMatchedCount()).isEqualTo(0);
        }
    }

    @Nested
    class ListTests {
        @Test
        void shouldListReconciliations() {
            Reconciliation r = new Reconciliation();
            r.setId(1L); r.setBatchNo("REC001"); r.setStatus(1);
            when(reconciliationMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.singletonList(r));
            List<ReconciliationVO> list = service.listReconciliations();
            assertThat(list).hasSize(1);
            assertThat(list.get(0).getStatusText()).isEqualTo("已完成");
        }

        @Test
        void shouldReturnEmptyListWhenNoRecords() {
            when(reconciliationMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());
            assertThat(service.listReconciliations()).isEmpty();
        }
    }

    @Nested
    class DetailTests {
        @Test
        void shouldGetReconciliationDetail() {
            Reconciliation r = new Reconciliation();
            r.setId(1L); r.setBatchNo("REC001"); r.setStatus(1);
            r.setMatchedCount(1); r.setUnmatchedCount(0);
            when(reconciliationMapper.selectById(1L)).thenReturn(r);

            ReconciliationDetail d = new ReconciliationDetail();
            d.setId(1L); d.setReconciliationId(1L); d.setRecordType("ORDER");
            d.setOrderNo("ORD001"); d.setAmount(new BigDecimal("100.00")); d.setMatchStatus("MATCHED");
            when(detailMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.singletonList(d));

            ReconciliationVO vo = service.getReconciliationDetail(1L);
            assertThat(vo).isNotNull();
            assertThat(vo.getDetails()).hasSize(1);
            assertThat(vo.getDetails().get(0).getAmount()).isEqualByComparingTo("100.00");
            assertThat(vo.getDetails().get(0).getMatchStatus()).isEqualTo("MATCHED");
        }

        @Test
        void shouldReturnNullWhenReconciliationNotFound() {
            when(reconciliationMapper.selectById(999L)).thenReturn(null);
            assertThat(service.getReconciliationDetail(999L)).isNull();
        }
    }

    @Nested
    class BoundaryTests {
        @Test
        void shouldHandleLargeDataSet() {
            List<Payment> largePayments = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                Payment p = new Payment();
                p.setId((long) i); p.setPaymentNo("PAY" + i); p.setOrderNo("ORD" + i);
                p.setAmount(new BigDecimal("10.00")); p.setStatus(1);
                largePayments.add(p);
            }
            List<ReconOrderVO> largeOrders = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                largeOrders.add(buildOrder("ORD" + i, new BigDecimal("10.00"), 1));
            }
            when(paymentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(largePayments);
            when(orderClient.getOrdersForRecon(any(), any())).thenReturn(Result.ok(largeOrders));
            when(reconciliationMapper.insert(any(Reconciliation.class))).thenReturn(1);
            when(detailMapper.insert(any(ReconciliationDetail.class))).thenReturn(1);

            ReconciliationVO vo = service.runReconciliation();
            assertThat(vo.getMatchedCount()).isEqualTo(100);
        }

        @Test
        void shouldHandleNullOrderAmount() {
            when(paymentMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.singletonList(payment1));
            List<ReconOrderVO> orders = Collections.singletonList(
                    buildOrder("ORD001", null, 1));
            when(orderClient.getOrdersForRecon(any(), any())).thenReturn(Result.ok(orders));
            when(reconciliationMapper.insert(any(Reconciliation.class))).thenReturn(1);
            when(detailMapper.insert(any(ReconciliationDetail.class))).thenReturn(1);

            assertThatCode(() -> service.runReconciliation()).doesNotThrowAnyException();
        }

        @Test
        void shouldHandlePaymentWithNullOrderNo() {
            payment1.setOrderNo(null);
            when(paymentMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.singletonList(payment1));
            when(orderClient.getOrdersForRecon(any(), any())).thenReturn(Result.ok(Collections.emptyList()));
            when(reconciliationMapper.insert(any(Reconciliation.class))).thenReturn(1);

            assertThatCode(() -> service.runReconciliation()).doesNotThrowAnyException();
        }

        @Test
        void shouldHandlePartialAmountMatch() {
            List<Payment> payments = Arrays.asList(payment1, payment2);
            List<ReconOrderVO> orders = Arrays.asList(
                    buildOrder("ORD001", new BigDecimal("100.00"), 1),
                    buildOrder("ORD002", new BigDecimal("199.99"), 1));
            when(paymentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(payments);
            when(orderClient.getOrdersForRecon(any(), any())).thenReturn(Result.ok(orders));
            when(reconciliationMapper.insert(any(Reconciliation.class))).thenReturn(1);
            when(detailMapper.insert(any(ReconciliationDetail.class))).thenReturn(1);

            ReconciliationVO vo = service.runReconciliation();
            assertThat(vo.getMatchedCount()).isEqualTo(1);
        }
    }

    private ReconOrderVO buildOrder(String orderNo, BigDecimal amount, Integer status) {
        ReconOrderVO vo = new ReconOrderVO();
        vo.setOrderNo(orderNo);
        vo.setAmount(amount);
        vo.setStatus(status);
        return vo;
    }
}
