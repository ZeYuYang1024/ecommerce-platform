package com.ecommerce.knowledge.service.impl;

import cn.hutool.json.JSONUtil;
import com.ecommerce.knowledge.service.DocumentIngestionService;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static dev.langchain4j.data.document.splitter.DocumentSplitters.recursive;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIngestionServiceImpl implements DocumentIngestionService {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    @Override
    public List<String> ingest(Long documentId, String title, String content, Long categoryId,
                               String ownerType, Long merchantId) {
        Metadata metadata = Metadata.from("document_id", String.valueOf(documentId))
                .put("category_id", String.valueOf(categoryId))
                .put("owner_type", ownerType)
                .put("title", title);
        if (merchantId != null) {
            metadata.put("merchant_id", merchantId);
        }
        Document document = Document.from(content, metadata);

        DocumentSplitter splitter = recursive(500, 100);
        List<TextSegment> segments = splitter.split(document);

        log.info("Document {} split into {} chunks", documentId, segments.size());

        List<String> chunkIds = new ArrayList<>();
        for (TextSegment segment : segments) {
            segment.metadata().put("document_id", String.valueOf(documentId));
            segment.metadata().put("category_id", String.valueOf(categoryId));
            segment.metadata().put("owner_type", ownerType);
            if (merchantId != null) {
                segment.metadata().put("merchant_id", merchantId);
            }
            segment.metadata().put("title", title);

            Embedding embedding = embeddingModel.embed(segment.text()).content();
            String id = embeddingStore.add(embedding, segment);
            chunkIds.add(id);
        }

        log.info("Document {} ingested with {} chunks, ids: {}",
                documentId, chunkIds.size(), JSONUtil.toJsonStr(chunkIds));

        return chunkIds;
    }

    @Override
    public void deleteVectors(List<String> milvusIds) {
        if (milvusIds == null || milvusIds.isEmpty()) {
            return;
        }
        for (String id : milvusIds) {
            embeddingStore.remove(id);
        }
        log.info("Deleted {} vectors from embedding store", milvusIds.size());
    }
}
