package com.ecommerce.warehouse.common;

public final class OutboundStatus {
    private OutboundStatus() {}
    public static final int PENDING = 0;       // 待拣货
    public static final int PICKING = 1;       // 拣货中
    public static final int SHIPPED = 2;       // 已出库
    public static final int DELIVERED = 3;     // 已送达

    public static String text(int status) {
        return switch (status) {
            case PENDING -> "待拣货";
            case PICKING -> "拣货中";
            case SHIPPED -> "已出库";
            case DELIVERED -> "已送达";
            default -> "未知";
        };
    }
}
