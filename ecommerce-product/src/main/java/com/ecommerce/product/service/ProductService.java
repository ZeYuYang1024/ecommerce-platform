package com.ecommerce.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.product.dto.request.CreateProductRequest;
import com.ecommerce.product.dto.response.ProductDetailVO;
import com.ecommerce.product.dto.response.SpuVO;
import com.ecommerce.product.entity.Category;
import com.ecommerce.product.entity.Sku;
import com.ecommerce.product.entity.Spu;

import java.util.List;

public interface ProductService {
    // Category
    List<Category> categoryTree();
    List<Category> categoryList();
    Category createCategory(Category category);
    Category updateCategory(Category category);
    void deleteCategory(Long id);

    // SPU
    Page<Spu> spuPage(int page, int size, Long categoryId, Integer status, String keyword);
    Page<Spu> spuPageByMerchant(int page, int size, Long categoryId, Integer status, String keyword, Long merchantId);
    Spu getSpuById(Long id);
    List<Spu> getSpusByIds(List<Long> ids);
    ProductDetailVO getProductDetail(Long id);
    SpuVO toSpuVO(Spu spu);
    Spu createProduct(CreateProductRequest request);
    Spu updateSpu(Spu spu);
    void updateStatus(Long id, Integer status);
    void deleteSpu(Long id);

    // SKU
    List<Sku> getSkusBySpuId(Long spuId);
    List<Sku> getSkusByIds(List<Long> ids);
    long countAll();
    List<Long> getSpuIdsByMerchant(Long merchantId);
    List<Long> getSkuIdsByMerchant(Long merchantId);
}
