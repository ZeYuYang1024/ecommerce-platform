package com.ecommerce.warehouse.common;

public final class WarehouseType {
    private WarehouseType() {
    }

    public static final int PLATFORM = 0;
    public static final int MERCHANT = 1;

    public static String text(Integer type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case PLATFORM -> "平台仓";
            case MERCHANT -> "商家仓";
            default -> String.valueOf(type);
        };
    }
}
