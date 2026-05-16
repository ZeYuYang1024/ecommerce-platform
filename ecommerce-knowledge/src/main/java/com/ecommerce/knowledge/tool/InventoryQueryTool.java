package com.ecommerce.knowledge.tool;

import com.ecommerce.knowledge.client.InventoryClient;
import com.ecommerce.knowledge.client.dto.InventoryVO;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryQueryTool {

    private final InventoryClient inventoryClient;

    @Tool("根据SKU ID查询库存详情，返回商品名、价格、总库存、锁定库存和可用库存。")
    public InventoryVO queryInventoryBySkuId(@P("SKU ID") Long skuId) {
        try {
            var result = inventoryClient.getBySkuId(skuId);
            if (result != null && result.getData() != null) {
                return result.getData();
            }
        } catch (Exception e) {
            log.warn("Failed to query inventory for skuId {}", skuId, e);
        }
        return null;
    }
}
