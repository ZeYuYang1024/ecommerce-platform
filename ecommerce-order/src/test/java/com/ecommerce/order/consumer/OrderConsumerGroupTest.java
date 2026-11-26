package com.ecommerce.order.consumer;

import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderConsumerGroupTest {

    @Test
    void eachOrderStateTopicHasItsOwnConsumerGroup() {
        List<RocketMQMessageListener> listeners = List.of(
                OrderPaidConsumer.class.getAnnotation(RocketMQMessageListener.class),
                ShippingDispatchedConsumer.class.getAnnotation(RocketMQMessageListener.class),
                OrderDeliveredConsumer.class.getAnnotation(RocketMQMessageListener.class));

        assertThat(listeners).extracting(RocketMQMessageListener::topic)
                .containsExactly("order-paid", "shipping-dispatched", "order-delivered");
        assertThat(listeners).extracting(RocketMQMessageListener::consumerGroup)
                .doesNotHaveDuplicates();
    }
}
