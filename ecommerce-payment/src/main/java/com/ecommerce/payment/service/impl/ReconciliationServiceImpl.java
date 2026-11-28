package com.ecommerce.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.util.SnowflakeUtils;
import com.ecommerce.common.dto.ReconOrderVO;
import com.ecommerce.common.constant.OrderStatus;
import com.ecommerce.payment.entity.Refund;
import com.ecommerce.payment.mapper.RefundMapper;
import com.ecommerce.payment.client.OrderClient;
import com.ecommerce.payment.dto.response.ReconciliationVO;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.Reconciliation;
import com.ecommerce.payment.entity.ReconciliationDetail;
import com.ecommerce.payment.mapper.PaymentMapper;
import com.ecommerce.payment.mapper.ReconciliationDetailMapper;
import com.ecommerce.payment.mapper.ReconciliationMapper;
import com.ecommerce.payment.service.ReconciliationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReconciliationServiceImpl implements ReconciliationService {

    private final ReconciliationMapper reconciliationMapper;
    private final ReconciliationDetailMapper detailMapper;
    private final PaymentMapper paymentMapper;
    private final OrderClient orderClient;
    private final RefundMapper refundMapper;

    public ReconciliationServiceImpl(ReconciliationMapper reconciliationMapper,
                                      ReconciliationDetailMapper detailMapper,
                                      PaymentMapper paymentMapper,
                                      OrderClient orderClient, RefundMapper refundMapper) {
        this.reconciliationMapper = reconciliationMapper;
        this.detailMapper = detailMapper;
        this.paymentMapper = paymentMapper;
        this.orderClient = orderClient;
        this.refundMapper = refundMapper;
    }

    @Override
    @Transactional
    public ReconciliationVO runReconciliation() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.minusDays(30);
        Reconciliation batch = new Reconciliation();
        batch.setId(SnowflakeUtils.nextId());
        batch.setBatchNo(generateBatchNo());
        batch.setStartDate(start);
        batch.setEndDate(now);
        batch.setTotalOrderCount(0);
        batch.setTotalPaymentCount(0);
        batch.setMatchedCount(0);
        batch.setUnmatchedCount(0);
        batch.setProcessingCount(0);

        List<ReconOrderVO> orders;
        try {
            var response = orderClient.getOrdersForRecon(start.toString(), now.toString());
            if (response == null || response.getCode() != 200 || response.getData() == null) {
                throw new IllegalStateException("Order source unavailable");
            }
            orders = response.getData();
        } catch (RuntimeException unavailable) {
            return failSource(batch);
        }

        List<Payment> payments = paymentMapper.selectList(new LambdaQueryWrapper<Payment>()
                .and(window -> window.nested(created -> created.ge(Payment::getCreatedAt, start).le(Payment::getCreatedAt, now))
                        .or(paid -> paid.ge(Payment::getPaidAt, start).le(Payment::getPaidAt, now))));
        Map<String, Payment> byPayment = payments.stream().filter(row -> row.getOrderNo() != null)
                .collect(Collectors.toMap(Payment::getOrderNo, row -> row, (first, second) -> first));
        List<Refund> recentRefunds = refundMapper.selectList(new LambdaQueryWrapper<Refund>()
                .ge(Refund::getCreatedAt, start).le(Refund::getCreatedAt, now).eq(Refund::getStatus, 1));
        Set<String> refundOrderNos = recentRefunds.stream().map(Refund::getOrderNo).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<String> historicalPayments = refundOrderNos.stream().filter(no -> !byPayment.containsKey(no)).toList();
        if (!historicalPayments.isEmpty()) {
            paymentMapper.selectList(new LambdaQueryWrapper<Payment>().in(Payment::getOrderNo, historicalPayments))
                    .forEach(row -> byPayment.putIfAbsent(row.getOrderNo(), row));
        }
        Map<String, ReconOrderVO> byOrder = orders.stream().filter(row -> row.getOrderNo() != null)
                .collect(Collectors.toMap(ReconOrderVO::getOrderNo, row -> row, (first, second) -> first));
        try {
            Set<String> orderNos = new TreeSet<>(byPayment.keySet());
            orderNos.addAll(refundOrderNos);
            List<String> missing = orderNos.stream().filter(number -> !byOrder.containsKey(number)).toList();
            for (int startAt = 0; startAt < missing.size(); startAt += 100) {
                var response = orderClient.getOrdersByNumberForRecon(
                        missing.subList(startAt, Math.min(startAt + 100, missing.size())));
                if (response == null || response.getCode() != 200 || response.getData() == null) {
                    return failSource(batch);
                }
                response.getData().forEach(row -> byOrder.put(row.getOrderNo(), row));
            }
        } catch (RuntimeException unavailable) {
            return failSource(batch);
        }
        Set<String> refundLookup = new TreeSet<>(byPayment.keySet());
        refundLookup.addAll(refundOrderNos);
        Map<String, List<Refund>> byRefund = refundLookup.isEmpty() ? Map.of() : refundMapper.selectList(
                        new LambdaQueryWrapper<Refund>().in(Refund::getOrderNo, refundLookup)
                                .eq(Refund::getStatus, 1))
                .stream().collect(Collectors.groupingBy(Refund::getOrderNo));

        Set<String> orderNumbers = new TreeSet<>(byOrder.keySet());
        orderNumbers.addAll(byPayment.keySet());
        orderNumbers.addAll(refundOrderNos);
        List<ReconciliationDetail> details = new ArrayList<>();
        for (String orderNo : orderNumbers) {
            ReconOrderVO order = byOrder.get(orderNo);
            Payment payment = byPayment.get(orderNo);
            List<Refund> refunds = byRefund.getOrDefault(orderNo, List.of());
            Match match = compare(order, payment, refunds, now.minusMinutes(5));
            switch (match.status()) {
                case "MATCHED" -> batch.setMatchedCount(batch.getMatchedCount() + 1);
                case "PROCESSING" -> batch.setProcessingCount(batch.getProcessingCount() + 1);
                case "NOT_REQUIRED" -> { }
                default -> batch.setUnmatchedCount(batch.getUnmatchedCount() + 1);
            }
            if (order != null) {
                addDetail(details, batch.getId(), "ORDER", orderNo, null,
                        order.getAmount(), order.getStatus(), match.status(), match.reason());
            }
            if (order == null && payment == null) {
                BigDecimal amount = refunds.stream().map(Refund::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                addDetail(details, batch.getId(), "REFUND", orderNo, null, amount, null, match.status(), match.reason());
            }
            if (payment != null && (order == null || !("MATCHED".equals(match.status()) || "NOT_REQUIRED".equals(match.status())))) {
                addDetail(details, batch.getId(), "PAYMENT", orderNo, payment.getPaymentNo(),
                        payment.getAmount(), payment.getStatus(), match.status(), match.reason());
            }
        }
        batch.setTotalOrderCount(byOrder.size());
        batch.setTotalPaymentCount(byPayment.size());
        batch.setStatus(1);
        reconciliationMapper.insert(batch);
        details.forEach(detailMapper::insert);
        return toVO(batch);
    }

    private ReconciliationVO failSource(Reconciliation batch) {
        batch.setStatus(2);
        batch.setFailureReason("订单数据读取失败，请重跑对账");
        reconciliationMapper.insert(batch);
        return toVO(batch);
    }

    private record Match(String status, String reason) {}

    private Match compare(ReconOrderVO order, Payment payment, List<Refund> refunds, LocalDateTime graceStart) {
        if (order == null) return payment == null
                ? new Match("REFUND_ONLY", "退款缺少原订单和支付记录")
                : new Match("PAYMENT_ONLY", "缺少订单记录");
        Integer status = order.getStatus();
        boolean unpaidOrder = Objects.equals(status, OrderStatus.PENDING) || Objects.equals(status, OrderStatus.CANCELLED);
        boolean captured = payment != null && Set.of(1, 3).contains(Objects.requireNonNullElse(payment.getStatus(), -1));
        if (!captured) {
            if (!refunds.isEmpty()) return new Match("STATUS_MISMATCH", "退款缺少原支付记录");
            return unpaidOrder ? new Match("NOT_REQUIRED", "订单未收款")
                    : new Match(payment == null ? "ORDER_ONLY" : "STATUS_MISMATCH", "订单缺少成功支付");
        }
        if (order.getAmount() == null || payment.getAmount() == null || order.getAmount().compareTo(payment.getAmount()) != 0) {
            return new Match("AMOUNT_MISMATCH", "订单金额=" + order.getAmount() + " 支付金额=" + payment.getAmount());
        }

        BigDecimal refunded = refunds.stream().map(Refund::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean fullyRefunded = Integer.valueOf(3).equals(payment.getStatus());
        if (refunded.signum() < 0 || refunded.compareTo(payment.getAmount()) > 0
                || (fullyRefunded && (refunds.isEmpty() || refunded.compareTo(payment.getAmount()) != 0))
                || (!fullyRefunded && refunded.signum() > 0 && refunded.compareTo(payment.getAmount()) == 0)) {
            return new Match("AMOUNT_MISMATCH", "支付状态与成功退款流水金额不一致");
        }
        boolean orderRefunded = Objects.equals(status, OrderStatus.REFUNDED);
        if ((orderRefunded && fullyRefunded)
                || (!fullyRefunded && Set.of(OrderStatus.PAID, OrderStatus.SHIPPED, OrderStatus.COMPLETED)
                .contains(Objects.requireNonNullElse(status, -1)))) {
            return new Match("MATCHED", null);
        }
        boolean recent = (payment.getPaidAt() != null && payment.getPaidAt().isAfter(graceStart))
                || (order.getUpdatedAt() != null && order.getUpdatedAt().isAfter(graceStart))
                || refunds.stream().anyMatch(refund -> refund.getCreatedAt() != null && refund.getCreatedAt().isAfter(graceStart));
        return recent ? new Match("PROCESSING", "支付或退款结果等待订单同步")
                : new Match("STATUS_MISMATCH", "支付或退款状态与订单状态不一致");
    }

    @Override
    public List<ReconciliationVO> listReconciliations() {
        List<Reconciliation> list = reconciliationMapper.selectList(
                new LambdaQueryWrapper<Reconciliation>().orderByDesc(Reconciliation::getCreatedAt));
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public Page<ReconciliationVO> listReconciliations(int page, int size) {
        IPage<Reconciliation> ipage = reconciliationMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<Reconciliation>().orderByDesc(Reconciliation::getCreatedAt));
        Page<ReconciliationVO> result = new Page<>(page, size);
        result.setTotal(ipage.getTotal());
        result.setRecords(ipage.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return result;
    }

    @Override
    public ReconciliationVO getReconciliationDetail(Long id) {
        Reconciliation rec = reconciliationMapper.selectById(id);
        if (rec == null) return null;
        ReconciliationVO vo = toVO(rec);
        List<ReconciliationDetail> details = detailMapper.selectList(
                new LambdaQueryWrapper<ReconciliationDetail>()
                        .eq(ReconciliationDetail::getReconciliationId, id));
        vo.setDetails(details.stream().map(this::toDetailVO).collect(Collectors.toList()));
        return vo;
    }

    @Override
    public Page<ReconciliationVO> listByMerchant(Long merchantId, int page, int size) {
        Set<String> merchantOrderNos = loadMerchantOrderNos(merchantId);
        if (merchantOrderNos.isEmpty()) {
            return new Page<>(page, size, 0);
        }
        List<Reconciliation> list = reconciliationMapper.selectList(
                new LambdaQueryWrapper<Reconciliation>().orderByDesc(Reconciliation::getCreatedAt));
        List<ReconciliationVO> merchantViews = list.stream()
                .map(rec -> toMerchantView(rec, merchantOrderNos, false))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        int fromIndex = Math.max((page - 1) * size, 0);
        if (fromIndex >= merchantViews.size()) {
            return new Page<>(page, size, merchantViews.size());
        }
        int toIndex = Math.min(fromIndex + size, merchantViews.size());
        Page<ReconciliationVO> result = new Page<>(page, size, merchantViews.size());
        result.setRecords(merchantViews.subList(fromIndex, toIndex));
        return result;
    }

    @Override
    public ReconciliationVO getReconciliationDetailByMerchant(Long merchantId, Long id) {
        Reconciliation rec = reconciliationMapper.selectById(id);
        if (rec == null) {
            return null;
        }
        Set<String> merchantOrderNos = loadMerchantOrderNos(merchantId);
        if (merchantOrderNos.isEmpty()) {
            return null;
        }
        return toMerchantView(rec, merchantOrderNos, true);
    }

    private void addDetail(List<ReconciliationDetail> details, Long recId,
                           String recordType, String orderNo, String paymentNo,
                           BigDecimal amount, Integer recordStatus, String matchStatus, String diffReason) {
        ReconciliationDetail d = new ReconciliationDetail();
        d.setId(SnowflakeUtils.nextId());
        d.setReconciliationId(recId);
        d.setRecordType(recordType);
        d.setOrderNo(orderNo);
        d.setPaymentNo(paymentNo);
        d.setAmount(amount);
        d.setRecordStatus(recordStatus);
        d.setMatchStatus(matchStatus);
        d.setDiffReason(diffReason);
        details.add(d);
    }

    private ReconciliationVO toVO(Reconciliation r) {
        ReconciliationVO vo = new ReconciliationVO();
        vo.setId(r.getId());
        vo.setBatchNo(r.getBatchNo());
        vo.setStartDate(r.getStartDate());
        vo.setEndDate(r.getEndDate());
        vo.setTotalOrderCount(r.getTotalOrderCount());
        vo.setTotalPaymentCount(r.getTotalPaymentCount());
        vo.setMatchedCount(r.getMatchedCount());
        vo.setUnmatchedCount(r.getUnmatchedCount());
        vo.setProcessingCount(r.getProcessingCount());
        vo.setFailureReason(r.getFailureReason());
        vo.setStatus(r.getStatus());
        vo.setStatusText(r.getStatus() == 1 ? "已完成" : r.getStatus() == 0 ? "进行中" : "失败");
        vo.setCreatedAt(r.getCreatedAt());
        return vo;
    }

    private String generateBatchNo() {
        return "REC" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", SnowflakeUtils.nextId() % 10000);
    }

    private Set<String> loadMerchantOrderNos(Long merchantId) {
        try {
            var response = orderClient.listOrderNosByMerchant(merchantId);
            List<String> orderNos = response.getData() != null ? response.getData() : Collections.emptyList();
            return new HashSet<>(orderNos);
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }

    private ReconciliationVO toMerchantView(Reconciliation rec, Set<String> merchantOrderNos, boolean includeDetails) {
        List<ReconciliationDetail> details = detailMapper.selectList(
                new LambdaQueryWrapper<ReconciliationDetail>()
                        .eq(ReconciliationDetail::getReconciliationId, rec.getId()));
        List<ReconciliationDetail> merchantDetails = details.stream()
                .filter(detail -> detail.getOrderNo() != null && merchantOrderNos.contains(detail.getOrderNo()))
                .collect(Collectors.toList());
        if (merchantDetails.isEmpty()) {
            return null;
        }

        Set<String> orderCountSet = merchantDetails.stream()
                .filter(detail -> "ORDER".equals(detail.getRecordType()))
                .map(ReconciliationDetail::getOrderNo)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<String> paymentCountSet = merchantDetails.stream()
                .filter(detail -> "PAYMENT".equals(detail.getRecordType()) || "MATCHED".equals(detail.getMatchStatus()))
                .map(ReconciliationDetail::getOrderNo)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<String> matchedOrderSet = merchantDetails.stream()
                .filter(detail -> "MATCHED".equals(detail.getMatchStatus()))
                .map(ReconciliationDetail::getOrderNo)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        ReconciliationVO vo = toVO(rec);
        vo.setTotalOrderCount(orderCountSet.size());
        vo.setTotalPaymentCount(paymentCountSet.size());
        vo.setMatchedCount(matchedOrderSet.size());
        vo.setProcessingCount((int) merchantDetails.stream().filter(detail -> "PROCESSING".equals(detail.getMatchStatus()))
                .map(ReconciliationDetail::getOrderNo).distinct().count());
        vo.setUnmatchedCount((int) merchantDetails.stream()
                .filter(detail -> !Set.of("MATCHED", "PROCESSING", "NOT_REQUIRED").contains(detail.getMatchStatus()))
                .map(ReconciliationDetail::getOrderNo).distinct().count());
        if (includeDetails) {
            vo.setDetails(merchantDetails.stream().map(this::toDetailVO).collect(Collectors.toList()));
        }
        return vo;
    }

    private ReconciliationVO.DetailVO toDetailVO(ReconciliationDetail detail) {
        ReconciliationVO.DetailVO dv = new ReconciliationVO.DetailVO();
        dv.setId(detail.getId());
        dv.setRecordType(detail.getRecordType());
        dv.setOrderNo(detail.getOrderNo());
        dv.setPaymentNo(detail.getPaymentNo());
        dv.setAmount(detail.getAmount());
        dv.setRecordStatus(detail.getRecordStatus());
        dv.setMatchStatus(detail.getMatchStatus());
        dv.setDiffReason(detail.getDiffReason());
        return dv;
    }


}
