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
import com.ecommerce.common.dto.ProductCreatedMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProductServiceImpl implements ProductService {

    private final CategoryMapper categoryMapper;
    private final SpuMapper spuMapper;
    private final SkuMapper skuMapper;
    private final RocketMQTemplate rocketMQTemplate;

    public ProductServiceImpl(CategoryMapper categoryMapper, SpuMapper spuMapper, SkuMapper skuMapper,
                              RocketMQTemplate rocketMQTemplate) {
        this.categoryMapper = categoryMapper;
        this.spuMapper = spuMapper;
        this.skuMapper = skuMapper;
        this.rocketMQTemplate = rocketMQTemplate;
    }

    @Override
    public List<Category> categoryList() {
        return categoryMapper.selectList(
                new LambdaQueryWrapper<Category>().orderByAsc(Category::getSort));
    }

    public List<Category> categoryTree() {
        List<Category> all = categoryList();
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
        if (categoryId != null) {
            List<Long> catIds = collectCategoryIds(categoryId);
            wrapper.in(Spu::getCategoryId, catIds);
        }
        if (status != null) wrapper.eq(Spu::getStatus, status);
        if (keyword != null && !keyword.isEmpty()) wrapper.like(Spu::getName, keyword);
        wrapper.orderByDesc(Spu::getCreatedAt);
        return spuMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public Page<Spu> spuPageByMerchant(int page, int size, Long categoryId, Integer status, String keyword, Long merchantId) {
        LambdaQueryWrapper<Spu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Spu::getMerchantId, merchantId);
        if (categoryId != null) {
            List<Long> catIds = collectCategoryIds(categoryId);
            wrapper.in(Spu::getCategoryId, catIds);
        }
        if (status != null) wrapper.eq(Spu::getStatus, status);
        if (keyword != null && !keyword.isEmpty()) wrapper.like(Spu::getName, keyword);
        wrapper.orderByDesc(Spu::getCreatedAt);
        return spuMapper.selectPage(new Page<>(page, size), wrapper);
    }

    private List<Long> collectCategoryIds(Long parentId) {
        List<Long> ids = new java.util.ArrayList<>();
        ids.add(parentId);
        List<Category> all = categoryList();
        collectChildIds(parentId, all, ids, new java.util.HashSet<>());
        return ids;
    }

    private void collectChildIds(Long parentId, List<Category> all, List<Long> result, java.util.Set<Long> visited) {
        if (!visited.add(parentId)) return; // prevent infinite recursion on circular refs
        for (Category c : all) {
            if (parentId.equals(c.getParentId())) {
                result.add(c.getId());
                collectChildIds(c.getId(), all, result, visited);
            }
        }
    }

    @Override
    public List<Spu> getSpusByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return spuMapper.selectBatchIds(ids);
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

                // 初始化库存为 0（best-effort）
                try { rocketMQTemplate.syncSend("product-created", new ProductCreatedMessage(spu.getId(), sku.getId())); } catch (Exception e) { log.error("MQ product-created failed", e); }
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
        vo.setMerchantId(spu.getMerchantId());
        vo.setDescription(spu.getDescription());
        vo.setMainImage(spu.getMainImage());
        vo.setImages(spu.getImages());
        vo.setDetail(spu.getDetail());
        vo.setStatus(spu.getStatus());
        vo.setAvgRating(spu.getAvgRating());
        vo.setReviewCount(spu.getReviewCount());
        vo.setCreatedAt(spu.getCreatedAt());

        List<Sku> skus = getSkusBySpuId(spu.getId());
        if (skus != null && !skus.isEmpty()) {
            BigDecimal min = null, max = null;
            for (Sku sku : skus) {
                BigDecimal price = sku.getPrice();
                if (price == null) continue;
                if (min == null || price.compareTo(min) < 0) min = price;
                if (max == null || price.compareTo(max) > 0) max = price;
            }
            vo.setMinPrice(min);
            vo.setMaxPrice(max);
        }
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
        // 尝试清理库存记录（best-effort）
        List<Sku> skus = skuMapper.selectList(new LambdaQueryWrapper<Sku>().eq(Sku::getSpuId, id));
        for (Sku sku : skus) {
            try { rocketMQTemplate.syncSend("product-created", new ProductCreatedMessage(id, sku.getId())); } catch (Exception e) { log.error("MQ product-created failed", e); }
        }
        skuMapper.delete(new LambdaQueryWrapper<Sku>().eq(Sku::getSpuId, id));
        spuMapper.deleteById(id);
    }

    @Override
    public List<Sku> getSkusBySpuId(Long spuId) {
        return skuMapper.selectList(
                new LambdaQueryWrapper<Sku>().eq(Sku::getSpuId, spuId));
    }

    @Override
    public List<Sku> getSkusByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return skuMapper.selectBatchIds(ids);
    }

    @Override
    public List<Long> getSpuIdsByMerchant(Long merchantId) {
        List<Spu> spus = spuMapper.selectList(
                new LambdaQueryWrapper<Spu>().eq(Spu::getMerchantId, merchantId));
        return spus.stream().map(Spu::getId).collect(java.util.stream.Collectors.toList());
    }

    @Override
    public List<Long> getSkuIdsByMerchant(Long merchantId) {
        List<Long> spuIds = getSpuIdsByMerchant(merchantId);
        if (spuIds.isEmpty()) {
            return List.of();
        }
        return skuMapper.selectList(new LambdaQueryWrapper<Sku>().in(Sku::getSpuId, spuIds))
                .stream()
                .map(Sku::getId)
                .toList();
    }

    public long countAll() {
        return spuMapper.selectCount(new LambdaQueryWrapper<>());
    }
}
