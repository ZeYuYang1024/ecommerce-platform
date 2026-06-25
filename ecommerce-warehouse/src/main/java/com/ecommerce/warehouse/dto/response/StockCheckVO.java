package com.ecommerce.warehouse.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class StockCheckVO {
    private Long id;
    private String checkNo;
    private Long warehouseId;
    private Integer status;
    private String statusText;
    private Long merchantId;
    private List<Item> items;
    private LocalDateTime createdAt;

    @Data
    public static class Item {
        private Long id;
        private Long skuId;
        private Integer systemQty;
        private Integer actualQty;
        private Integer diffQty;
        private String remark;
    }
}
