package com.ecommerce.inventory.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.inventory.dto.response.StockVO;
import com.ecommerce.inventory.entity.Stock;
import java.util.List;

public interface StockService {
    Stock getBySkuId(Long skuId);
    List<Stock> batchQuery(List<Long> skuIds);
    Page<StockVO> list(Long skuId, Integer stockStatus, int page, int size);
    void deduct(Long skuId, int quantity);
    void release(Long skuId, int quantity);
    void setStock(Long skuId, int totalStock);
    StockVO toVO(Stock stock);
}
