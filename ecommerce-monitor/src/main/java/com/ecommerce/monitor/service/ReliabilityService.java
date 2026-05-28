package com.ecommerce.monitor.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.monitor.dto.response.InventoryEventLogVO;
import com.ecommerce.monitor.dto.response.InventoryEventSummaryVO;
import com.ecommerce.monitor.dto.response.OutboxMessageVO;
import com.ecommerce.monitor.dto.response.ReliabilityOverviewVO;

public interface ReliabilityService {

    ReliabilityOverviewVO getOverview();

    Page<OutboxMessageVO> listOutbox(String service, Integer status, String topic, String aggregateId, int page, int size);

    int retryOutboxMessage(String service, Long messageId);

    int retryOutboxBatch(String service, Integer status, String topic, String aggregateId, int limit);

    Page<InventoryEventLogVO> listInventoryEvents(String topic, String orderNo, Integer status, int page, int size);

    InventoryEventSummaryVO getInventorySummary(String topic, String orderNo, Integer status);
}
