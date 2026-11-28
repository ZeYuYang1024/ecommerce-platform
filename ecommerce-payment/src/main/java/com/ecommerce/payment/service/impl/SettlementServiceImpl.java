package com.ecommerce.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.util.SnowflakeUtils;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.payment.client.OrderClient;
import com.ecommerce.payment.common.PaymentErrorCode;
import com.ecommerce.payment.dto.response.SettlementVO;
import com.ecommerce.payment.entity.DailySettlement;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.Refund;
import com.ecommerce.payment.entity.SettlementAdjustment;
import com.ecommerce.payment.mapper.DailySettlementMapper;
import com.ecommerce.payment.mapper.PaymentMapper;
import com.ecommerce.payment.mapper.RefundMapper;
import com.ecommerce.payment.mapper.SettlementAdjustmentMapper;
import com.ecommerce.payment.service.SettlementService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class SettlementServiceImpl implements SettlementService {

    private final DailySettlementMapper settlementMapper;
    private final PaymentMapper paymentMapper;
    private final RefundMapper refundMapper;
    private final OrderClient orderClient;
    private final SettlementAdjustmentMapper adjustmentMapper;

    public SettlementServiceImpl(DailySettlementMapper settlementMapper,
                                 PaymentMapper paymentMapper,
                                 RefundMapper refundMapper,
                                 OrderClient orderClient, SettlementAdjustmentMapper adjustmentMapper) {
        this.settlementMapper = settlementMapper;
        this.paymentMapper = paymentMapper;
        this.refundMapper = refundMapper;
        this.orderClient = orderClient;
        this.adjustmentMapper = adjustmentMapper;
    }

    @Override
    @Transactional
    public SettlementVO generateSettlement(String dateStr) {
        LocalDate today = LocalDate.now();
        LocalDate date;
        try {
            date = dateStr == null ? today.minusDays(1) : LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            throw new BusinessException(PaymentErrorCode.SETTLEMENT_DATE_INVALID);
        }
        if (!date.isBefore(today)) throw new BusinessException(PaymentErrorCode.SETTLEMENT_DATE_INVALID);

        settlementMapper.ensureDay(SnowflakeUtils.nextId(), date);
        DailySettlement settlement = settlementMapper.selectOne(new LambdaQueryWrapper<DailySettlement>()
                .eq(DailySettlement::getSettlementDate, date).last("FOR UPDATE"));

        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
        List<Payment> payments = paymentMapper.selectList(
                new LambdaQueryWrapper<Payment>()
                        .in(Payment::getStatus, 1, 3)
                        .and(window -> window
                                .nested(paid -> paid.ge(Payment::getPaidAt, dayStart).lt(Payment::getPaidAt, dayEnd))
                                .or(legacy -> legacy.isNull(Payment::getPaidAt)
                                        .ge(Payment::getCreatedAt, dayStart).lt(Payment::getCreatedAt, dayEnd))));
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

        if (!Integer.valueOf(1).equals(settlement.getStatus())) {
            settlement.setTotalPaymentCount(totalPaymentCount);
            settlement.setTotalPaymentAmount(totalPaymentAmount);
            settlement.setTotalRefundCount(totalRefundCount);
            settlement.setTotalRefundAmount(totalRefundAmount);
            settlement.setNetAmount(totalPaymentAmount.subtract(totalRefundAmount));
            settlement.setStatus(1);
            settlementMapper.updateById(settlement);
            return toVO(settlement, List.of());
        }

        List<SettlementAdjustment> adjustments = new ArrayList<>(adjustmentsFor(List.of(settlement.getId())));
        SettlementVO previous = toVO(settlement, adjustments);
        if (!Objects.equals(previous.getTotalPaymentCount(), totalPaymentCount)
                || !Objects.equals(previous.getTotalRefundCount(), totalRefundCount)
                || amountOrZero(previous.getTotalPaymentAmount()).compareTo(totalPaymentAmount) != 0
                || amountOrZero(previous.getTotalRefundAmount()).compareTo(totalRefundAmount) != 0) {
            SettlementAdjustment adjustment = new SettlementAdjustment();
            adjustment.setId(SnowflakeUtils.nextId());
            adjustment.setSettlementId(settlement.getId());
            adjustment.setTotalPaymentCount(totalPaymentCount);
            adjustment.setTotalPaymentAmount(totalPaymentAmount);
            adjustment.setTotalRefundCount(totalRefundCount);
            adjustment.setTotalRefundAmount(totalRefundAmount);
            adjustment.setCreatedAt(LocalDateTime.now());
            adjustmentMapper.insert(adjustment);
            adjustments.add(adjustment);
        }
        return toVO(settlement, adjustments);
    }

    private List<SettlementAdjustment> adjustmentsFor(List<Long> ids) {
        if (ids.isEmpty()) return List.of();
        return adjustmentMapper.selectList(new LambdaQueryWrapper<SettlementAdjustment>()
                .in(SettlementAdjustment::getSettlementId, ids)
                .orderByAsc(SettlementAdjustment::getId));
    }

    @Override
    public Page<SettlementVO> listSettlements(int page, int size) {
        Page<DailySettlement> pageReq = new Page<>(page, size);
        settlementMapper.selectPage(pageReq,
                new LambdaQueryWrapper<DailySettlement>()
                        .lt(DailySettlement::getSettlementDate, LocalDate.now())
                        .orderByDesc(DailySettlement::getSettlementDate));
        List<Long> ids = pageReq.getRecords().stream().map(DailySettlement::getId).toList();
        Map<Long, List<SettlementAdjustment>> adjustments = adjustmentsFor(ids).stream()
                .collect(Collectors.groupingBy(SettlementAdjustment::getSettlementId));
        return new Page<SettlementVO>(pageReq.getCurrent(), pageReq.getSize(), pageReq.getTotal())
                .setRecords(pageReq.getRecords().stream()
                        .map(row -> toVO(row, adjustments.getOrDefault(row.getId(), List.of())))
                        .toList());
    }

    @Override
    public Page<SettlementVO> listByMerchant(Long merchantId, int page, int size) {
        List<String> orderNos = loadMerchantOrderNos(merchantId);
        if (orderNos.isEmpty()) {
            return new Page<>(page, size, 0);
        }

        List<Payment> payments = paymentMapper.selectList(new LambdaQueryWrapper<Payment>()
                .in(Payment::getOrderNo, orderNos)
                .in(Payment::getStatus, 1, 3));
        List<Refund> refunds = refundMapper.selectList(new LambdaQueryWrapper<Refund>()
                .in(Refund::getOrderNo, orderNos)
                .eq(Refund::getStatus, 1));

        LocalDate today = LocalDate.now();
        Map<LocalDate, SettlementAccumulator> grouped = new HashMap<>();
        for (Payment payment : payments) {
            LocalDate settlementDate = resolveDate(payment.getPaidAt(), payment.getCreatedAt());
            if (!settlementDate.isBefore(today)) continue;
            SettlementAccumulator accumulator = grouped.computeIfAbsent(settlementDate, key -> new SettlementAccumulator());
            accumulator.totalPaymentCount++;
            accumulator.totalPaymentAmount = accumulator.totalPaymentAmount.add(amountOrZero(payment.getAmount()));
        }
        for (Refund refund : refunds) {
            LocalDate settlementDate = refund.getCreatedAt() != null ? refund.getCreatedAt().toLocalDate() : LocalDate.now();
            if (!settlementDate.isBefore(today)) continue;
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

    private SettlementVO toVO(DailySettlement s, List<SettlementAdjustment> adjustments) {
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
        vo.setBaseNetAmount(s.getNetAmount());
        vo.setAdjustmentCount(adjustments.size());
        if (!adjustments.isEmpty()) {
            SettlementAdjustment last = adjustments.getLast();
            vo.setTotalPaymentCount(last.getTotalPaymentCount());
            vo.setTotalPaymentAmount(last.getTotalPaymentAmount());
            vo.setTotalRefundCount(last.getTotalRefundCount());
            vo.setTotalRefundAmount(last.getTotalRefundAmount());
            vo.setNetAmount(last.getTotalPaymentAmount().subtract(last.getTotalRefundAmount()));
        }
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
