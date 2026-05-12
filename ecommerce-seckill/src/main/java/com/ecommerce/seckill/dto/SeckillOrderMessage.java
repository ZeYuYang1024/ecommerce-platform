package com.ecommerce.seckill.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeckillOrderMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long itemId;
    private Long userId;
    private Long skuId;
    private BigDecimal seckillPrice;
}
