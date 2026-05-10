package com.ecommerce.payment.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public class SettlementVO {
    private Long id;
    private LocalDate settlementDate;
    private Integer totalOrderCount;
    private BigDecimal totalOrderAmount;
    private Integer totalPaymentCount;
    private BigDecimal totalPaymentAmount;
    private Integer totalRefundCount;
    private BigDecimal totalRefundAmount;
    private BigDecimal netAmount;
    private Integer status;
    private String statusText;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getSettlementDate() { return settlementDate; }
    public void setSettlementDate(LocalDate settlementDate) { this.settlementDate = settlementDate; }
    public Integer getTotalOrderCount() { return totalOrderCount; }
    public void setTotalOrderCount(Integer totalOrderCount) { this.totalOrderCount = totalOrderCount; }
    public BigDecimal getTotalOrderAmount() { return totalOrderAmount; }
    public void setTotalOrderAmount(BigDecimal totalOrderAmount) { this.totalOrderAmount = totalOrderAmount; }
    public Integer getTotalPaymentCount() { return totalPaymentCount; }
    public void setTotalPaymentCount(Integer totalPaymentCount) { this.totalPaymentCount = totalPaymentCount; }
    public BigDecimal getTotalPaymentAmount() { return totalPaymentAmount; }
    public void setTotalPaymentAmount(BigDecimal totalPaymentAmount) { this.totalPaymentAmount = totalPaymentAmount; }
    public Integer getTotalRefundCount() { return totalRefundCount; }
    public void setTotalRefundCount(Integer totalRefundCount) { this.totalRefundCount = totalRefundCount; }
    public BigDecimal getTotalRefundAmount() { return totalRefundAmount; }
    public void setTotalRefundAmount(BigDecimal totalRefundAmount) { this.totalRefundAmount = totalRefundAmount; }
    public BigDecimal getNetAmount() { return netAmount; }
    public void setNetAmount(BigDecimal netAmount) { this.netAmount = netAmount; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getStatusText() { return statusText; }
    public void setStatusText(String statusText) { this.statusText = statusText; }
}
