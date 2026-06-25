package com.ecommerce.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.Result;
import com.ecommerce.order.client.LogisticsClient;
import com.ecommerce.order.client.ProductSpuClient;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.mapper.OrderItemMapper;
import com.ecommerce.order.mapper.OrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplMerchantTest {

    @Mock private OrderMapper orderMapper;
    @Mock private RocketMQTemplate rocketMQTemplate;
    @Mock private OrderItemMapper itemMapper;
    @Mock private ProductSpuClient productSpuClient;
    @Mock private LogisticsClient logisticsClient;
    @InjectMocks private OrderServiceImpl service;

    @BeforeEach
    void setUp() {
        // All mocks via annotations
    }

    @Nested
    class ListByMerchantTests {
        @Test
        void shouldReturnEmptyWhenNoSpuIds() {
            when(productSpuClient.getSpuIdsByMerchant(100L)).thenReturn(Result.ok(Collections.emptyList()));
            var result = service.listByMerchant(100L, 1, 10, null);
            assertThat(result.getTotal()).isEqualTo(0);
            assertThat(result.getRecords()).isEmpty();
        }

        @Test
        void shouldReturnEmptyWhenFeignFails() {
            when(productSpuClient.getSpuIdsByMerchant(anyLong()))
                    .thenThrow(new RuntimeException("timeout"));
            var result = service.listByMerchant(100L, 1, 10, null);
            assertThat(result.getTotal()).isEqualTo(0);
        }

        @Test
        void shouldReturnEmptyWhenNoOrderItems() {
            when(productSpuClient.getSpuIdsByMerchant(100L))
                    .thenReturn(Result.ok(List.of(1L, 2L)));
            when(itemMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());
            var result = service.listByMerchant(100L, 1, 10, null);
            assertThat(result.getTotal()).isEqualTo(0);
        }

        @Test
        void shouldReturnOrdersWhenMatchFound() {
            when(productSpuClient.getSpuIdsByMerchant(100L))
                    .thenReturn(Result.ok(List.of(1L)));

            OrderItem item = new OrderItem();
            item.setOrderId(10L); item.setSpuId(1L);
            when(itemMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.singletonList(item),
                                Collections.singletonList(item));

            Order order = new Order();
            order.setId(10L); order.setOrderNo("ORD001"); order.setUserId(1L);
            order.setTotalAmount(new BigDecimal("99.00")); order.setStatus(1);
            order.setReceiverName("测试"); order.setReceiverPhone("138"); order.setReceiverAddress("地址");

            Page<Order> orderPage = new Page<>(1, 10, 1);
            orderPage.setRecords(Collections.singletonList(order));
            when(orderMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(orderPage);

            var result = service.listByMerchant(100L, 1, 10, null);
            assertThat(result.getTotal()).isEqualTo(1);
            assertThat(result.getRecords().get(0).getOrderNo()).isEqualTo("ORD001");
        }

        @Test
        void shouldFilterByStatus() {
            when(productSpuClient.getSpuIdsByMerchant(100L))
                    .thenReturn(Result.ok(List.of(1L)));

            OrderItem item = new OrderItem();
            item.setOrderId(10L); item.setSpuId(1L);
            when(itemMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.singletonList(item));

            Page<Order> orderPage = new Page<>(1, 10, 0);
            orderPage.setRecords(Collections.emptyList());
            when(orderMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(orderPage);

            var result = service.listByMerchant(100L, 1, 10, 3);
            assertThat(result.getTotal()).isEqualTo(0);
        }

        @Test
        void shouldHandleDuplicateSpuIdsInOrders() {
            when(productSpuClient.getSpuIdsByMerchant(100L))
                    .thenReturn(Result.ok(List.of(1L, 2L)));

            // Two items from same order (different SPUs)
            OrderItem item1 = new OrderItem();
            item1.setOrderId(10L); item1.setSpuId(1L);
            OrderItem item2 = new OrderItem();
            item2.setOrderId(10L); item2.setSpuId(2L);
            when(itemMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(item1, item2), List.of(item1, item2));

            Order order = new Order();
            order.setId(10L); order.setOrderNo("ORD001"); order.setUserId(1L);
            order.setTotalAmount(new BigDecimal("99.00")); order.setStatus(1);

            Page<Order> orderPage = new Page<>(1, 10, 1);
            orderPage.setRecords(Collections.singletonList(order));
            when(orderMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(orderPage);

            var result = service.listByMerchant(100L, 1, 10, null);
            assertThat(result.getTotal()).isEqualTo(1); // deduplicated
        }
    }

    @Nested
    class BoundaryTests {
        @Test
        void shouldHandleNullSpuIdsResponse() {
            when(productSpuClient.getSpuIdsByMerchant(anyLong()))
                    .thenReturn(Result.ok(null));
            var result = service.listByMerchant(100L, 1, 10, null);
            assertThat(result.getRecords()).isEmpty();
        }

        @Test
        void shouldHandleMaxPage() {
            when(productSpuClient.getSpuIdsByMerchant(100L))
                    .thenReturn(Result.ok(Collections.emptyList()));
            var result = service.listByMerchant(100L, Integer.MAX_VALUE, 100, null);
            assertThat(result.getRecords()).isEmpty();
        }

        @Test
        void shouldHandleMinPage() {
            when(productSpuClient.getSpuIdsByMerchant(100L))
                    .thenReturn(Result.ok(Collections.emptyList()));
            var result = service.listByMerchant(100L, 1, 1, null);
            assertThat(result.getRecords()).isEmpty();
        }

        @Test
        void shouldHandleNullStatus() {
            when(productSpuClient.getSpuIdsByMerchant(100L))
                    .thenReturn(Result.ok(Collections.emptyList()));
            var result = service.listByMerchant(100L, 1, 10, null);
            assertThat(result).isNotNull();
        }
    }

    @Nested
    class MerchantOrderNoTests {
        @Test
        void shouldListOrderNosByMerchant() throws Exception {
            when(productSpuClient.getSpuIdsByMerchant(100L))
                    .thenReturn(Result.ok(List.of(1L, 2L)));

            OrderItem item1 = new OrderItem();
            item1.setOrderId(10L);
            item1.setSpuId(1L);
            OrderItem item2 = new OrderItem();
            item2.setOrderId(11L);
            item2.setSpuId(2L);
            when(itemMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(item1, item2));

            Order order1 = new Order();
            order1.setId(10L);
            order1.setOrderNo("ORD001");
            Order order2 = new Order();
            order2.setId(11L);
            order2.setOrderNo("ORD002");
            when(orderMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(order1, order2));

            Method method = OrderServiceImpl.class.getMethod("listOrderNosByMerchant", Long.class);
            @SuppressWarnings("unchecked")
            List<String> orderNos = (List<String>) method.invoke(service, 100L);

            assertThat(orderNos).containsExactlyInAnyOrder("ORD001", "ORD002");
        }
    }
}
