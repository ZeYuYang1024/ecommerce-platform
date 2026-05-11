package com.ecommerce.payment.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReconciliationVO {
    private Long id;
    private String batchNo;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer totalOrderCount;
    private Integer totalPaymentCount;
    private Integer matchedCount;
    private Integer unmatchedCount;
    private Integer status;
    private String statusText;
    private LocalDateTime createdAt;
    private List<DetailVO> details;

    @Data
    public static class DetailVO {
        private Long id;
        private String recordType;
        private String orderNo;
        private String paymentNo;
        private BigDecimal amount;
        private Integer recordStatus;
        private String matchStatus;
        private String diffReason;
    }
}
