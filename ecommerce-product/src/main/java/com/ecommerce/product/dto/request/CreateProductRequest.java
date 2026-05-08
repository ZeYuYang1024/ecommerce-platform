package com.ecommerce.product.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class CreateProductRequest {
    @Valid
    @NotNull
    private SpuRequest spu;

    @Valid
    private List<SkuRequest> skus;

    public SpuRequest getSpu() { return spu; }
    public void setSpu(SpuRequest spu) { this.spu = spu; }
    public List<SkuRequest> getSkus() { return skus; }
    public void setSkus(List<SkuRequest> skus) { this.skus = skus; }

    public static class SpuRequest {
        @NotBlank(message = "商品名称不能为空")
        private String name;
        private Long categoryId;
        private Long brandId;
        private String description;
        private String mainImage;
        private String images;
        private String detail;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Long getCategoryId() { return categoryId; }
        public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
        public Long getBrandId() { return brandId; }
        public void setBrandId(Long brandId) { this.brandId = brandId; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getMainImage() { return mainImage; }
        public void setMainImage(String mainImage) { this.mainImage = mainImage; }
        public String getImages() { return images; }
        public void setImages(String images) { this.images = images; }
        public String getDetail() { return detail; }
        public void setDetail(String detail) { this.detail = detail; }
    }

    public static class SkuRequest {
        private String name;
        private String spec;
        private String price;
        private String originalPrice;
        private String image;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getSpec() { return spec; }
        public void setSpec(String spec) { this.spec = spec; }
        public String getPrice() { return price; }
        public void setPrice(String price) { this.price = price; }
        public String getOriginalPrice() { return originalPrice; }
        public void setOriginalPrice(String originalPrice) { this.originalPrice = originalPrice; }
        public String getImage() { return image; }
        public void setImage(String image) { this.image = image; }
    }
}
