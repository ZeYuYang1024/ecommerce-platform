package com.ecommerce.common.dto;

import java.math.BigDecimal;
import java.util.List;

public record RecommendationProduct(Long id, Long categoryId, Long brandId, String name, String description,
                                    String mainImage, BigDecimal minPrice, BigDecimal maxPrice, Integer reviewCount,
                                    List<Long> skuIds) {
    public record Query(List<Long> ids, List<Long> categoryIds, boolean includeLatest, int latestPage) {
        public Query(List<Long> ids, List<Long> categoryIds, boolean includeLatest) {
            this(ids, categoryIds, includeLatest, 1);
        }
    }

    public static final int LATEST_BATCH_SIZE = 48;
}
