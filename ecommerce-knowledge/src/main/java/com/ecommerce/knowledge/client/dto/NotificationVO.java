package com.ecommerce.knowledge.client.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationVO {
    private Long id;
    private String type;
    private String title;
    private String content;
    private Integer status;
    private String errorMsg;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
}
