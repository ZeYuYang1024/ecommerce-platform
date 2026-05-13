package com.ecommerce.seckill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.seckill.common.SeckillErrorCode;
import com.ecommerce.seckill.dto.SeckillOrderMessage;
import com.ecommerce.seckill.entity.SeckillItem;
import com.ecommerce.seckill.entity.SeckillSession;
import com.ecommerce.seckill.mapper.SeckillItemMapper;
import com.ecommerce.seckill.mapper.SeckillSessionMapper;
import com.ecommerce.seckill.service.SeckillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillServiceImpl implements SeckillService {

    private static final String STOCK_KEY_PREFIX = "seckill:stock:";

    private final SeckillSessionMapper sessionMapper;
    private final SeckillItemMapper itemMapper;
    private final StringRedisTemplate redisTemplate;
    private final RocketMQTemplate rocketMQTemplate;

    private static final String LUA_DEDUCT =
        "local key = KEYS[1]\n" +
        "local stock = redis.call('get', key)\n" +
        "if not stock or tonumber(stock) <= 0 then return 0 end\n" +
        "redis.call('decr', key)\n" +
        "return 1";

    private final DefaultRedisScript<Long> deductScript = new DefaultRedisScript<>(LUA_DEDUCT, Long.class);

    @Override
    public SeckillSession createSession(SeckillSession session) {
        sessionMapper.insert(session);
        return session;
    }

    @Override
    public List<SeckillSession> listSessions() {
        return sessionMapper.selectList(new LambdaQueryWrapper<SeckillSession>()
                .orderByDesc(SeckillSession::getStartTime));
    }

    @Override
    public Page<SeckillSession> listSessions(int page, int size) {
        Page<SeckillSession> pageReq = new Page<>(page, size);
        return sessionMapper.selectPage(pageReq, new LambdaQueryWrapper<SeckillSession>()
                .orderByDesc(SeckillSession::getStartTime));
    }

    @Override
    public List<SeckillSession> activeSessions() {
        LocalDateTime now = LocalDateTime.now();
        return sessionMapper.selectList(new LambdaQueryWrapper<SeckillSession>()
                .le(SeckillSession::getStartTime, now)
                .ge(SeckillSession::getEndTime, now)
                .orderByAsc(SeckillSession::getStartTime));
    }

    @Override
    @Transactional
    public SeckillItem createItem(SeckillItem item) {
        itemMapper.insert(item);
        preloadStock(item.getId());
        return item;
    }

    @Override
    public List<SeckillItem> listItems(Long sessionId) {
        return itemMapper.selectList(new LambdaQueryWrapper<SeckillItem>()
                .eq(sessionId != null, SeckillItem::getSessionId, sessionId)
                .eq(SeckillItem::getStatus, 1)
                .orderByDesc(SeckillItem::getCreatedAt));
    }

    @Override
    public Page<SeckillItem> listItems(Long sessionId, int page, int size) {
        Page<SeckillItem> pageReq = new Page<>(page, size);
        return itemMapper.selectPage(pageReq, new LambdaQueryWrapper<SeckillItem>()
                .eq(sessionId != null, SeckillItem::getSessionId, sessionId)
                .eq(SeckillItem::getStatus, 1)
                .orderByDesc(SeckillItem::getCreatedAt));
    }

    @Override
    public long placeOrder(Long itemId, Long userId) {
        SeckillItem item = itemMapper.selectById(itemId);
        if (item == null || item.getStatus() == 0) throw new BusinessException(SeckillErrorCode.ITEM_NOT_FOUND);

        SeckillSession session = sessionMapper.selectById(item.getSessionId());
        if (session == null) throw new BusinessException(SeckillErrorCode.SESSION_NOT_FOUND);
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(session.getStartTime()) || now.isAfter(session.getEndTime()))
            throw new BusinessException(SeckillErrorCode.SESSION_NOT_ACTIVE);

        // Redis Lua 原子扣库存
        String stockKey = STOCK_KEY_PREFIX + itemId;
        Long result = redisTemplate.execute(deductScript, List.of(stockKey));
        if (result == null || result == 0) throw new BusinessException(SeckillErrorCode.STOCK_INSUFFICIENT);

        // 异步创建订单
        SeckillOrderMessage msg = new SeckillOrderMessage(
                itemId, userId, item.getSkuId(), item.getSeckillPrice());
        rocketMQTemplate.syncSend("seckill-order", msg);
        log.info("Seckill order placed: itemId={}, userId={}", itemId, userId);

        // 同步扣减 DB 库存
        int rows = itemMapper.update(null,
                new LambdaUpdateWrapper<SeckillItem>()
                        .eq(SeckillItem::getId, itemId)
                        .gt(SeckillItem::getRemainingCount, 0)
                        .setSql("remaining_count = remaining_count - 1"));
        if (rows == 0) log.warn("DB stock deduct failed for itemId={}", itemId);

        return result;
    }

    @Override
    public void preloadStock(Long itemId) {
        SeckillItem item = itemMapper.selectById(itemId);
        if (item != null) {
            String key = STOCK_KEY_PREFIX + itemId;
            redisTemplate.opsForValue().set(key, String.valueOf(item.getRemainingCount()));
            log.info("Stock preloaded: {} -> {}", key, item.getRemainingCount());
        }
    }
}
