package com.ecommerce.knowledge.tool;

import com.ecommerce.knowledge.client.InventoryClient;
import com.ecommerce.knowledge.client.dto.InventoryVO;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class InventoryQueryTool {

    private final InventoryClient inventoryClient;

    @Tool("查询商品库存信息，可按SKU编码或名称查询。返回SKU编码、名称、可用库存量等信息。")
    public List<InventoryVO> queryInventory(@P("SKU编码或名称，可为空") String keyword) {
        try {
            var result = inventoryClient.query(keyword, keyword, 1, 10);
            if (result != null && result.getData() != null) {
                return result.getData();
            }
        } catch (Exception e) {
            log.warn("Failed to query inventory with keyword '{}'", keyword, e);
        }
        return Collections.emptyList();
    }
}
