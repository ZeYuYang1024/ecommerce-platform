package com.ecommerce.knowledge.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.knowledge.dto.request.CreateDocumentRequest;
import com.ecommerce.knowledge.dto.request.UpdateDocumentRequest;
import com.ecommerce.knowledge.dto.response.DocumentVO;

public interface KbDocumentService {
    DocumentVO create(CreateDocumentRequest request);
    DocumentVO update(Long id, UpdateDocumentRequest request);
    void delete(Long id);
    DocumentVO getById(Long id);
    Page<DocumentVO> page(int pageNum, int pageSize, Long categoryId, String status);
    void reindex(Long id);
}
