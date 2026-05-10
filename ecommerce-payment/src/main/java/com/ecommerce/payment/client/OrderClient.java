package com.ecommerce.payment.client;

import com.ecommerce.common.result.Result;
import com.ecommerce.common.dto.ReconOrderVO;
import com.ecommerce.payment.dto.request.StatusRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;


@FeignClient(name = "ecommerce-order", fallbackFactory = OrderClientFallback.class)
public interface OrderClient {

    @PutMapping("/api/v1/admin/orders/{id}/status")
    Result<Void> updateStatus(@PathVariable Long id, @RequestBody StatusRequest request);

    @GetMapping("/api/v1/internal/orders/no/{orderNo}")
    Result<java.util.Map<String, Object>> getOrderByOrderNo(@PathVariable String orderNo, @RequestParam("userId") Long userId);

    @GetMapping("/api/v1/admin/orders/recon")
    Result<List<ReconOrderVO>> getOrdersForRecon(@RequestParam("start") String start,
                                                         @RequestParam("end") String end);
}
