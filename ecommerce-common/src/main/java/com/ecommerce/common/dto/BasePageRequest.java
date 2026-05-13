package com.ecommerce.common.dto;

import lombok.Data;

@Data
public class BasePageRequest {
    private int page = 1;
    private int size = 10;
}
