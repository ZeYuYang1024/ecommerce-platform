package com.ecommerce.payment.client;

import com.ecommerce.common.result.Result;
import com.ecommerce.payment.dto.request.StatusRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ecommerce-order")
public interface OrderClient {

    @PutMapping("/api/v1/admin/orders/{id}/status")
    Result<Void> updateStatus(@PathVariable Long id, @RequestBody StatusRequest request);
}
