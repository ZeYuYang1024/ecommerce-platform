package com.ecommerce.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductCreatedMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long spuId;
    private Long skuId;
}
