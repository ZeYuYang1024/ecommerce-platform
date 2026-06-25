package com.ecommerce.warehouse.common;

public final class WarehouseZoneType {
    private WarehouseZoneType() {
    }

    public static final int STORAGE = 0;
    public static final int PICKING = 1;
    public static final int RETURN = 2;

    public static String text(Integer type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case STORAGE -> "存储区";
            case PICKING -> "拣货区";
            case RETURN -> "退货区";
            default -> String.valueOf(type);
        };
    }
}
