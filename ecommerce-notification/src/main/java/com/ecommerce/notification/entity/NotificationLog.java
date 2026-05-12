package com.ecommerce.notification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ecommerce.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notification_log")
public class NotificationLog extends BaseEntity {
    private Long userId;
    private Long templateId;
    private String type;
    private String recipient;
    private String title;
    private String content;
    private Integer status;
    private String errorMsg;
    private LocalDateTime sentAt;
}
