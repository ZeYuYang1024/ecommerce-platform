package com.ecommerce.knowledge.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.knowledge.agent.AgentUserContext;
import com.ecommerce.knowledge.agent.AgentUserContextHolder;
import com.ecommerce.knowledge.agent.KnowledgeAgent;
import com.ecommerce.knowledge.chat.KnowledgeQueryClassifier;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    private static final int MAX_RAG_MEMORY_MESSAGES = 12;
    private static final int MAX_TOOL_MEMORY_MESSAGES = 8;
    private static final int MAX_SOURCE_LENGTH = 180;
    private static final int PAID_ORDER_STATUS = 1;
    private static final long DEFAULT_STREAM_TIMEOUT_MS = 60_000L;
    private static final String OWNER_PLATFORM = "platform";
    private static final String OWNER_MERCHANT = "merchant";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Pattern ORDER_NO_PATTERN = Pattern.compile("(?i)\\b(?:ord-?\\d+|\\d{12,32})\\b");

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
    private final KnowledgeQueryClassifier knowledgeQueryClassifier;

    private final Map<String, ChatMemory> ragChatMemories = new ConcurrentHashMap<>();
    private final Map<String, ChatMemory> toolChatMemories = new ConcurrentHashMap<>();
    private final Map<Long, KnowledgeAgent> merchantAgents = new ConcurrentHashMap<>();

    @Value("${knowledge.chat.fast-path-enabled:true}")
    private boolean fastPathEnabled = true;

    @Value("${knowledge.chat.tool-only-agent-enabled:true}")
    private boolean toolOnlyAgentEnabled = true;

    @Value("${knowledge.chat.streaming-enabled:true}")
    private boolean streamingEnabled = true;

    @Value("${knowledge.chat.structured-query-timeout-ms:1500}")
    private long structuredQueryTimeoutMs = 1500L;

    private KnowledgeAgent platformAgent;
    private KnowledgeAgent toolOnlyAgent;

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
                           KbChatSessionMapper chatSessionMapper,
                           KnowledgeQueryClassifier knowledgeQueryClassifier) {
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
        this.knowledgeQueryClassifier = knowledgeQueryClassifier;
    }

    @PostConstruct
    public void init() {
        this.platformAgent = buildRagAgent(buildOwnershipFilter(OWNER_PLATFORM, null));
        this.toolOnlyAgent = buildToolOnlyAgent();
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
        if (!streamingEnabled) {
            throw new BusinessException(KnowledgeErrorCode.VALIDATION_ERROR);
        }

        SseEmitter emitter = new SseEmitter(Math.max(DEFAULT_STREAM_TIMEOUT_MS, structuredQueryTimeoutMs));
        KnowledgeQueryRoute route = knowledgeQueryClassifier.classify(request.getQuestion());
        CompletableFuture.runAsync(() -> {
            try {
                sendStreamEvent(emitter, "route", route.name());
                ChatResponse response = chat(request, userId, userType);
                sendStreamEvent(emitter, "answer", response);
                sendStreamEvent(emitter, "done", response.getSessionId());
                emitter.complete();
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

    private ChatResponse executeChat(KnowledgeAgent agent,
                                     ChatRequest request,
                                     Long userId,
                                     String userType,
                                     Long merchantId,
                                     String ownerType) {
        long requestStart = System.nanoTime();
        if (StrUtil.isBlank(request.getQuestion())) {
            throw new BusinessException(KnowledgeErrorCode.VALIDATION_ERROR);
        }

        String sessionId = StrUtil.blankToDefault(request.getSessionId(), UUID.randomUUID().toString());
        String memoryId = buildMemoryId(ownerType, merchantId, sessionId);

        try {
            AgentUserContextHolder.set(new AgentUserContext(userId, userType));

            long routeSelectionStart = System.nanoTime();
            KnowledgeQueryRoute route = knowledgeQueryClassifier.classify(request.getQuestion());
            log.info("Chat stage=routeSelection route={} structured={} sessionId={} elapsedMs={}",
                    route, route.isStructured(), sessionId, elapsedMillis(routeSelectionStart));

            long fastPathStart = System.nanoTime();
            FastPathDecision fastPathDecision = determineFastPath(route, request.getQuestion(), userId);
            log.info("Chat stage=fastPath route={} candidate={} available={} executed={} sessionId={} elapsedMs={}",
                    route,
                    fastPathDecision.candidate(),
                    fastPathDecision.available(),
                    fastPathDecision.executed(),
                    sessionId,
                    elapsedMillis(fastPathStart));
            if (fastPathDecision.executed()) {
                rememberFastPathTurn(memoryId, request.getQuestion(), fastPathDecision.answer());
                upsertSession(sessionId, request.getQuestion(), userId);
                logStructuredLatencyIfNeeded(route, sessionId, requestStart);
                return buildFastPathResponse(fastPathDecision.answer(), sessionId);
            }

            KnowledgeAgent selectedAgent = selectAgent(route, userId, agent);
            log.info("Chat stage=agentSelection route={} agentType={} sessionId={}",
                    route, selectedAgent == toolOnlyAgent ? "toolOnly" : "rag", sessionId);

            long agentExecutionStart = System.nanoTime();
            dev.langchain4j.service.Result<String> result;
            try {
                result = selectedAgent.chat(memoryId, buildPromptInput(request.getQuestion(), userId, userType, ownerType, merchantId));
            } finally {
                log.info("Chat stage=agentExecution route={} sessionId={} elapsedMs={}",
                        route, sessionId, elapsedMillis(agentExecutionStart));
            }

            upsertSession(sessionId, request.getQuestion(), userId);

            ChatResponse response = new ChatResponse();
            response.setAnswer(result.content());
            response.setSessionId(sessionId);
            response.setSources(toSources(result.sources()));
            logStructuredLatencyIfNeeded(route, sessionId, requestStart);
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

    private KnowledgeAgent buildRagAgent(Filter filter) {
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(3)
                .minScore(0.60)
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
        return userId != null ? toolOnlyAgent : ragAgent;
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

    private void sendStreamEvent(SseEmitter emitter, String eventName, Object data) throws IOException {
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
            case AFTER_SALE -> new FastPathDecision(true, true, true, buildAfterSaleAnswer(question));
            case ORDER_LIST -> new FastPathDecision(true, true, true, buildOrderListAnswer());
            case CART -> new FastPathDecision(true, true, true, buildCartAnswer());
            case ADDRESS -> new FastPathDecision(true, true, true, buildAddressAnswer());
            case COUPON -> new FastPathDecision(true, true, true, buildCouponAnswer());
            case NOTIFICATION -> new FastPathDecision(true, true, true, buildNotificationAnswer());
            case PAYMENT_BY_ORDER_NO -> buildPaymentFastPath(question);
            default -> new FastPathDecision(true, false, false, null);
        };
    }

    private FastPathDecision buildPaymentFastPath(String question) {
        String orderNo = extractOrderNo(question);
        if (StrUtil.isBlank(orderNo)) {
            return new FastPathDecision(true, false, false, null);
        }
        return new FastPathDecision(true, true, true, buildPaymentAnswer(orderNo));
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
            OrderVO order = orderQueryTool.queryOrderByNo(orderNo);
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

        List<OrderSummaryVO> eligibleOrders = safeList(orderQueryTool.queryCurrentUserOrderSummaries(5)).stream()
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
        List<OrderSummaryVO> orders = safeList(orderQueryTool.queryCurrentUserOrderSummaries(5));
        if (orders.isEmpty()) {
            return "暂未查询到最近订单。";
        }
        return "最近订单如下：\n" + formatOrderSummaryLines(orders, 5);
    }

    private String buildCartAnswer() {
        List<CartItemVO> cartItems = safeList(cartQueryTool.queryCurrentUserCart());
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
        List<AddressVO> addresses = safeList(addressQueryTool.queryCurrentUserAddresses());
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
        List<CouponVO> coupons = safeList(couponQueryTool.queryCurrentUserCoupons());
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
        List<NotificationVO> notifications = safeList(notificationQueryTool.queryCurrentUserNotifications());
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
        PaymentVO payment = paymentQueryTool.queryCurrentUserPaymentByOrderNo(orderNo);
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

    private String extractOrderNo(String question) {
        if (StrUtil.isBlank(question)) {
            return null;
        }
        var matcher = ORDER_NO_PATTERN.matcher(question);
        return matcher.find() ? matcher.group() : null;
    }

    private record FastPathDecision(boolean candidate, boolean available, boolean executed, String answer) {
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
