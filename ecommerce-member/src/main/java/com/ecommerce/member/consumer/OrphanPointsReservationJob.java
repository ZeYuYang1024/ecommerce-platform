package com.ecommerce.member.consumer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.member.client.OrderClient;
import com.ecommerce.member.entity.PointsReservation;
import com.ecommerce.member.mapper.PointsReservationMapper;
import com.ecommerce.member.service.MemberOrderPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrphanPointsReservationJob {
    private final PointsReservationMapper reservations;
    private final OrderClient orders;
    private final MemberOrderPaymentService payments;

    @Scheduled(fixedDelayString = "${member.orphan-reservation-scan-ms:60000}")
    public void releaseOrphanReservations() {
        long cursor = 0;
        LocalDateTime now = LocalDateTime.now();
        while (true) {
            var page = reservations.selectList(new LambdaQueryWrapper<PointsReservation>()
                    .gt(PointsReservation::getId, cursor).eq(PointsReservation::getStatus, "RESERVED")
                    .le(PointsReservation::getExpiredAt, now).orderByAsc(PointsReservation::getId).last("LIMIT 100"));
            if (page.isEmpty()) return;
            for (PointsReservation reservation : page) {
                try {
                    var order = orders.getOrderForMember(reservation.getOrderNo());
                    if (order != null && order.getCode() == 404) payments.cancel(reservation.getOrderNo());
                } catch (RuntimeException failure) {
                    log.warn("Could not confirm order for points reservation {}", reservation.getReservationNo(), failure);
                }
            }
            cursor = page.getLast().getId();
        }
    }
}
