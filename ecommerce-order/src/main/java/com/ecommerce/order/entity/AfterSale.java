package com.ecommerce.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("order_after_sale")
public class AfterSale {
    public enum Status { PENDING, REJECTED, REFUNDING, REFUNDED, FAILED }

    @TableId(type = IdType.INPUT)
    private Long id;
    private Long orderId;
    private String orderNo;
    private Long userId;
    private Long merchantId;
    private BigDecimal amount;
    private String reason;
    private Status status;
    private String reviewNote;
    private String refundNo;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String failureReason;
    private int refundAttempt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
