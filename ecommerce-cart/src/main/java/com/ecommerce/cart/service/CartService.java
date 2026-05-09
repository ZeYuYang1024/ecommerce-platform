package com.ecommerce.cart.service;

import com.ecommerce.cart.dto.CartItem;

import java.util.List;

public interface CartService {
    void addItem(Long userId, CartItem item);
    void updateQuantity(Long userId, Long skuId, int quantity);
    void removeItem(Long userId, Long skuId);
    void clearCart(Long userId);
    List<CartItem> getCart(Long userId);
    int getCount(Long userId);
    void toggleCheck(Long userId, Long skuId);
    void mergeCart(Long userId, String anonymousToken);
}
