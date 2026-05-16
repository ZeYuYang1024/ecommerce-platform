package com.ecommerce.knowledge.tool;

import com.ecommerce.knowledge.agent.AgentUserContextHolder;
import com.ecommerce.knowledge.client.AddressClient;
import com.ecommerce.knowledge.client.dto.AddressVO;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AddressQueryTool {

    private final AddressClient addressClient;

    @Tool("查询当前登录用户保存的收货地址列表。")
    public List<AddressVO> queryCurrentUserAddresses() {
        Long userId = currentUserId();
        if (userId == null) {
            return Collections.emptyList();
        }
        try {
            var result = addressClient.getCurrentUserAddresses(userId);
            if (result != null && result.getData() != null) {
                return result.getData();
            }
        } catch (Exception e) {
            log.warn("Failed to query addresses for user {}", userId, e);
        }
        return Collections.emptyList();
    }

    @Tool("查询当前登录用户的默认收货地址。")
    public AddressVO queryCurrentUserDefaultAddress() {
        Long userId = currentUserId();
        if (userId == null) {
            return null;
        }
        try {
            var result = addressClient.getCurrentUserAddresses(userId);
            if (result != null && result.getData() != null) {
                return result.getData().stream()
                        .filter(address -> Integer.valueOf(1).equals(address.getIsDefault()))
                        .findFirst()
                        .orElse(null);
            }
        } catch (Exception e) {
            log.warn("Failed to query default address for user {}", userId, e);
        }
        return null;
    }

    private Long currentUserId() {
        var context = AgentUserContextHolder.get();
        return context != null ? context.userId() : null;
    }
}
