package com.ecommerce.knowledge.client;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.knowledge.client.dto.ProductVO;
import com.ecommerce.knowledge.common.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "ecommerce-product", path = "/api/v1")
public interface ProductClient {

    @GetMapping("/products")
    Result<Page<ProductVO>> search(@RequestParam String keyword,
                                   @RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "5") int size);
}
