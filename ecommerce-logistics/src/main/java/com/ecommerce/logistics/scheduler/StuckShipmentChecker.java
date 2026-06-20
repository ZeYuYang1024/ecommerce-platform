package com.ecommerce.logistics.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.common.dto.ShippingExceptionMessage;
import com.ecommerce.common.outbox.OutboxService;
import com.ecommerce.logistics.common.ShippingStatus;
import com.ecommerce.logistics.entity.ShippingOrder;
import com.ecommerce.logistics.mapper.ShippingOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StuckShipmentChecker {

    private final ShippingOrderMapper shippingOrderMapper;
    private final OutboxService outboxService;

    @Scheduled(cron = "0 0 */2 * * ?")
    public void checkStuckShipments() {
        log.info("Stuck shipment check started");
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        List<ShippingOrder> stuckOrders = shippingOrderMapper.selectList(
                new LambdaQueryWrapper<ShippingOrder>()
                        .eq(ShippingOrder::getShippingStatus, ShippingStatus.DISPATCHED)
                        .le(ShippingOrder::getShippedAt, sevenDaysAgo));

        for (ShippingOrder order : stuckOrders) {
            order.setShippingStatus(ShippingStatus.EXCEPTION);
            shippingOrderMapper.updateById(order);

            ShippingExceptionMessage msg = new ShippingExceptionMessage();
            msg.setShippingId(order.getId());
            msg.setOrderId(order.getOrderId());
            msg.setOrderNo(order.getOrderNo());
            msg.setTrackingNo(order.getTrackingNo());
            msg.setExceptionDesc("物流超时未签收，已超过7天");
            msg.setTransactionId("stuck-check-" + order.getShippingNo());
            msg.setIdempotencyKey("shipping-exception:" + order.getShippingNo());
            msg.setOccurredAt(LocalDateTime.now());

            outboxService.enqueue("shipping", order.getShippingNo(), "shipping-exception", msg);
            log.warn("Stuck shipment flagged: shippingId={}, orderNo={}, trackingNo={}, shippedAt={}",
                    order.getId(), order.getOrderNo(), order.getTrackingNo(), order.getShippedAt());
        }

        log.info("Stuck shipment check completed: found={}", stuckOrders.size());
    }
}
