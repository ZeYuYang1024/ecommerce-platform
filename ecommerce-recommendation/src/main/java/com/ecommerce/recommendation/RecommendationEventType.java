package com.ecommerce.recommendation;

enum RecommendationEventType {
    VIEW(1), CART_ADD(4), PAID(6), IMPRESSION(0), CLICK(0);

    final int weight;

    RecommendationEventType(int weight) {
        this.weight = weight;
    }
}
