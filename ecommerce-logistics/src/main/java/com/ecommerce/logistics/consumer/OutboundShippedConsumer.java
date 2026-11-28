package com.ecommerce.logistics.consumer;

import com.ecommerce.common.dto.OutboundShippedMessage;
import com.ecommerce.common.dto.ShippingDispatchedMessage;
import com.ecommerce.common.outbox.OutboxService;
import com.ecommerce.logistics.common.ShippingStatus;
import com.ecommerce.logistics.entity.ShippingOrder;
import com.ecommerce.logistics.mapper.ShippingOrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RocketMQMessageListener(
    topic = "outbound-shipped",
    consumerGroup = "${spring.application.name}-consumer"
)
public class OutboundShippedConsumer implements RocketMQListener<OutboundShippedMessage> {

    private final ShippingOrderMapper shippingOrderMapper;
    private final OutboxService outboxService;

    public OutboundShippedConsumer(ShippingOrderMapper shippingOrderMapper, OutboxService outboxService) {
        this.shippingOrderMapper = shippingOrderMapper;
        this.outboxService = outboxService;
    }

    @Override
    @Transactional
    public void onMessage(OutboundShippedMessage message) {
        log.info("MQ received: outbound-shipped, shippingId={}, outboundId={}, warehouseId={}",
                message.getShippingId(), message.getOutboundId(), message.getWarehouseId());

        if (message.getShippingId() == null || message.getShippingId() == 0) {
            log.warn("OutboundShippedMessage has no shippingId, outboundId={}", message.getOutboundId());
            return;
        }

        ShippingOrder order = shippingOrderMapper.selectById(message.getShippingId());
        if (order == null) {
            throw new IllegalStateException("Shipped outbound " + message.getOutboundId()
                    + " needs reconciliation: shipping record " + message.getShippingId() + " is missing");
        }

        // Transition from PENDING to DISPATCHED
        if (order.getShippingStatus() != null && order.getShippingStatus() == ShippingStatus.PENDING) {
            order.setShippingStatus(ShippingStatus.DISPATCHED);
            order.setShippedAt(LocalDateTime.now());
            if (shippingOrderMapper.updateById(order) == 0) {
                throw new IllegalStateException("Shipment changed concurrently: " + order.getId());
            }
            log.info("Shipping order status updated to DISPATCHED: shippingId={}", message.getShippingId());
        } else {
            log.info("Shipping order already dispatched or in unexpected status: shippingId={}, status={}",
                    message.getShippingId(), order.getShippingStatus());
            return;
        }

        // Enqueue shipping-dispatched event via outbox
        ShippingDispatchedMessage dispatchedMsg = new ShippingDispatchedMessage();
        dispatchedMsg.setShippingId(order.getId());
        dispatchedMsg.setOrderId(order.getOrderId());
        dispatchedMsg.setOrderNo(order.getOrderNo());
        dispatchedMsg.setTrackingNo(order.getTrackingNo());
        dispatchedMsg.setShippingStatus(order.getShippingStatus());
        dispatchedMsg.setWholeOrder(Boolean.TRUE.equals(order.getWholeOrder()));
        dispatchedMsg.setMerchantId(order.getMerchantId());
        dispatchedMsg.setTransactionId(message.getTransactionId() != null
                ? message.getTransactionId() : "outbound-shipped-" + order.getId());
        dispatchedMsg.setIdempotencyKey("shipping-dispatched:" + order.getShippingNo());
        dispatchedMsg.setOccurredAt(LocalDateTime.now());
        outboxService.enqueue("shipping", order.getShippingNo(), "shipping-dispatched", dispatchedMsg);

        log.info("shipping-dispatched event enqueued for shippingId={}", order.getId());
    }
}
