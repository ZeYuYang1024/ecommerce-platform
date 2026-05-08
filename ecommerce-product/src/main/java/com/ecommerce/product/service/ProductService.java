package com.ecommerce.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.product.dto.request.CreateProductRequest;
import com.ecommerce.product.dto.response.ProductDetailVO;
import com.ecommerce.product.entity.Category;
import com.ecommerce.product.entity.Sku;
import com.ecommerce.product.entity.Spu;

import java.util.List;

public interface ProductService {
    // Category
    List<Category> categoryTree();
    Category createCategory(Category category);
    Category updateCategory(Category category);
    void deleteCategory(Long id);

    // SPU
    Page<Spu> spuPage(int page, int size, Long categoryId, Integer status, String keyword);
    Spu getSpuById(Long id);
    ProductDetailVO getProductDetail(Long id);
    Spu createProduct(CreateProductRequest request);
    Spu updateSpu(Spu spu);
    void updateStatus(Long id, Integer status);
    void deleteSpu(Long id);

    // SKU
    List<Sku> getSkusBySpuId(Long spuId);
}
