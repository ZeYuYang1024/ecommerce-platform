package com.ecommerce.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.common.util.SnowflakeUtils;
import com.ecommerce.product.common.ProductErrorCode;
import com.ecommerce.product.entity.Category;
import com.ecommerce.product.entity.Sku;
import com.ecommerce.product.entity.Spu;
import com.ecommerce.product.mapper.CategoryMapper;
import com.ecommerce.product.mapper.SkuMapper;
import com.ecommerce.product.mapper.SpuMapper;
import com.ecommerce.product.dto.request.CreateProductRequest;
import com.ecommerce.product.dto.response.ProductDetailVO;
import com.ecommerce.product.dto.response.SkuVO;
import com.ecommerce.product.dto.response.SpuVO;
import com.ecommerce.product.service.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    private final CategoryMapper categoryMapper;
    private final SpuMapper spuMapper;
    private final SkuMapper skuMapper;

    public ProductServiceImpl(CategoryMapper categoryMapper, SpuMapper spuMapper, SkuMapper skuMapper) {
        this.categoryMapper = categoryMapper;
        this.spuMapper = spuMapper;
        this.skuMapper = skuMapper;
    }

    @Override
    public List<Category> categoryTree() {
        List<Category> all = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>().orderByAsc(Category::getSort));
        Map<Long, List<Category>> childrenMap = all.stream()
                .filter(c -> c.getParentId() != null && c.getParentId() > 0)
                .collect(Collectors.groupingBy(Category::getParentId));
        for (Category c : all) {
            c.setChildren(childrenMap.getOrDefault(c.getId(), java.util.Collections.emptyList()));
        }
        List<Category> roots = new ArrayList<>();
        for (Category c : all) {
            if (c.getParentId() == null || c.getParentId() == 0) {
                roots.add(c);
            }
        }
        return roots;
    }

    @Override
    public Category createCategory(Category category) {
        category.setId(SnowflakeUtils.nextId());
        if (category.getLevel() == null) category.setLevel(1);
        if (category.getSort() == null) category.setSort(0);
        categoryMapper.insert(category);
        return category;
    }

    @Override
    public Category updateCategory(Category category) {
        categoryMapper.updateById(category);
        return category;
    }

    @Override
    public void deleteCategory(Long id) {
        categoryMapper.deleteById(id);
    }

    @Override
    public Page<Spu> spuPage(int page, int size, Long categoryId, Integer status, String keyword) {
        LambdaQueryWrapper<Spu> wrapper = new LambdaQueryWrapper<>();
        if (categoryId != null) wrapper.eq(Spu::getCategoryId, categoryId);
        if (status != null) wrapper.eq(Spu::getStatus, status);
        if (keyword != null && !keyword.isEmpty()) wrapper.like(Spu::getName, keyword);
        wrapper.orderByDesc(Spu::getCreatedAt);
        return spuMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public Spu getSpuById(Long id) {
        Spu spu = spuMapper.selectById(id);
        if (spu == null) throw new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND);
        return spu;
    }

    @Override
    public ProductDetailVO getProductDetail(Long id) {
        Spu spu = getSpuById(id);
        List<Sku> skus = getSkusBySpuId(id);
        return toProductDetailVO(spu, skus);
    }

    @Override
    @Transactional
    public Spu createProduct(CreateProductRequest request) {
        Spu spu = new Spu();
        spu.setName(request.getSpu().getName());
        spu.setCategoryId(request.getSpu().getCategoryId());
        spu.setBrandId(request.getSpu().getBrandId());
        spu.setDescription(request.getSpu().getDescription());
        spu.setMainImage(request.getSpu().getMainImage());
        spu.setImages(request.getSpu().getImages());
        spu.setDetail(request.getSpu().getDetail());

        spu.setId(SnowflakeUtils.nextId());
        spuMapper.insert(spu);

        if (request.getSkus() != null) {
            for (CreateProductRequest.SkuRequest sr : request.getSkus()) {
                Sku sku = new Sku();
                sku.setId(SnowflakeUtils.nextId());
                sku.setSpuId(spu.getId());
                sku.setName(sr.getName());
                sku.setSpec(sr.getSpec());
                if (sr.getPrice() != null && !sr.getPrice().isEmpty()) {
                    try {
                        sku.setPrice(new java.math.BigDecimal(sr.getPrice()));
                    } catch (NumberFormatException e) {
                        throw new BusinessException(ProductErrorCode.INVALID_PRICE_FORMAT);
                    }
                }
                if (sr.getOriginalPrice() != null && !sr.getOriginalPrice().isEmpty()) {
                    try {
                        sku.setOriginalPrice(new java.math.BigDecimal(sr.getOriginalPrice()));
                    } catch (NumberFormatException e) {
                        throw new BusinessException(ProductErrorCode.INVALID_PRICE_FORMAT);
                    }
                }
                sku.setImage(sr.getImage());
                skuMapper.insert(sku);
            }
        }
        return spu;
    }

    @Override
    public Spu updateSpu(Spu spu) {
        spuMapper.updateById(spu);
        return spu;
    }

    @Override
    public SpuVO toSpuVO(Spu spu) {
        SpuVO vo = new SpuVO();
        vo.setId(spu.getId());
        vo.setName(spu.getName());
        vo.setCategoryId(spu.getCategoryId());
        vo.setBrandId(spu.getBrandId());
        vo.setDescription(spu.getDescription());
        vo.setMainImage(spu.getMainImage());
        vo.setImages(spu.getImages());
        vo.setDetail(spu.getDetail());
        vo.setStatus(spu.getStatus());
        vo.setAvgRating(spu.getAvgRating());
        vo.setReviewCount(spu.getReviewCount());
        vo.setCreatedAt(spu.getCreatedAt());
        return vo;
    }

    @Transactional
    @Override
    public void updateStatus(Long id, Integer status) {
        Spu spu = getSpuById(id);
        spu.setStatus(status);
        spuMapper.updateById(spu);
    }

    private ProductDetailVO toProductDetailVO(Spu spu, List<Sku> skus) {
        List<SkuVO> skuVOs = new ArrayList<>();
        if (skus != null) {
            for (Sku sku : skus) {
                SkuVO vo = new SkuVO();
                vo.setId(sku.getId());
                vo.setSpuId(sku.getSpuId());
                vo.setName(sku.getName());
                vo.setSpec(sku.getSpec());
                vo.setPrice(sku.getPrice());
                vo.setOriginalPrice(sku.getOriginalPrice());
                vo.setImage(sku.getImage());
                skuVOs.add(vo);
            }
        }

        ProductDetailVO vo = new ProductDetailVO();
        vo.setSpu(toSpuVO(spu));
        vo.setSkus(skuVOs);
        return vo;
    }

    @Transactional
    @Override
    public void deleteSpu(Long id) {
        skuMapper.delete(new LambdaQueryWrapper<Sku>().eq(Sku::getSpuId, id));
        spuMapper.deleteById(id);
    }

    @Override
    public List<Sku> getSkusBySpuId(Long spuId) {
        return skuMapper.selectList(
                new LambdaQueryWrapper<Sku>().eq(Sku::getSpuId, spuId));
    }
}
