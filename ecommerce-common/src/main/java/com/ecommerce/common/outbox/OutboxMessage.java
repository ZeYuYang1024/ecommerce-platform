package com.ecommerce.common.outbox;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("message_outbox")
public class OutboxMessage {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String aggregateType;
    private String aggregateId;
    private String topic;
    private String payloadJson;
    private Integer status;
    private Integer retryCount;
    private Integer maxRetryCount;
    private LocalDateTime nextRetryAt;
    private String lastError;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
