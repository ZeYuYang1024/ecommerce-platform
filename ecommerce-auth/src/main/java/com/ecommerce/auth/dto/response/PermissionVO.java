package com.ecommerce.auth.dto.response;

import lombok.Data;

@Data
public class PermissionVO {
    private Long id;
    private String name;
    private String code;
    private String type;
    private String path;
    private Long parentId;
    private Integer sort;
}
