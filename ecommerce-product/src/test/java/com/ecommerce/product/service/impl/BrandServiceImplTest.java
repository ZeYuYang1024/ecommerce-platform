package com.ecommerce.product.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.product.entity.Brand;
import com.ecommerce.product.mapper.BrandMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrandServiceImplTest {

    @Mock
    private BrandMapper brandMapper;

    @InjectMocks
    private BrandServiceImpl brandService;

    @Test
    void createMerchantBrand_shouldAssignPendingTenantFields() {
        Brand request = new Brand();
        request.setName("商家品牌A");

        when(brandMapper.insert(any(Brand.class))).thenReturn(1);

        Brand created = brandService.createMerchantBrand(2001L, request);

        assertThat(created.getMerchantId()).isEqualTo(2001L);
        assertThat(created.getSourceType()).isEqualTo("merchant");
        assertThat(created.getAuditStatus()).isEqualTo("pending");
        verify(brandMapper).insert(any(Brand.class));
    }

    @Test
    void validateMerchantBrandSelectable_shouldRejectPendingMerchantBrand() {
        Brand brand = new Brand();
        brand.setId(3001L);
        brand.setMerchantId(2001L);
        brand.setSourceType("merchant");
        brand.setAuditStatus("pending");
        when(brandMapper.selectById(3001L)).thenReturn(brand);

        assertThatThrownBy(() -> brandService.validateMerchantBrandSelectable(2001L, 3001L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void validateMerchantBrandSelectable_shouldAllowApprovedPlatformBrand() {
        Brand brand = new Brand();
        brand.setId(3002L);
        brand.setSourceType("platform");
        brand.setAuditStatus("approved");
        when(brandMapper.selectById(3002L)).thenReturn(brand);

        brandService.validateMerchantBrandSelectable(2001L, 3002L);
    }

    @Test
    void pageForMerchant_shouldQueryVisibleBrands() {
        Page<Brand> page = new Page<>(1, 10);
        page.setRecords(List.of(new Brand()));
        when(brandMapper.selectPage(any(Page.class), any())).thenReturn(page);

        Page<Brand> result = brandService.pageForMerchant(2001L, 1, 10);

        assertThat(result.getRecords()).hasSize(1);
        ArgumentCaptor<Page<Brand>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(brandMapper).selectPage(pageCaptor.capture(), any());
        assertThat(pageCaptor.getValue().getCurrent()).isEqualTo(1);
    }

    @Test
    void updateMerchantBrand_shouldRejectCrossTenantUpdate() {
        Brand existing = new Brand();
        existing.setId(3003L);
        existing.setMerchantId(2002L);
        existing.setSourceType("merchant");
        when(brandMapper.selectById(3003L)).thenReturn(existing);

        Brand incoming = new Brand();
        incoming.setName("新名字");

        assertThatThrownBy(() -> brandService.updateMerchantBrand(2001L, 3003L, incoming))
                .isInstanceOf(BusinessException.class);
    }
}
