package com.ecommerce.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderInventoryMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private String orderNo;
    private String transactionId;
    private String idempotencyKey;
    private List<OrderItemMessage> items;

    public OrderInventoryMessage(String orderNo, List<OrderItemMessage> items) {
        this(orderNo, null, null, items);
    }
}
