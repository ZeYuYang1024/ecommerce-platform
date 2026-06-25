package com.ecommerce.warehouse.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class InboundOrderVO {
    private Long id;
    private String inboundNo;
    private Long warehouseId;
    private Integer inboundType;
    private String inboundTypeText;
    private String sourceOrderNo;
    private Integer status;
    private String statusText;
    private Long merchantId;
    private String remark;
    private List<Item> items;
    private LocalDateTime createdAt;

    @Data
    public static class Item {
        private Long id;
        private Long skuId;
        private Integer quantity;
        private Long binId;
    }
}
