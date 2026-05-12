package com.ecommerce.notification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ecommerce.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notification_template")
public class NotificationTemplate extends BaseEntity {
    private String templateCode;
    private String name;
    private String type;
    private String title;
    private String content;
    private Integer status;
}
