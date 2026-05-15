package com.ecommerce.knowledge.service.impl;

import cn.hutool.core.util.StrUtil;
import com.ecommerce.knowledge.agent.KnowledgeAgent;
import com.ecommerce.knowledge.common.BusinessException;
import com.ecommerce.knowledge.common.KnowledgeErrorCode;
import com.ecommerce.knowledge.dto.request.ChatRequest;
import com.ecommerce.knowledge.dto.response.ChatResponse;
import com.ecommerce.knowledge.service.ChatService;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    private final ChatModel chatModel;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    private KnowledgeAgent agent;

    public ChatServiceImpl(ChatModel chatModel,
                           EmbeddingModel embeddingModel,
                           EmbeddingStore<TextSegment> embeddingStore) {
        this.chatModel = chatModel;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }

    @PostConstruct
    public void init() {
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(5)
                .minScore(0.5)
                .build();

        RetrievalAugmentor augmentor = DefaultRetrievalAugmentor.builder()
                .contentRetriever(contentRetriever)
                .build();

        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(20);

        this.agent = AiServices.builder(KnowledgeAgent.class)
                .chatModel(chatModel)
                .retrievalAugmentor(augmentor)
                .chatMemory(chatMemory)
                .build();

        log.info("KnowledgeAgent initialized with RAG");
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        if (StrUtil.isBlank(request.getQuestion())) {
            throw new BusinessException(KnowledgeErrorCode.VALIDATION_ERROR);
        }

        String sessionId = request.getSessionId();
        if (StrUtil.isBlank(sessionId)) {
            sessionId = UUID.randomUUID().toString();
        }

        try {
            String answer = agent.chat(request.getQuestion());

            ChatResponse response = new ChatResponse();
            response.setAnswer(answer);
            response.setSessionId(sessionId);
            return response;

        } catch (Exception e) {
            log.error("Chat failed for session {}", sessionId, e);
            throw new BusinessException(KnowledgeErrorCode.LLM_CALL_FAILED);
        }
    }
}
