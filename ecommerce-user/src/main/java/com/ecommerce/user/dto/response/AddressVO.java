package com.ecommerce.user.dto.response;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AddressVO {
    private Long id;
    private Long userId;
    private String receiverName;
    private String receiverPhone;
    private String province;
    private String city;
    private String district;
    private String detail;
    private Integer isDefault;
    private LocalDateTime createdAt;
}
