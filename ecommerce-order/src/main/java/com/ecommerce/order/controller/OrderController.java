package com.ecommerce.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.dto.OrderInternalVO;
import com.ecommerce.common.result.Result;
import com.ecommerce.order.dto.request.CreateOrderRequest;
import com.ecommerce.order.mapper.OrderMapper;
import com.ecommerce.order.entity.Order;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.order.dto.response.OrderVO;
import com.ecommerce.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class OrderController {
    private final OrderMapper orderMapper;


    private final OrderService orderService;

    public OrderController(OrderService orderService, OrderMapper orderMapper) {
        this.orderService = orderService;
        this.orderMapper = orderMapper;
    }

    @PostMapping("/orders")
    public Result<OrderVO> create(@RequestHeader("X-User-Id") Long userId,
                                   @Valid @RequestBody CreateOrderRequest request) {
        return Result.ok(orderService.createOrder(userId, request));
    }

    @GetMapping("/orders")
    public Result<Page<OrderVO>> listByUser(@RequestHeader("X-User-Id") Long userId,
                                             @RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size) {
        return Result.ok(orderService.listByUser(userId, page, size));
    }

    @GetMapping("/internal/orders/no/{orderNo}")
    public Result<OrderInternalVO> internalGetByOrderNo(@PathVariable String orderNo, @RequestParam("userId") Long userId) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo).eq(Order::getUserId, userId));
        if (order == null) return Result.fail(404, "订单不存在");
        OrderInternalVO vo = new OrderInternalVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        return Result.ok(vo);
    }

    @GetMapping("/orders/no/{orderNo}")
    public Result<OrderVO> detailByOrderNo(@RequestHeader("X-User-Id") Long userId, @PathVariable String orderNo) {
        return Result.ok(orderService.getOrderByOrderNo(userId, orderNo));
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
