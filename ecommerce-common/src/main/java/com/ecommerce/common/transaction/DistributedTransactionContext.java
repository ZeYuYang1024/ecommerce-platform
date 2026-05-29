package com.ecommerce.common.transaction;

import java.time.LocalDateTime;

public class DistributedTransactionContext {

    private static final int MAX_ERROR_LENGTH = 512;

    private final String transactionId;
    private final String businessNo;
    private final String idempotencyKey;
    private final LocalDateTime createdAt;

    private DistributedTransactionStatus status;
    private String currentStep;
    private String errorMessage;
    private LocalDateTime updatedAt;

    private DistributedTransactionContext(String transactionId, String businessNo, String idempotencyKey) {
        this.transactionId = transactionId;
        this.businessNo = businessNo;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = createdAt;
        this.status = DistributedTransactionStatus.INIT;
    }

    public static DistributedTransactionContext start(String transactionId, String businessNo, String idempotencyKey) {
        return new DistributedTransactionContext(transactionId, businessNo, idempotencyKey);
    }

    public void apply(DistributedTransactionEvent event, String step, String errorMessage) {
        if (event == null || status.isTerminal()) {
            return;
        }
        switch (event) {
            case BEGIN -> begin(step);
            case COMPLETE -> complete(step);
            case FAIL -> fail(step, errorMessage);
            case COMPENSATE -> compensate(step, errorMessage);
        }
    }

    private void begin(String step) {
        if (status != DistributedTransactionStatus.INIT) {
            return;
        }
        this.status = DistributedTransactionStatus.PROCESSING;
        this.currentStep = step;
        this.errorMessage = null;
        touch();
    }

    private void complete(String step) {
        if (status != DistributedTransactionStatus.PROCESSING) {
            return;
        }
        this.status = DistributedTransactionStatus.SUCCESS;
        this.currentStep = step;
        this.errorMessage = null;
        touch();
    }

    private void fail(String step, String errorMessage) {
        if (status != DistributedTransactionStatus.PROCESSING) {
            return;
        }
        this.status = DistributedTransactionStatus.FAILED;
        this.currentStep = step;
        this.errorMessage = trim(errorMessage);
        touch();
    }

    private void compensate(String step, String errorMessage) {
        if (status != DistributedTransactionStatus.PROCESSING) {
            return;
        }
        this.status = DistributedTransactionStatus.COMPENSATED;
        this.currentStep = step;
        this.errorMessage = trim(errorMessage);
        touch();
    }

    private String trim(String value) {
        if (value == null || value.length() <= MAX_ERROR_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_ERROR_LENGTH);
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getBusinessNo() {
        return businessNo;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public DistributedTransactionStatus getStatus() {
        return status;
    }

    public String getCurrentStep() {
        return currentStep;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
