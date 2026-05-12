package com.ecommerce.seckill.controller;

import com.ecommerce.common.result.Result;
import com.ecommerce.seckill.dto.request.SeckillOrderRequest;
import com.ecommerce.seckill.entity.SeckillItem;
import com.ecommerce.seckill.entity.SeckillSession;
import com.ecommerce.seckill.service.SeckillService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SeckillController {

    private final SeckillService seckillService;

    @GetMapping("/seckill/sessions")
    public Result<List<SeckillSession>> activeSessions() {
        return Result.ok(seckillService.activeSessions());
    }

    @GetMapping("/seckill/items")
    public Result<List<SeckillItem>> items(@RequestParam Long sessionId) {
        return Result.ok(seckillService.listItems(sessionId));
    }

    @PostMapping("/seckill/order")
    public Result<String> placeOrder(@RequestBody SeckillOrderRequest request) {
        seckillService.placeOrder(request.getItemId(), request.getUserId());
        return Result.ok("秒杀下单成功");
    }
}
