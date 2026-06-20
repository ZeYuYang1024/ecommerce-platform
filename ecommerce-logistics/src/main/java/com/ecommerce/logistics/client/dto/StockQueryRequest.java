package com.ecommerce.logistics.client.dto;

import lombok.Data;

import java.util.List;

@Data
public class StockQueryRequest {
    private Long warehouseId;
    private List<Long> skuIds;
}
