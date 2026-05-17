package com.ecommerce.seckill.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.Result;
import com.ecommerce.seckill.entity.SeckillItem;
import com.ecommerce.seckill.entity.SeckillSession;
import com.ecommerce.seckill.service.SeckillService;
import jakarta.validation.Valid;
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

    @GetMapping("/merchant/seckill/sessions")
    public Result<Page<SeckillSession>> merchantListSessions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader("X-Merchant-Id") Long merchantId) {
        return Result.ok(seckillService.listSessionsByMerchant(merchantId, page, size));
    }

    @PostMapping("/seckill/sessions")
    public Result<SeckillSession> createSession(@Valid @RequestBody SeckillSession session) {
        return Result.ok(seckillService.createSession(session));
    }

    @PostMapping("/merchant/seckill/sessions")
    public Result<SeckillSession> merchantCreateSession(@Valid @RequestBody SeckillSession session,
                                                        @RequestHeader("X-Merchant-Id") Long merchantId) {
        return Result.ok(seckillService.createSession(session, merchantId));
    }

    @GetMapping("/seckill/items")
    public Result<Page<SeckillItem>> listItems(
            @RequestParam(required = false) Long sessionId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(seckillService.listItems(sessionId, page, size));
    }

    @GetMapping("/merchant/seckill/items")
    public Result<Page<SeckillItem>> merchantListItems(
            @RequestParam(required = false) Long sessionId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader("X-Merchant-Id") Long merchantId) {
        return Result.ok(seckillService.listItemsByMerchant(merchantId, sessionId, page, size));
    }

    @PostMapping("/seckill/items")
    public Result<SeckillItem> createItem(@Valid @RequestBody SeckillItem item) {
        return Result.ok(seckillService.createItem(item));
    }

    @PostMapping("/merchant/seckill/items")
    public Result<SeckillItem> merchantCreateItem(@Valid @RequestBody SeckillItem item,
                                                  @RequestHeader("X-Merchant-Id") Long merchantId) {
        return Result.ok(seckillService.createItem(item, merchantId));
    }
}
