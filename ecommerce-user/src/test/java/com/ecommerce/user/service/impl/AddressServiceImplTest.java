package com.ecommerce.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.common.util.JwtUtils;
import com.ecommerce.user.common.UserErrorCode;
import com.ecommerce.user.dto.request.AddressRequest;
import com.ecommerce.user.dto.response.AddressVO;
import com.ecommerce.user.entity.Address;
import com.ecommerce.user.mapper.AddressMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddressServiceImplTest {

    @Mock private AddressMapper addressMapper;
    @InjectMocks private AddressServiceImpl service;

    private String token;
    private Address addr;

    @BeforeEach
    void setUp() {
        token = "Bearer " + JwtUtils.generate(1L, "test", "user");
        addr = new Address();
        addr.setId(1L); addr.setUserId(1L);
        addr.setReceiverName("张三"); addr.setReceiverPhone("13800001111");
        addr.setProvince("北京市"); addr.setCity("北京市"); addr.setDistrict("朝阳区");
        addr.setDetail("望京街道100号"); addr.setIsDefault(1);
    }

    private AddressRequest req(String name, String phone, String detail) {
        AddressRequest r = new AddressRequest();
        r.setReceiverName(name); r.setReceiverPhone(phone); r.setDetail(detail);
        return r;
    }

    @Nested
    class ListTests {
        @Test
        void shouldListAddresses() {
            when(addressMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.singletonList(addr));
            List<AddressVO> list = service.listByToken(token);
            assertThat(list).hasSize(1);
            assertThat(list.get(0).getReceiverName()).isEqualTo("张三");
        }

        @Test
        void shouldReturnEmptyList() {
            when(addressMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());
            assertThat(service.listByToken(token)).isEmpty();
        }
    }

    @Nested
    class CreateTests {
        @Test
        void shouldCreateAddress() {
            when(addressMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());
            when(addressMapper.insert(any(Address.class))).thenReturn(1);
            AddressVO vo = service.create(token, req("李四", "13900001111", "北京市"));
            assertThat(vo.getReceiverName()).isEqualTo("李四");
        }

        @Test
        void shouldRejectWhenLimitExceeded() {
            List<Address> full = new ArrayList<>();
            for (int i = 0; i < 20; i++) full.add(new Address());
            when(addressMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(full);
            assertThatThrownBy(() -> service.create(token, req("x", "1", "x")))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(UserErrorCode.ADDRESS_LIMIT_EXCEEDED.getCode());
        }

        @Test
        void shouldSetDefaultForFirstAddress() {
            when(addressMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());
            when(addressMapper.insert(any(Address.class))).thenReturn(1);
            AddressVO vo = service.create(token, req("First", "1", "x"));
            assertThat(vo.getIsDefault()).isEqualTo(1);
        }
    }

    @Nested
    class UpdateTests {
        @Test
        void shouldUpdateAddress() {
            when(addressMapper.selectById(1L)).thenReturn(addr);
            when(addressMapper.updateById(any(Address.class))).thenReturn(1);
            AddressVO vo = service.update(1L, req("王五", "13700001111", "新地址"));
            assertThat(vo.getReceiverName()).isEqualTo("王五");
        }

        @Test
        void shouldThrowWhenUpdateNotFound() {
            when(addressMapper.selectById(999L)).thenReturn(null);
            assertThatThrownBy(() -> service.update(999L, req("x", "1", "x")))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(UserErrorCode.ADDRESS_NOT_FOUND.getCode());
        }
    }

    @Nested
    class DeleteTests {
        @Test
        void shouldDeleteAddress() {
            when(addressMapper.selectById(1L)).thenReturn(addr);
            when(addressMapper.deleteById(1L)).thenReturn(1);
            service.delete(1L);
            verify(addressMapper).deleteById(1L);
        }

        @Test
        void shouldThrowWhenDeleteNotFound() {
            when(addressMapper.selectById(999L)).thenReturn(null);
            assertThatThrownBy(() -> service.delete(999L))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    class SetDefaultTests {
        @Test
        void shouldThrowWhenAddressNotFound() {
            when(addressMapper.selectById(999L)).thenReturn(null);
            assertThatThrownBy(() -> service.setDefault(token, 999L))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    class BoundaryTests {
        @Test
        void shouldCreateWithLongFields() {
            when(addressMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());
            when(addressMapper.insert(any(Address.class))).thenReturn(1);
            assertThatCode(() -> service.create(token,
                    req("非常长长长长长长长长长收货人", "1", "非常长长长长长长长长长详细地址"))).doesNotThrowAnyException();
        }

        @Test
        void shouldCreateWithAllProvinceCityFields() {
            when(addressMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());
            when(addressMapper.insert(any(Address.class))).thenReturn(1);
            AddressRequest r = new AddressRequest();
            r.setReceiverName("完整"); r.setReceiverPhone("13912345678");
            r.setProvince("广东省"); r.setCity("深圳市"); r.setDistrict("南山区");
            r.setDetail("科技园路100号A栋502");
            AddressVO vo = service.create(token, r);
            assertThat(vo.getProvince()).isEqualTo("广东省");
        }

        @Test
        void shouldNotAutoDefaultWhenHasExisting() {
            when(addressMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.singletonList(addr));
            when(addressMapper.insert(any(Address.class))).thenReturn(1);
            AddressVO vo = service.create(token, req("Second", "2", "y"));
            assertThat(vo.getIsDefault()).isNull();
        }

        @Test
        void shouldHandleTokenWithoutBearer() {
            String raw = JwtUtils.generate(2L, "user2", "user");
            when(addressMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());
            assertThatCode(() -> service.listByToken(raw)).doesNotThrowAnyException();
        }

        @Test
        void shouldHandleExactly20Addresses() {
            List<Address> full = new ArrayList<>();
            for (int i = 0; i < 20; i++) full.add(new Address());
            when(addressMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(full);
            assertThatThrownBy(() -> service.create(token, req("21st", "1", "z")))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        void shouldHandle19AddressesAllowingCreate() {
            List<Address> almostFull = new ArrayList<>();
            for (int i = 0; i < 19; i++) almostFull.add(new Address());
            when(addressMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(almostFull);
            when(addressMapper.insert(any(Address.class))).thenReturn(1);
            assertThatCode(() -> service.create(token, req("20th", "1", "z")))
                    .doesNotThrowAnyException();
        }
    }
}
