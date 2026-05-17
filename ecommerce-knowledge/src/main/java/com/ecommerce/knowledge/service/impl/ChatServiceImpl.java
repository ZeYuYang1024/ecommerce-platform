package com.ecommerce.knowledge.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.knowledge.agent.AgentUserContext;
import com.ecommerce.knowledge.agent.AgentUserContextHolder;
import com.ecommerce.knowledge.agent.KnowledgeAgent;
import com.ecommerce.knowledge.common.BusinessException;
import com.ecommerce.knowledge.common.KnowledgeErrorCode;
import com.ecommerce.knowledge.dto.request.ChatRequest;
import com.ecommerce.knowledge.dto.response.ChatResponse;
import com.ecommerce.knowledge.entity.KbChatSession;
import com.ecommerce.knowledge.mapper.KbChatSessionMapper;
import com.ecommerce.knowledge.service.ChatService;
import com.ecommerce.knowledge.tool.AddressQueryTool;
import com.ecommerce.knowledge.tool.CartQueryTool;
import com.ecommerce.knowledge.tool.CouponQueryTool;
import com.ecommerce.knowledge.tool.InventoryQueryTool;
import com.ecommerce.knowledge.tool.NotificationQueryTool;
import com.ecommerce.knowledge.tool.OrderQueryTool;
import com.ecommerce.knowledge.tool.PaymentQueryTool;
import com.ecommerce.knowledge.tool.ProductQueryTool;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.ContentMetadata;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    private static final int MAX_MEMORY_MESSAGES = 24;
    private static final int MAX_SOURCE_LENGTH = 180;
    private static final String OWNER_PLATFORM = "platform";
    private static final String OWNER_MERCHANT = "merchant";

    private final ChatModel chatModel;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final ProductQueryTool productQueryTool;
    private final OrderQueryTool orderQueryTool;
    private final InventoryQueryTool inventoryQueryTool;
    private final CouponQueryTool couponQueryTool;
    private final CartQueryTool cartQueryTool;
    private final AddressQueryTool addressQueryTool;
    private final NotificationQueryTool notificationQueryTool;
    private final PaymentQueryTool paymentQueryTool;
    private final KbChatSessionMapper chatSessionMapper;

    private final Map<String, ChatMemory> chatMemories = new ConcurrentHashMap<>();
    private final Map<Long, KnowledgeAgent> merchantAgents = new ConcurrentHashMap<>();

    private KnowledgeAgent platformAgent;

    public ChatServiceImpl(ChatModel chatModel,
                           EmbeddingModel embeddingModel,
                           EmbeddingStore<TextSegment> embeddingStore,
                           ProductQueryTool productQueryTool,
                           OrderQueryTool orderQueryTool,
                           InventoryQueryTool inventoryQueryTool,
                           CouponQueryTool couponQueryTool,
                           CartQueryTool cartQueryTool,
                           AddressQueryTool addressQueryTool,
                           NotificationQueryTool notificationQueryTool,
                           PaymentQueryTool paymentQueryTool,
                           KbChatSessionMapper chatSessionMapper) {
        this.chatModel = chatModel;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.productQueryTool = productQueryTool;
        this.orderQueryTool = orderQueryTool;
        this.inventoryQueryTool = inventoryQueryTool;
        this.couponQueryTool = couponQueryTool;
        this.cartQueryTool = cartQueryTool;
        this.addressQueryTool = addressQueryTool;
        this.notificationQueryTool = notificationQueryTool;
        this.paymentQueryTool = paymentQueryTool;
        this.chatSessionMapper = chatSessionMapper;
    }

    @PostConstruct
    public void init() {
        this.platformAgent = buildAgent(buildOwnershipFilter(OWNER_PLATFORM, null));
        log.info("KnowledgeAgent initialized with tenant-scoped RAG retrievers");
    }

    @Override
    public ChatResponse chat(ChatRequest request, Long userId, String userType) {
        return executeChat(platformAgent, request, userId, userType, null, OWNER_PLATFORM);
    }

    @Override
    public ChatResponse merchantChat(ChatRequest request, Long userId, String userType, Long merchantId) {
        if (merchantId == null) {
            throw new BusinessException(KnowledgeErrorCode.VALIDATION_ERROR);
        }
        KnowledgeAgent merchantAgent = merchantAgents.computeIfAbsent(
                merchantId, id -> buildAgent(buildOwnershipFilter(OWNER_MERCHANT, id)));
        return executeChat(merchantAgent, request, userId, userType, merchantId, OWNER_MERCHANT);
    }

    private ChatResponse executeChat(KnowledgeAgent agent,
                                     ChatRequest request,
                                     Long userId,
                                     String userType,
                                     Long merchantId,
                                     String ownerType) {
        if (StrUtil.isBlank(request.getQuestion())) {
            throw new BusinessException(KnowledgeErrorCode.VALIDATION_ERROR);
        }

        String sessionId = StrUtil.blankToDefault(request.getSessionId(), UUID.randomUUID().toString());
        String memoryId = buildMemoryId(ownerType, merchantId, sessionId);

        try {
            AgentUserContextHolder.set(new AgentUserContext(userId, userType));

            dev.langchain4j.service.Result<String> result =
                    agent.chat(memoryId, buildPromptInput(request.getQuestion(), userId, userType, ownerType, merchantId));

            upsertSession(sessionId, request.getQuestion(), userId);

            ChatResponse response = new ChatResponse();
            response.setAnswer(result.content());
            response.setSessionId(sessionId);
            response.setSources(toSources(result.sources()));
            return response;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Chat failed for session {}", sessionId, e);
            throw new BusinessException(KnowledgeErrorCode.LLM_CALL_FAILED);
        } finally {
            AgentUserContextHolder.clear();
        }
    }

    private KnowledgeAgent buildAgent(Filter filter) {
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(5)
                .minScore(0.55)
                .filter(filter)
                .build();

        RetrievalAugmentor augmentor = DefaultRetrievalAugmentor.builder()
                .contentRetriever(contentRetriever)
                .build();

        return AiServices.builder(KnowledgeAgent.class)
                .chatModel(chatModel)
                .retrievalAugmentor(augmentor)
                .chatMemoryProvider(memoryId -> chatMemories.computeIfAbsent(String.valueOf(memoryId),
                        ignored -> MessageWindowChatMemory.builder()
                                .id(String.valueOf(memoryId))
                                .maxMessages(MAX_MEMORY_MESSAGES)
                                .build()))
                .tools(
                        productQueryTool,
                        orderQueryTool,
                        inventoryQueryTool,
                        couponQueryTool,
                        cartQueryTool,
                        addressQueryTool,
                        notificationQueryTool,
                        paymentQueryTool
                )
                .build();
    }

    private Filter buildOwnershipFilter(String ownerType, Long merchantId) {
        Filter filter = MetadataFilterBuilder.metadataKey("owner_type").isEqualTo(ownerType);
        if (merchantId != null) {
            filter = filter.and(MetadataFilterBuilder.metadataKey("merchant_id").isEqualTo(merchantId));
        }
        return filter;
    }

    private String buildMemoryId(String ownerType, Long merchantId, String sessionId) {
        if (OWNER_MERCHANT.equals(ownerType)) {
            return "merchant:" + merchantId + ":" + sessionId;
        }
        return "platform:" + sessionId;
    }

    private String buildPromptInput(String question, Long userId, String userType, String ownerType, Long merchantId) {
        StringBuilder builder = new StringBuilder();
        builder.append("用户问题：").append(question.trim());

        builder.append("\n\n[知识库范围]");
        if (OWNER_MERCHANT.equals(ownerType)) {
            builder.append("\n当前只能检索当前商家的私有知识库")
                    .append("\n商家ID=").append(merchantId)
                    .append("\n不要引用平台知识库，也不要回答其他商家的知识文档内容。");
        } else {
            builder.append("\n当前只能检索平台知识库。");
        }

        if (userId != null) {
            builder.append("\n\n[会话上下文]")
                    .append("\n当前用户已登录")
                    .append("\n用户ID=").append(userId);
            if (StrUtil.isNotBlank(userType)) {
                builder.append("\n用户类型=").append(userType);
            }
            builder.append("\n如果用户询问的是他/她的购物车、订单、优惠券、收货地址、通知或支付状态，直接使用工具查询当前会话用户的数据。")
                    .append("\n不要要求用户再次提供自己的 userId。")
                    .append("\n如果工具没有查到数据或调用失败，只回答该问题本身，不要展开无关的订单规则、优惠券说明或平台百科。");
        } else {
            builder.append("\n\n[会话上下文]")
                    .append("\n当前用户未登录。")
                    .append("\n如果用户询问“我的购物车”“我的订单”“我的优惠券”“我的地址”“我的通知”“我的支付”这类个人数据，先明确说明登录后才能查询，不要编造结果。");
        }

        return builder.toString();
    }

    private List<ChatResponse.Source> toSources(List<dev.langchain4j.rag.content.Content> sources) {
        if (sources == null || sources.isEmpty()) {
            return Collections.emptyList();
        }

        return sources.stream()
                .map(source -> new ChatResponse.Source(
                        StrUtil.blankToDefault(source.textSegment().metadata().getString("title"), "知识库文档"),
                        abbreviate(source.textSegment().text()),
                        extractScore(source.metadata())
                ))
                .toList();
    }

    private Double extractScore(Map<ContentMetadata, Object> metadata) {
        if (metadata == null) {
            return null;
        }
        Object value = metadata.get(ContentMetadata.SCORE);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return null;
    }

    private String abbreviate(String text) {
        if (StrUtil.isBlank(text) || text.length() <= MAX_SOURCE_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_SOURCE_LENGTH) + "...";
    }

    private void upsertSession(String sessionId, String question, Long userId) {
        if (userId == null) {
            return;
        }

        KbChatSession session = chatSessionMapper.selectOne(new LambdaQueryWrapper<KbChatSession>()
                .eq(KbChatSession::getSessionId, sessionId)
                .last("limit 1"));

        if (session == null) {
            session = new KbChatSession();
            session.setUserId(userId);
            session.setSessionId(sessionId);
            session.setTitle(buildSessionTitle(question));
            session.setMessageCount(1);
            chatSessionMapper.insert(session);
            return;
        }

        session.setMessageCount((session.getMessageCount() == null ? 0 : session.getMessageCount()) + 1);
        if (StrUtil.isBlank(session.getTitle())) {
            session.setTitle(buildSessionTitle(question));
        }
        chatSessionMapper.updateById(session);
    }

    private String buildSessionTitle(String question) {
        String normalized = StrUtil.trim(question);
        if (normalized.length() <= 20) {
            return normalized;
        }
        return normalized.substring(0, 20) + "...";
    }
}
