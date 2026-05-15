package com.ecommerce.knowledge.service;

import java.util.List;

public interface DocumentIngestionService {

    /**
     * 将文档内容向量化并存入 Milvus
     * @return Milvus 中的向量 ID 列表
     */
    List<String> ingest(Long documentId, String title, String content, Long categoryId);

    /**
     * 删除文档在 Milvus 中的所有向量
     */
    void deleteVectors(List<String> milvusIds);
}
