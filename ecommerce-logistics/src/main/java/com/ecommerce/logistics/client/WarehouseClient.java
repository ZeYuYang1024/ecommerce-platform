package com.ecommerce.logistics.client;

import com.ecommerce.common.result.Result;
import com.ecommerce.logistics.client.dto.CreateOutboundRequest;
import com.ecommerce.logistics.client.dto.OutboundOrderVO;
import com.ecommerce.logistics.client.dto.PhysicalStockVO;
import com.ecommerce.logistics.client.dto.StockQueryRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "ecommerce-warehouse", path = "/api/v1/internal/warehouse")
public interface WarehouseClient {

    @PostMapping("/outbounds")
    Result<OutboundOrderVO> createOutbound(@RequestBody CreateOutboundRequest request);

    @PostMapping("/stock/query")
    Result<List<PhysicalStockVO>> queryStock(@RequestBody StockQueryRequest request);
}
