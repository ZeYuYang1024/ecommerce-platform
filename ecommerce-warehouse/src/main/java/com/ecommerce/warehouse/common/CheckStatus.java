package com.ecommerce.warehouse.common;

public final class CheckStatus {
    private CheckStatus() {}
    public static final int IN_PROGRESS = 0;   // 盘点中
    public static final int COMPLETED = 1;     // 已完成
    public static final int DIFF_PENDING = 2;  // 待处理差异

    public static String text(int status) {
        return switch (status) {
            case IN_PROGRESS -> "盘点中";
            case COMPLETED -> "已完成";
            case DIFF_PENDING -> "待处理差异";
            default -> "未知";
        };
    }
}
