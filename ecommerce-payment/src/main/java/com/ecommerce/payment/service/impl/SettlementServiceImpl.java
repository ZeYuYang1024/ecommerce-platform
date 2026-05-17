package com.ecommerce.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.util.SnowflakeUtils;
import com.ecommerce.payment.client.OrderClient;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SettlementServiceImpl implements SettlementService {

    private final DailySettlementMapper settlementMapper;
    private final PaymentMapper paymentMapper;
    private final RefundMapper refundMapper;
    private final OrderClient orderClient;

    public SettlementServiceImpl(DailySettlementMapper settlementMapper,
                                 PaymentMapper paymentMapper,
                                 RefundMapper refundMapper,
                                 OrderClient orderClient) {
        this.settlementMapper = settlementMapper;
        this.paymentMapper = paymentMapper;
        this.refundMapper = refundMapper;
        this.orderClient = orderClient;
    }

    @Override
    @Transactional
    public SettlementVO generateSettlement(String dateStr) {
        LocalDate date = dateStr != null ? LocalDate.parse(dateStr) : LocalDate.now().minusDays(1);

        DailySettlement existing = settlementMapper.selectOne(
                new LambdaQueryWrapper<DailySettlement>().eq(DailySettlement::getSettlementDate, date));
        if (existing != null) {
            return toVO(existing);
        }

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
        settlement.setTotalOrderCount(0);
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
    public Page<SettlementVO> listSettlements(int page, int size) {
        Page<DailySettlement> pageReq = new Page<>(page, size);
        settlementMapper.selectPage(pageReq,
                new LambdaQueryWrapper<DailySettlement>()
                        .orderByDesc(DailySettlement::getSettlementDate));
        return new Page<SettlementVO>(pageReq.getCurrent(), pageReq.getSize(), pageReq.getTotal())
                .setRecords(pageReq.getRecords().stream()
                        .map(this::toVO)
                        .collect(Collectors.toList()));
    }

    @Override
    public Page<SettlementVO> listByMerchant(Long merchantId, int page, int size) {
        List<String> orderNos = loadMerchantOrderNos(merchantId);
        if (orderNos.isEmpty()) {
            return new Page<>(page, size, 0);
        }

        List<Payment> payments = paymentMapper.selectList(new LambdaQueryWrapper<Payment>()
                .in(Payment::getOrderNo, orderNos)
                .eq(Payment::getStatus, 1));
        List<Refund> refunds = refundMapper.selectList(new LambdaQueryWrapper<Refund>()
                .in(Refund::getOrderNo, orderNos)
                .eq(Refund::getStatus, 1));

        Map<LocalDate, SettlementAccumulator> grouped = new HashMap<>();
        for (Payment payment : payments) {
            LocalDate settlementDate = resolveDate(payment.getCreatedAt(), payment.getPaidAt());
            SettlementAccumulator accumulator = grouped.computeIfAbsent(settlementDate, key -> new SettlementAccumulator());
            accumulator.totalPaymentCount++;
            accumulator.totalPaymentAmount = accumulator.totalPaymentAmount.add(amountOrZero(payment.getAmount()));
        }
        for (Refund refund : refunds) {
            LocalDate settlementDate = refund.getCreatedAt() != null ? refund.getCreatedAt().toLocalDate() : LocalDate.now();
            SettlementAccumulator accumulator = grouped.computeIfAbsent(settlementDate, key -> new SettlementAccumulator());
            accumulator.totalRefundCount++;
            accumulator.totalRefundAmount = accumulator.totalRefundAmount.add(amountOrZero(refund.getAmount()));
        }

        List<SettlementVO> records = new ArrayList<>();
        grouped.entrySet().stream()
                .sorted(Map.Entry.<LocalDate, SettlementAccumulator>comparingByKey(Comparator.reverseOrder()))
                .forEach(entry -> records.add(toMerchantSettlement(entry.getKey(), entry.getValue())));

        int fromIndex = Math.max((page - 1) * size, 0);
        if (fromIndex >= records.size()) {
            return new Page<>(page, size, records.size());
        }
        int toIndex = Math.min(fromIndex + size, records.size());
        Page<SettlementVO> result = new Page<>(page, size, records.size());
        result.setRecords(records.subList(fromIndex, toIndex));
        return result;
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

    private List<String> loadMerchantOrderNos(Long merchantId) {
        try {
            var response = orderClient.listOrderNosByMerchant(merchantId);
            return response.getData() != null ? response.getData() : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private LocalDate resolveDate(LocalDateTime createdAt, LocalDateTime fallback) {
        if (createdAt != null) {
            return createdAt.toLocalDate();
        }
        if (fallback != null) {
            return fallback.toLocalDate();
        }
        return LocalDate.now();
    }

    private BigDecimal amountOrZero(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }

    private SettlementVO toMerchantSettlement(LocalDate settlementDate, SettlementAccumulator accumulator) {
        SettlementVO vo = new SettlementVO();
        vo.setId(settlementDate.toEpochDay());
        vo.setSettlementDate(settlementDate);
        vo.setTotalOrderCount(0);
        vo.setTotalOrderAmount(BigDecimal.ZERO);
        vo.setTotalPaymentCount(accumulator.totalPaymentCount);
        vo.setTotalPaymentAmount(accumulator.totalPaymentAmount);
        vo.setTotalRefundCount(accumulator.totalRefundCount);
        vo.setTotalRefundAmount(accumulator.totalRefundAmount);
        vo.setNetAmount(accumulator.totalPaymentAmount.subtract(accumulator.totalRefundAmount));
        vo.setStatus(1);
        vo.setStatusText("已确认");
        return vo;
    }

    private static class SettlementAccumulator {
        private int totalPaymentCount;
        private BigDecimal totalPaymentAmount = BigDecimal.ZERO;
        private int totalRefundCount;
        private BigDecimal totalRefundAmount = BigDecimal.ZERO;
    }
}
