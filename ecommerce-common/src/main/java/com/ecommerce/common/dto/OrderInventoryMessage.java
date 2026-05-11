package com.ecommerce.common.dto;

import java.io.Serializable;
import java.util.List;

public class OrderInventoryMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private String orderNo;
    private List<OrderItemMessage> items;

    public OrderInventoryMessage() {}

    public OrderInventoryMessage(String orderNo, List<OrderItemMessage> items) {
        this.orderNo = orderNo;
        this.items = items;
    }

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public List<OrderItemMessage> getItems() { return items; }
    public void setItems(List<OrderItemMessage> items) { this.items = items; }
}
