package com.ecommerce.seckill.service.impl;

import com.ecommerce.common.result.BusinessException;
import com.ecommerce.seckill.entity.SeckillItem;
import com.ecommerce.seckill.entity.SeckillSession;
import com.ecommerce.seckill.mapper.SeckillItemMapper;
import com.ecommerce.seckill.mapper.SeckillSessionMapper;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeckillServiceImplTest {

    @Mock
    private SeckillSessionMapper sessionMapper;

    @Mock
    private SeckillItemMapper itemMapper;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private RocketMQTemplate rocketMQTemplate;

    @InjectMocks
    private SeckillServiceImpl seckillService;

    @Test
    void createItem_shouldRejectCrossTenantSessionBinding() {
        SeckillSession foreignSession = new SeckillSession();
        foreignSession.setId(7001L);
        foreignSession.setMerchantId(3002L);
        foreignSession.setName("foreign-session");
        when(sessionMapper.selectById(7001L)).thenReturn(foreignSession);

        SeckillItem item = new SeckillItem();
        item.setSessionId(7001L);
        item.setSpuId(11L);
        item.setSkuId(12L);
        item.setName("hack-item");
        item.setOriginalPrice(new BigDecimal("99.00"));
        item.setSeckillPrice(new BigDecimal("59.00"));
        item.setStockCount(10);
        item.setRemainingCount(10);
        item.setStatus(1);

        assertThatThrownBy(() -> seckillService.createItem(item, 2001L))
                .isInstanceOf(BusinessException.class);

        verify(itemMapper, never()).insert(any(SeckillItem.class));
    }
}
