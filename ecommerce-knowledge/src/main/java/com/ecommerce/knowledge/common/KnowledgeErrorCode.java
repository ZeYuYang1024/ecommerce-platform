package com.ecommerce.knowledge.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum KnowledgeErrorCode implements ErrorCode {

    // 96xxx: ecommerce-knowledge 错误码
    DOCUMENT_NOT_FOUND(96001, "文档不存在"),
    CATEGORY_NOT_FOUND(96002, "分类不存在"),
    CATEGORY_HAS_DOCUMENTS(96003, "分类下存在文档，无法删除"),
    DOCUMENT_IMPORT_FAILED(96004, "文档导入失败"),
    DOCUMENT_REINDEX_FAILED(96005, "文档重新索引失败"),
    CHAT_SESSION_NOT_FOUND(96006, "会话不存在"),
    MILVUS_OPERATION_FAILED(96007, "Milvus 操作失败"),
    EMBEDDING_FAILED(96008, "向量化失败"),
    LLM_CALL_FAILED(96009, "LLM 调用失败"),
    VALIDATION_ERROR(96010, "参数校验失败");

    private final int code;
    private final String message;
}
