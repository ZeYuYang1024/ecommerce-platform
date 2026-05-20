package com.ecommerce.knowledge.client;

import com.ecommerce.knowledge.client.dto.AddressVO;
import com.ecommerce.knowledge.common.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(name = "ecommerce-user", path = "/api/v1")
public interface AddressClient {

    @GetMapping("/users/addresses/current")
    Result<List<AddressVO>> getCurrentUserAddresses(@RequestHeader("X-User-Id") Long userId);

    @GetMapping("/users/addresses/default")
    Result<AddressVO> getCurrentUserDefaultAddress(@RequestHeader("X-User-Id") Long userId);
}
