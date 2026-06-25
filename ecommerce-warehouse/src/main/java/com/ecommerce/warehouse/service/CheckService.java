package com.ecommerce.warehouse.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ecommerce.warehouse.dto.request.CreateCheckRequest;
import com.ecommerce.warehouse.dto.response.StockCheckVO;

public interface CheckService {

    IPage<StockCheckVO> listChecks(int page, int size, Long warehouseId, Long merchantId);

    StockCheckVO getCheck(Long id);

    StockCheckVO createCheck(CreateCheckRequest req);

    void recordCheckItem(Long checkId, Long itemId, int actualQty);

    void completeCheck(Long id);
}
