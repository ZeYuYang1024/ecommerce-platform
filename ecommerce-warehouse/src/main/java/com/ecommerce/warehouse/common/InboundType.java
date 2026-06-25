package com.ecommerce.warehouse.common;

public final class InboundType {
    private InboundType() {
    }

    public static final int PURCHASE = 0;
    public static final int RETURN = 1;
    public static final int TRANSFER = 2;
    public static final int STOCK_CHECK = 3;

    public static String text(Integer type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case PURCHASE -> "采购入库";
            case RETURN -> "退货入库";
            case TRANSFER -> "调拨入库";
            case STOCK_CHECK -> "盘点调整";
            default -> String.valueOf(type);
        };
    }
}
