package com.ecommerce.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.common.dto.OrderMemberVO;
import com.ecommerce.common.result.Result;
import com.ecommerce.member.client.OrderClient;
import com.ecommerce.member.dto.request.internal.RefundCompensationRequest;
import com.ecommerce.member.dto.response.internal.RefundCompensationResult;
import com.ecommerce.member.entity.PointsReservation;
import com.ecommerce.member.entity.PointsTransaction;
import com.ecommerce.member.mapper.GrowthTransactionMapper;
import com.ecommerce.member.mapper.MemberProfileMapper;
import com.ecommerce.member.mapper.PointsConsumeDetailMapper;
import com.ecommerce.member.mapper.PointsReservationMapper;
import com.ecommerce.member.mapper.PointsTransactionMapper;
import com.ecommerce.member.service.GrowthService;
import com.ecommerce.member.service.PointsService;
import com.ecommerce.member.service.RefundCompensationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundCompensationServiceImpl implements RefundCompensationService {

    private final PointsReservationMapper pointsReservationMapper;
    private final PointsConsumeDetailMapper pointsConsumeDetailMapper;
    private final PointsTransactionMapper pointsTransactionMapper;
    private final GrowthTransactionMapper growthTransactionMapper;
    private final MemberProfileMapper memberProfileMapper;
    private final PointsService pointsService;
    private final GrowthService growthService;
    private final OrderClient orderClient;

    @Override
    @Transactional
    public RefundCompensationResult compensate(RefundCompensationRequest request) {
        RefundCompensationResult result = new RefundCompensationResult();
        result.setDuplicate(false);
        result.setRestoredPoints(0);
        result.setReversedGrowth(0);

        String pointsBizKey = buildPointsBizKey(request);
        if (pointsTransactionMapper.selectCount(new LambdaQueryWrapper<PointsTransaction>()
                .eq(PointsTransaction::getBizKey, pointsBizKey)) > 0) {
            result.setDuplicate(true);
            return result;
        }

        PointsReservation reservation = pointsReservationMapper.selectOne(new LambdaQueryWrapper<PointsReservation>()
                .eq(PointsReservation::getOrderNo, request.getOrderNo()));
        if (reservation == null) {
            return result;
        }

        int restoredPoints = calculateRestoredPoints(request, reservation);
        if (restoredPoints <= 0) {
            updateReservationStatus(reservation, request.getRefundType());
            return result;
        }

        pointsService.reverseSpend(request.getUserId(), restoredPoints, "REFUND", request.getRefundNo(),
                pointsBizKey, "退款退回积分", reservation.getReservationNo(), null);
        growthService.add(request.getUserId(), request.getRefundAmount().intValue() * -1, "REFUND",
                request.getRefundNo(), buildGrowthBizKey(request), "退款扣回成长值");

        updateReservationStatus(reservation, request.getRefundType());
        result.setRestoredPoints(restoredPoints);
        result.setReversedGrowth(request.getRefundAmount().intValue());
        return result;
    }

    private int calculateRestoredPoints(RefundCompensationRequest request, PointsReservation reservation) {
        if ("FULL".equalsIgnoreCase(request.getRefundType())) {
            return reservation.getConsumedPoints() == null ? 0 : reservation.getConsumedPoints();
        }

        Result<OrderMemberVO> orderResult = orderClient.getOrderForMember(request.getOrderNo());
        if (orderResult == null || orderResult.getData() == null) {
            log.warn("Skip partial refund compensation due to missing order total: orderNo={}, refundNo={}",
                    request.getOrderNo(), request.getRefundNo());
            return 0;
        }

        OrderMemberVO order = orderResult.getData();
        if (order.getPointsUsed() != null && order.getPointsUsed() > 0
                && order.getPointsDeductionAmount() != null
                && order.getPointsDeductionAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal effectiveRefund = request.getRefundAmount().min(order.getPointsDeductionAmount());
            BigDecimal ratio = effectiveRefund.divide(order.getPointsDeductionAmount(), 8, RoundingMode.DOWN);
            return BigDecimal.valueOf(order.getPointsUsed())
                    .multiply(ratio)
                    .setScale(0, RoundingMode.DOWN)
                    .intValue();
        }

        if (order.getTotalAmount() == null || order.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Skip partial refund compensation due to missing order total: orderNo={}, refundNo={}",
                    request.getOrderNo(), request.getRefundNo());
            return 0;
        }

        BigDecimal ratio = request.getRefundAmount()
                .divide(order.getTotalAmount(), 8, RoundingMode.DOWN);
        return BigDecimal.valueOf(reservation.getConsumedPoints() == null ? 0 : reservation.getConsumedPoints())
                .multiply(ratio)
                .setScale(0, RoundingMode.DOWN)
                .intValue();
    }

    private void updateReservationStatus(PointsReservation reservation, String refundType) {
        reservation.setStatus("FULL".equalsIgnoreCase(refundType) ? "REFUNDED" : "PARTIAL_REFUNDED");
        pointsReservationMapper.updateById(reservation);
    }

    private String buildPointsBizKey(RefundCompensationRequest request) {
        return "REFUND:" + request.getRefundNo() + ":POINTS";
    }

    private String buildGrowthBizKey(RefundCompensationRequest request) {
        return "REFUND:" + request.getRefundNo() + ":GROWTH";
    }
}
