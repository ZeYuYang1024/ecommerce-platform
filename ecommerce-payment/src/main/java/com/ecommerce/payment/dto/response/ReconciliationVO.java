package com.ecommerce.payment.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;

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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
    public Integer getTotalOrderCount() { return totalOrderCount; }
    public void setTotalOrderCount(Integer totalOrderCount) { this.totalOrderCount = totalOrderCount; }
    public Integer getTotalPaymentCount() { return totalPaymentCount; }
    public void setTotalPaymentCount(Integer totalPaymentCount) { this.totalPaymentCount = totalPaymentCount; }
    public Integer getMatchedCount() { return matchedCount; }
    public void setMatchedCount(Integer matchedCount) { this.matchedCount = matchedCount; }
    public Integer getUnmatchedCount() { return unmatchedCount; }
    public void setUnmatchedCount(Integer unmatchedCount) { this.unmatchedCount = unmatchedCount; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getStatusText() { return statusText; }
    public void setStatusText(String statusText) { this.statusText = statusText; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public List<DetailVO> getDetails() { return details; }
    public void setDetails(List<DetailVO> details) { this.details = details; }

    public static class DetailVO {
        private Long id;
        private String recordType;
        private String orderNo;
        private String paymentNo;
        private java.math.BigDecimal amount;
        private Integer recordStatus;
        private String matchStatus;
        private String diffReason;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getRecordType() { return recordType; }
        public void setRecordType(String recordType) { this.recordType = recordType; }
        public String getOrderNo() { return orderNo; }
        public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
        public String getPaymentNo() { return paymentNo; }
        public void setPaymentNo(String paymentNo) { this.paymentNo = paymentNo; }
        public java.math.BigDecimal getAmount() { return amount; }
        public void setAmount(java.math.BigDecimal amount) { this.amount = amount; }
        public Integer getRecordStatus() { return recordStatus; }
        public void setRecordStatus(Integer recordStatus) { this.recordStatus = recordStatus; }
        public String getMatchStatus() { return matchStatus; }
        public void setMatchStatus(String matchStatus) { this.matchStatus = matchStatus; }
        public String getDiffReason() { return diffReason; }
        public void setDiffReason(String diffReason) { this.diffReason = diffReason; }
    }
}
