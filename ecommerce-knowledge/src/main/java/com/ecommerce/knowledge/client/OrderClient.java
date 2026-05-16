package com.ecommerce.knowledge.client;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.knowledge.client.dto.OrderVO;
import com.ecommerce.knowledge.common.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "ecommerce-order", path = "/api/v1")
public interface OrderClient {

    @GetMapping("/orders")
    Result<Page<OrderVO>> listByUser(@RequestHeader("X-User-Id") Long userId,
                                     @RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "10") int size);

    @GetMapping("/orders/no/{orderNo}")
    Result<OrderVO> getByOrderNo(@RequestHeader("X-User-Id") Long userId,
                                 @PathVariable String orderNo);
}
