package com.ecommerce.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.product.common.ProductErrorCode;
import com.ecommerce.product.entity.Brand;
import com.ecommerce.product.mapper.BrandMapper;
import com.ecommerce.product.service.BrandService;
import org.springframework.stereotype.Service;

@Service
public class BrandServiceImpl implements BrandService {

    private static final String SOURCE_PLATFORM = "platform";
    private static final String SOURCE_MERCHANT = "merchant";
    private static final String AUDIT_PENDING = "pending";
    private static final String AUDIT_APPROVED = "approved";
    private static final String AUDIT_REJECTED = "rejected";

    private final BrandMapper brandMapper;

    public BrandServiceImpl(BrandMapper brandMapper) {
        this.brandMapper = brandMapper;
    }

    @Override
    public Page<Brand> pageAll(int page, int size) {
        Page<Brand> pageReq = new Page<>(page, size);
        return brandMapper.selectPage(pageReq, new LambdaQueryWrapper<Brand>().orderByAsc(Brand::getName));
    }

    @Override
    public Page<Brand> pageForMerchant(Long merchantId, int page, int size) {
        Page<Brand> pageReq = new Page<>(page, size);
        LambdaQueryWrapper<Brand> wrapper = new LambdaQueryWrapper<Brand>()
                .and(query -> query.eq(Brand::getSourceType, SOURCE_PLATFORM)
                                .eq(Brand::getAuditStatus, AUDIT_APPROVED)
                        .or(or -> or.eq(Brand::getSourceType, SOURCE_MERCHANT)
                                .eq(Brand::getMerchantId, merchantId)))
                .orderByAsc(Brand::getSourceType)
                .orderByAsc(Brand::getName);
        return brandMapper.selectPage(pageReq, wrapper);
    }

    @Override
    public Brand getById(Long id) {
        Brand brand = brandMapper.selectById(id);
        if (brand == null) {
            throw new BusinessException(ProductErrorCode.BRAND_NOT_FOUND);
        }
        return brand;
    }

    @Override
    public Brand createPlatformBrand(Brand brand) {
        brand.setMerchantId(null);
        brand.setSourceType(SOURCE_PLATFORM);
        brand.setAuditStatus(AUDIT_APPROVED);
        brandMapper.insert(brand);
        return brand;
    }

    @Override
    public Brand createMerchantBrand(Long merchantId, Brand brand) {
        brand.setMerchantId(merchantId);
        brand.setSourceType(SOURCE_MERCHANT);
        brand.setAuditStatus(AUDIT_PENDING);
        brandMapper.insert(brand);
        return brand;
    }

    @Override
    public Brand updatePlatformBrand(Long id, Brand brand) {
        Brand existing = getById(id);
        existing.setName(brand.getName());
        existing.setLogo(brand.getLogo());
        existing.setDescription(brand.getDescription());
        brandMapper.updateById(existing);
        return brandMapper.selectById(id);
    }

    @Override
    public Brand updateMerchantBrand(Long merchantId, Long id, Brand brand) {
        Brand existing = requireMerchantOwnedBrand(merchantId, id);
        existing.setName(brand.getName());
        existing.setLogo(brand.getLogo());
        existing.setDescription(brand.getDescription());
        existing.setAuditStatus(AUDIT_PENDING);
        brandMapper.updateById(existing);
        return brandMapper.selectById(id);
    }

    @Override
    public void deletePlatformBrand(Long id) {
        getById(id);
        brandMapper.deleteById(id);
    }

    @Override
    public void deleteMerchantBrand(Long merchantId, Long id) {
        requireMerchantOwnedBrand(merchantId, id);
        brandMapper.deleteById(id);
    }

    @Override
    public Brand auditMerchantBrand(Long id, String status) {
        if (!AUDIT_APPROVED.equals(status) && !AUDIT_REJECTED.equals(status)) {
            throw new BusinessException(ProductErrorCode.BRAND_AUDIT_STATUS_INVALID);
        }
        Brand brand = getById(id);
        if (!SOURCE_MERCHANT.equals(brand.getSourceType())) {
            throw new BusinessException(ProductErrorCode.BRAND_FORBIDDEN);
        }
        brand.setAuditStatus(status);
        brandMapper.updateById(brand);
        return brandMapper.selectById(id);
    }

    @Override
    public void validateMerchantBrandSelectable(Long merchantId, Long brandId) {
        Brand brand = getById(brandId);
        boolean platformAllowed = SOURCE_PLATFORM.equals(brand.getSourceType())
                && AUDIT_APPROVED.equals(brand.getAuditStatus());
        boolean merchantAllowed = SOURCE_MERCHANT.equals(brand.getSourceType())
                && merchantId.equals(brand.getMerchantId())
                && AUDIT_APPROVED.equals(brand.getAuditStatus());
        if (!platformAllowed && !merchantAllowed) {
            throw new BusinessException(ProductErrorCode.BRAND_FORBIDDEN);
        }
    }

    private Brand requireMerchantOwnedBrand(Long merchantId, Long id) {
        Brand brand = getById(id);
        if (!SOURCE_MERCHANT.equals(brand.getSourceType()) || !merchantId.equals(brand.getMerchantId())) {
            throw new BusinessException(ProductErrorCode.BRAND_FORBIDDEN);
        }
        return brand;
    }
}
