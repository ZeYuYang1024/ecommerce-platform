package com.ecommerce.order.controller;

import com.ecommerce.common.result.Result;
import com.ecommerce.order.dto.request.CreateOrderRequest;
import com.ecommerce.order.dto.response.OrderVO;
import com.ecommerce.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/orders")
    public Result<OrderVO> create(@RequestHeader("X-User-Id") Long userId,
                                   @Valid @RequestBody CreateOrderRequest request) {
        return Result.ok(orderService.createOrder(userId, request));
    }

    @GetMapping("/orders")
    public Result<List<OrderVO>> listByUser(@RequestHeader("X-User-Id") Long userId) {
        return Result.ok(orderService.listByUser(userId));
    }

    @GetMapping("/orders/{id}")
    public Result<OrderVO> detail(@PathVariable Long id) {
        return Result.ok(orderService.getOrder(id));
    }

    @PutMapping("/orders/{id}/cancel")
    public Result<Void> cancel(@RequestHeader("X-User-Id") Long userId,
                                @PathVariable Long id) {
        orderService.cancelOrder(userId, id);
        return Result.ok();
    }
}
