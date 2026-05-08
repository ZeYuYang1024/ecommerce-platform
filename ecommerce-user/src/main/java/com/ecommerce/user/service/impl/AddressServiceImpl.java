package com.ecommerce.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.common.util.JwtUtils;
import com.ecommerce.common.util.SnowflakeUtils;
import com.ecommerce.user.common.UserErrorCode;
import com.ecommerce.user.dto.request.AddressRequest;
import com.ecommerce.user.dto.response.AddressVO;
import com.ecommerce.user.entity.Address;
import com.ecommerce.user.mapper.AddressMapper;
import com.ecommerce.user.service.AddressService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AddressServiceImpl implements AddressService {

    private final AddressMapper addressMapper;

    public AddressServiceImpl(AddressMapper addressMapper) {
        this.addressMapper = addressMapper;
    }

    @Override
    public List<AddressVO> listByToken(String token) {
        Long userId = extractUserId(token);
        List<Address> addresses = addressMapper.selectList(
                new LambdaQueryWrapper<Address>()
                        .eq(Address::getUserId, userId)
                        .orderByDesc(Address::getIsDefault));
        return toVOList(addresses);
    }

    @Override
    public AddressVO create(String token, AddressRequest request) {
        Long userId = extractUserId(token);
        List<Address> existing = addressMapper.selectList(
                new LambdaQueryWrapper<Address>().eq(Address::getUserId, userId));
        if (existing.size() >= 20) {
            throw new BusinessException(UserErrorCode.ADDRESS_LIMIT_EXCEEDED);
        }

        Address address = new Address();
        address.setId(SnowflakeUtils.nextId());
        address.setUserId(userId);
        applyRequest(address, request);
        if (existing.isEmpty()) {
            address.setIsDefault(1);
        }
        addressMapper.insert(address);
        return toVO(address);
    }

    @Override
    public AddressVO update(Long id, AddressRequest request) {
        Address address = addressMapper.selectById(id);
        if (address == null) {
            throw new BusinessException(UserErrorCode.ADDRESS_NOT_FOUND);
        }
        applyRequest(address, request);
        addressMapper.updateById(address);
        return toVO(address);
    }

    @Override
    public void delete(Long id) {
        if (addressMapper.selectById(id) == null) {
            throw new BusinessException(UserErrorCode.ADDRESS_NOT_FOUND);
        }
        addressMapper.deleteById(id);
    }

    @Transactional
    @Override
    public void setDefault(String token, Long id) {
        Long userId = extractUserId(token);
        Address addr = addressMapper.selectById(id);
        if (addr == null) {
            throw new BusinessException(UserErrorCode.ADDRESS_NOT_FOUND);
        }
        addressMapper.update(null,
                new LambdaUpdateWrapper<Address>()
                        .eq(Address::getUserId, userId)
                        .set(Address::getIsDefault, 0));
        addr.setIsDefault(1);
        addressMapper.updateById(addr);
    }

    private Long extractUserId(String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return JwtUtils.getUserId(token);
    }

    private void applyRequest(Address address, AddressRequest request) {
        address.setReceiverName(request.getReceiverName());
        address.setReceiverPhone(request.getReceiverPhone());
        address.setProvince(request.getProvince());
        address.setCity(request.getCity());
        address.setDistrict(request.getDistrict());
        address.setDetail(request.getDetail());
        if (request.getIsDefault() != null) {
            address.setIsDefault(request.getIsDefault());
        }
    }

    private AddressVO toVO(Address address) {
        AddressVO vo = new AddressVO();
        vo.setId(address.getId());
        vo.setUserId(address.getUserId());
        vo.setReceiverName(address.getReceiverName());
        vo.setReceiverPhone(address.getReceiverPhone());
        vo.setProvince(address.getProvince());
        vo.setCity(address.getCity());
        vo.setDistrict(address.getDistrict());
        vo.setDetail(address.getDetail());
        vo.setIsDefault(address.getIsDefault());
        vo.setCreatedAt(address.getCreatedAt());
        return vo;
    }

    private List<AddressVO> toVOList(List<Address> addresses) {
        List<AddressVO> vos = new ArrayList<>();
        for (Address a : addresses) {
            vos.add(toVO(a));
        }
        return vos;
    }
}
