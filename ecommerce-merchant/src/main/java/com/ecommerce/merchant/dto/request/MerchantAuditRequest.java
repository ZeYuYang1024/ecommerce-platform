package com.ecommerce.merchant.dto.request;

import jakarta.validation.constraints.NotNull;

public class MerchantAuditRequest {
    @NotNull(message = "审核动作不能为空")
    private Integer action;
    private String comment;

    public Integer getAction() { return action; }
    public void setAction(Integer action) { this.action = action; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
