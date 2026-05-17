package com.ecommerce.knowledge.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.knowledge.dto.request.CreateCategoryRequest;
import com.ecommerce.knowledge.dto.response.CategoryVO;

import java.util.List;

public interface KbCategoryService {
    CategoryVO createPlatform(CreateCategoryRequest request);
    List<CategoryVO> listPlatform();
    void deletePlatform(Long id);

    CategoryVO createForMerchant(Long merchantId, CreateCategoryRequest request);
    List<CategoryVO> listForMerchant(Long merchantId);
    void deleteForMerchant(Long merchantId, Long id);
}
