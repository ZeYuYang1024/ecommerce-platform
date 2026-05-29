package com.ecommerce.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.inventory.dto.response.InventoryEventLogVO;
import com.ecommerce.inventory.dto.response.InventoryEventSummaryVO;
import com.ecommerce.inventory.entity.InventoryEventLog;
import com.ecommerce.inventory.mapper.InventoryEventLogMapper;
import com.ecommerce.inventory.service.InventoryEventAdminService;
import org.springframework.stereotype.Service;

@Service
public class InventoryEventAdminServiceImpl implements InventoryEventAdminService {

    private static final int STATUS_PROCESSING = 0;
    private static final int STATUS_PROCESSED = 1;
    private static final int STATUS_FAILED = 2;

    private final InventoryEventLogMapper inventoryEventLogMapper;

    public InventoryEventAdminServiceImpl(InventoryEventLogMapper inventoryEventLogMapper) {
        this.inventoryEventLogMapper = inventoryEventLogMapper;
    }

    @Override
    public Page<InventoryEventLogVO> listEvents(String topic, String orderNo, Integer status, int page, int size) {
        Page<InventoryEventLog> result = inventoryEventLogMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<InventoryEventLog>()
                        .eq(topic != null && !topic.isBlank(), InventoryEventLog::getTopic, topic)
                        .eq(orderNo != null && !orderNo.isBlank(), InventoryEventLog::getOrderNo, orderNo)
                        .eq(status != null, InventoryEventLog::getStatus, status)
                        .orderByDesc(InventoryEventLog::getCreatedAt));
        Page<InventoryEventLogVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    @Override
    public InventoryEventSummaryVO summarize(String topic, String orderNo, Integer status) {
        var logs = inventoryEventLogMapper.selectList(new LambdaQueryWrapper<InventoryEventLog>()
                .eq(topic != null && !topic.isBlank(), InventoryEventLog::getTopic, topic)
                .eq(orderNo != null && !orderNo.isBlank(), InventoryEventLog::getOrderNo, orderNo)
                .orderByDesc(InventoryEventLog::getCreatedAt));
        int processingCount = (int) logs.stream().filter(log -> log.getStatus() != null && log.getStatus() == STATUS_PROCESSING).count();
        int processedCount = (int) logs.stream().filter(log -> log.getStatus() != null && log.getStatus() == STATUS_PROCESSED).count();
        int failedCount = (int) logs.stream().filter(log -> log.getStatus() != null && log.getStatus() == STATUS_FAILED).count();
        if (status != null) {
            if (status == STATUS_PROCESSING) {
                return new InventoryEventSummaryVO(processingCount, 0, 0);
            }
            if (status == STATUS_PROCESSED) {
                return new InventoryEventSummaryVO(0, processedCount, 0);
            }
            if (status == STATUS_FAILED) {
                return new InventoryEventSummaryVO(0, 0, failedCount);
            }
        }
        return new InventoryEventSummaryVO(processingCount, processedCount, failedCount);
    }

    private InventoryEventLogVO toVO(InventoryEventLog log) {
        InventoryEventLogVO vo = new InventoryEventLogVO();
        vo.setId(log.getId());
        vo.setTopic(log.getTopic());
        vo.setOrderNo(log.getOrderNo());
        vo.setStatus(log.getStatus());
        vo.setCreatedAt(log.getCreatedAt());
        return vo;
    }
}
