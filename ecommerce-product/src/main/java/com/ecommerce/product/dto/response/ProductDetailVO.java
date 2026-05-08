package com.ecommerce.product.dto.response;

import java.util.List;

public class ProductDetailVO {
    private SpuVO spu;
    private List<SkuVO> skus;

    public SpuVO getSpu() { return spu; }
    public void setSpu(SpuVO spu) { this.spu = spu; }
    public List<SkuVO> getSkus() { return skus; }
    public void setSkus(List<SkuVO> skus) { this.skus = skus; }
}
