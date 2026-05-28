package com.ecommerce.monitor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReliabilityWarningVO {

    private String code;
    private String severity;
    private String message;
}
