package com.ecommerce.knowledge.tool;

import com.ecommerce.knowledge.agent.AgentUserContext;
import com.ecommerce.knowledge.agent.AgentUserContextHolder;
import com.ecommerce.knowledge.client.CartClient;
import com.ecommerce.knowledge.client.dto.CartItemVO;
import com.ecommerce.knowledge.common.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartQueryToolTest {

    @Mock
    private CartClient cartClient;

    @InjectMocks
    private CartQueryTool cartQueryTool;

    @AfterEach
    void tearDown() {
        AgentUserContextHolder.clear();
    }

    @Test
    void queryCurrentUserCartReturnsCartItemsForCurrentUser() {
        AgentUserContextHolder.set(new AgentUserContext(1001L, "USER"));
        CartItemVO item = new CartItemVO();
        item.setSkuId(11L);
        item.setName("Keyboard");
        item.setQuantity(2);
        item.setPrice(new BigDecimal("199.00"));
        when(cartClient.getCurrentUserCart(1001L)).thenReturn(Result.ok(List.of(item)));

        List<CartItemVO> result = cartQueryTool.queryCurrentUserCart();

        assertEquals(1, result.size());
        assertEquals("Keyboard", result.getFirst().getName());
        verify(cartClient).getCurrentUserCart(1001L);
    }

    @Test
    void queryCurrentUserCartReturnsEmptyListWhenCurrentUserMissing() {
        List<CartItemVO> result = cartQueryTool.queryCurrentUserCart();

        assertTrue(result.isEmpty());
        verify(cartClient, never()).getCurrentUserCart(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void queryCurrentUserCartReturnsEmptyListWhenClientThrows() {
        AgentUserContextHolder.set(new AgentUserContext(1001L, "USER"));
        when(cartClient.getCurrentUserCart(1001L)).thenThrow(new RuntimeException("boom"));

        List<CartItemVO> result = cartQueryTool.queryCurrentUserCart();

        assertTrue(result.isEmpty());
        verify(cartClient).getCurrentUserCart(1001L);
    }

    @Test
    void queryCurrentUserCartCountReturnsCountForCurrentUser() {
        AgentUserContextHolder.set(new AgentUserContext(1001L, "USER"));
        when(cartClient.getCurrentUserCartCount(1001L)).thenReturn(Result.ok(3));

        Integer result = cartQueryTool.queryCurrentUserCartCount();

        assertEquals(3, result);
        verify(cartClient).getCurrentUserCartCount(1001L);
    }

    @Test
    void queryCurrentUserCartCountReturnsNullWhenCurrentUserMissing() {
        Integer result = cartQueryTool.queryCurrentUserCartCount();

        org.junit.jupiter.api.Assertions.assertNull(result);
        verify(cartClient, never()).getCurrentUserCartCount(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void queryCurrentUserCartCountReturnsNullWhenClientThrows() {
        AgentUserContextHolder.set(new AgentUserContext(1001L, "USER"));
        when(cartClient.getCurrentUserCartCount(1001L)).thenThrow(new RuntimeException("boom"));

        Integer result = cartQueryTool.queryCurrentUserCartCount();

        org.junit.jupiter.api.Assertions.assertNull(result);
        verify(cartClient).getCurrentUserCartCount(1001L);
    }
}
