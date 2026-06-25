package com.ecommerce.common.constant;

public final class WarehouseStockMode {
    private WarehouseStockMode() {
    }

    public static final int LIGHT = 0;
    public static final int MANAGED = 1;

    public static String text(Integer mode) {
        if (mode == null) {
            return null;
        }
        return switch (mode) {
            case LIGHT -> "轻仓";
            case MANAGED -> "托管";
            default -> String.valueOf(mode);
        };
    }
}
