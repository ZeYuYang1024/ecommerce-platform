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
    private List<OrderItemMessage> items;
}
