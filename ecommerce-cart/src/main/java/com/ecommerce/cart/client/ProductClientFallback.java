package com.ecommerce.cart.client;

import com.ecommerce.common.result.Result;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class ProductClientFallback implements FallbackFactory<ProductClient> {
    @Override
    public ProductClient create(Throwable cause) {
        return ids -> Result.fail(503, "暂时无法读取商品信息");
    }
}
