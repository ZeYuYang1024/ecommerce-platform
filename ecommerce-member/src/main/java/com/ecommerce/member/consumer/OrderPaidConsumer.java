package com.ecommerce.member.consumer;

import com.ecommerce.common.dto.OrderMemberVO;
import com.ecommerce.common.dto.OrderPaidMessage;
import com.ecommerce.common.result.Result;
import com.ecommerce.member.client.OrderClient;
import com.ecommerce.member.service.GrowthService;
import com.ecommerce.member.service.PointsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
    topic = "order-paid",
    consumerGroup = "${spring.application.name}-consumer"
)
public class OrderPaidConsumer implements RocketMQListener<OrderPaidMessage> {

    private final OrderClient orderClient;
    private final PointsService pointsService;
    private final GrowthService growthService;

    @Override
    public void onMessage(OrderPaidMessage message) {
        log.info("Member MQ received: order-paid, orderNo={}", message.getOrderNo());

        Result<OrderMemberVO> result = orderClient.getOrderForMember(message.getOrderNo());
        if (result == null || result.getData() == null) {
            log.warn("Order not found for member: orderNo={}", message.getOrderNo());
            return;
        }

        OrderMemberVO order = result.getData();
        Long userId = order.getUserId();
        String orderNo = message.getOrderNo();

        // 按实付金额（元）计算积分和成长值
        int amountInYuan = order.getTotalAmount().intValue();
        if (amountInYuan <= 0) {
            log.info("Order amount is zero, skip: orderNo={}", orderNo);
            return;
        }

        // 发放积分 (倍率在 PointsService 内根据等级计算)
        pointsService.earn(userId, amountInYuan, "ORDER", orderNo,
                "ORDER:" + orderNo + ":EARN", "订单支付获得积分");

        // 发放成长值
        growthService.add(userId, amountInYuan, "ORDER", orderNo,
                "ORDER:" + orderNo + ":GROWTH", "订单支付获得成长值");
    }
}
