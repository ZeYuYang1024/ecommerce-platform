package com.ecommerce.warehouse.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OutboundOrderVO {
    private Long id;
    private String outboundNo;
    private Long warehouseId;
    private Integer outboundType;
    private String outboundTypeText;
    private Long shippingId;
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
