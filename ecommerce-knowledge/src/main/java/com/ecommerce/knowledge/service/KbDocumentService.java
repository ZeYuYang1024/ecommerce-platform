package com.ecommerce.knowledge.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.knowledge.dto.request.CreateDocumentRequest;
import com.ecommerce.knowledge.dto.request.UpdateDocumentRequest;
import com.ecommerce.knowledge.dto.response.DocumentVO;

public interface KbDocumentService {
    DocumentVO createPlatform(CreateDocumentRequest request);
    DocumentVO updatePlatform(Long id, UpdateDocumentRequest request);
    void deletePlatform(Long id);
    DocumentVO getPlatformById(Long id);
    Page<DocumentVO> pagePlatform(int pageNum, int pageSize, Long categoryId, String status);
    void reindexPlatform(Long id);

    DocumentVO createForMerchant(Long merchantId, CreateDocumentRequest request);
    DocumentVO updateForMerchant(Long merchantId, Long id, UpdateDocumentRequest request);
    void deleteForMerchant(Long merchantId, Long id);
    DocumentVO getForMerchant(Long merchantId, Long id);
    Page<DocumentVO> pageForMerchant(Long merchantId, int pageNum, int pageSize, Long categoryId, String status);
    void reindexForMerchant(Long merchantId, Long id);
}
