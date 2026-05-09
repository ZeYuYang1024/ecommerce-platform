package com.ecommerce.cart.controller;

import com.ecommerce.cart.dto.CartItem;
import com.ecommerce.cart.dto.MergeCartRequest;
import com.ecommerce.cart.dto.UpdateQuantityRequest;
import com.ecommerce.cart.service.CartService;
import com.ecommerce.common.result.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public Result<List<CartItem>> getCart(@RequestHeader("X-User-Id") Long userId) {
        return Result.ok(cartService.getCart(userId));
    }

    @GetMapping("/count")
    public Result<Integer> getCount(@RequestHeader("X-User-Id") Long userId) {
        return Result.ok(cartService.getCount(userId));
    }

    @PostMapping("/items")
    public Result<Void> addItem(@RequestHeader("X-User-Id") Long userId,
                                 @RequestBody CartItem item) {
        cartService.addItem(userId, item);
        return Result.ok();
    }

    @PutMapping("/items/{skuId}")
    public Result<Void> updateQuantity(@RequestHeader("X-User-Id") Long userId,
                                        @PathVariable Long skuId,
                                        @RequestBody UpdateQuantityRequest request) {
        cartService.updateQuantity(userId, skuId, request.getQuantity());
        return Result.ok();
    }

    @DeleteMapping("/items/{skuId}")
    public Result<Void> removeItem(@RequestHeader("X-User-Id") Long userId,
                                    @PathVariable Long skuId) {
        cartService.removeItem(userId, skuId);
        return Result.ok();
    }

    @DeleteMapping
    public Result<Void> clearCart(@RequestHeader("X-User-Id") Long userId) {
        cartService.clearCart(userId);
        return Result.ok();
    }

    @PutMapping("/items/{skuId}/check")
    public Result<Void> toggleCheck(@RequestHeader("X-User-Id") Long userId,
                                     @PathVariable Long skuId) {
        cartService.toggleCheck(userId, skuId);
        return Result.ok();
    }

    @PostMapping("/merge")
    public Result<Void> mergeCart(@RequestHeader("X-User-Id") Long userId,
                                   @RequestBody MergeCartRequest request) {
        cartService.mergeCart(userId, request.getAnonymousToken());
        return Result.ok();
    }
}
