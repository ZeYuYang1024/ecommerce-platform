package com.ecommerce.seckill.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.seckill.entity.SeckillItem;
import com.ecommerce.seckill.entity.SeckillSession;

import java.util.List;

public interface SeckillService {
    SeckillSession createSession(SeckillSession session);

    SeckillSession createSession(SeckillSession session, Long merchantId);

    List<SeckillSession> listSessions();

    Page<SeckillSession> listSessions(int page, int size);

    Page<SeckillSession> listSessionsByMerchant(Long merchantId, int page, int size);

    List<SeckillSession> activeSessions();

    SeckillItem createItem(SeckillItem item);

    SeckillItem createItem(SeckillItem item, Long merchantId);

    List<SeckillItem> listItems(Long sessionId);

    Page<SeckillItem> listItems(Long sessionId, int page, int size);

    Page<SeckillItem> listItemsByMerchant(Long merchantId, Long sessionId, int page, int size);

    long placeOrder(Long itemId, Long userId);

    void preloadStock(Long itemId);
}
