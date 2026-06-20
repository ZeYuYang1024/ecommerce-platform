package com.ecommerce.logistics.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.ecommerce.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("shipping_order")
public class ShippingOrder extends BaseEntity {
    private String shippingNo;
    private String clientRequestId;
    private Long orderId;
    private String orderNo;
    private Long warehouseId;
    private Long providerId;
    private String providerCode;
    private String trackingNo;
    private Integer dispatchType;
    private Integer sourceType;
    private BigDecimal shippingFee;
    private Integer shippingStatus;
    private String senderInfo;
    private String receiverInfo;
    private Integer packageWeight;
    private String packageSize;
    private String waybillUrl;
    private LocalDateTime lastTraceTime;
    private String lastTraceDesc;
    private LocalDateTime shippedAt;
    private LocalDateTime signedAt;
    private Long merchantId;
    @Version
    private Integer version;
}
