package com.ecommerce.product.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateProductRequest {
    @Valid
    @NotNull
    private SpuRequest spu;

    @Valid
    private List<SkuRequest> skus;

    @Data
    public static class SpuRequest {
        @NotBlank(message = "商品名称不能为空")
        private String name;
        private Long categoryId;
        private Long brandId;
        private String description;
        private String mainImage;
        private String images;
        private String detail;
    }

    @Data
    public static class SkuRequest {
        private String name;
        private String spec;
        private String price;
        private String originalPrice;
        private String image;
    }
}
