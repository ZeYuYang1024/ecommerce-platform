package com.ecommerce.warehouse.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ecommerce.warehouse.dto.request.CreateInboundRequest;
import com.ecommerce.warehouse.dto.response.InboundOrderVO;

public interface InboundService {

    IPage<InboundOrderVO> listInbounds(int page, int size, Long warehouseId, Long merchantId);

    InboundOrderVO getInbound(Long id);

    InboundOrderVO createInbound(CreateInboundRequest req);

    void confirmReceived(Long id);

    void confirmShelved(Long id);

    void completeInbound(Long id);
}
