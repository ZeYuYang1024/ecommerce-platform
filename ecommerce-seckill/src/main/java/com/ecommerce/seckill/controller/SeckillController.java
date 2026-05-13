package com.ecommerce.seckill.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.Result;
import com.ecommerce.seckill.dto.request.SeckillOrderRequest;
import com.ecommerce.seckill.entity.SeckillItem;
import com.ecommerce.seckill.entity.SeckillSession;
import com.ecommerce.seckill.service.SeckillService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SeckillController {

    private final SeckillService seckillService;

    @GetMapping("/seckill/sessions")
    public Result<Page<SeckillSession>> activeSessions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(seckillService.listSessions(page, size));
    }

    @GetMapping("/seckill/items")
    public Result<Page<SeckillItem>> items(
            @RequestParam Long sessionId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(seckillService.listItems(sessionId, page, size));
    }

    @PostMapping("/seckill/order")
    public Result<String> placeOrder(@RequestBody SeckillOrderRequest request) {
        seckillService.placeOrder(request.getItemId(), request.getUserId());
        return Result.ok("秒杀下单成功");
    }
}
