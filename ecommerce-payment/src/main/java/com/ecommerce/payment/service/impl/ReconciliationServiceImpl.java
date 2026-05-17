package com.ecommerce.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.util.SnowflakeUtils;
import com.ecommerce.common.dto.ReconOrderVO;
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

    public ReconciliationServiceImpl(ReconciliationMapper reconciliationMapper,
                                      ReconciliationDetailMapper detailMapper,
                                      PaymentMapper paymentMapper,
                                      OrderClient orderClient) {
        this.reconciliationMapper = reconciliationMapper;
        this.detailMapper = detailMapper;
        this.paymentMapper = paymentMapper;
        this.orderClient = orderClient;
    }

    @Override
    @Transactional
    public ReconciliationVO runReconciliation() {
        // Fetch payments from last 30 days to avoid OOM
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<Payment> payments = paymentMapper.selectList(
                new LambdaQueryWrapper<Payment>().ge(Payment::getCreatedAt, thirtyDaysAgo));
        Map<String, Payment> paymentMap = new HashMap<>();
        for (Payment p : payments) {
            if (p.getOrderNo() != null) {
                paymentMap.put(p.getOrderNo(), p);
            }
        }

        // Fetch orders from same window via Feign
        String startStr = thirtyDaysAgo.toString();
        String endStr = LocalDateTime.now().toString();
        List<ReconOrderVO> orders;
        try {
            var res = orderClient.getOrdersForRecon(startStr, endStr);
            orders = res.getData() != null ? res.getData() : Collections.emptyList();
        } catch (Exception e) {
            orders = Collections.emptyList();
        }
        Map<String, ReconOrderVO> orderMap = new HashMap<>();
        for (ReconOrderVO o : orders) {
            String orderNo = (String) o.getOrderNo();
            if (orderNo != null) {
                orderMap.put(orderNo, o);
            }
        }

        // Create reconciliation batch
        Reconciliation rec = new Reconciliation();
        rec.setId(SnowflakeUtils.nextId());
        rec.setBatchNo(generateBatchNo());
        rec.setStartDate(LocalDateTime.now());
        rec.setEndDate(LocalDateTime.now());

        int matched = 0;
        List<ReconciliationDetail> details = new ArrayList<>();

        // Match: iterate over all order_nos from both sets
        Set<String> allOrderNos = new HashSet<>();
        allOrderNos.addAll(orderMap.keySet());
        allOrderNos.addAll(paymentMap.keySet());

        for (String orderNo : allOrderNos) {
            ReconOrderVO orderData = orderMap.get(orderNo);
            Payment payment = paymentMap.get(orderNo);

            if (orderData != null && payment != null) {
                // Both exist: check amount match
                BigDecimal orderAmount = orderData.getAmount();
                BigDecimal paymentAmount = payment.getAmount();
                boolean amountMatch = orderAmount != null && paymentAmount != null
                        && orderAmount.compareTo(paymentAmount) == 0;

                if (amountMatch) {
                    matched++;
                    addDetail(details, rec.getId(), "ORDER", orderNo, null, orderAmount, orderData.getStatus(), "MATCHED", null);
                } else {
                    addDetail(details, rec.getId(), "ORDER", orderNo, null, orderAmount, orderData.getStatus(), "AMOUNT_MISMATCH",
                            "订单金额=" + orderAmount + " 支付金额=" + paymentAmount);
                    addDetail(details, rec.getId(), "PAYMENT", orderNo, payment.getPaymentNo(), paymentAmount, payment.getStatus(), "AMOUNT_MISMATCH",
                            "订单金额=" + orderAmount + " 支付金额=" + paymentAmount);
                }
            } else if (orderData != null) {
                // Order only - missing payment
                BigDecimal orderAmount = orderData.getAmount();
                addDetail(details, rec.getId(), "ORDER", orderNo, null, orderAmount, orderData.getStatus(), "ORDER_ONLY", "缺少支付记录");
            } else {
                // Payment only - missing order
                addDetail(details, rec.getId(), "PAYMENT", orderNo, payment.getPaymentNo(), payment.getAmount(), payment.getStatus(), "PAYMENT_ONLY", "缺少订单记录");
            }
        }

        int unmatched = details.size() - matched;

        rec.setTotalOrderCount(orders.size());
        rec.setTotalPaymentCount(payments.size());
        rec.setMatchedCount(matched);
        rec.setUnmatchedCount(unmatched);
        rec.setStatus(1); // completed
        reconciliationMapper.insert(rec);

        // Batch insert details
        for (ReconciliationDetail detail : details) {
            detailMapper.insert(detail);
        }

        return toVO(rec);
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
        vo.setUnmatchedCount((int) merchantDetails.stream()
                .filter(detail -> !"MATCHED".equals(detail.getMatchStatus()))
                .count());
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
