package com.ecommerce.common.constant;

public final class OrderStatus {
    private OrderStatus() {
    }

    public static final int PENDING = 0;
    public static final int PAID = 1;
    public static final int SHIPPED = 2;
    public static final int COMPLETED = 3;
    public static final int CANCELLED = 4;

    public static String text(Integer status) {
        if (status == null) {
            return "UNKNOWN";
        }
        return switch (status) {
            case PENDING -> "PENDING";
            case PAID -> "PAID";
            case SHIPPED -> "SHIPPED";
            case COMPLETED -> "COMPLETED";
            case CANCELLED -> "CANCELLED";
            default -> "UNKNOWN";
        };
    }
}
