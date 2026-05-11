package com.ecommerce.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderPaidMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private String orderNo;
    private Integer status;
    private LocalDateTime paidAt;
}
