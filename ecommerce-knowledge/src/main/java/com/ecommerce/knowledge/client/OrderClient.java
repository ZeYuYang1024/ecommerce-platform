package com.ecommerce.knowledge.client;

import com.ecommerce.knowledge.client.dto.OrderVO;
import com.ecommerce.knowledge.common.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "ecommerce-order", path = "/api/v1")
public interface OrderClient {

    @GetMapping("/internal/orders/byUser/{userId}")
    Result<List<OrderVO>> listByUserId(@PathVariable Long userId,
                                        @RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "10") int size);

    @GetMapping("/internal/orders/{orderNo}")
    Result<OrderVO> getByOrderNo(@PathVariable String orderNo);
}
