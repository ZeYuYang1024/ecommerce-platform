package com.ecommerce.knowledge.chat;

public enum KnowledgeQueryRoute {
    AFTER_SALE(true),
    PAYMENT_BY_ORDER_NO(true),
    ORDER_LIST(true),
    CART(true),
    ADDRESS(true),
    NOTIFICATION(true),
    COUPON(true),
    INVENTORY(true),
    PRODUCT(true),
    RAG_FAQ(false);

    private final boolean structured;

    KnowledgeQueryRoute(boolean structured) {
        this.structured = structured;
    }

    public boolean isStructured() {
        return structured;
    }
}
