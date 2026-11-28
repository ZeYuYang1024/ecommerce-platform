package com.ecommerce.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.common.dto.OrderMemberVO;
import com.ecommerce.common.result.Result;
import com.ecommerce.member.client.OrderClient;
import com.ecommerce.member.dto.request.internal.RefundCompensationRequest;
import com.ecommerce.member.dto.request.internal.PointsReservationConfirmRequest;
import com.ecommerce.member.dto.response.internal.RefundCompensationResult;
import com.ecommerce.member.entity.GrowthTransaction;
import com.ecommerce.member.entity.PointsReservation;
import com.ecommerce.member.entity.PointsTransaction;
import com.ecommerce.member.mapper.GrowthTransactionMapper;
import com.ecommerce.member.mapper.MemberOrderFinanceMapper;
import com.ecommerce.member.mapper.PointsReservationMapper;
import com.ecommerce.member.mapper.PointsTransactionMapper;
import com.ecommerce.member.service.GrowthService;
import com.ecommerce.member.service.PointsService;
import com.ecommerce.member.service.PointsReservationService;
import com.ecommerce.member.service.RefundCompensationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundCompensationServiceImpl implements RefundCompensationService {

    private final PointsReservationMapper pointsReservationMapper;
    private final MemberOrderFinanceMapper finance;
    private final PointsTransactionMapper pointsTransactionMapper;
    private final GrowthTransactionMapper growthTransactionMapper;
    private final PointsReservationService reservations;
    private final PointsService pointsService;
    private final GrowthService growthService;
    private final OrderClient orderClient;

    @Override
    @Transactional
    public RefundCompensationResult compensate(RefundCompensationRequest request) {
        RefundCompensationResult result = newResult();
        String orderNo = request.getOrderNo();
        finance.lockOrder(orderNo);
        if (finance.isFullyRefunded(orderNo)) {
            result.setDuplicate(true);
            return result;
        }
        if ("FULL".equalsIgnoreCase(request.getRefundType())) {
            applyBenefits(orderNo, request.getUserId(), null, null, true, result);
            finance.markFullyRefunded(orderNo);
            return result;
        }

        Result<OrderMemberVO> reply = orderClient.getOrderForMember(orderNo);
        if (reply == null || reply.getCode() != 200 || reply.getData() == null) {
            throw new IllegalStateException("Cannot load order for member refund: " + orderNo);
        }
        OrderMemberVO order = reply.getData();
        BigDecimal cumulative = request.cumulativeAmount();
        if (!Objects.equals(order.getUserId(), request.getUserId()) || cumulative == null || cumulative.signum() <= 0
                || order.getTotalAmount() == null || cumulative.compareTo(order.getTotalAmount()) >= 0) {
            log.error("Invalid partial refund for member benefits: orderNo={}, refundNo={}", orderNo, request.getRefundNo());
            return result;
        }

        BigDecimal previous = finance.partialRefundAmount(orderNo);
        if (previous != null && cumulative.compareTo(previous) <= 0) {
            result.setDuplicate(true);
            return result;
        }
        finance.recordPartialRefund(orderNo, cumulative);
        applyBenefits(orderNo, order.getUserId(), order.getTotalAmount(), cumulative, false, result);
        return result;
    }

    @Override
    @Transactional
    public void settlePendingPartialRefund(OrderMemberVO order) {
        finance.lockOrder(order.getOrderNo());
        BigDecimal cumulative = finance.partialRefundAmount(order.getOrderNo());
        if (cumulative != null && cumulative.signum() > 0 && !finance.isFullyRefunded(order.getOrderNo())) {
            applyBenefits(order.getOrderNo(), order.getUserId(), order.getTotalAmount(), cumulative, false, newResult());
        }
    }

    private RefundCompensationResult newResult() {
        RefundCompensationResult result = new RefundCompensationResult();
        result.setDuplicate(false);
        result.setRestoredPoints(0);
        result.setReversedGrowth(0);
        return result;
    }

    private void applyBenefits(String orderNo, Long userId, BigDecimal paid, BigDecimal refunded, boolean full,
                               RefundCompensationResult result) {
        PointsReservation reservation = pointsReservationMapper.selectOne(new LambdaQueryWrapper<PointsReservation>()
                .eq(PointsReservation::getOrderNo, orderNo));
        List<PointsTransaction> earlierReturns = List.of();
        if (reservation != null && !"RELEASED".equals(reservation.getStatus())) {
            if ("RESERVED".equals(reservation.getStatus())) {
                reservations.confirm(new PointsReservationConfirmRequest(
                        reservation.getReservationNo(), "ORDER:" + orderNo + ":SPEND"));
                reservation.setConsumedPoints(reservation.getReservedPoints());
            }
            earlierReturns = pointsTransactionMapper.selectList(new LambdaQueryWrapper<PointsTransaction>()
                    .eq(PointsTransaction::getRelatedReservationNo, reservation.getReservationNo())
                    .eq(PointsTransaction::getSourceType, "REFUND").eq(PointsTransaction::getDirection, "EARN"));
            int previous = earlierReturns.stream().mapToInt(PointsTransaction::getAmount).sum();
            int used = Objects.requireNonNullElse(reservation.getConsumedPoints(), 0);
            if (used == 0) used = Objects.requireNonNullElse(reservation.getReservedPoints(), 0);
            int target = targetAmount(used, paid, refunded, full);
            if (target > previous) {
                pointsService.reverseSpend(userId, target - previous, "REFUND", orderNo,
                        "ORDER:" + orderNo + ":RESTORE_POINTS:" + target, "退款退回积分", reservation.getReservationNo(), null);
                result.setRestoredPoints(target - previous);
            }
            reservation.setStatus(full ? "REFUNDED" : "PARTIAL_REFUNDED");
            pointsReservationMapper.updateById(reservation);
        }

        PointsTransaction reward = pointsTransactionMapper.selectOne(new LambdaQueryWrapper<PointsTransaction>()
                .eq(PointsTransaction::getBizKey, "ORDER:" + orderNo + ":EARN"));
        if (reward != null) {
            int target = targetAmount(reward.getAmount(), paid, refunded, full);
            pointsService.reverseEarn(reward, orderNo, target, "ORDER:" + orderNo + ":EARN_REVERSED:" + target);
        }

        GrowthTransaction earned = growthTransactionMapper.selectOne(new LambdaQueryWrapper<GrowthTransaction>()
                .eq(GrowthTransaction::getBizKey, "ORDER:" + orderNo + ":GROWTH"));
        if (earned != null) {
            List<String> legacyKeys = earlierReturns.stream().map(PointsTransaction::getSourceId)
                    .filter(sourceId -> !orderNo.equals(sourceId))
                    .map(refundNo -> "REFUND:" + refundNo + ":GROWTH").toList();
            int previous = growthTransactionMapper.selectList(new LambdaQueryWrapper<GrowthTransaction>()
                            .and(q -> q.nested(orderRefund -> orderRefund.eq(GrowthTransaction::getSourceType, "ORDER_REFUND")
                                            .eq(GrowthTransaction::getSourceId, orderNo))
                                    .or(!legacyKeys.isEmpty(), legacy -> legacy.in(GrowthTransaction::getBizKey, legacyKeys))))
                    .stream().mapToInt(tx -> -tx.getAmount()).sum();
            int target = targetAmount(earned.getAmount(), paid, refunded, full);
            if (target > previous) {
                growthService.add(userId, previous - target, "ORDER_REFUND", orderNo,
                        "ORDER:" + orderNo + ":GROWTH_REVERSED:" + target, "退款扣回成长值");
                result.setReversedGrowth(target - previous);
            }
        }
    }

    private int targetAmount(int initial, BigDecimal paid, BigDecimal refunded, boolean full) {
        return full ? initial
                : BigDecimal.valueOf(initial).multiply(refunded).divide(paid, 0, RoundingMode.DOWN).intValueExact();
    }
}
