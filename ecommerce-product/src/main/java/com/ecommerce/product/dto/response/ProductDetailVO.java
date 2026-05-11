package com.ecommerce.product.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class ProductDetailVO {
    private SpuVO spu;
    private List<SkuVO> skus;
}
