package com.ecommerce.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.common.util.SnowflakeUtils;
import com.ecommerce.payment.dto.response.SettlementVO;
import com.ecommerce.payment.entity.DailySettlement;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.Refund;
import com.ecommerce.payment.mapper.DailySettlementMapper;
import com.ecommerce.payment.mapper.PaymentMapper;
import com.ecommerce.payment.mapper.RefundMapper;
import com.ecommerce.payment.service.SettlementService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SettlementServiceImpl implements SettlementService {

    private final DailySettlementMapper settlementMapper;
    private final PaymentMapper paymentMapper;
    private final RefundMapper refundMapper;

    public SettlementServiceImpl(DailySettlementMapper settlementMapper,
                                  PaymentMapper paymentMapper,
                                  RefundMapper refundMapper) {
        this.settlementMapper = settlementMapper;
        this.paymentMapper = paymentMapper;
        this.refundMapper = refundMapper;
    }

    @Override
    @Transactional
    public SettlementVO generateSettlement(String dateStr) {
        LocalDate date = dateStr != null ? LocalDate.parse(dateStr) : LocalDate.now().minusDays(1);

        // Check existing
        DailySettlement existing = settlementMapper.selectOne(
                new LambdaQueryWrapper<DailySettlement>().eq(DailySettlement::getSettlementDate, date));
        if (existing != null) {
            return toVO(existing);
        }

        // Aggregate payments on this date using DB-level filtering
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
        List<Payment> payments = paymentMapper.selectList(
                new LambdaQueryWrapper<Payment>()
                        .ge(Payment::getCreatedAt, dayStart)
                        .lt(Payment::getCreatedAt, dayEnd)
                        .eq(Payment::getStatus, 1));
        BigDecimal totalPaymentAmount = payments.stream()
                .map(p -> p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalPaymentCount = payments.size();

        // Aggregate refunds on this date using DB-level filtering
        List<Refund> refunds = refundMapper.selectList(
                new LambdaQueryWrapper<Refund>()
                        .ge(Refund::getCreatedAt, dayStart)
                        .lt(Refund::getCreatedAt, dayEnd)
                        .eq(Refund::getStatus, 1));
        BigDecimal totalRefundAmount = refunds.stream()
                .map(r -> r.getAmount() != null ? r.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalRefundCount = refunds.size();

        DailySettlement settlement = new DailySettlement();
        settlement.setId(SnowflakeUtils.nextId());
        settlement.setSettlementDate(date);
        settlement.setTotalOrderCount(0); // populated by order service if needed
        settlement.setTotalOrderAmount(BigDecimal.ZERO);
        settlement.setTotalPaymentCount(totalPaymentCount);
        settlement.setTotalPaymentAmount(totalPaymentAmount);
        settlement.setTotalRefundCount(totalRefundCount);
        settlement.setTotalRefundAmount(totalRefundAmount);
        settlement.setNetAmount(totalPaymentAmount.subtract(totalRefundAmount));
        settlement.setStatus(1);
        settlementMapper.insert(settlement);

        return toVO(settlement);
    }

    @Override
    public List<SettlementVO> listSettlements() {
        return settlementMapper.selectList(
                new LambdaQueryWrapper<DailySettlement>()
                        .orderByDesc(DailySettlement::getSettlementDate))
                .stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    private SettlementVO toVO(DailySettlement s) {
        SettlementVO vo = new SettlementVO();
        vo.setId(s.getId());
        vo.setSettlementDate(s.getSettlementDate());
        vo.setTotalOrderCount(s.getTotalOrderCount());
        vo.setTotalOrderAmount(s.getTotalOrderAmount());
        vo.setTotalPaymentCount(s.getTotalPaymentCount());
        vo.setTotalPaymentAmount(s.getTotalPaymentAmount());
        vo.setTotalRefundCount(s.getTotalRefundCount());
        vo.setTotalRefundAmount(s.getTotalRefundAmount());
        vo.setNetAmount(s.getNetAmount());
        vo.setStatus(s.getStatus());
        vo.setStatusText(s.getStatus() == 1 ? "已确认" : "草稿");
        return vo;
    }
}
