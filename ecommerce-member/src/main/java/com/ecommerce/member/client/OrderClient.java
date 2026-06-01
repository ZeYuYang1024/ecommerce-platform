package com.ecommerce.member.client;

import com.ecommerce.common.dto.OrderMemberVO;
import com.ecommerce.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "ecommerce-order")
public interface OrderClient {

    /**
     * 查询订单信息（供 member 模块，返回含 userId 的完整信息）
     */
    @GetMapping("/api/v1/internal/orders/no/{orderNo}/member")
    Result<OrderMemberVO> getOrderForMember(@PathVariable String orderNo);
}
