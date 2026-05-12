package com.ecommerce.seckill.service;

import com.ecommerce.seckill.entity.SeckillItem;
import com.ecommerce.seckill.entity.SeckillSession;
import java.util.List;

public interface SeckillService {
    // 场次管理
    SeckillSession createSession(SeckillSession session);
    List<SeckillSession> listSessions();
    List<SeckillSession> activeSessions();

    // 商品管理
    SeckillItem createItem(SeckillItem item);
    List<SeckillItem> listItems(Long sessionId);

    // 秒杀下单（Redis Lua 原子扣库存）
    long placeOrder(Long itemId, Long userId);

    // 库存预加载到 Redis
    void preloadStock(Long itemId);
}
