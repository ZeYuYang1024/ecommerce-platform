package com.ecommerce.knowledge.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.knowledge.dto.request.CreateCategoryRequest;
import com.ecommerce.knowledge.dto.response.CategoryVO;

import java.util.List;

public interface KbCategoryService {
    CategoryVO create(CreateCategoryRequest request);
    List<CategoryVO> listAll();
    void delete(Long id);
}
