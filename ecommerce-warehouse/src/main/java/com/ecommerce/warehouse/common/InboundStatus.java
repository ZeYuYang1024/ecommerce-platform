package com.ecommerce.warehouse.common;

public final class InboundStatus {
    private InboundStatus() {}
    public static final int PENDING = 0;       // 待收货
    public static final int RECEIVED = 1;      // 已收货
    public static final int SHELVED = 2;       // 已上架
    public static final int COMPLETED = 3;     // 已完成

    public static String text(int status) {
        return switch (status) {
            case PENDING -> "待收货";
            case RECEIVED -> "已收货";
            case SHELVED -> "已上架";
            case COMPLETED -> "已完成";
            default -> "未知";
        };
    }
}
