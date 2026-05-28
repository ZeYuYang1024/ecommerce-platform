package com.ecommerce.monitor.client;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.Result;
import com.ecommerce.monitor.dto.response.InventoryEventLogVO;
import com.ecommerce.monitor.dto.response.InventoryEventSummaryVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "ecommerce-inventory")
public interface InventoryReliabilityClient {

    @GetMapping("/api/v1/admin/inventory/events")
    Result<Page<InventoryEventLogVO>> listEvents(@RequestParam(required = false) String topic,
                                                 @RequestParam(required = false) String orderNo,
                                                 @RequestParam(required = false) Integer status,
                                                 @RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int size);

    @GetMapping("/api/v1/admin/inventory/events/summary")
    Result<InventoryEventSummaryVO> getEventSummary(@RequestParam(required = false) String topic,
                                                    @RequestParam(required = false) String orderNo,
                                                    @RequestParam(required = false) Integer status);
}
