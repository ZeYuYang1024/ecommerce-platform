package com.ecommerce.seckill.controller;

import com.ecommerce.common.result.Result;
import com.ecommerce.seckill.entity.SeckillItem;
import com.ecommerce.seckill.entity.SeckillSession;
import com.ecommerce.seckill.service.SeckillService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminSeckillController {

    private final SeckillService seckillService;

    @GetMapping("/seckill/sessions")
    public Result<List<SeckillSession>> listSessions() {
        return Result.ok(seckillService.listSessions());
    }

    @PostMapping("/seckill/sessions")
    public Result<SeckillSession> createSession(@RequestBody SeckillSession session) {
        return Result.ok(seckillService.createSession(session));
    }

    @GetMapping("/seckill/items")
    public Result<List<SeckillItem>> listItems(@RequestParam(required = false) Long sessionId) {
        return Result.ok(seckillService.listItems(sessionId));
    }

    @PostMapping("/seckill/items")
    public Result<SeckillItem> createItem(@RequestBody SeckillItem item) {
        return Result.ok(seckillService.createItem(item));
    }
}
