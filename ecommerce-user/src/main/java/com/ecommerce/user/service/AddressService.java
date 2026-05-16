package com.ecommerce.user.service;

import com.ecommerce.user.dto.request.AddressRequest;
import com.ecommerce.user.dto.response.AddressVO;

import java.util.List;

public interface AddressService {
    List<AddressVO> listByUserId(Long userId);
    List<AddressVO> listByToken(String token);
    AddressVO create(String token, AddressRequest request);
    AddressVO update(String token, Long id, AddressRequest request);
    void delete(String token, Long id);
    void setDefault(String token, Long id);
}
