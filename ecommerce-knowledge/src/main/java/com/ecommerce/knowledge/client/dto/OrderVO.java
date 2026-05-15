package com.ecommerce.knowledge.client.dto;

import lombok.Data;

@Data
public class OrderVO {
    private Long id;
    private String orderNo;
    private Long userId;
    private String status;
    private String statusDesc;
    private Long totalAmount;
    private String createTime;
}
