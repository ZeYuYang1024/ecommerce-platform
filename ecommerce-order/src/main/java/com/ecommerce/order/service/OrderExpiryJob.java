package com.ecommerce.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderExpiryJob {
    private final OrderService orders;

    @Scheduled(fixedDelayString = "${order.expiry-scan-ms:30000}")
    public void closeExpiredOrders() {
        for (Long orderId : orders.expiredPendingOrders(200)) {
            try {
                orders.expirePendingOrder(orderId);
            } catch (RuntimeException failure) {
                log.warn("Could not close expired order {}", orderId, failure);
            }
        }
    }
}
