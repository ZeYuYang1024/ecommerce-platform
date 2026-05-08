package com.ecommerce.inventory.service;

import com.ecommerce.inventory.entity.Stock;
import java.util.List;

public interface StockService {
    Stock getBySkuId(Long skuId);
    List<Stock> batchQuery(List<Long> skuIds);
    void deduct(Long skuId, int quantity);
    void release(Long skuId, int quantity);
    void setStock(Long skuId, int totalStock);
}
