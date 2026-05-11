package com.ecommerce.common.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public class OrderPaidMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private String orderNo;
    private Integer status;
    private LocalDateTime paidAt;

    public OrderPaidMessage() {}

    public OrderPaidMessage(String orderNo, Integer status, LocalDateTime paidAt) {
        this.orderNo = orderNo;
        this.status = status;
        this.paidAt = paidAt;
    }

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
}
