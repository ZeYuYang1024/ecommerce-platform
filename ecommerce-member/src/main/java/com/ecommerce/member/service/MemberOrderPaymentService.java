package com.ecommerce.member.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.common.dto.OrderMemberVO;
import com.ecommerce.member.dto.request.internal.PointsReservationConfirmRequest;
import com.ecommerce.member.dto.request.internal.PointsReservationReleaseRequest;
import com.ecommerce.member.entity.PointsReservation;
import com.ecommerce.member.mapper.MemberOrderFinanceMapper;
import com.ecommerce.member.mapper.PointsReservationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberOrderPaymentService {
    private final MemberOrderFinanceMapper finance;
    private final PointsReservationMapper reservations;
    private final PointsReservationService reservationService;
    private final PointsService points;
    private final GrowthService growth;

    @Transactional
    public void confirmPayment(OrderMemberVO order) {
        String orderNo = order.getOrderNo();
        finance.lockOrder(orderNo);
        if (finance.isFullyRefunded(orderNo)) return;

        PointsReservation reservation = findReservation(orderNo);
        if (reservation != null && "RESERVED".equals(reservation.getStatus())) {
            reservationService.confirm(new PointsReservationConfirmRequest(
                    reservation.getReservationNo(), "ORDER:" + orderNo + ":SPEND"));
        }

        int paidYuan = order.getTotalAmount().intValue();
        if (paidYuan <= 0) return;
        points.earn(order.getUserId(), paidYuan, "ORDER", orderNo, "ORDER:" + orderNo + ":EARN", "订单支付获得积分");
        growth.add(order.getUserId(), paidYuan, "ORDER", orderNo, "ORDER:" + orderNo + ":GROWTH", "订单支付获得成长值");
    }

    @Transactional
    public void cancel(String orderNo) {
        finance.lockOrder(orderNo);
        PointsReservation reservation = findReservation(orderNo);
        if (reservation != null && "RESERVED".equals(reservation.getStatus())) {
            reservationService.release(new PointsReservationReleaseRequest(
                    reservation.getReservationNo(), "ORDER_CANCELLED", "ORDER:" + orderNo + ":RELEASE"));
        }
    }

    private PointsReservation findReservation(String orderNo) {
        return reservations.selectOne(new LambdaQueryWrapper<PointsReservation>()
                .eq(PointsReservation::getOrderNo, orderNo));
    }
}
