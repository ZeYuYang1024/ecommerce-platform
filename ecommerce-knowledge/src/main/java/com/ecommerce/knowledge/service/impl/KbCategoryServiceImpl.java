package com.ecommerce.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.knowledge.common.BusinessException;
import com.ecommerce.knowledge.common.KnowledgeErrorCode;
import com.ecommerce.knowledge.dto.request.CreateCategoryRequest;
import com.ecommerce.knowledge.dto.response.CategoryVO;
import com.ecommerce.knowledge.entity.KbCategory;
import com.ecommerce.knowledge.mapper.KbCategoryMapper;
import com.ecommerce.knowledge.service.KbCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KbCategoryServiceImpl implements KbCategoryService {

    private final KbCategoryMapper categoryMapper;

    @Override
    public CategoryVO create(CreateCategoryRequest request) {
        KbCategory category = new KbCategory();
        category.setName(request.getName());
        category.setCode(request.getCode());
        category.setParentId(request.getParentId() != null ? request.getParentId() : 0L);
        category.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        categoryMapper.insert(category);
        return toVO(category);
    }

    @Override
    public List<CategoryVO> listAll() {
        return categoryMapper.selectList(new LambdaQueryWrapper<KbCategory>()
                        .orderByAsc(KbCategory::getSortOrder))
                .stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(Long id) {
        KbCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(KnowledgeErrorCode.CATEGORY_NOT_FOUND);
        }
        categoryMapper.deleteById(id);
    }

    private CategoryVO toVO(KbCategory category) {
        CategoryVO vo = new CategoryVO();
        vo.setId(category.getId());
        vo.setName(category.getName());
        vo.setCode(category.getCode());
        vo.setParentId(category.getParentId());
        vo.setSortOrder(category.getSortOrder());
        vo.setCreateTime(category.getCreateTime());
        return vo;
    }
}
