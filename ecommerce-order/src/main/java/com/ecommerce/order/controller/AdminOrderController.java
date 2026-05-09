package com.ecommerce.order.controller;

import com.ecommerce.common.result.Result;
import com.ecommerce.order.dto.request.UpdateOrderStatusRequest;
import com.ecommerce.order.dto.response.OrderVO;
import com.ecommerce.order.service.OrderService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/orders")
    public Result<List<OrderVO>> listAll(@RequestParam(required = false) Integer status) {
        return Result.ok(orderService.listAll(status));
    }

    @PutMapping("/orders/{id}/ship")
    public Result<Void> ship(@PathVariable Long id) {
        orderService.markShipped(id);
        return Result.ok();
    }

    @PutMapping("/orders/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody UpdateOrderStatusRequest request) {
        orderService.updateStatus(id, request.getStatus());
        return Result.ok();
    }
}
