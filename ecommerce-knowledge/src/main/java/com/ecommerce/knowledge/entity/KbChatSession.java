package com.ecommerce.knowledge.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("kb_chat_session")
public class KbChatSession {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private String sessionId;

    private String title;

    private Integer messageCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
