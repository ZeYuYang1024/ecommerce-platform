package com.ecommerce.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.inventory.dto.response.InventoryEventLogVO;
import com.ecommerce.inventory.dto.response.InventoryEventSummaryVO;
import com.ecommerce.inventory.entity.InventoryEventLog;
import com.ecommerce.inventory.mapper.InventoryEventLogMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryEventAdminServiceImplTest {

    @BeforeAll
    static void initMybatisMetadata() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new Configuration(), "inventory-event-log-test");
        assistant.setCurrentNamespace("inventory-event-log-test");
        TableInfoHelper.initTableInfo(assistant, InventoryEventLog.class);
    }

    @Mock
    private InventoryEventLogMapper inventoryEventLogMapper;

    @Test
    void listEventsShouldFilterByTopicOrderNoAndStatus() {
        LambdaQueryWrapper<?>[] wrapperHolder = new LambdaQueryWrapper<?>[1];
        when(inventoryEventLogMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenAnswer(invocation -> {
            wrapperHolder[0] = invocation.getArgument(1);
            return pageWith(log(3001L, "order-created", "ORD-1", 1));
        });

        InventoryEventAdminServiceImpl service = new InventoryEventAdminServiceImpl(inventoryEventLogMapper);
        Page<InventoryEventLogVO> result = service.listEvents("order-created", "ORD-1", 1, 1, 10);

        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().getFirst().getOrderNo()).isEqualTo("ORD-1");
        assertThat(wrapperHolder[0].getSqlSegment()).contains("topic", "orderNo", "status");
        assertThat(wrapperHolder[0].getParamNameValuePairs().values()).contains("order-created", "ORD-1", 1);
    }

    @Test
    void summarizeShouldCountProcessingAndProcessedEvents() {
        LambdaQueryWrapper<?>[] wrapperHolder = new LambdaQueryWrapper<?>[1];
        when(inventoryEventLogMapper.selectList(any(LambdaQueryWrapper.class))).thenAnswer(invocation -> {
            wrapperHolder[0] = invocation.getArgument(0);
            return List.of(
                    log(3001L, "order-created", "ORD-1", 0),
                    log(3002L, "order-created", "ORD-2", 1),
                    log(3003L, "order-cancelled", "ORD-3", 1));
        });

        InventoryEventAdminServiceImpl service = new InventoryEventAdminServiceImpl(inventoryEventLogMapper);
        InventoryEventSummaryVO result = service.summarize("order-created", null, null);

        assertThat(result.getProcessingCount()).isEqualTo(1);
        assertThat(result.getProcessedCount()).isEqualTo(2);
        assertThat(wrapperHolder[0].getSqlSegment()).contains("topic").doesNotContain("status");
        assertThat(wrapperHolder[0].getParamNameValuePairs().values()).contains("order-created");
    }

    private Page<InventoryEventLog> pageWith(InventoryEventLog log) {
        Page<InventoryEventLog> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(log));
        return page;
    }

    private InventoryEventLog log(Long id, String topic, String orderNo, int status) {
        InventoryEventLog log = new InventoryEventLog();
        log.setId(id);
        log.setTopic(topic);
        log.setOrderNo(orderNo);
        log.setStatus(status);
        log.setCreatedAt(LocalDateTime.of(2026, 5, 28, 15, 0));
        return log;
    }
}
