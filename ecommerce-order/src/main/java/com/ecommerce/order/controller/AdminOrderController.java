package com.ecommerce.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.Result;
import com.ecommerce.order.dto.request.UpdateOrderStatusRequest;
import com.ecommerce.common.dto.ReconOrderVO;
import com.ecommerce.order.dto.response.OrderVO;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.service.OrderService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/orders")
    public Result<Page<OrderVO>> listAll(@RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "10") int size,
                                          @RequestParam(required = false) Integer status) {
        return Result.ok(orderService.listAll(page, size, status));
    }

    @PutMapping("/orders/{id}/ship")
    public Result<Void> ship(@PathVariable Long id) {
        orderService.markShipped(id);
        return Result.ok();
    }

    @GetMapping("/merchant/orders")
    public Result<Page<OrderVO>> listByMerchant(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int size,
                                                  @RequestParam(required = false) Integer status,
                                                  @RequestHeader("X-Merchant-Id") Long merchantId) {
        return Result.ok(orderService.listByMerchant(merchantId, page, size, status));
    }

    @PutMapping("/orders/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody UpdateOrderStatusRequest request) {
        orderService.updateStatus(id, request.getStatus());
        return Result.ok();
    }

    @GetMapping("/orders/recon")
    public Result<List<Map<String, Object>>> getOrdersForRecon(@RequestParam("start") String start,
                                                                @RequestParam("end") String end) {
        LocalDateTime startTime = null, endTime = null;
        try {
            if (start != null && !start.isEmpty()) startTime = LocalDateTime.parse(start);
            if (end != null && !end.isEmpty()) endTime = LocalDateTime.parse(end);
        } catch (Exception e) {
            return Result.fail(400, "日期格式错误");
        }
        List<Order> orders = orderService.listForRecon(startTime, endTime);
        List<Map<String, Object>> result = orders.stream().map(o -> {
            Map<String, Object> m = new HashMap<>();
            m.put("orderNo", o.getOrderNo());
            m.put("amount", o.getTotalAmount());
            m.put("status", o.getStatus());
            return m;
        }).collect(Collectors.toList());
        return Result.ok(result);
    }
}
