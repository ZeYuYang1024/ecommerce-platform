package com.ecommerce.knowledge.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.knowledge.agent.AgentUserContext;
import com.ecommerce.knowledge.agent.AgentUserContextHolder;
import com.ecommerce.knowledge.agent.KnowledgeAgent;
import com.ecommerce.knowledge.agent.StreamingKnowledgeAgent;
import com.ecommerce.knowledge.chat.KnowledgeLightRoute;
import com.ecommerce.knowledge.chat.KnowledgeLightRouteDecider;
import com.ecommerce.knowledge.chat.KnowledgeQueryClassifier;
import com.ecommerce.knowledge.chat.KnowledgeQueryFeatures;
import com.ecommerce.knowledge.chat.KnowledgeQueryRoute;
import com.ecommerce.knowledge.client.dto.AddressVO;
import com.ecommerce.knowledge.client.dto.CartItemVO;
import com.ecommerce.knowledge.client.dto.CouponVO;
import com.ecommerce.knowledge.client.dto.NotificationVO;
import com.ecommerce.knowledge.client.dto.OrderSummaryVO;
import com.ecommerce.knowledge.client.dto.OrderVO;
import com.ecommerce.knowledge.client.dto.PaymentVO;
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
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.ContentMetadata;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    private static final int MAX_RAG_MEMORY_MESSAGES = 12;
    private static final int MAX_TOOL_MEMORY_MESSAGES = 8;
    private static final int MAX_SOURCE_LENGTH = 180;
    private static final int PAID_ORDER_STATUS = 1;
    private static final long DEFAULT_STREAM_TIMEOUT_MS = 60_000L;
    private static final int RETRIEVAL_MAX_RESULTS = 3;
    private static final double RETRIEVAL_MIN_SCORE = 0.60;
    private static final String OWNER_PLATFORM = "platform";
    private static final String OWNER_MERCHANT = "merchant";
    private static final String FAQ_CACHE_KEY_PREFIX = "knowledge:faq:";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Pattern ORDER_NO_PATTERN = Pattern.compile("(?i)\\b(?:ord-?\\d+|\\d{12,32})\\b");

    private final ChatModel chatModel;
    private final StreamingChatModel streamingChatModel;
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
    private final ChatExecutionMetricsRecorder chatExecutionMetricsRecorder;
    private final KnowledgeQueryClassifier knowledgeQueryClassifier;
    private final StringRedisTemplate stringRedisTemplate;
    private final JsonMapper jsonMapper;
    private final KnowledgeLightRouteDecider knowledgeLightRouteDecider = new KnowledgeLightRouteDecider();

    private final Map<String, ChatMemory> ragChatMemories = new ConcurrentHashMap<>();
    private final Map<String, ChatMemory> toolChatMemories = new ConcurrentHashMap<>();
    private final Map<Long, KnowledgeAgent> merchantAgents = new ConcurrentHashMap<>();
    private final Map<Long, StreamingKnowledgeAgent> streamingMerchantAgents = new ConcurrentHashMap<>();
    private final Map<String, FaqCacheEntry> faqCache = new ConcurrentHashMap<>();

    @Value("${knowledge.chat.fast-path-enabled:true}")
    private boolean fastPathEnabled = true;

    @Value("${knowledge.chat.tool-only-agent-enabled:true}")
    private boolean toolOnlyAgentEnabled = true;

    @Value("${knowledge.chat.streaming-enabled:true}")
    private boolean streamingEnabled = true;

    @Value("${knowledge.chat.structured-query-timeout-ms:1500}")
    private long structuredQueryTimeoutMs = 1500L;

    @Value("${knowledge.chat.downstream-soft-timeout-ms:400}")
    private long downstreamSoftTimeoutMs = 400L;

    @Value("${knowledge.chat.faq-cache-enabled:true}")
    private boolean faqCacheEnabled = true;

    @Value("${knowledge.chat.faq-cache-ttl-ms:300000}")
    private long faqCacheTtlMs = 300_000L;

    @Value("${knowledge.chat.faq-cache-max-entries:200}")
    private int faqCacheMaxEntries = 200;

    private KnowledgeAgent platformAgent;
    private KnowledgeAgent toolOnlyAgent;
    private StreamingKnowledgeAgent streamingPlatformAgent;
    private StreamingKnowledgeAgent streamingToolOnlyAgent;

    public ChatServiceImpl(ChatModel chatModel,
                           StreamingChatModel streamingChatModel,
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
                           KbChatSessionMapper chatSessionMapper,
                           ChatExecutionMetricsRecorder chatExecutionMetricsRecorder,
                           KnowledgeQueryClassifier knowledgeQueryClassifier,
                           StringRedisTemplate stringRedisTemplate,
                           JsonMapper jsonMapper) {
        this.chatModel = chatModel;
        this.streamingChatModel = streamingChatModel;
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
        this.chatExecutionMetricsRecorder = chatExecutionMetricsRecorder;
        this.knowledgeQueryClassifier = knowledgeQueryClassifier;
        this.stringRedisTemplate = stringRedisTemplate;
        this.jsonMapper = jsonMapper;
    }

    @PostConstruct
    public void init() {
        this.platformAgent = buildRagAgent(buildOwnershipFilter(OWNER_PLATFORM, null));
        this.toolOnlyAgent = buildToolOnlyAgent();
        this.streamingPlatformAgent = buildStreamingRagAgent(buildOwnershipFilter(OWNER_PLATFORM, null));
        this.streamingToolOnlyAgent = buildStreamingToolOnlyAgent();
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
                merchantId, id -> buildRagAgent(buildOwnershipFilter(OWNER_MERCHANT, id)));
        return executeChat(merchantAgent, request, userId, userType, merchantId, OWNER_MERCHANT);
    }

    @Override
    public SseEmitter stream(ChatRequest request, Long userId, String userType) {
        return stream(request, userId, userType, null, OWNER_PLATFORM);
    }

    @Override
    public SseEmitter merchantStream(ChatRequest request, Long userId, String userType, Long merchantId) {
        if (merchantId == null) {
            throw new BusinessException(KnowledgeErrorCode.VALIDATION_ERROR);
        }
        return stream(request, userId, userType, merchantId, OWNER_MERCHANT);
    }

    private SseEmitter stream(ChatRequest request,
                              Long userId,
                              String userType,
                              Long merchantId,
                              String ownerType) {
        if (!streamingEnabled) {
            throw new BusinessException(KnowledgeErrorCode.VALIDATION_ERROR);
        }

        SseEmitter emitter = new SseEmitter(Math.max(DEFAULT_STREAM_TIMEOUT_MS, structuredQueryTimeoutMs));
        String sessionId = ensureSessionId(request);
        ChatExecutionContext executionContext = buildExecutionContext(request.getQuestion(), userId, sessionId);
        CompletableFuture.runAsync(() -> {
            try {
                sendStreamEvent(emitter, "start", sessionId);
                sendStreamEvent(emitter, "lightRoute", executionContext.lightRoute().name());
                sendStreamEvent(emitter, "route", executionContext.route().name());
                streamChat(emitter, request, userId, userType, merchantId, ownerType, sessionId, executionContext);
            } catch (Exception e) {
                try {
                    sendStreamEvent(emitter, "error", StrUtil.blankToDefault(e.getMessage(), "chat failed"));
                } catch (IOException ioException) {
                    log.debug("Failed to send error event", ioException);
                }
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    private void streamChat(SseEmitter emitter,
                            ChatRequest request,
                            Long userId,
                            String userType,
                            Long merchantId,
                            String ownerType,
                            String sessionId,
                            ChatExecutionContext executionContext) throws IOException {
        long requestStart = System.nanoTime();
        if (StrUtil.isBlank(request.getQuestion())) {
            throw new BusinessException(KnowledgeErrorCode.VALIDATION_ERROR);
        }

        String memoryId = buildMemoryId(ownerType, merchantId, sessionId);
        AgentUserContext userContext = new AgentUserContext(userId, userType);
        AgentUserContextHolder.set(userContext);
        try {
            log.info("Chat stage=lightRoute lightRoute={} sessionId={}",
                    executionContext.lightRoute(), sessionId);
            long routeSelectionStart = System.nanoTime();
            KnowledgeQueryRoute route = executionContext.route();
            log.info("Chat stage=routeSelection route={} structured={} sessionId={} elapsedMs={}",
                    route, route.isStructured(), sessionId, elapsedMillis(routeSelectionStart));
            chatExecutionMetricsRecorder.recordLightRoute(executionContext.lightRoute());
            chatExecutionMetricsRecorder.recordRoute(route);

            FaqCacheEntry faqCacheEntry = findFaqCache(executionContext, ownerType, merchantId, request.getQuestion(), sessionId);
            if (faqCacheEntry != null) {
                upsertSession(sessionId, request.getQuestion(), userId);
                ChatResponse response = buildResponse(sessionId, faqCacheEntry.answer(), faqCacheEntry.sources());
                sendStreamEvent(emitter, "chunk", response.getAnswer());
                sendStreamEvent(emitter, "answer", response);
                sendStreamEvent(emitter, "done", response.getSessionId());
                logExecution(executionContext, "faqCache", 0, false, requestStart);
                emitter.complete();
                return;
            }

            long fastPathStart = System.nanoTime();
            FastPathDecision fastPathDecision = determineFastPath(route, request.getQuestion(), userId);
            log.info("Chat stage=fastPath route={} candidate={} available={} executed={} sessionId={} elapsedMs={}",
                    route,
                    fastPathDecision.candidate(),
                    fastPathDecision.available(),
                    fastPathDecision.executed(),
                    sessionId,
                    elapsedMillis(fastPathStart));
            chatExecutionMetricsRecorder.recordFastPath(route,
                    fastPathDecision.candidate(),
                    fastPathDecision.available(),
                    fastPathDecision.executed());
            if (fastPathDecision.executed()) {
                rememberFastPathTurn(memoryId, request.getQuestion(), fastPathDecision.answer());
                upsertSession(sessionId, request.getQuestion(), userId);
                ChatResponse response = buildFastPathResponse(fastPathDecision.answer(), sessionId);
                sendStreamEvent(emitter, "chunk", response.getAnswer());
                sendStreamEvent(emitter, "answer", response);
                sendStreamEvent(emitter, "done", response.getSessionId());
                logStructuredLatencyIfNeeded(route, sessionId, requestStart);
                logExecution(executionContext, "fastPath", 0, fastPathDecision.degraded(), requestStart);
                emitter.complete();
                return;
            }

            streamAgentResponse(emitter, request, userId, userType, merchantId, ownerType, sessionId, executionContext, memoryId, requestStart);
        } finally {
            AgentUserContextHolder.clear();
        }
    }

    private void streamAgentResponse(SseEmitter emitter,
                                     ChatRequest request,
                                     Long userId,
                                     String userType,
                                     Long merchantId,
                                     String ownerType,
                                     String sessionId,
                                     ChatExecutionContext executionContext,
                                     String memoryId,
                                     long requestStart) {
        KnowledgeQueryRoute route = executionContext.route();
        StreamingKnowledgeAgent selectedAgent = selectStreamingAgent(route, merchantId, ownerType);
        boolean toolOnly = selectedAgent == streamingToolOnlyAgent;
        String agentType = toolOnly ? "toolOnly" : "rag";
        log.info("Chat stage=agentSelection route={} agentType={} sessionId={}",
                route, agentType, sessionId);

        long agentExecutionStart = System.nanoTime();
        String promptInput = buildPromptInput(request.getQuestion(), userId, userType, ownerType, merchantId);
        StringBuilder answerBuilder = new StringBuilder();
        List<Content> retrievedContents = Collections.synchronizedList(new ArrayList<>());
        AgentUserContext userContext = new AgentUserContext(userId, userType);

        AgentUserContextHolder.set(userContext);
        selectedAgent.chat(memoryId, promptInput)
                .onPartialResponse(chunk -> {
                    answerBuilder.append(chunk);
                    try {
                        sendStreamEvent(emitter, "chunk", chunk);
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                })
                .onRetrieved(retrievedContents::addAll)
                .beforeToolExecution(ignored -> AgentUserContextHolder.set(userContext))
                .onToolExecuted(ignored -> AgentUserContextHolder.clear())
                .onCompleteResponse(response -> {
                    try {
                        String finalAnswer = response.aiMessage() != null && StrUtil.isNotBlank(response.aiMessage().text())
                                ? response.aiMessage().text()
                                : answerBuilder.toString();
                        upsertSession(sessionId, request.getQuestion(), userId);
                        ChatResponse chatResponse = buildResponse(sessionId, finalAnswer, toSources(retrievedContents));
                        cacheFaqIfEligible(executionContext, ownerType, merchantId, request.getQuestion(), chatResponse, sessionId);
                        sendStreamEvent(emitter, "answer", chatResponse);
                        sendStreamEvent(emitter, "done", chatResponse.getSessionId());
                        Duration agentDuration = Duration.ofNanos(System.nanoTime() - agentExecutionStart);
                        chatExecutionMetricsRecorder.recordAgentExecution(route, agentType, agentDuration);
                        log.info("Chat stage=agentExecution route={} sessionId={} elapsedMs={} promptChars={}",
                                route, sessionId, elapsedMillis(agentExecutionStart), promptInput.length());
                        logStructuredLatencyIfNeeded(route, sessionId, requestStart);
                        logExecution(executionContext, toolOnly ? "toolOnlyAgent" : "ragAgent", 1, false, requestStart);
                        emitter.complete();
                    } catch (Exception e) {
                        completeStreamWithError(emitter, e);
                    } finally {
                        AgentUserContextHolder.clear();
                    }
                })
                .onError(error -> {
                    Duration agentDuration = Duration.ofNanos(System.nanoTime() - agentExecutionStart);
                    chatExecutionMetricsRecorder.recordAgentExecution(route, agentType, agentDuration);
                    log.warn("Chat stage=agentExecution route={} sessionId={} failed=true elapsedMs={} message={}",
                            route, sessionId, elapsedMillis(agentExecutionStart),
                            StrUtil.blankToDefault(error.getMessage(), error.getClass().getSimpleName()));
                    AgentUserContextHolder.clear();
                    completeStreamWithError(emitter, error);
                })
                .start();
    }

    private void completeStreamWithError(SseEmitter emitter, Throwable error) {
        try {
            sendStreamEvent(emitter, "error", StrUtil.blankToDefault(error.getMessage(), "chat failed"));
        } catch (IOException ioException) {
            log.debug("Failed to send error event", ioException);
        }
        emitter.completeWithError(error);
    }

    private ChatResponse executeChat(KnowledgeAgent agent,
                                     ChatRequest request,
                                     Long userId,
                                     String userType,
                                     Long merchantId,
                                     String ownerType) {
        String sessionId = ensureSessionId(request);
        ChatExecutionContext executionContext = buildExecutionContext(request.getQuestion(), userId, sessionId);
        return executeChat(agent, request, userId, userType, merchantId, ownerType, sessionId, executionContext);
    }

    private ChatResponse executeChat(KnowledgeAgent agent,
                                     ChatRequest request,
                                     Long userId,
                                     String userType,
                                     Long merchantId,
                                     String ownerType,
                                     String sessionId,
                                     ChatExecutionContext executionContext) {
        long requestStart = System.nanoTime();
        if (StrUtil.isBlank(request.getQuestion())) {
            throw new BusinessException(KnowledgeErrorCode.VALIDATION_ERROR);
        }

        String memoryId = buildMemoryId(ownerType, merchantId, sessionId);

        try {
            AgentUserContextHolder.set(new AgentUserContext(userId, userType));

            log.info("Chat stage=lightRoute lightRoute={} sessionId={}",
                    executionContext.lightRoute(), sessionId);
            long routeSelectionStart = System.nanoTime();
            KnowledgeQueryRoute route = executionContext.route();
            log.info("Chat stage=routeSelection route={} structured={} sessionId={} elapsedMs={}",
                    route, route.isStructured(), sessionId, elapsedMillis(routeSelectionStart));
            chatExecutionMetricsRecorder.recordLightRoute(executionContext.lightRoute());
            chatExecutionMetricsRecorder.recordRoute(route);

            FaqCacheEntry faqCacheEntry = findFaqCache(executionContext, ownerType, merchantId, request.getQuestion(), sessionId);
            if (faqCacheEntry != null) {
                upsertSession(sessionId, request.getQuestion(), userId);
                logExecution(executionContext, "faqCache", 0, false, requestStart);
                return buildResponse(sessionId, faqCacheEntry.answer(), faqCacheEntry.sources());
            }

            long fastPathStart = System.nanoTime();
            FastPathDecision fastPathDecision = determineFastPath(route, request.getQuestion(), userId);
            log.info("Chat stage=fastPath route={} candidate={} available={} executed={} sessionId={} elapsedMs={}",
                    route,
                    fastPathDecision.candidate(),
                    fastPathDecision.available(),
                    fastPathDecision.executed(),
                    sessionId,
                    elapsedMillis(fastPathStart));
            chatExecutionMetricsRecorder.recordFastPath(route,
                    fastPathDecision.candidate(),
                    fastPathDecision.available(),
                    fastPathDecision.executed());
            if (fastPathDecision.executed()) {
                rememberFastPathTurn(memoryId, request.getQuestion(), fastPathDecision.answer());
                upsertSession(sessionId, request.getQuestion(), userId);
                logStructuredLatencyIfNeeded(route, sessionId, requestStart);
                logExecution(executionContext, "fastPath", 0, fastPathDecision.degraded(), requestStart);
                return buildFastPathResponse(fastPathDecision.answer(), sessionId);
            }

            KnowledgeAgent selectedAgent = selectAgent(route, userId, agent);
            log.info("Chat stage=agentSelection route={} agentType={} sessionId={}",
                    route, selectedAgent == toolOnlyAgent ? "toolOnly" : "rag", sessionId);

            long agentExecutionStart = System.nanoTime();
            String agentType = selectedAgent == toolOnlyAgent ? "toolOnly" : "rag";
            String promptInput = buildPromptInput(request.getQuestion(), userId, userType, ownerType, merchantId);
            dev.langchain4j.service.Result<String> result;
            try {
                result = selectedAgent.chat(memoryId, promptInput);
            } finally {
                Duration agentDuration = Duration.ofNanos(System.nanoTime() - agentExecutionStart);
                chatExecutionMetricsRecorder.recordAgentExecution(route, agentType, agentDuration);
                log.info("Chat stage=agentExecution route={} sessionId={} elapsedMs={} promptChars={}",
                        route, sessionId, elapsedMillis(agentExecutionStart), promptInput.length());
            }

            upsertSession(sessionId, request.getQuestion(), userId);

            ChatResponse response = new ChatResponse();
            response.setAnswer(result.content());
            response.setSessionId(sessionId);
            response.setSources(toSources(result.sources()));
            cacheFaqIfEligible(executionContext, ownerType, merchantId, request.getQuestion(), response, sessionId);
            logStructuredLatencyIfNeeded(route, sessionId, requestStart);
            logExecution(executionContext, selectedAgent == toolOnlyAgent ? "toolOnlyAgent" : "ragAgent", 1, false, requestStart);
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

    private String ensureSessionId(ChatRequest request) {
        String sessionId = StrUtil.blankToDefault(request.getSessionId(), UUID.randomUUID().toString());
        request.setSessionId(sessionId);
        return sessionId;
    }

    private ChatExecutionContext buildExecutionContext(String question, Long userId, String sessionId) {
        KnowledgeQueryFeatures features = knowledgeQueryClassifier.extractFeatures(question);
        KnowledgeQueryRoute route = features != null
                ? knowledgeQueryClassifier.classify(features)
                : knowledgeQueryClassifier.classify(question);
        KnowledgeLightRoute lightRoute = features != null
                ? knowledgeLightRouteDecider.decide(features, userId)
                : fallbackLightRoute(route, userId);
        return new ChatExecutionContext(sessionId, features, lightRoute, route);
    }

    private KnowledgeLightRoute fallbackLightRoute(KnowledgeQueryRoute route, Long userId) {
        if (route == null) {
            return KnowledgeLightRoute.RAG_FAQ_CHANNEL;
        }
        if (route == KnowledgeQueryRoute.RAG_FAQ) {
            return KnowledgeLightRoute.RAG_FAQ_CHANNEL;
        }
        if (route == KnowledgeQueryRoute.PRODUCT || route == KnowledgeQueryRoute.INVENTORY) {
            return KnowledgeLightRoute.TOOL_ONLY_AGENT_CHANNEL;
        }
        return userId != null ? KnowledgeLightRoute.FAST_PATH_CHANNEL : KnowledgeLightRoute.RAG_FAQ_CHANNEL;
    }

    private FaqCacheEntry findFaqCache(ChatExecutionContext context,
                                       String ownerType,
                                       Long merchantId,
                                       String question,
                                       String sessionId) {
        if (!faqCacheEnabled || !isFaqCacheEligible(context)) {
            return null;
        }
        String cacheKey = buildFaqCacheKey(ownerType, merchantId, question);
        FaqCacheEntry entry = findFaqCacheInRedis(cacheKey, sessionId);
        if (entry == null) {
            entry = findFaqCacheInLocal(cacheKey);
        }
        chatExecutionMetricsRecorder.recordFaqCache(entry == null ? "miss" : "hit", context.lightRoute(), context.route());
        log.info("Chat stage=faqCache event={} sessionId={} cacheKey={}",
                entry == null ? "miss" : "hit", sessionId, cacheKey);
        return entry;
    }

    private void cacheFaqIfEligible(ChatExecutionContext context,
                                    String ownerType,
                                    Long merchantId,
                                    String question,
                                    ChatResponse response,
                                    String sessionId) {
        if (!faqCacheEnabled || !isFaqCacheEligible(context) || response == null || StrUtil.isBlank(response.getAnswer())) {
            return;
        }
        String cacheKey = buildFaqCacheKey(ownerType, merchantId, question);
        FaqCacheEntry entry = new FaqCacheEntry(response.getAnswer(), response.getSources());
        cacheFaqInLocal(cacheKey, entry);
        cacheFaqInRedis(cacheKey, entry, sessionId);
        chatExecutionMetricsRecorder.recordFaqCache("store", context.lightRoute(), context.route());
        log.info("Chat stage=faqCache event=store sessionId={} cacheKey={}", sessionId, cacheKey);
    }

    private boolean isFaqCacheEligible(ChatExecutionContext context) {
        return context != null
                && context.lightRoute() == KnowledgeLightRoute.RAG_FAQ_CHANNEL
                && context.route() == KnowledgeQueryRoute.RAG_FAQ;
    }

    private String buildFaqCacheKey(String ownerType, Long merchantId, String question) {
        return FAQ_CACHE_KEY_PREFIX
                + StrUtil.blankToDefault(ownerType, OWNER_PLATFORM)
                + ":" + String.valueOf(merchantId)
                + ":" + StrUtil.trimToEmpty(question).toLowerCase(Locale.ROOT);
    }

    private FaqCacheEntry findFaqCacheInRedis(String cacheKey, String sessionId) {
        if (stringRedisTemplate == null || jsonMapper == null) {
            return null;
        }
        try {
            String payload = stringRedisTemplate.opsForValue().get(cacheKey);
            if (StrUtil.isBlank(payload)) {
                return null;
            }
            return jsonMapper.readValue(payload, FaqCacheEntry.class);
        } catch (Exception e) {
            log.warn("Chat stage=faqCache event=redisReadFallback sessionId={} cacheKey={} message={}",
                    sessionId, cacheKey, StrUtil.blankToDefault(e.getMessage(), e.getClass().getSimpleName()));
            return null;
        }
    }

    private FaqCacheEntry findFaqCacheInLocal(String cacheKey) {
        FaqCacheEntry entry = faqCache.get(cacheKey);
        if (entry != null && isFaqCacheExpired(entry)) {
            faqCache.remove(cacheKey, entry);
            return null;
        }
        return entry;
    }

    private void cacheFaqInLocal(String cacheKey, FaqCacheEntry entry) {
        evictFaqCacheIfNecessary();
        faqCache.put(cacheKey, entry);
    }

    private void cacheFaqInRedis(String cacheKey, FaqCacheEntry entry, String sessionId) {
        if (stringRedisTemplate == null || jsonMapper == null) {
            return;
        }
        try {
            String payload = jsonMapper.writeValueAsString(entry);
            Duration ttl = faqCacheTtlMs > 0L ? Duration.ofMillis(faqCacheTtlMs) : Duration.ofDays(1);
            stringRedisTemplate.opsForValue().set(cacheKey, payload, ttl);
        } catch (Exception e) {
            log.warn("Chat stage=faqCache event=redisWriteFallback sessionId={} cacheKey={} message={}",
                    sessionId, cacheKey, StrUtil.blankToDefault(e.getMessage(), e.getClass().getSimpleName()));
        }
    }

    private boolean isFaqCacheExpired(FaqCacheEntry entry) {
        if (entry == null || faqCacheTtlMs <= 0L) {
            return false;
        }
        return System.currentTimeMillis() - entry.cachedAtMillis() > faqCacheTtlMs;
    }

    private void evictFaqCacheIfNecessary() {
        if (faqCacheMaxEntries <= 0) {
            faqCache.clear();
            return;
        }
        while (faqCache.size() >= faqCacheMaxEntries) {
            String eldestKey = faqCache.entrySet().stream()
                    .min(Map.Entry.comparingByValue((left, right) -> Long.compare(left.cachedAtMillis(), right.cachedAtMillis())))
                    .map(Map.Entry::getKey)
                    .orElse(null);
            if (eldestKey == null) {
                return;
            }
            faqCache.remove(eldestKey);
        }
    }

    private void logExecution(ChatExecutionContext context,
                              String executionMode,
                              int agentCalls,
                              boolean degraded,
                              long requestStart) {
        Duration duration = Duration.ofNanos(System.nanoTime() - requestStart);
        chatExecutionMetricsRecorder.recordExecution(
                context.lightRoute(),
                context.route(),
                executionMode,
                agentCalls,
                degraded,
                duration);
        log.info("Chat stage=execution lightRoute={} route={} executionMode={} agentCalls={} degraded={} sessionId={} elapsedMs={}",
                context.lightRoute(),
                context.route(),
                executionMode,
                agentCalls,
                degraded,
                context.sessionId(),
                elapsedMillis(requestStart));
    }


    private KnowledgeAgent buildRagAgent(Filter filter) {
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(3)
                .minScore(0.60)
                .maxResults(RETRIEVAL_MAX_RESULTS)
                .minScore(RETRIEVAL_MIN_SCORE)
                .filter(filter)
                .build();

        RetrievalAugmentor augmentor = DefaultRetrievalAugmentor.builder()
                .contentRetriever(contentRetriever)
                .build();

        return AiServices.builder(KnowledgeAgent.class)
                .chatModel(chatModel)
                .retrievalAugmentor(augmentor)
                .chatMemoryProvider(memoryId -> getOrCreateChatMemory(ragChatMemories, String.valueOf(memoryId), MAX_RAG_MEMORY_MESSAGES))
                .tools(
                        productQueryTool,
                        inventoryQueryTool
                )
                .build();
    }

    private StreamingKnowledgeAgent buildStreamingRagAgent(Filter filter) {
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(RETRIEVAL_MAX_RESULTS)
                .minScore(RETRIEVAL_MIN_SCORE)
                .filter(filter)
                .build();

        RetrievalAugmentor augmentor = DefaultRetrievalAugmentor.builder()
                .contentRetriever(contentRetriever)
                .build();

        return AiServices.builder(StreamingKnowledgeAgent.class)
                .streamingChatModel(streamingChatModel)
                .retrievalAugmentor(augmentor)
                .chatMemoryProvider(memoryId -> getOrCreateChatMemory(ragChatMemories, String.valueOf(memoryId), MAX_RAG_MEMORY_MESSAGES))
                .tools(
                        productQueryTool,
                        inventoryQueryTool
                )
                .build();
    }

    private KnowledgeAgent buildToolOnlyAgent() {
        return AiServices.builder(KnowledgeAgent.class)
                .chatModel(chatModel)
                .chatMemoryProvider(memoryId -> getOrCreateChatMemory(toolChatMemories, String.valueOf(memoryId), MAX_TOOL_MEMORY_MESSAGES))
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

    private StreamingKnowledgeAgent buildStreamingToolOnlyAgent() {
        return AiServices.builder(StreamingKnowledgeAgent.class)
                .streamingChatModel(streamingChatModel)
                .chatMemoryProvider(memoryId -> getOrCreateChatMemory(toolChatMemories, String.valueOf(memoryId), MAX_TOOL_MEMORY_MESSAGES))
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
        builder.append("\n知识库范围：");
        if (OWNER_MERCHANT.equals(ownerType)) {
            builder.append("merchant");
            if (merchantId != null) {
                builder.append("，merchantId=").append(merchantId);
            }
        } else {
            builder.append("platform");
        }

        if (userId != null) {
            builder.append("\n登录用户：是");
            builder.append("\nuserId=").append(userId);
            if (StrUtil.isNotBlank(userType)) {
                builder.append("\nuserType=").append(userType);
            }
            builder.append("\n涉及我的订单、购物车、优惠券、地址、通知、支付时，优先使用工具查询当前用户数据。");
            builder.append("\n查不到就直接说未查到，不要编造。");
        } else {
            builder.append("\n登录用户：否");
            builder.append("\n涉及个人数据时，先提示登录后再查询，不要猜测结果。");
        }

        return builder.toString();
    }

    private ChatResponse buildResponse(String sessionId, String answer, List<ChatResponse.Source> sources) {
        ChatResponse response = new ChatResponse();
        response.setAnswer(answer);
        response.setSessionId(sessionId);
        response.setSources(sources);
        return response;
    }

    private String extractOrderNo(String question) {
        Matcher matcher = ORDER_NO_PATTERN.matcher(StrUtil.blankToDefault(question, ""));
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    private String joinAddress(com.ecommerce.knowledge.client.dto.AddressVO address) {
        return StrUtil.blankToDefault(address.getProvince(), "")
                + StrUtil.blankToDefault(address.getCity(), "")
                + StrUtil.blankToDefault(address.getDistrict(), "")
                + StrUtil.blankToDefault(address.getDetail(), "");
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

    private ChatMemory getOrCreateChatMemory(Map<String, ChatMemory> memories, String memoryId, int maxMessages) {
        return memories.computeIfAbsent(memoryId,
                ignored -> MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .maxMessages(maxMessages)
                        .build());
    }

    private void rememberFastPathTurn(String memoryId, String question, String answer) {
        ChatMemory chatMemory = getOrCreateChatMemory(toolChatMemories, memoryId, MAX_TOOL_MEMORY_MESSAGES);
        chatMemory.add(UserMessage.from(question));
        chatMemory.add(AiMessage.from(answer));
    }

    private KnowledgeAgent selectAgent(KnowledgeQueryRoute route, Long userId, KnowledgeAgent ragAgent) {
        if (route == KnowledgeQueryRoute.RAG_FAQ) {
            return ragAgent;
        }
        if (!toolOnlyAgentEnabled) {
            return ragAgent;
        }
        if (route == KnowledgeQueryRoute.PRODUCT || route == KnowledgeQueryRoute.INVENTORY) {
            return toolOnlyAgent;
        }
        return ragAgent;
    }

    private StreamingKnowledgeAgent selectStreamingAgent(KnowledgeQueryRoute route,
                                                         Long merchantId,
                                                         String ownerType) {
        StreamingKnowledgeAgent ragAgent = OWNER_MERCHANT.equals(ownerType)
                ? streamingMerchantAgents.computeIfAbsent(
                        merchantId, id -> buildStreamingRagAgent(buildOwnershipFilter(OWNER_MERCHANT, id)))
                : streamingPlatformAgent;
        if (route == KnowledgeQueryRoute.RAG_FAQ) {
            return ragAgent;
        }
        if (!toolOnlyAgentEnabled) {
            return ragAgent;
        }
        if (route == KnowledgeQueryRoute.PRODUCT || route == KnowledgeQueryRoute.INVENTORY) {
            return streamingToolOnlyAgent;
        }
        return ragAgent;
    }

    private long elapsedMillis(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }

    private void logStructuredLatencyIfNeeded(KnowledgeQueryRoute route, String sessionId, long requestStart) {
        if (!route.isStructured()) {
            return;
        }
        long elapsedMs = elapsedMillis(requestStart);
        if (elapsedMs > structuredQueryTimeoutMs) {
            log.warn("Chat stage=structuredLatency route={} sessionId={} elapsedMs={} thresholdMs={}",
                    route, sessionId, elapsedMs, structuredQueryTimeoutMs);
        }
    }

    protected void sendStreamEvent(SseEmitter emitter, String eventName, Object data) throws IOException {
        emitter.send(SseEmitter.event().name(eventName).data(data));
    }

    private FastPathDecision determineFastPath(KnowledgeQueryRoute route, String question, Long userId) {
        boolean candidate = route.isStructured() && userId != null;
        if (!candidate) {
            return new FastPathDecision(false, false, false, null);
        }
        if (!fastPathEnabled) {
            return new FastPathDecision(true, false, false, null);
        }

        return switch (route) {
            case AFTER_SALE -> executeRealtimeFastPath(route, "order", () -> buildAfterSaleAnswer(question));
            case ORDER_LIST -> executeRealtimeFastPath(route, "order", this::buildOrderListAnswer);
            case CART -> executeRealtimeFastPath(route, "cart", this::buildCartAnswer);
            case ADDRESS -> executeRealtimeFastPath(route, "address", this::buildAddressAnswer);
            case COUPON -> executeRealtimeFastPath(route, "coupon", this::buildCouponAnswer);
            case NOTIFICATION -> executeRealtimeFastPath(route, "notification", this::buildNotificationAnswer);
            case PAYMENT_BY_ORDER_NO -> buildPaymentFastPath(question);
            default -> new FastPathDecision(true, false, false, null);
        };
    }

    private FastPathDecision executeRealtimeFastPath(KnowledgeQueryRoute route, String toolName, Supplier<String> supplier) {
        long timeoutMs = Math.max(1L, Math.min(structuredQueryTimeoutMs, downstreamSoftTimeoutMs));
        long startNanos = System.nanoTime();
        AgentUserContext currentContext = AgentUserContextHolder.get();
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            if (currentContext != null) {
                AgentUserContextHolder.set(currentContext);
            }
            try {
                return supplier.get();
            } finally {
                AgentUserContextHolder.clear();
            }
        });
        try {
            String answer = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            chatExecutionMetricsRecorder.recordDownstream(toolName, route, Duration.ofNanos(System.nanoTime() - startNanos), false);
            return new FastPathDecision(true, true, true, answer, false);
        } catch (TimeoutException e) {
            future.cancel(true);
            log.warn("Chat stage=fastPathTimeout route={} timeoutMs={}", route, timeoutMs);
            chatExecutionMetricsRecorder.recordDownstream(toolName, route, Duration.ofNanos(System.nanoTime() - startNanos), true);
            return new FastPathDecision(true, true, true, buildSlowServiceFallback(route), true);
        } catch (Exception e) {
            future.cancel(true);
            log.warn("Chat stage=fastPathFallback route={} timeoutMs={} message={}",
                    route, timeoutMs, StrUtil.blankToDefault(e.getMessage(), e.getClass().getSimpleName()));
            chatExecutionMetricsRecorder.recordDownstream(toolName, route, Duration.ofNanos(System.nanoTime() - startNanos), false);
            return new FastPathDecision(true, true, true, buildSlowServiceFallback(route), true);
        }
    }

    private String buildSlowServiceFallback(KnowledgeQueryRoute route) {
        return switch (route) {
            case AFTER_SALE, ORDER_LIST -> "当前订单服务响应较慢，请稍后重试。";
            case CART -> "当前购物车服务响应较慢，请稍后重试。";
            case ADDRESS -> "当前地址服务响应较慢，请稍后重试。";
            case COUPON -> "当前优惠券服务响应较慢，请稍后重试。";
            case NOTIFICATION -> "当前通知服务响应较慢，请稍后重试。";
            case PAYMENT_BY_ORDER_NO -> "当前支付服务响应较慢，请稍后重试。";
            default -> "当前服务响应较慢，请稍后重试。";
        };
    }

    private FastPathDecision buildPaymentFastPath(String question) {
        String orderNo = extractOrderNo(question);
        if (StrUtil.isBlank(orderNo)) {
            return new FastPathDecision(true, false, false, null);
        }
        return executeRealtimeFastPath(KnowledgeQueryRoute.PAYMENT_BY_ORDER_NO, "payment", () -> buildPaymentAnswer(orderNo));
    }

    private ChatResponse buildFastPathResponse(String answer, String sessionId) {
        ChatResponse response = new ChatResponse();
        response.setAnswer(answer);
        response.setSessionId(sessionId);
        response.setSources(Collections.emptyList());
        return response;
    }

    private String buildAfterSaleAnswer(String question) {
        String orderNo = extractOrderNo(question);
        if (StrUtil.isNotBlank(orderNo)) {
            OrderVO order = timedDownstream("order", "queryOrderByNo", KnowledgeQueryRoute.AFTER_SALE,
                    () -> orderQueryTool.queryOrderByNo(orderNo));
            if (order == null) {
                return "未查询到订单号为 " + orderNo + " 的订单，请确认后再试。";
            }
            if (isAfterSaleEligible(order)) {
                return "该订单可申请退换货：\n- " + formatAfterSaleOrderLine(order)
                        + "\n退换货操作：进入订单详情后点击“申请售后”，选择退货退款或换货。"
                        + "\n请直接回复订单号，我继续帮你处理。";
            }
            return "订单 " + StrUtil.blankToDefault(order.getOrderNo(), orderNo)
                    + " 当前状态为 " + StrUtil.blankToDefault(order.getStatusText(), "未知")
                    + "，暂不在可申请售后的订单范围内。";
        }

        List<OrderSummaryVO> eligibleOrders = safeList(timedDownstream("order", "queryCurrentUserOrderSummaries", KnowledgeQueryRoute.AFTER_SALE,
                () -> orderQueryTool.queryCurrentUserOrderSummaries(5))).stream()
                .filter(this::isAfterSaleEligible)
                .toList();
        if (eligibleOrders.isEmpty()) {
            return "当前没有可申请退换货的已支付订单。";
        }
        return "可申请退换货的订单如下：\n" + formatAfterSaleOrderSummaryLines(eligibleOrders, 5)
                + "\n退换货操作：进入订单详情后点击“申请售后”，选择退货退款或换货。"
                + "\n请直接回复订单号，我继续帮你处理。";
    }

    private String buildOrderListAnswer() {
        List<OrderSummaryVO> orders = safeList(timedDownstream("order", "queryCurrentUserOrderSummaries", KnowledgeQueryRoute.ORDER_LIST,
                () -> orderQueryTool.queryCurrentUserOrderSummaries(5)));
        if (orders.isEmpty()) {
            return "暂未查询到最近订单。";
        }
        return "最近订单如下：\n" + formatOrderSummaryLines(orders, 5);
    }

    private String buildCartAnswer() {
        List<CartItemVO> cartItems = safeList(timedDownstream("cart", "queryCurrentUserCart", KnowledgeQueryRoute.CART,
                cartQueryTool::queryCurrentUserCart));
        if (cartItems.isEmpty()) {
            return "你的购物车还是空的。";
        }

        int itemCount = cartItems.stream()
                .map(CartItemVO::getQuantity)
                .filter(quantity -> quantity != null)
                .mapToInt(Integer::intValue)
                .sum();

        StringBuilder builder = new StringBuilder("购物车商品如下：");
        builder.append("\n- 商品总件数：").append(itemCount);
        int limit = Math.min(cartItems.size(), 5);
        for (int i = 0; i < limit; i++) {
            CartItemVO item = cartItems.get(i);
            builder.append("\n- ").append(StrUtil.blankToDefault(item.getName(), "未命名商品"))
                    .append(" x").append(item.getQuantity() == null ? 0 : item.getQuantity())
                    .append("｜单价：").append(formatAmount(item.getPrice())).append("元");
        }
        return builder.toString();
    }

    private String buildAddressAnswer() {
        List<AddressVO> addresses = safeList(timedDownstream("address", "queryCurrentUserAddresses", KnowledgeQueryRoute.ADDRESS,
                addressQueryTool::queryCurrentUserAddresses));
        if (addresses.isEmpty()) {
            return "暂未查询到收货地址。";
        }
        AddressVO defaultAddress = addresses.stream()
                .filter(address -> Integer.valueOf(1).equals(address.getIsDefault()))
                .findFirst()
                .orElse(addresses.getFirst());
        return "默认收货地址：\n- " + formatAddress(defaultAddress);
    }

    private String buildCouponAnswer() {
        List<CouponVO> coupons = safeList(timedDownstream("coupon", "queryCurrentUserCoupons", KnowledgeQueryRoute.COUPON,
                couponQueryTool::queryCurrentUserCoupons));
        if (coupons.isEmpty()) {
            return "暂未查询到可用优惠券。";
        }

        StringBuilder builder = new StringBuilder("你的优惠券如下：");
        int limit = Math.min(coupons.size(), 5);
        for (int i = 0; i < limit; i++) {
            CouponVO coupon = coupons.get(i);
            builder.append("\n- ").append(StrUtil.blankToDefault(coupon.getName(), "优惠券"))
                    .append("｜门槛：").append(formatAmount(coupon.getMinAmount())).append("元")
                    .append("｜有效期至：").append(formatDateTime(coupon.getEndTime()));
        }
        return builder.toString();
    }

    private String buildNotificationAnswer() {
        List<NotificationVO> notifications = safeList(timedDownstream("notification", "queryCurrentUserNotifications", KnowledgeQueryRoute.NOTIFICATION,
                notificationQueryTool::queryCurrentUserNotifications));
        if (notifications.isEmpty()) {
            return "暂未查询到最近通知。";
        }

        StringBuilder builder = new StringBuilder("最近通知如下：");
        int limit = Math.min(notifications.size(), 5);
        for (int i = 0; i < limit; i++) {
            NotificationVO notification = notifications.get(i);
            builder.append("\n- ").append(StrUtil.blankToDefault(notification.getTitle(), "通知"))
                    .append("｜时间：").append(formatDateTime(firstNonNull(notification.getSentAt(), notification.getCreatedAt())));
        }
        return builder.toString();
    }

    private String buildPaymentAnswer(String orderNo) {
        PaymentVO payment = timedDownstream("payment", "queryCurrentUserPaymentByOrderNo", KnowledgeQueryRoute.PAYMENT_BY_ORDER_NO,
                () -> paymentQueryTool.queryCurrentUserPaymentByOrderNo(orderNo));
        if (payment == null) {
            return "暂未查询到订单 " + orderNo + " 的支付信息。";
        }
        return "支付信息如下：\n- 订单号：" + StrUtil.blankToDefault(payment.getOrderNo(), orderNo)
                + "｜状态：" + StrUtil.blankToDefault(payment.getStatusText(), "未知")
                + "｜金额：" + formatAmount(payment.getAmount()) + "元"
                + "｜方式：" + StrUtil.blankToDefault(payment.getPayMethod(), "暂无");
    }

    private String formatOrderLines(List<OrderVO> orders, int limit) {
        StringBuilder builder = new StringBuilder();
        int size = Math.min(orders.size(), limit);
        for (int i = 0; i < size; i++) {
            if (i > 0) {
                builder.append('\n');
            }
            builder.append("- ").append(formatOrderLine(orders.get(i)));
        }
        return builder.toString();
    }

    private String formatOrderLine(OrderVO order) {
        return "订单号：" + StrUtil.blankToDefault(order.getOrderNo(), "暂无")
                + "｜状态：" + StrUtil.blankToDefault(order.getStatusText(), "未知")
                + "｜金额：" + formatAmount(order.getTotalAmount()) + "元"
                + "｜下单时间：" + formatDateTime(order.getCreatedAt());
    }

    private String formatOrderSummaryLines(List<OrderSummaryVO> orders, int limit) {
        StringBuilder builder = new StringBuilder();
        int size = Math.min(orders.size(), limit);
        for (int i = 0; i < size; i++) {
            if (i > 0) {
                builder.append('\n');
            }
            builder.append("- ").append(formatOrderSummaryLine(orders.get(i)));
        }
        return builder.toString();
    }

    private String formatOrderSummaryLine(OrderSummaryVO order) {
        return "订单号：" + StrUtil.blankToDefault(order.getOrderNo(), "暂无")
                + "｜状态：" + StrUtil.blankToDefault(order.getStatusText(), "未知")
                + "｜金额：" + formatAmount(order.getTotalAmount()) + "元"
                + "｜商品：" + StrUtil.blankToDefault(order.getItemSummary(), "商品信息暂无")
                + "｜下单时间：" + formatDateTime(order.getCreatedAt());
    }

    private String formatAfterSaleOrderLines(List<OrderVO> orders, int limit) {
        StringBuilder builder = new StringBuilder();
        int size = Math.min(orders.size(), limit);
        for (int i = 0; i < size; i++) {
            if (i > 0) {
                builder.append('\n');
            }
            builder.append("- ").append(formatAfterSaleOrderLine(orders.get(i)));
        }
        return builder.toString();
    }

    private String formatAfterSaleOrderLine(OrderVO order) {
        return "订单号：" + StrUtil.blankToDefault(order.getOrderNo(), "暂无")
                + "｜商品：" + formatOrderItems(order)
                + "｜金额：" + formatAmount(order.getTotalAmount()) + "元"
                + "｜状态：" + StrUtil.blankToDefault(order.getStatusText(), "未知");
    }

    private String formatAfterSaleOrderSummaryLines(List<OrderSummaryVO> orders, int limit) {
        StringBuilder builder = new StringBuilder();
        int size = Math.min(orders.size(), limit);
        for (int i = 0; i < size; i++) {
            if (i > 0) {
                builder.append('\n');
            }
            builder.append("- ").append(formatAfterSaleOrderSummaryLine(orders.get(i)));
        }
        return builder.toString();
    }

    private String formatAfterSaleOrderSummaryLine(OrderSummaryVO order) {
        return "订单号：" + StrUtil.blankToDefault(order.getOrderNo(), "暂无")
                + "｜商品：" + StrUtil.blankToDefault(order.getItemSummary(), "商品信息暂无")
                + "｜金额：" + formatAmount(order.getTotalAmount()) + "元"
                + "｜状态：" + StrUtil.blankToDefault(order.getStatusText(), "未知");
    }

    private boolean isAfterSaleEligible(OrderVO order) {
        return order != null && Integer.valueOf(PAID_ORDER_STATUS).equals(order.getStatus());
    }

    private boolean isAfterSaleEligible(OrderSummaryVO order) {
        return order != null && Integer.valueOf(PAID_ORDER_STATUS).equals(order.getStatus());
    }

    private String formatOrderItems(OrderVO order) {
        List<OrderVO.OrderItemVO> items = order == null || order.getItems() == null ? Collections.emptyList() : order.getItems();
        if (items.isEmpty()) {
            return "商品信息暂无";
        }
        return items.stream()
                .map(item -> {
                    String name = StrUtil.blankToDefault(item.getName(), "未命名商品");
                    Integer quantity = item.getQuantity();
                    return quantity != null && quantity > 1 ? name + " × " + quantity : name;
                })
                .limit(3)
                .reduce((left, right) -> left + "、" + right)
                .orElse("商品信息暂无");
    }

    private String formatAddress(AddressVO address) {
        return StrUtil.blankToDefault(address.getReceiverName(), "未知联系人")
                + "｜" + StrUtil.blankToDefault(address.getReceiverPhone(), "暂无电话")
                + "｜" + StrUtil.blankToDefault(address.getProvince(), "")
                + StrUtil.blankToDefault(address.getCity(), "")
                + StrUtil.blankToDefault(address.getDistrict(), "")
                + StrUtil.blankToDefault(address.getDetail(), "");
    }

    private String formatAmount(BigDecimal amount) {
        return amount == null ? "N/A" : amount.stripTrailingZeros().toPlainString();
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "N/A" : DATE_TIME_FORMATTER.format(value);
    }

    private <T> T firstNonNull(T first, T second) {
        return first != null ? first : second;
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }

    private <T> T timedDownstream(String toolName,
                                  String operation,
                                  KnowledgeQueryRoute route,
                                  Supplier<T> supplier) {
        long startNanos = System.nanoTime();
        boolean timedOut = false;
        try {
            return supplier.get();
        } catch (Exception e) {
            timedOut = isTimeoutException(e);
            throw e;
        } finally {
            chatExecutionMetricsRecorder.recordDownstream(
                    toolName,
                    operation,
                    route,
                    Duration.ofNanos(System.nanoTime() - startNanos),
                    timedOut);
        }
    }

    private boolean isTimeoutException(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        String simpleName = throwable.getClass().getSimpleName().toLowerCase(Locale.ROOT);
        if (simpleName.contains("timeout")) {
            return true;
        }
        String message = throwable.getMessage();
        return message != null && message.toLowerCase(Locale.ROOT).contains("timeout");
    }

    private record FastPathDecision(boolean candidate, boolean available, boolean executed, String answer, boolean degraded) {
        private FastPathDecision(boolean candidate, boolean available, boolean executed, String answer) {
            this(candidate, available, executed, answer, false);
        }
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
