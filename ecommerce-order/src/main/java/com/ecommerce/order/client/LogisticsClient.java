package com.ecommerce.order.client;

import com.ecommerce.common.result.Result;
import com.ecommerce.order.client.dto.FulfillmentSummaryVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "ecommerce-logistics")
public interface LogisticsClient {

    @PostMapping("/api/v1/internal/logistics/fulfillment-summary")
    Result<List<FulfillmentSummaryVO>> getFulfillmentSummary(@RequestBody List<Long> orderIds);
}
