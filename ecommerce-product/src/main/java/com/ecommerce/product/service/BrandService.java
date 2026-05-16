package com.ecommerce.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.product.entity.Brand;

public interface BrandService {

    Page<Brand> pageAll(int page, int size);

    Page<Brand> pageForMerchant(Long merchantId, int page, int size);

    Brand getById(Long id);

    Brand createPlatformBrand(Brand brand);

    Brand createMerchantBrand(Long merchantId, Brand brand);

    Brand updatePlatformBrand(Long id, Brand brand);

    Brand updateMerchantBrand(Long merchantId, Long id, Brand brand);

    void deletePlatformBrand(Long id);

    void deleteMerchantBrand(Long merchantId, Long id);

    Brand auditMerchantBrand(Long id, String status);

    void validateMerchantBrandSelectable(Long merchantId, Long brandId);
}
