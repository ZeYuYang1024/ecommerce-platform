package com.ecommerce.inventory.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.inventory.dto.response.InventoryEventLogVO;
import com.ecommerce.inventory.dto.response.InventoryEventSummaryVO;

public interface InventoryEventAdminService {

    Page<InventoryEventLogVO> listEvents(String topic, String orderNo, Integer status, int page, int size);

    InventoryEventSummaryVO summarize(String topic, String orderNo, Integer status);
}
