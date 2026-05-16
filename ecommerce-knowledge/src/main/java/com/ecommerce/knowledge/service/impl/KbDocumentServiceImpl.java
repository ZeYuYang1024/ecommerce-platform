package com.ecommerce.knowledge.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.knowledge.common.BusinessException;
import com.ecommerce.knowledge.common.KnowledgeErrorCode;
import com.ecommerce.knowledge.dto.request.CreateDocumentRequest;
import com.ecommerce.knowledge.dto.request.UpdateDocumentRequest;
import com.ecommerce.knowledge.dto.response.DocumentVO;
import com.ecommerce.knowledge.entity.KbCategory;
import com.ecommerce.knowledge.entity.KbDocument;
import com.ecommerce.knowledge.mapper.KbCategoryMapper;
import com.ecommerce.knowledge.mapper.KbDocumentMapper;
import com.ecommerce.knowledge.service.DocumentIngestionService;
import com.ecommerce.knowledge.service.KbDocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KbDocumentServiceImpl implements KbDocumentService {

    private final KbDocumentMapper documentMapper;
    private final KbCategoryMapper categoryMapper;
    private final DocumentIngestionService ingestionService;

    @Override
    @Transactional
    public DocumentVO create(CreateDocumentRequest request) {
        try {
            KbDocument doc = new KbDocument();
            doc.setCategoryId(request.getCategoryId());
            doc.setTitle(request.getTitle());
            doc.setContent(request.getContent());
            doc.setSourceType(request.getSourceType() != null ? request.getSourceType() : "manual");
            doc.setStatus("published");
            doc.setChunkCount(0);
            documentMapper.insert(doc);
            log.info("Document {} inserted, starting ingestion...", doc.getId());

            try {
                List<String> milvusIds = ingestionService.ingest(doc.getId(), doc.getTitle(),
                        doc.getContent(), doc.getCategoryId());
                doc.setMilvusIds(JSONUtil.toJsonStr(milvusIds));
                doc.setChunkCount(milvusIds.size());
                documentMapper.updateById(doc);
                log.info("Document {} ingestion complete, {} chunks", doc.getId(), milvusIds.size());
            } catch (Exception e) {
                log.error("Failed to ingest document {}: {}", doc.getId(), e.getMessage(), e);
                doc.setStatus("draft");
                documentMapper.updateById(doc);
            }

            return toVO(doc);
        } catch (Exception e) {
            log.error("Create document failed", e);
            throw new BusinessException(96998, "创建失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public DocumentVO update(Long id, UpdateDocumentRequest request) {
        KbDocument doc = documentMapper.selectById(id);
        if (doc == null) {
            throw new BusinessException(KnowledgeErrorCode.DOCUMENT_NOT_FOUND);
        }

        String originalStatus = doc.getStatus();
        boolean needReindex = false;
        boolean statusChanged = false;
        if (request.getTitle() != null && !request.getTitle().equals(doc.getTitle())) {
            doc.setTitle(request.getTitle());
            needReindex = true;
        }
        if (request.getContent() != null && !request.getContent().equals(doc.getContent())) {
            doc.setContent(request.getContent());
            needReindex = true;
        }
        if (request.getCategoryId() != null) {
            doc.setCategoryId(request.getCategoryId());
            needReindex = true;
        }
        if (request.getStatus() != null && !request.getStatus().equals(doc.getStatus())) {
            doc.setStatus(request.getStatus());
            statusChanged = true;
        }

        documentMapper.updateById(doc);

        if (!"published".equals(doc.getStatus())) {
            deleteMilvusVectors(doc);
            doc.setMilvusIds(null);
            doc.setChunkCount(0);
            documentMapper.updateById(doc);
            return toVO(doc);
        }

        boolean shouldReindex = needReindex || statusChanged || !"published".equals(originalStatus);
        if (shouldReindex) {
            deleteMilvusVectors(doc);
            try {
                List<String> milvusIds = ingestionService.ingest(doc.getId(), doc.getTitle(),
                        doc.getContent(), doc.getCategoryId());
                doc.setMilvusIds(JSONUtil.toJsonStr(milvusIds));
                doc.setChunkCount(milvusIds.size());
                doc.setStatus("published");
                documentMapper.updateById(doc);
            } catch (Exception e) {
                log.error("Failed to reindex document {}", doc.getId(), e);
                doc.setStatus("draft");
                documentMapper.updateById(doc);
            }
        }

        return toVO(doc);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        KbDocument doc = documentMapper.selectById(id);
        if (doc == null) {
            throw new BusinessException(KnowledgeErrorCode.DOCUMENT_NOT_FOUND);
        }
        deleteMilvusVectors(doc);
        documentMapper.deleteById(id);
    }

    @Override
    public DocumentVO getById(Long id) {
        KbDocument doc = documentMapper.selectById(id);
        if (doc == null) {
            throw new BusinessException(KnowledgeErrorCode.DOCUMENT_NOT_FOUND);
        }
        return toVO(doc);
    }

    @Override
    public Page<DocumentVO> page(int pageNum, int pageSize, Long categoryId, String status) {
        LambdaQueryWrapper<KbDocument> wrapper = new LambdaQueryWrapper<>();
        if (categoryId != null) {
            wrapper.eq(KbDocument::getCategoryId, categoryId);
        }
        if (StrUtil.isNotBlank(status)) {
            wrapper.eq(KbDocument::getStatus, status);
        }
        wrapper.orderByDesc(KbDocument::getUpdateTime);

        Page<KbDocument> page = documentMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        Page<DocumentVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    @Override
    @Transactional
    public void reindex(Long id) {
        KbDocument doc = documentMapper.selectById(id);
        if (doc == null) {
            throw new BusinessException(KnowledgeErrorCode.DOCUMENT_NOT_FOUND);
        }
        deleteMilvusVectors(doc);
        try {
            List<String> milvusIds = ingestionService.ingest(doc.getId(), doc.getTitle(),
                    doc.getContent(), doc.getCategoryId());
            doc.setMilvusIds(JSONUtil.toJsonStr(milvusIds));
            doc.setChunkCount(milvusIds.size());
            doc.setStatus("published");
            documentMapper.updateById(doc);
        } catch (Exception e) {
            log.error("Failed to reindex document {}", doc.getId(), e);
            throw new BusinessException(KnowledgeErrorCode.DOCUMENT_REINDEX_FAILED);
        }
    }

    private void deleteMilvusVectors(KbDocument doc) {
        if (StrUtil.isNotBlank(doc.getMilvusIds())) {
            try {
                List<String> ids = JSONUtil.toList(doc.getMilvusIds(), String.class);
                ingestionService.deleteVectors(ids);
            } catch (Exception e) {
                log.warn("Failed to delete vectors for document {}", doc.getId(), e);
            }
        }
    }

    private DocumentVO toVO(KbDocument doc) {
        DocumentVO vo = new DocumentVO();
        vo.setId(doc.getId());
        vo.setCategoryId(doc.getCategoryId());
        vo.setTitle(doc.getTitle());
        vo.setContent(doc.getContent());
        vo.setSourceType(doc.getSourceType());
        vo.setStatus(doc.getStatus());
        vo.setChunkCount(doc.getChunkCount());
        vo.setCreateTime(doc.getCreateTime());
        vo.setUpdateTime(doc.getUpdateTime());

        if (doc.getCategoryId() != null) {
            KbCategory category = categoryMapper.selectById(doc.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getName());
            }
        }
        return vo;
    }
}
