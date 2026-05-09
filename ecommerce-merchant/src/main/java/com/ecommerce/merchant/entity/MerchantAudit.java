package com.ecommerce.merchant.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ecommerce.common.entity.BaseEntity;

@TableName("merchant_audit")
public class MerchantAudit extends BaseEntity {
    private Long merchantId;
    private Long auditorId;
    private Integer action;
    private String comment;

    public Long getMerchantId() { return merchantId; }
    public void setMerchantId(Long merchantId) { this.merchantId = merchantId; }
    public Long getAuditorId() { return auditorId; }
    public void setAuditorId(Long auditorId) { this.auditorId = auditorId; }
    public Integer getAction() { return action; }
    public void setAction(Integer action) { this.action = action; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
