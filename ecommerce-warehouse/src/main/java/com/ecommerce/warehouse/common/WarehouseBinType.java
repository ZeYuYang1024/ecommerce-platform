package com.ecommerce.warehouse.common;

public final class WarehouseBinType {
    private WarehouseBinType() {
    }

    public static final int STANDARD = 0;
    public static final int OVERSIZE = 1;
    public static final int COLD = 2;

    public static String text(Integer type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case STANDARD -> "普通货位";
            case OVERSIZE -> "重型货位";
            case COLD -> "冷藏货位";
            default -> String.valueOf(type);
        };
    }
}
