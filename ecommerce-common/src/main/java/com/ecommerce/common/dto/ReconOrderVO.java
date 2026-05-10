package com.ecommerce.common.dto;

import java.math.BigDecimal;

public class ReconOrderVO {
    private String orderNo;
    private BigDecimal amount;
    private Integer status;

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
