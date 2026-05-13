package com.ecommerce.seckill.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.Result;
import com.ecommerce.seckill.entity.SeckillItem;
import com.ecommerce.seckill.entity.SeckillSession;
import com.ecommerce.seckill.service.SeckillService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminSeckillController {

    private final SeckillService seckillService;

    @GetMapping("/seckill/sessions")
    public Result<Page<SeckillSession>> listSessions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(seckillService.listSessions(page, size));
    }

    @PostMapping("/seckill/sessions")
    public Result<SeckillSession> createSession(@RequestBody SeckillSession session) {
        return Result.ok(seckillService.createSession(session));
    }

    @GetMapping("/seckill/items")
    public Result<Page<SeckillItem>> listItems(
            @RequestParam(required = false) Long sessionId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(seckillService.listItems(sessionId, page, size));
    }

    @PostMapping("/seckill/items")
    public Result<SeckillItem> createItem(@RequestBody SeckillItem item) {
        return Result.ok(seckillService.createItem(item));
    }
}
