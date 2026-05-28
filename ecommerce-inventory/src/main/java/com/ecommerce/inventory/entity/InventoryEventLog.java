package com.ecommerce.inventory.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ecommerce.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inventory_event_log")
public class InventoryEventLog extends BaseEntity {

    private String topic;
    private String orderNo;
    private Integer status;
}
