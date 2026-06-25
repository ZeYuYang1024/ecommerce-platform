package com.ecommerce.common.constant;

public final class OutboundType {
    private OutboundType() {
    }

    public static final int SALES = 0;
    public static final int TRANSFER = 1;
    public static final int STOCK_CHECK = 2;

    public static String text(Integer type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case SALES -> "销售出库";
            case TRANSFER -> "调拨出库";
            case STOCK_CHECK -> "盘点调整";
            default -> String.valueOf(type);
        };
    }
}
