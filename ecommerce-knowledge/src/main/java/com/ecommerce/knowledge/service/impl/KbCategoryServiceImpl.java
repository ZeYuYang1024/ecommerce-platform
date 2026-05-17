package com.ecommerce.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.knowledge.common.BusinessException;
import com.ecommerce.knowledge.common.KnowledgeErrorCode;
import com.ecommerce.knowledge.dto.request.CreateCategoryRequest;
import com.ecommerce.knowledge.dto.response.CategoryVO;
import com.ecommerce.knowledge.entity.KbCategory;
import com.ecommerce.knowledge.entity.KbDocument;
import com.ecommerce.knowledge.mapper.KbCategoryMapper;
import com.ecommerce.knowledge.mapper.KbDocumentMapper;
import com.ecommerce.knowledge.service.KbCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KbCategoryServiceImpl implements KbCategoryService {

    private static final String OWNER_PLATFORM = "platform";
    private static final String OWNER_MERCHANT = "merchant";

    private final KbCategoryMapper categoryMapper;
    private final KbDocumentMapper documentMapper;

    @Override
    public CategoryVO createPlatform(CreateCategoryRequest request) {
        return createInternal(request, OWNER_PLATFORM, null);
    }

    @Override
    public List<CategoryVO> listPlatform() {
        return listInternal(OWNER_PLATFORM, null);
    }

    @Override
    public void deletePlatform(Long id) {
        deleteInternal(id, OWNER_PLATFORM, null);
    }

    @Override
    public CategoryVO createForMerchant(Long merchantId, CreateCategoryRequest request) {
        return createInternal(request, OWNER_MERCHANT, merchantId);
    }

    @Override
    public List<CategoryVO> listForMerchant(Long merchantId) {
        return listInternal(OWNER_MERCHANT, merchantId);
    }

    @Override
    public void deleteForMerchant(Long merchantId, Long id) {
        deleteInternal(id, OWNER_MERCHANT, merchantId);
    }

    private CategoryVO createInternal(CreateCategoryRequest request, String ownerType, Long merchantId) {
        KbCategory category = new KbCategory();
        category.setName(request.getName());
        category.setCode(request.getCode());
        category.setParentId(request.getParentId() != null ? request.getParentId() : 0L);
        category.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        category.setOwnerType(ownerType);
        category.setMerchantId(merchantId);
        if (category.getParentId() != null && category.getParentId() > 0) {
            loadOwnedCategory(category.getParentId(), ownerType, merchantId);
        }
        categoryMapper.insert(category);
        return toVO(category);
    }

    private List<CategoryVO> listInternal(String ownerType, Long merchantId) {
        return categoryMapper.selectList(new LambdaQueryWrapper<KbCategory>()
                        .eq(KbCategory::getOwnerType, ownerType)
                        .eq(merchantId != null, KbCategory::getMerchantId, merchantId)
                        .orderByAsc(KbCategory::getSortOrder))
                .stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    private void deleteInternal(Long id, String ownerType, Long merchantId) {
        KbCategory category = loadOwnedCategory(id, ownerType, merchantId);
        Long documentCount = documentMapper.selectCount(new LambdaQueryWrapper<KbDocument>()
                .eq(KbDocument::getCategoryId, category.getId())
                .eq(KbDocument::getOwnerType, ownerType)
                .eq(merchantId != null, KbDocument::getMerchantId, merchantId));
        if (documentCount != null && documentCount > 0) {
            throw new BusinessException(KnowledgeErrorCode.CATEGORY_HAS_DOCUMENTS);
        }
        categoryMapper.deleteById(id);
    }

    private KbCategory loadOwnedCategory(Long id, String ownerType, Long merchantId) {
        KbCategory category = categoryMapper.selectById(id);
        if (category == null || !ownerType.equals(category.getOwnerType())
                || (merchantId != null && !merchantId.equals(category.getMerchantId()))) {
            throw new BusinessException(KnowledgeErrorCode.CATEGORY_NOT_FOUND);
        }
        return category;
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
