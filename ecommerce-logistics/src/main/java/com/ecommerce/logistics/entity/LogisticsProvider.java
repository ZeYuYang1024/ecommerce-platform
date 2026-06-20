package com.ecommerce.logistics.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ecommerce.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("logistics_provider")
public class LogisticsProvider extends BaseEntity {
    private String providerCode;
    private String providerName;
    private String providerLogo;
    private String customerAccount;
    private String apiKey;
    private String apiSecret;
    private String aggregationCode;
    private Integer supportWaybill;
    private Integer status;
    private Integer priority;
}
