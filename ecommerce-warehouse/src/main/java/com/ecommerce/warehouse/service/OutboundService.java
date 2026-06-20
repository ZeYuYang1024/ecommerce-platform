package com.ecommerce.warehouse.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ecommerce.warehouse.dto.request.CreateOutboundRequest;
import com.ecommerce.warehouse.dto.response.OutboundOrderVO;

public interface OutboundService {

    IPage<OutboundOrderVO> listOutbounds(int page, int size, Long warehouseId, Long merchantId);

    OutboundOrderVO getOutbound(Long id);

    OutboundOrderVO createOutbound(CreateOutboundRequest req);

    void startPicking(Long id);

    void confirmShipped(Long id);

    void confirmDelivered(Long id);
}
