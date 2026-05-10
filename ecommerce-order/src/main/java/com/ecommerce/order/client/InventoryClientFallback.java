package com.ecommerce.order.client;

import com.ecommerce.common.result.Result;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class InventoryClientFallback implements FallbackFactory<InventoryClient> {
    @Override
    public InventoryClient create(Throwable cause) {
        return new InventoryClient() {
            @Override
            public Result<Void> deduct(StockOperateRequest request) {
                return Result.ok();
            }
            @Override
            public Result<Void> release(StockOperateRequest request) {
                return Result.ok();
            }
        };
    }
}
