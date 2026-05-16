package com.ecommerce.knowledge.tool;

import com.ecommerce.knowledge.client.ProductClient;
import com.ecommerce.knowledge.client.dto.ProductVO;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductQueryTool {

    private final ProductClient productClient;

    @Tool("根据商品关键词搜索在售商品，返回商品名称、价格区间、评分和简介等摘要信息。")
    public List<ProductVO> searchProducts(@P("搜索关键词") String keyword) {
        try {
            var result = productClient.search(keyword, 1, 5);
            if (result != null && result.getData() != null) {
                return result.getData().getRecords();
            }
        } catch (Exception e) {
            log.warn("Failed to search products with keyword '{}'", keyword, e);
        }
        return Collections.emptyList();
    }
}
