package com.ecommerce.auth.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class AdminUserVO {
    private Long id;
    private String username;
    private String type;
    private Integer status;
    private List<Long> roleIds;
}
