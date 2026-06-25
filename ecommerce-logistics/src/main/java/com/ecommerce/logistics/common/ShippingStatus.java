package com.ecommerce.logistics.common;

public final class ShippingStatus {
    private ShippingStatus() {}
    public static final int PENDING = 0;        // 待交运
    public static final int DISPATCHED = 1;     // 已交运
    public static final int IN_TRANSIT = 2;     // 运输中
    public static final int DELIVERING = 3;     // 派送中
    public static final int SIGNED = 4;         // 已签收
    public static final int EXCEPTION = 5;      // 异常
    public static final int RETURNED = 6;       // 已退回

    public static String text(int status) {
        return switch (status) {
            case PENDING -> "待交运";
            case DISPATCHED -> "已交运";
            case IN_TRANSIT -> "运输中";
            case DELIVERING -> "派送中";
            case SIGNED -> "已签收";
            case EXCEPTION -> "异常";
            case RETURNED -> "已退回";
            default -> "未知";
        };
    }
}
