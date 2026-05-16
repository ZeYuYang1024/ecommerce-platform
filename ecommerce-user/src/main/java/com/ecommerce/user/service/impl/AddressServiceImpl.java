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
    public List<AddressVO> listByUserId(Long userId) {
        List<Address> addresses = addressMapper.selectList(
                new LambdaQueryWrapper<Address>()
                        .eq(Address::getUserId, userId)
                        .orderByDesc(Address::getIsDefault));
        return toVOList(addresses);
    }

    @Override
    public List<AddressVO> listByToken(String token) {
        return listByUserId(extractUserId(token));
    }

    @Override
    @Transactional
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
        } else if (Integer.valueOf(1).equals(request.getIsDefault())) {
            clearDefaultForUser(userId);
        } else if (address.getIsDefault() == null) {
            address.setIsDefault(0);
        }
        addressMapper.insert(address);
        return toVO(address);
    }

    @Override
    @Transactional
    public AddressVO update(String token, Long id, AddressRequest request) {
        Address address = getOwnedAddress(token, id);
        applyRequest(address, request);
        if (Integer.valueOf(1).equals(request.getIsDefault())) {
            clearDefaultForUser(address.getUserId());
            address.setIsDefault(1);
        }
        addressMapper.updateById(address);
        return toVO(address);
    }

    @Override
    public void delete(String token, Long id) {
        Address address = getOwnedAddress(token, id);
        addressMapper.deleteById(address.getId());
    }

    @Transactional
    @Override
    public void setDefault(String token, Long id) {
        Long userId = extractUserId(token);
        Address addr = getOwnedAddress(token, id);
        clearDefaultForUser(userId);
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
            address.setIsDefault(normalizeDefaultFlag(request.getIsDefault()));
        }
    }

    private Integer normalizeDefaultFlag(Integer isDefault) {
        return Integer.valueOf(1).equals(isDefault) ? 1 : 0;
    }

    private Address getOwnedAddress(String token, Long id) {
        Long userId = extractUserId(token);
        Address address = addressMapper.selectById(id);
        if (address == null || !userId.equals(address.getUserId())) {
            throw new BusinessException(UserErrorCode.ADDRESS_NOT_FOUND);
        }
        return address;
    }

    private void clearDefaultForUser(Long userId) {
        addressMapper.update(null,
                new LambdaUpdateWrapper<Address>()
                        .eq(Address::getUserId, userId)
                        .set(Address::getIsDefault, 0));
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
