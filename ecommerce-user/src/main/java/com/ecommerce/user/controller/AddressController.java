package com.ecommerce.user.controller;

import com.ecommerce.common.result.Result;
import com.ecommerce.user.dto.request.AddressRequest;
import com.ecommerce.user.dto.response.AddressVO;
import com.ecommerce.user.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping("/addresses/current")
    public Result<List<AddressVO>> listCurrent(@RequestHeader("X-User-Id") Long userId) {
        return Result.ok(addressService.listByUserId(userId));
    }

    @GetMapping("/addresses")
    public Result<List<AddressVO>> list(@RequestHeader("Authorization") String token) {
        return Result.ok(addressService.listByToken(token));
    }

    @PostMapping("/addresses")
    public Result<AddressVO> create(@RequestHeader("Authorization") String token,
                                     @Valid @RequestBody AddressRequest request) {
        return Result.ok(addressService.create(token, request));
    }

    @PutMapping("/addresses/{id}")
    public Result<AddressVO> update(@PathVariable Long id,
                                     @RequestHeader("Authorization") String token,
                                     @Valid @RequestBody AddressRequest request) {
        return Result.ok(addressService.update(token, id, request));
    }

    @DeleteMapping("/addresses/{id}")
    public Result<Void> delete(@PathVariable Long id,
                               @RequestHeader("Authorization") String token) {
        addressService.delete(token, id);
        return Result.ok();
    }

    @PutMapping("/addresses/{id}/default")
    public Result<Void> setDefault(@PathVariable Long id,
                                    @RequestHeader("Authorization") String token) {
        addressService.setDefault(token, id);
        return Result.ok();
    }
}
