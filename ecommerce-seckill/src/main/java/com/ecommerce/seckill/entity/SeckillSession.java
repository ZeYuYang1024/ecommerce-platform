package com.ecommerce.seckill.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ecommerce.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("seckill_session")
public class SeckillSession extends BaseEntity {
    private String name;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
}
