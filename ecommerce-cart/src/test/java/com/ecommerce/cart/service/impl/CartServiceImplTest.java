package com.ecommerce.cart.service.impl;

import com.ecommerce.cart.common.CartErrorCode;
import com.ecommerce.cart.dto.CartItem;
import com.ecommerce.common.result.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CartServiceImplTest {

    private RedisTemplate<String, Object> redisTemplate;
    private HashOperations<String, Object, Object> hashOps;
    private CartServiceImpl service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        hashOps = mock(HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        service = new CartServiceImpl(redisTemplate, null);
    }

    private CartItem createItem(Long skuId, BigDecimal price, int qty) {
        CartItem item = new CartItem();
        item.setSkuId(skuId); item.setSpuId(100L); item.setName("Test");
        item.setPrice(price); item.setQuantity(qty); item.setChecked(true);
        return item;
    }

    @Nested
    class AddTests {
        @Test
        void shouldAddNewItem() throws Exception {
            CartItem item = createItem(1L, new BigDecimal("99.00"), 1);
            when(hashOps.get(anyString(), eq("1"))).thenReturn(null);
            service.addItem(1L, item);
            verify(hashOps).put(eq("cart:user:1"), eq("1"), anyString());
            verify(redisTemplate).expire(anyString(), eq(30L), eq(TimeUnit.DAYS));
        }

        @Test
        void shouldIncrementQuantityWhenExisting() throws Exception {
            CartItem existing = createItem(1L, new BigDecimal("99.00"), 2);
            when(hashOps.get("cart:user:1", "1"))
                    .thenReturn("{\"skuId\":1,\"spuId\":100,\"name\":\"Test\",\"price\":99.00,\"quantity\":2,\"checked\":true}");
            service.addItem(1L, createItem(1L, new BigDecimal("99.00"), 1));
            verify(hashOps).put(eq("cart:user:1"), eq("1"), anyString());
        }

        @Test
        void shouldRejectZeroQuantity() {
            CartItem item = createItem(1L, new BigDecimal("99.00"), 0);
            assertThatThrownBy(() -> service.addItem(1L, item))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        void shouldRejectNegativeQuantity() {
            CartItem item = createItem(1L, new BigDecimal("99.00"), -1);
            assertThatThrownBy(() -> service.addItem(1L, item))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        void shouldSetCheckedTrueWhenNull() throws Exception {
            CartItem item = createItem(1L, new BigDecimal("99.00"), 1);
            item.setChecked(null);
            when(hashOps.get(anyString(), anyString())).thenReturn(null);
            service.addItem(1L, item);
            assertThat(item.getChecked()).isTrue();
        }
    }

    @Nested
    class UpdateQuantityTests {
        @Test
        void shouldUpdateQuantity() throws Exception {
            when(hashOps.get("cart:user:1", "1"))
                    .thenReturn("{\"skuId\":1,\"spuId\":100,\"name\":\"Test\",\"price\":99.00,\"quantity\":1,\"checked\":true}");
            service.updateQuantity(1L, 1L, 5);
            verify(hashOps).put(eq("cart:user:1"), eq("1"), anyString());
        }

        @Test
        void shouldDeleteWhenQuantityZero() {
            when(hashOps.get("cart:user:1", "1"))
                    .thenReturn("{\"skuId\":1,\"spuId\":100,\"name\":\"Test\",\"price\":99.00,\"quantity\":1,\"checked\":true}");
            service.updateQuantity(1L, 1L, 0);
            verify(hashOps).delete("cart:user:1", "1");
        }

        @Test
        void shouldRejectNegativeQuantity() {
            assertThatThrownBy(() -> service.updateQuantity(1L, 1L, -1))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        void shouldThrowWhenItemNotFound() {
            when(hashOps.get(anyString(), eq("999"))).thenReturn(null);
            assertThatThrownBy(() -> service.updateQuantity(1L, 999L, 1))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(CartErrorCode.CART_ITEM_NOT_FOUND.getCode());
        }
    }

    @Nested
    class RemoveTests {
        @Test
        void shouldRemoveItem() {
            service.removeItem(1L, 1L);
            verify(hashOps).delete("cart:user:1", "1");
        }
    }

    @Nested
    class ClearTests {
        @Test
        void shouldClearCart() {
            service.clearCart(1L);
            verify(redisTemplate).delete("cart:user:1");
        }
    }

    @Nested
    class ToggleCheckTests {
        @Test
        void shouldToggleFromTrueToFalse() throws Exception {
            when(hashOps.get("cart:user:1", "1"))
                    .thenReturn("{\"skuId\":1,\"spuId\":100,\"name\":\"Test\",\"price\":99.00,\"quantity\":1,\"checked\":true}");
            service.toggleCheck(1L, 1L);
            verify(hashOps).put(eq("cart:user:1"), eq("1"), anyString());
        }

        @Test
        void shouldToggleFromFalseToTrue() throws Exception {
            when(hashOps.get("cart:user:1", "1"))
                    .thenReturn("{\"skuId\":1,\"spuId\":100,\"name\":\"Test\",\"price\":99.00,\"quantity\":1,\"checked\":false}");
            service.toggleCheck(1L, 1L);
            verify(hashOps).put(eq("cart:user:1"), eq("1"), anyString());
        }

        @Test
        void shouldThrowWhenItemNotFound() {
            when(hashOps.get(anyString(), eq("999"))).thenReturn(null);
            assertThatThrownBy(() -> service.toggleCheck(1L, 999L))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    class GetCartTests {
        @Test
        void shouldReturnCartItems() {
            Map<Object, Object> entries = new HashMap<>();
            entries.put("1", "{\"skuId\":1,\"spuId\":100,\"name\":\"Test\",\"price\":99.00,\"quantity\":1,\"checked\":true}");
            when(hashOps.entries("cart:user:1")).thenReturn(entries);

            var items = service.getCart(1L);
            assertThat(items).hasSize(1);
            assertThat(items.get(0).getSkuId()).isEqualTo(1L);
        }

        @Test
        void shouldReturnEmptyListWhenCartEmpty() {
            when(hashOps.entries("cart:user:1")).thenReturn(new HashMap<>());
            assertThat(service.getCart(1L)).isEmpty();
        }
    }

    @Nested
    class GetCountTests {
        @Test
        void shouldReturnTotalQuantity() {
            Map<Object, Object> entries = new HashMap<>();
            entries.put("1", "{\"skuId\":1,\"spuId\":100,\"name\":\"A\",\"price\":10.00,\"quantity\":2,\"checked\":true}");
            entries.put("2", "{\"skuId\":2,\"spuId\":100,\"name\":\"B\",\"price\":20.00,\"quantity\":3,\"checked\":true}");
            when(hashOps.entries("cart:user:1")).thenReturn(entries);

            assertThat(service.getCount(1L)).isEqualTo(5);
        }

        @Test
        void shouldReturnZeroWhenCartEmpty() {
            when(hashOps.entries("cart:user:1")).thenReturn(new HashMap<>());
            assertThat(service.getCount(1L)).isEqualTo(0);
        }
    }

    @Nested
    class BoundaryTests {
        @Test
        void shouldHandleLargeQuantity() throws Exception {
            CartItem item = createItem(1L, new BigDecimal("0.01"), Integer.MAX_VALUE);
            when(hashOps.get(anyString(), anyString())).thenReturn(null);
            assertThatCode(() -> service.addItem(1L, item)).doesNotThrowAnyException();
        }

        @Test
        void shouldHandleZeroPrice() throws Exception {
            CartItem item = createItem(1L, BigDecimal.ZERO, 1);
            when(hashOps.get(anyString(), anyString())).thenReturn(null);
            assertThatCode(() -> service.addItem(1L, item)).doesNotThrowAnyException();
        }

        @Test
        void shouldHandleVeryLargePrice() throws Exception {
            CartItem item = createItem(1L, new BigDecimal("99999999.99"), 1);
            when(hashOps.get(anyString(), anyString())).thenReturn(null);
            assertThatCode(() -> service.addItem(1L, item)).doesNotThrowAnyException();
        }
    }
}
