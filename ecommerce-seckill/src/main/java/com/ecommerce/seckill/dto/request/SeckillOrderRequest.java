package com.ecommerce.seckill.dto.request;

import lombok.Data;

@Data
public class SeckillOrderRequest {
    private Long itemId;
    private Long userId;
}
