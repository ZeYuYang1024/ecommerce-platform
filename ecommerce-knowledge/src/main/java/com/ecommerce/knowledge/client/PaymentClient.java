package com.ecommerce.knowledge.client;

import com.ecommerce.knowledge.client.dto.PaymentVO;
import com.ecommerce.knowledge.common.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "ecommerce-payment", path = "/api/v1")
public interface PaymentClient {

    @GetMapping("/payment/orders/{orderNo}")
    Result<PaymentVO> getPaymentByOrderNo(@RequestHeader("X-User-Id") Long userId,
                                          @PathVariable String orderNo);
}
