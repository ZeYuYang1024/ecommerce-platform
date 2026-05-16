package com.ecommerce.knowledge.tool;

import com.ecommerce.knowledge.agent.AgentUserContextHolder;
import com.ecommerce.knowledge.client.CartClient;
import com.ecommerce.knowledge.client.dto.CartItemVO;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CartQueryTool {

    private final CartClient cartClient;

    @Tool("查询当前登录用户的购物车商品列表，返回商品名称、数量、价格和勾选状态。")
    public List<CartItemVO> queryCurrentUserCart() {
        Long userId = currentUserId();
        if (userId == null) {
            return Collections.emptyList();
        }
        try {
            var result = cartClient.getCurrentUserCart(userId);
            if (result != null && result.getData() != null) {
                return result.getData();
            }
        } catch (Exception e) {
            log.warn("Failed to query cart for user {}", userId, e);
        }
        return Collections.emptyList();
    }

    @Tool("查询当前登录用户购物车中的商品总件数。")
    public Integer queryCurrentUserCartCount() {
        Long userId = currentUserId();
        if (userId == null) {
            return null;
        }
        try {
            var result = cartClient.getCurrentUserCartCount(userId);
            if (result != null) {
                return result.getData();
            }
        } catch (Exception e) {
            log.warn("Failed to query cart count for user {}", userId, e);
        }
        return null;
    }

    private Long currentUserId() {
        var context = AgentUserContextHolder.get();
        return context != null ? context.userId() : null;
    }
}
