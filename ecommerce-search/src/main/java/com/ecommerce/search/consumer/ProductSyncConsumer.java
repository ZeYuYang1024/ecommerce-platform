package com.ecommerce.search.consumer;

import com.ecommerce.common.dto.ProductCreatedMessage;
import com.ecommerce.common.result.Result;
import com.ecommerce.search.client.ProductDetailVO;
import com.ecommerce.search.client.ProductClient;
import com.ecommerce.search.entity.ProductDocument;
import com.ecommerce.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
    topic = "product-created",
    consumerGroup = "${rocketmq.consumer.group}"
)
public class ProductSyncConsumer implements RocketMQListener<ProductCreatedMessage> {

    private final SearchService searchService;
    private final ProductClient productClient;

    @Override
    public void onMessage(ProductCreatedMessage msg) {
        log.info("Sync product to ES: spuId={}", msg.getSpuId());
        try {
            Result<ProductDetailVO> result = productClient.getProductDetail(msg.getSpuId());
            if (result.getCode() != 200 || result.getData() == null) {
                log.warn("Product detail not found: spuId={}", msg.getSpuId());
                return;
            }
            ProductDetailVO detail = result.getData();
            ProductDocument doc = new ProductDocument();
            doc.setId(String.valueOf(detail.getSpu().getId()));
            doc.setName(detail.getSpu().getName());
            doc.setCategoryId(detail.getSpu().getCategoryId());
            doc.setBrandId(detail.getSpu().getBrandId());
            doc.setMerchantId(detail.getSpu().getMerchantId());
            doc.setDescription(detail.getSpu().getDescription());
            doc.setMainImage(detail.getSpu().getMainImage());
            doc.setMinPrice(detail.getSpu().getMinPrice());
            doc.setMaxPrice(detail.getSpu().getMaxPrice());
            doc.setStatus(detail.getSpu().getStatus());
            doc.setAvgRating(detail.getSpu().getAvgRating());
            doc.setReviewCount(detail.getSpu().getReviewCount());
            doc.setCreatedAt(detail.getSpu().getCreatedAt());
            searchService.indexProduct(doc);
            log.info("Product synced to ES: spuId={}", msg.getSpuId());
        } catch (Exception e) {
            log.error("Failed to sync product: spuId={}", msg.getSpuId(), e);
            throw e;
        }
    }
}
