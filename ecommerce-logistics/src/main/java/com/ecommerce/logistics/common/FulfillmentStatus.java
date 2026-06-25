package com.ecommerce.logistics.common;

public final class FulfillmentStatus {
    private FulfillmentStatus() {}
    public static final String WAITING_SHIP = "WAITING_SHIP";
    public static final String PARTIALLY_DISPATCHED = "PARTIALLY_DISPATCHED";
    public static final String DISPATCHED = "DISPATCHED";
    public static final String DELIVERED = "DELIVERED";
    public static final String EXCEPTION = "EXCEPTION";

    public static String text(String status) {
        return switch (status) {
            case WAITING_SHIP -> "待发货";
            case PARTIALLY_DISPATCHED -> "部分发货";
            case DISPATCHED -> "已发货";
            case DELIVERED -> "已签收";
            case EXCEPTION -> "异常";
            default -> "未知";
        };
    }
}
