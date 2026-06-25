package com.ecommerce.logistics.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ecommerce.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tracking_record")
public class TrackingRecord extends BaseEntity {
    private Long shippingId;
    private String providerCode;
    private String trackingNo;
    private String traceHash;
    private LocalDateTime traceTime;
    private String traceDesc;
    private String traceStatus;
    private String eventType;
    private String location;
    private String rawData;
}
