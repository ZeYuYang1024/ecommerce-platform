package com.ecommerce.user.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.common.util.JwtUtils;
import com.ecommerce.user.common.UserErrorCode;
import com.ecommerce.user.dto.request.AddressRequest;
import com.ecommerce.user.dto.response.AddressVO;
import com.ecommerce.user.entity.Address;
import com.ecommerce.user.mapper.AddressMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressServiceImplTest {

    @Mock
    private AddressMapper addressMapper;

    @InjectMocks
    private AddressServiceImpl service;

    private String token;
    private Address addr;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Address.class);
        token = "Bearer " + JwtUtils.generate(1L, "test", "user");
        addr = address(1L, 1L, 1);
    }

    private AddressRequest req(String name, String phone, String detail) {
        AddressRequest request = new AddressRequest();
        request.setReceiverName(name);
        request.setReceiverPhone(phone);
        request.setProvince("Province");
        request.setCity("City");
        request.setDistrict("District");
        request.setDetail(detail);
        return request;
    }

    private Address address(Long id, Long userId, Integer isDefault) {
        Address address = new Address();
        address.setId(id);
        address.setUserId(userId);
        address.setReceiverName("Alice");
        address.setReceiverPhone("13800001111");
        address.setProvince("Province");
        address.setCity("City");
        address.setDistrict("District");
        address.setDetail("Road 100");
        address.setIsDefault(isDefault);
        return address;
    }

    @Nested
    class ListTests {
        @Test
        void shouldListAddresses() {
            when(addressMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.singletonList(addr));

            List<AddressVO> list = service.listByToken(token);

            assertThat(list).hasSize(1);
            assertThat(list.get(0).getReceiverName()).isEqualTo("Alice");
        }

        @Test
        @SuppressWarnings("unchecked")
        void shouldListAddressesByUserId() {
            when(addressMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.singletonList(addr));

            List<AddressVO> list = service.listByUserId(1L);

            ArgumentCaptor<LambdaQueryWrapper<Address>> queryCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
            verify(addressMapper).selectList(queryCaptor.capture());
            assertThat(list).hasSize(1);
            assertThat(list.get(0).getUserId()).isEqualTo(1L);
            assertThat(queryCaptor.getValue().getSqlSegment()).contains("user_id");
            assertThat(queryCaptor.getValue().getParamNameValuePairs()).containsValue(1L);
        }

        @Test
        void shouldReturnEmptyList() {
            when(addressMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());

            assertThat(service.listByToken(token)).isEmpty();
        }

        @Test
        void shouldReturnDefaultAddressByUserId() {
            when(addressMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(address(2L, 1L, 1), address(1L, 1L, 0)));

            AddressVO addressVO = service.getDefaultByUserId(1L);

            assertThat(addressVO).isNotNull();
            assertThat(addressVO.getId()).isEqualTo(2L);
            assertThat(addressVO.getIsDefault()).isEqualTo(1);
        }

        @Test
        void shouldReturnNullWhenNoDefaultAddressExists() {
            when(addressMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());

            assertThat(service.getDefaultByUserId(1L)).isNull();
        }
    }

    @Nested
    class CreateTests {
        @Test
        void shouldCreateAddress() {
            when(addressMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());
            when(addressMapper.insert(any(Address.class))).thenReturn(1);

            AddressVO vo = service.create(token, req("Bob", "13900001111", "Road 101"));

            assertThat(vo.getReceiverName()).isEqualTo("Bob");
        }

        @Test
        void shouldRejectWhenLimitExceeded() {
            List<Address> full = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                full.add(new Address());
            }
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

        @Test
        @SuppressWarnings("unchecked")
        void shouldClearPreviousDefaultWhenCreatingExplicitDefault() {
            Address existing = address(2L, 1L, 1);
            AddressRequest request = req("Second", "2", "y");
            request.setIsDefault(1);
            when(addressMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.singletonList(existing));
            when(addressMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);
            when(addressMapper.insert(any(Address.class))).thenReturn(1);

            AddressVO vo = service.create(token, request);

            ArgumentCaptor<LambdaUpdateWrapper<Address>> updateCaptor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
            verify(addressMapper).update(isNull(), updateCaptor.capture());
            assertThat(updateCaptor.getValue().getSqlSegment()).contains("user_id");
            assertThat(updateCaptor.getValue().getParamNameValuePairs()).containsValue(1L).containsValue(0);
            assertThat(vo.getIsDefault()).isEqualTo(1);
        }

        @Test
        void shouldNormalizeInvalidDefaultFlagWhenCreating() {
            AddressRequest request = req("Second", "2", "y");
            request.setIsDefault(2);
            when(addressMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.singletonList(addr));
            when(addressMapper.insert(any(Address.class))).thenReturn(1);

            AddressVO vo = service.create(token, request);

            ArgumentCaptor<Address> addressCaptor = ArgumentCaptor.forClass(Address.class);
            verify(addressMapper).insert(addressCaptor.capture());
            verify(addressMapper, never()).update(isNull(), any(LambdaUpdateWrapper.class));
            assertThat(addressCaptor.getValue().getIsDefault()).isEqualTo(0);
            assertThat(vo.getIsDefault()).isEqualTo(0);
        }
    }

    @Nested
    class UpdateTests {
        @Test
        void shouldUpdateAddress() {
            when(addressMapper.selectById(1L)).thenReturn(addr);
            when(addressMapper.updateById(any(Address.class))).thenReturn(1);

            AddressVO vo = service.update(token, 1L, req("Carol", "13700001111", "New detail"));

            assertThat(vo.getReceiverName()).isEqualTo("Carol");
        }

        @Test
        void shouldThrowWhenUpdateNotFound() {
            when(addressMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> service.update(token, 999L, req("x", "1", "x")))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(UserErrorCode.ADDRESS_NOT_FOUND.getCode());
        }

        @Test
        void shouldRejectCrossUserUpdate() {
            when(addressMapper.selectById(1L)).thenReturn(address(1L, 2L, 0));

            assertThatThrownBy(() -> service.update(token, 1L, req("x", "1", "x")))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(UserErrorCode.ADDRESS_NOT_FOUND.getCode());

            verify(addressMapper, never()).updateById(any(Address.class));
        }

        @Test
        void shouldClearPreviousDefaultWhenUpdatingToExplicitDefault() {
            Address target = address(1L, 1L, 0);
            AddressRequest request = req("Updated", "13700001111", "detail");
            request.setIsDefault(1);
            when(addressMapper.selectById(1L)).thenReturn(target);
            when(addressMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);
            when(addressMapper.updateById(any(Address.class))).thenReturn(1);

            AddressVO vo = service.update(token, 1L, request);

            verify(addressMapper).update(isNull(), any(LambdaUpdateWrapper.class));
            assertThat(vo.getIsDefault()).isEqualTo(1);
        }

        @Test
        void shouldNormalizeInvalidDefaultFlagWhenUpdating() {
            Address target = address(1L, 1L, 1);
            AddressRequest request = req("Updated", "13700001111", "detail");
            request.setIsDefault(-1);
            when(addressMapper.selectById(1L)).thenReturn(target);
            when(addressMapper.updateById(any(Address.class))).thenReturn(1);

            AddressVO vo = service.update(token, 1L, request);

            ArgumentCaptor<Address> addressCaptor = ArgumentCaptor.forClass(Address.class);
            verify(addressMapper).updateById(addressCaptor.capture());
            verify(addressMapper, never()).update(isNull(), any(LambdaUpdateWrapper.class));
            assertThat(addressCaptor.getValue().getIsDefault()).isEqualTo(0);
            assertThat(vo.getIsDefault()).isEqualTo(0);
        }
    }

    @Nested
    class DeleteTests {
        @Test
        void shouldDeleteAddress() {
            when(addressMapper.selectById(1L)).thenReturn(addr);
            when(addressMapper.deleteById(1L)).thenReturn(1);

            service.delete(token, 1L);

            verify(addressMapper).deleteById(1L);
        }

        @Test
        void shouldThrowWhenDeleteNotFound() {
            when(addressMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> service.delete(token, 999L))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        void shouldRejectCrossUserDelete() {
            when(addressMapper.selectById(1L)).thenReturn(address(1L, 2L, 0));

            assertThatThrownBy(() -> service.delete(token, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(UserErrorCode.ADDRESS_NOT_FOUND.getCode());

            verify(addressMapper, never()).deleteById(anyLong());
        }
    }

    @Nested
    class SetDefaultTests {
        @Test
        @SuppressWarnings("unchecked")
        void shouldSetDefaultForOwnedAddress() {
            Address target = address(1L, 1L, 0);
            when(addressMapper.selectById(1L)).thenReturn(target);
            when(addressMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);
            when(addressMapper.updateById(any(Address.class))).thenReturn(1);

            service.setDefault(token, 1L);

            ArgumentCaptor<LambdaUpdateWrapper<Address>> updateCaptor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
            ArgumentCaptor<Address> addressCaptor = ArgumentCaptor.forClass(Address.class);
            verify(addressMapper).update(isNull(), updateCaptor.capture());
            verify(addressMapper).updateById(addressCaptor.capture());
            assertThat(updateCaptor.getValue().getSqlSegment()).contains("user_id");
            assertThat(updateCaptor.getValue().getParamNameValuePairs()).containsValue(1L).containsValue(0);
            assertThat(addressCaptor.getValue().getIsDefault()).isEqualTo(1);
        }

        @Test
        void shouldThrowWhenAddressNotFound() {
            when(addressMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> service.setDefault(token, 999L))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        void shouldRejectCrossUserSetDefault() {
            when(addressMapper.selectById(1L)).thenReturn(address(1L, 2L, 0));

            assertThatThrownBy(() -> service.setDefault(token, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(UserErrorCode.ADDRESS_NOT_FOUND.getCode());

            verify(addressMapper, never()).update(isNull(), any(LambdaUpdateWrapper.class));
            verify(addressMapper, never()).updateById(any(Address.class));
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
                    req("VeryLongReceiverName", "1", "VeryLongDetailAddress")))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldCreateWithAllProvinceCityFields() {
            when(addressMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());
            when(addressMapper.insert(any(Address.class))).thenReturn(1);
            AddressRequest request = new AddressRequest();
            request.setReceiverName("Complete");
            request.setReceiverPhone("13912345678");
            request.setProvince("Guangdong");
            request.setCity("Shenzhen");
            request.setDistrict("Nanshan");
            request.setDetail("Science Park 100");

            AddressVO vo = service.create(token, request);

            assertThat(vo.getProvince()).isEqualTo("Guangdong");
        }

        @Test
        void shouldNotAutoDefaultWhenHasExisting() {
            when(addressMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.singletonList(addr));
            when(addressMapper.insert(any(Address.class))).thenReturn(1);

            AddressVO vo = service.create(token, req("Second", "2", "y"));

            assertThat(vo.getIsDefault()).isEqualTo(0);
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
            for (int i = 0; i < 20; i++) {
                full.add(new Address());
            }
            when(addressMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(full);

            assertThatThrownBy(() -> service.create(token, req("21st", "1", "z")))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        void shouldHandle19AddressesAllowingCreate() {
            List<Address> almostFull = new ArrayList<>();
            for (int i = 0; i < 19; i++) {
                almostFull.add(new Address());
            }
            when(addressMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(almostFull);
            when(addressMapper.insert(any(Address.class))).thenReturn(1);

            assertThatCode(() -> service.create(token, req("20th", "1", "z")))
                    .doesNotThrowAnyException();
        }
    }
}
