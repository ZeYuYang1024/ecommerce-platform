package com.ecommerce.member.consumer;

import com.ecommerce.member.service.PointsService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
    topic = "review-created",
    consumerGroup = "${spring.application.name}-consumer"
)
public class ReviewCreatedConsumer implements RocketMQListener<ReviewCreatedConsumer.ReviewCreatedMessage> {

    private final PointsService pointsService;

    @Override
    public void onMessage(ReviewCreatedMessage message) {
        log.info("Member MQ received: review-created, reviewId={}", message.getReviewId());

        int points;
        if (message.getHasImages() != null && message.getHasImages()) {
            points = 10; // 带图评价
        } else {
            points = 5;  // 纯文字评价
        }

        pointsService.earn(
                message.getUserId(),
                points,
                "REVIEW",
                message.getReviewId().toString(),
                "REVIEW:" + message.getReviewId() + ":EARN",
                "商品评价获得积分"
        );
    }

    @Data
    public static class ReviewCreatedMessage implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long reviewId;
        private Long userId;
        private Long orderId;
        private Boolean hasImages;
    }
}
