package com.ecommerce.logistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.logistics.common.LogisticsErrorCode;
import com.ecommerce.logistics.dto.response.LogisticsProviderVO;
import com.ecommerce.logistics.entity.LogisticsProvider;
import com.ecommerce.logistics.mapper.LogisticsProviderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogisticsProviderServiceImplTest {

    @Mock
    private LogisticsProviderMapper providerMapper;

    @InjectMocks
    private LogisticsProviderServiceImpl service;

    private LogisticsProvider provider;

    @BeforeEach
    void setUp() {
        provider = new LogisticsProvider();
        provider.setId(1L);
        provider.setProviderCode("SF");
        provider.setProviderName("顺丰速运");
        provider.setProviderLogo("https://example.com/sf.png");
        provider.setCustomerAccount("account-1");
        provider.setSupportWaybill(1);
        provider.setStatus(1);
        provider.setPriority(10);
    }

    @Nested
    class ListTests {

        @Test
        void shouldListProvidersWithPagination() {
            Page<LogisticsProvider> mockPage = new Page<>(1, 10, 1);
            mockPage.setRecords(Collections.singletonList(provider));
            when(providerMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(mockPage);

            IPage<LogisticsProviderVO> result = service.listProviders(1, 10);

            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getTotal()).isEqualTo(1);
            assertThat(result.getRecords().get(0).getProviderCode()).isEqualTo("SF");
            assertThat(result.getRecords().get(0).getProviderName()).isEqualTo("顺丰速运");
        }

        @Test
        void shouldReturnEmptyPage() {
            Page<LogisticsProvider> mockPage = new Page<>(1, 10, 0);
            mockPage.setRecords(Collections.emptyList());
            when(providerMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(mockPage);

            IPage<LogisticsProviderVO> result = service.listProviders(1, 10);

            assertThat(result.getRecords()).isEmpty();
            assertThat(result.getTotal()).isZero();
        }
    }

    @Nested
    class ListAllEnabledTests {

        @Test
        void shouldListAllEnabledProviders() {
            LogisticsProvider provider2 = new LogisticsProvider();
            provider2.setId(2L);
            provider2.setProviderCode("YTO");
            provider2.setProviderName("圆通速递");
            provider2.setStatus(1);
            when(providerMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(provider, provider2));

            List<LogisticsProviderVO> result = service.listAllEnabled();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getProviderCode()).isEqualTo("SF");
            assertThat(result.get(1).getProviderCode()).isEqualTo("YTO");
        }

        @Test
        void shouldReturnEmptyListWhenNoEnabledProviders() {
            when(providerMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());

            List<LogisticsProviderVO> result = service.listAllEnabled();

            assertThat(result).isEmpty();
        }

        @Test
        void shouldSetStatusTextForEnabledProvider() {
            provider.setStatus(1);
            when(providerMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(provider));

            List<LogisticsProviderVO> result = service.listAllEnabled();

            assertThat(result.get(0).getStatusText()).isEqualTo("启用");
        }

        @Test
        void shouldSetStatusTextForDisabledProvider() {
            provider.setStatus(0);
            when(providerMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(provider));

            List<LogisticsProviderVO> result = service.listAllEnabled();

            assertThat(result.get(0).getStatusText()).isEqualTo("停用");
        }
    }

    @Nested
    class GetProviderTests {

        @Test
        void shouldGetProviderById() {
            when(providerMapper.selectById(1L)).thenReturn(provider);

            LogisticsProviderVO result = service.getProvider(1L);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getProviderCode()).isEqualTo("SF");
            assertThat(result.getProviderName()).isEqualTo("顺丰速运");
        }

        @Test
        void shouldThrowWhenProviderNotFound() {
            when(providerMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> service.getProvider(999L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(LogisticsErrorCode.PROVIDER_NOT_FOUND.getCode());
        }
    }

    @Nested
    class CreateProviderTests {

        @Test
        void shouldCreateProvider() {
            when(providerMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(false);
            when(providerMapper.insert(any(LogisticsProvider.class))).thenReturn(1);

            LogisticsProviderVO vo = new LogisticsProviderVO();
            vo.setProviderCode("ZT");
            vo.setProviderName("中通快递");
            vo.setProviderLogo("https://example.com/zt.png");
            vo.setSupportWaybill(1);
            vo.setStatus(1);
            vo.setPriority(5);

            LogisticsProviderVO result = service.createProvider(vo);

            assertThat(result.getProviderCode()).isEqualTo("ZT");
            assertThat(result.getProviderName()).isEqualTo("中通快递");
            assertThat(result.getStatus()).isEqualTo(1);
            verify(providerMapper).insert(any(LogisticsProvider.class));
        }

        @Test
        void shouldCreateProviderWithDefaultStatus() {
            when(providerMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(false);
            when(providerMapper.insert(any(LogisticsProvider.class))).thenReturn(1);

            LogisticsProviderVO vo = new LogisticsProviderVO();
            vo.setProviderCode("YD");
            vo.setProviderName("韵达快递");
            // status not set — should default to 1

            LogisticsProviderVO result = service.createProvider(vo);

            assertThat(result.getStatus()).isEqualTo(1);
        }

        @Test
        void shouldCreateProviderWithDefaultPriority() {
            when(providerMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(false);
            when(providerMapper.insert(any(LogisticsProvider.class))).thenReturn(1);

            LogisticsProviderVO vo = new LogisticsProviderVO();
            vo.setProviderCode("DB");
            vo.setProviderName("德邦快递");
            // priority not set — should default to 99

            LogisticsProviderVO result = service.createProvider(vo);

            assertThat(result.getPriority()).isEqualTo(99);
        }

        @Test
        void shouldRejectDuplicateProviderCode() {
            when(providerMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(true);

            LogisticsProviderVO vo = new LogisticsProviderVO();
            vo.setProviderCode("SF");

            assertThatThrownBy(() -> service.createProvider(vo))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(LogisticsErrorCode.PROVIDER_CODE_EXISTS.getCode());

            verify(providerMapper, never()).insert(any(LogisticsProvider.class));
        }
    }

    @Nested
    class UpdateProviderTests {

        @Test
        void shouldUpdateAllFields() {
            when(providerMapper.selectById(1L)).thenReturn(provider);
            when(providerMapper.updateById(any(LogisticsProvider.class))).thenReturn(1);

            LogisticsProviderVO vo = new LogisticsProviderVO();
            vo.setProviderName("顺丰速运(已更新)");
            vo.setProviderLogo("https://example.com/sf-new.png");
            vo.setSupportWaybill(0);
            vo.setPriority(1);

            LogisticsProviderVO result = service.updateProvider(1L, vo);

            assertThat(result.getProviderName()).isEqualTo("顺丰速运(已更新)");
            verify(providerMapper).updateById(any(LogisticsProvider.class));
        }

        @Test
        void shouldUpdatePartialFields() {
            when(providerMapper.selectById(1L)).thenReturn(provider);
            when(providerMapper.updateById(any(LogisticsProvider.class))).thenReturn(1);

            LogisticsProviderVO vo = new LogisticsProviderVO();
            vo.setPriority(20);
            // only update priority, other fields unchanged

            LogisticsProviderVO result = service.updateProvider(1L, vo);

            assertThat(result.getPriority()).isEqualTo(20);
            assertThat(result.getProviderName()).isEqualTo("顺丰速运");
            verify(providerMapper).updateById(any(LogisticsProvider.class));
        }

        @Test
        void shouldRejectUpdateForNonexistentProvider() {
            when(providerMapper.selectById(999L)).thenReturn(null);

            LogisticsProviderVO vo = new LogisticsProviderVO();
            vo.setProviderName("不存在");

            assertThatThrownBy(() -> service.updateProvider(999L, vo))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(LogisticsErrorCode.PROVIDER_NOT_FOUND.getCode());

            verify(providerMapper, never()).updateById(any(LogisticsProvider.class));
        }
    }

    @Nested
    class DeleteProviderTests {

        @Test
        void shouldDeleteProvider() {
            when(providerMapper.selectById(1L)).thenReturn(provider);
            when(providerMapper.deleteById(1L)).thenReturn(1);

            service.deleteProvider(1L);

            verify(providerMapper).deleteById(1L);
        }

        @Test
        void shouldThrowWhenDeletingNonexistentProvider() {
            when(providerMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> service.deleteProvider(999L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(LogisticsErrorCode.PROVIDER_NOT_FOUND.getCode());

            verify(providerMapper, never()).deleteById(anyLong());
        }
    }

    @Nested
    class ToggleStatusTests {

        @Test
        void shouldEnableProvider() {
            provider.setStatus(0);
            when(providerMapper.selectById(1L)).thenReturn(provider);
            when(providerMapper.updateById(any(LogisticsProvider.class))).thenReturn(1);

            service.toggleStatus(1L, 1);

            assertThat(provider.getStatus()).isEqualTo(1);
            verify(providerMapper).updateById(provider);
        }

        @Test
        void shouldDisableProvider() {
            provider.setStatus(1);
            when(providerMapper.selectById(1L)).thenReturn(provider);
            when(providerMapper.updateById(any(LogisticsProvider.class))).thenReturn(1);

            service.toggleStatus(1L, 0);

            assertThat(provider.getStatus()).isEqualTo(0);
            verify(providerMapper).updateById(provider);
        }

        @Test
        void shouldThrowWhenTogglingNonexistentProvider() {
            when(providerMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> service.toggleStatus(999L, 1))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(LogisticsErrorCode.PROVIDER_NOT_FOUND.getCode());

            verify(providerMapper, never()).updateById(any(LogisticsProvider.class));
        }
    }
}
