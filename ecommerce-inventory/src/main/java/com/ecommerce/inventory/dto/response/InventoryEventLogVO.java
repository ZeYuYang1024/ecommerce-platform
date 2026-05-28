package com.ecommerce.inventory.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InventoryEventLogVO {

    private Long id;
    private String topic;
    private String orderNo;
    private Integer status;
    private LocalDateTime createdAt;
}
