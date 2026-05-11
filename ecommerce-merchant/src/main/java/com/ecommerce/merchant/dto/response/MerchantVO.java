package com.ecommerce.merchant.dto.response;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class MerchantVO {
    private Long id;
    private String name;
    private String logo;
    private String contactName;
    private String contactPhone;
    private String businessLicense;
    private Integer status;
    private String statusText;
    private String reason;
    private LocalDateTime createdAt;
}
