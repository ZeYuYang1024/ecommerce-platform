package com.ecommerce.search.client;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProductDetailVO {
    private SpuVO spu;
    private List<SkuVO> skus;

    @Data
    public static class SpuVO {
        private Long id;
        private String name;
        private Long categoryId;
        private Long brandId;
        private Long merchantId;
        private String description;
        private String mainImage;
        private Integer status;
        private BigDecimal avgRating;
        private Integer reviewCount;
        private BigDecimal minPrice;
        private BigDecimal maxPrice;
        private LocalDateTime createdAt;
    }

    @Data
    public static class SkuVO {
        private Long id;
        private BigDecimal price;
    }
}
