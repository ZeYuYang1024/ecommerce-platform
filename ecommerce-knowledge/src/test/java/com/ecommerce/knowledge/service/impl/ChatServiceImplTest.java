package com.ecommerce.knowledge.service.impl;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.ecommerce.knowledge.agent.KnowledgeAgent;
import com.ecommerce.knowledge.agent.StreamingKnowledgeAgent;
import com.ecommerce.knowledge.chat.KnowledgeLightRoute;
import com.ecommerce.knowledge.chat.KnowledgeQueryClassifier;
import com.ecommerce.knowledge.chat.KnowledgeQueryRoute;
import com.ecommerce.knowledge.client.dto.OrderSummaryVO;
import com.ecommerce.knowledge.client.dto.OrderVO;
import com.ecommerce.knowledge.client.dto.OrderVO.OrderItemVO;
import com.ecommerce.knowledge.dto.request.ChatRequest;
import com.ecommerce.knowledge.dto.response.ChatResponse;
import com.ecommerce.knowledge.mapper.KbChatSessionMapper;
import com.ecommerce.knowledge.tool.AddressQueryTool;
import com.ecommerce.knowledge.tool.CartQueryTool;
import com.ecommerce.knowledge.tool.CouponQueryTool;
import com.ecommerce.knowledge.tool.InventoryQueryTool;
import com.ecommerce.knowledge.tool.NotificationQueryTool;
import com.ecommerce.knowledge.tool.OrderQueryTool;
import com.ecommerce.knowledge.tool.PaymentQueryTool;
import com.ecommerce.knowledge.tool.ProductQueryTool;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.BeforeToolExecution;
import dev.langchain4j.service.tool.ToolExecution;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatServiceImplTest {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    @Mock
    private ChatModel chatModel;

    @Mock
    private StreamingChatModel streamingChatModel;

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private EmbeddingStore<TextSegment> embeddingStore;

    @Mock
    private ProductQueryTool productQueryTool;

    @Mock
    private OrderQueryTool orderQueryTool;

    @Mock
    private InventoryQueryTool inventoryQueryTool;

    @Mock
    private CouponQueryTool couponQueryTool;

    @Mock
    private CartQueryTool cartQueryTool;

    @Mock
    private AddressQueryTool addressQueryTool;

    @Mock
    private NotificationQueryTool notificationQueryTool;

    @Mock
    private PaymentQueryTool paymentQueryTool;

    @Mock
    private KbChatSessionMapper chatSessionMapper;

    @Mock
    private KnowledgeQueryClassifier knowledgeQueryClassifier;

    @Mock
    private KnowledgeAgent knowledgeAgent;

    @Mock
    private StreamingKnowledgeAgent streamingKnowledgeAgent;

    @Mock
    private ChatExecutionMetricsRecorder chatExecutionMetricsRecorder;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> stringValueOperations;

    @Test
    void chat_shouldHandleChineseAfterSaleQuestionWithEligibleOrdersWithoutCallingAgent() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setQuestion("如何退换货？");
        request.setSessionId("session-1");

        ChatServiceImpl service = createService(new KnowledgeQueryClassifier());
        when(orderQueryTool.queryCurrentUserOrderSummaries(5)).thenReturn(List.of(
                orderSummary("202605200001", 1, "已支付", "北欧风布艺沙发", new BigDecimal("2999")),
                orderSummary("202605200002", 0, "待支付", "测试商品", new BigDecimal("199"))
        ));
        setField(service, "platformAgent", knowledgeAgent);

        Logger logger = (Logger) LoggerFactory.getLogger(ChatServiceImpl.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            ChatResponse response = service.chat(request, 1001L, "USER");

            assertThat(response.getAnswer()).contains("可申请退换货");
            assertThat(response.getAnswer()).contains("202605200001");
            assertThat(response.getAnswer()).doesNotContain("202605200002");
            assertThat(response.getAnswer()).contains("请直接回复订单号");
            assertThat(response.getSources()).isEmpty();
            verify(orderQueryTool).queryCurrentUserOrderSummaries(5);
            verify(orderQueryTool, never()).queryCurrentUserOrders();
            verify(knowledgeAgent, never()).chat(anyString(), anyString());
            assertThat(findStageMessage(appender.list, "routeSelection"))
                    .contains("route=AFTER_SALE")
                    .contains("structured=true");
            assertThat(findStageMessage(appender.list, "fastPath"))
                    .contains("candidate=true")
                    .contains("available=true")
                    .contains("executed=true");
            assertThat(findStageMessage(appender.list, "agentExecution")).isNull();
        } finally {
            logger.detachAppender(appender);
        }
    }

    @Test
    void chat_shouldBypassAgentForStructuredOrderListQuestion() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setQuestion("show my order list");
        request.setSessionId("session-2");

        ChatServiceImpl service = createService(knowledgeQueryClassifier);
        OrderVO order = new OrderVO();
        order.setOrderNo("ORD-2");
        order.setStatusText("SHIPPED");
        order.setTotalAmount(new BigDecimal("199.00"));
        order.setCreatedAt(LocalDateTime.of(2026, 5, 19, 12, 0));
        when(knowledgeQueryClassifier.classify(request.getQuestion())).thenReturn(KnowledgeQueryRoute.ORDER_LIST);
        when(orderQueryTool.queryCurrentUserOrderSummaries(5)).thenReturn(List.of(
                orderSummary("ORD-2", 2, "SHIPPED", "Phone", new BigDecimal("199.00"))
        ));
        setField(service, "platformAgent", knowledgeAgent);

        ChatResponse response = service.chat(request, 1001L, "USER");

        assertThat(response.getAnswer()).contains("ORD-2");
        verify(orderQueryTool).queryCurrentUserOrderSummaries(5);
        verify(orderQueryTool, never()).queryCurrentUserOrders();
        verify(knowledgeAgent, never()).chat(anyString(), anyString());
    }

    @Test
    void chat_shouldUseAgentPathForOrderRuleFaqEvenWhenUserLoggedIn() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setQuestion("帮我查一下订单取消规则");
        request.setSessionId("session-4");

        ChatServiceImpl service = createService(new KnowledgeQueryClassifier());
        @SuppressWarnings("unchecked")
        Result<String> agentResult = org.mockito.Mockito.mock(Result.class);
        when(agentResult.content()).thenReturn("规则说明");
        when(agentResult.sources()).thenReturn(List.of());
        when(knowledgeAgent.chat(anyString(), anyString())).thenReturn(agentResult);
        setField(service, "platformAgent", knowledgeAgent);

        ChatResponse response = service.chat(request, 1001L, "USER");

        assertThat(response.getAnswer()).isEqualTo("规则说明");
        verify(knowledgeAgent).chat(anyString(), anyString());
        verify(orderQueryTool, never()).queryCurrentUserOrders();
    }

    @Test
    void chat_shouldUseToolOnlyAgentForStructuredProductQuestion() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setQuestion("帮我看看 iPhone 15");
        request.setSessionId("session-5");

        ChatServiceImpl service = createService(knowledgeQueryClassifier);
        @SuppressWarnings("unchecked")
        Result<String> toolResult = org.mockito.Mockito.mock(Result.class);
        KnowledgeAgent ragAgent = org.mockito.Mockito.mock(KnowledgeAgent.class);
        when(knowledgeQueryClassifier.classify(request.getQuestion())).thenReturn(KnowledgeQueryRoute.PRODUCT);
        when(toolResult.content()).thenReturn("tool answer");
        when(toolResult.sources()).thenReturn(List.of());
        when(knowledgeAgent.chat(anyString(), anyString())).thenReturn(toolResult);
        setField(service, "platformAgent", ragAgent);
        assertThat(setFieldIfPresent(service, "toolOnlyAgent", knowledgeAgent)).isTrue();

        ChatResponse response = service.chat(request, 1001L, "USER");

        assertThat(response.getAnswer()).isEqualTo("tool answer");
        verify(knowledgeAgent).chat(anyString(), anyString());
        verify(ragAgent, never()).chat(anyString(), anyString());
    }

    @Test
    void chat_shouldUseRagAgentForFaqQuestionWhenToolOnlyAgentExists() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setQuestion("平台退货规则是什么");
        request.setSessionId("session-6");

        ChatServiceImpl service = createService(knowledgeQueryClassifier);
        @SuppressWarnings("unchecked")
        Result<String> ragResult = org.mockito.Mockito.mock(Result.class);
        KnowledgeAgent toolOnlyAgent = org.mockito.Mockito.mock(KnowledgeAgent.class);
        when(knowledgeQueryClassifier.classify(request.getQuestion())).thenReturn(KnowledgeQueryRoute.RAG_FAQ);
        when(ragResult.content()).thenReturn("faq answer");
        when(ragResult.sources()).thenReturn(List.of());
        when(knowledgeAgent.chat(anyString(), anyString())).thenReturn(ragResult);
        setField(service, "platformAgent", knowledgeAgent);
        assertThat(setFieldIfPresent(service, "toolOnlyAgent", toolOnlyAgent)).isTrue();

        ChatResponse response = service.chat(request, 1001L, "USER");

        assertThat(response.getAnswer()).isEqualTo("faq answer");
        verify(knowledgeAgent).chat(anyString(), anyString());
        verify(toolOnlyAgent, never()).chat(anyString(), anyString());
    }

    @Test
    void chat_shouldFallbackToRagAgentWhenToolOnlyAgentFlagDisabled() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setQuestion("帮我看看 iPhone 15");
        request.setSessionId("session-7");

        ChatServiceImpl service = createService(knowledgeQueryClassifier);
        @SuppressWarnings("unchecked")
        Result<String> ragResult = org.mockito.Mockito.mock(Result.class);
        KnowledgeAgent toolOnlyAgent = org.mockito.Mockito.mock(KnowledgeAgent.class);
        when(knowledgeQueryClassifier.classify(request.getQuestion())).thenReturn(KnowledgeQueryRoute.PRODUCT);
        when(ragResult.content()).thenReturn("rag fallback");
        when(ragResult.sources()).thenReturn(List.of());
        when(knowledgeAgent.chat(anyString(), anyString())).thenReturn(ragResult);
        setField(service, "platformAgent", knowledgeAgent);
        assertThat(setFieldIfPresent(service, "toolOnlyAgent", toolOnlyAgent)).isTrue();
        setField(service, "toolOnlyAgentEnabled", false);

        ChatResponse response = service.chat(request, 1001L, "USER");

        assertThat(response.getAnswer()).isEqualTo("rag fallback");
        verify(knowledgeAgent).chat(anyString(), anyString());
        verify(toolOnlyAgent, never()).chat(anyString(), anyString());
    }

    @Test
    void chat_shouldSkipFastPathWhenFeatureFlagDisabled() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setQuestion("show my order list");
        request.setSessionId("session-8");

        ChatServiceImpl service = createService(knowledgeQueryClassifier);
        @SuppressWarnings("unchecked")
        Result<String> agentResult = org.mockito.Mockito.mock(Result.class);
        when(knowledgeQueryClassifier.classify(request.getQuestion())).thenReturn(KnowledgeQueryRoute.ORDER_LIST);
        when(agentResult.content()).thenReturn("agent answer");
        when(agentResult.sources()).thenReturn(List.of());
        when(knowledgeAgent.chat(anyString(), anyString())).thenReturn(agentResult);
        setField(service, "platformAgent", knowledgeAgent);
        assertThat(setFieldIfPresent(service, "toolOnlyAgent", knowledgeAgent)).isTrue();
        setField(service, "fastPathEnabled", false);

        ChatResponse response = service.chat(request, 1001L, "USER");

        assertThat(response.getAnswer()).isEqualTo("agent answer");
        verify(orderQueryTool, never()).queryCurrentUserOrderSummaries(5);
        verify(knowledgeAgent).chat(anyString(), anyString());
    }

    @Test
    void chat_shouldFallbackToRagAgentForRealtimeRouteWhenFastPathDisabled() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setQuestion("show my order list");
        request.setSessionId("session-8-rag");

        ChatServiceImpl service = createService(knowledgeQueryClassifier);
        KnowledgeAgent toolOnlyAgent = org.mockito.Mockito.mock(KnowledgeAgent.class);
        Result<String> ragResult = agentResult("rag order fallback");
        Result<String> toolResult = agentResult("tool order fallback");
        when(knowledgeQueryClassifier.classify(request.getQuestion())).thenReturn(KnowledgeQueryRoute.ORDER_LIST);
        when(knowledgeAgent.chat(anyString(), anyString())).thenReturn(ragResult);
        when(toolOnlyAgent.chat(anyString(), anyString())).thenReturn(toolResult);
        setField(service, "platformAgent", knowledgeAgent);
        assertThat(setFieldIfPresent(service, "toolOnlyAgent", toolOnlyAgent)).isTrue();
        setField(service, "fastPathEnabled", false);

        ChatResponse response = service.chat(request, 1001L, "USER");

        assertThat(response.getAnswer()).isEqualTo("rag order fallback");
        verify(orderQueryTool, never()).queryCurrentUserOrderSummaries(5);
        verify(knowledgeAgent).chat(anyString(), anyString());
        verify(toolOnlyAgent, never()).chat(anyString(), anyString());
    }

    @Test
    void chat_shouldUseAgentPathForFaqQuestion() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setQuestion("what is the return policy");
        request.setSessionId("session-3");

        ChatServiceImpl service = createService(knowledgeQueryClassifier);
        @SuppressWarnings("unchecked")
        Result<String> agentResult = org.mockito.Mockito.mock(Result.class);
        when(knowledgeQueryClassifier.classify(request.getQuestion())).thenReturn(KnowledgeQueryRoute.RAG_FAQ);
        when(agentResult.content()).thenReturn("faq answer");
        when(agentResult.sources()).thenReturn(List.of());
        when(knowledgeAgent.chat(anyString(), anyString())).thenReturn(agentResult);
        setField(service, "platformAgent", knowledgeAgent);

        Logger logger = (Logger) LoggerFactory.getLogger(ChatServiceImpl.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            ChatResponse response = service.chat(request, null, null);

            assertThat(response.getAnswer()).isEqualTo("faq answer");
            verify(knowledgeQueryClassifier).classify(request.getQuestion());
            verify(knowledgeAgent).chat(anyString(), anyString());
            assertThat(findStageMessage(appender.list, "lightRoute"))
                    .contains("lightRoute=RAG_FAQ_CHANNEL");
            assertThat(findStageMessage(appender.list, "routeSelection"))
                    .contains("route=RAG_FAQ")
                    .contains("structured=false");
            assertThat(findStageMessage(appender.list, "fastPath"))
                    .contains("candidate=false")
                    .contains("available=false")
                    .contains("executed=false");
            assertThat(findStageMessage(appender.list, "agentExecution"))
                    .contains("route=RAG_FAQ");
        } finally {
            logger.detachAppender(appender);
        }
    }

    @Test
    void chat_shouldReuseFaqCacheForStableFaqQuestion() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setQuestion("what is the return policy");
        request.setSessionId("session-cache-1");

        ChatServiceImpl service = createService(knowledgeQueryClassifier);
        when(knowledgeQueryClassifier.classify(request.getQuestion())).thenReturn(KnowledgeQueryRoute.RAG_FAQ);
        Result<String> faqResult = agentResult("faq answer");
        when(knowledgeAgent.chat(anyString(), anyString())).thenReturn(faqResult);
        setField(service, "platformAgent", knowledgeAgent);

        ChatResponse first = service.chat(request, null, null);
        ChatResponse second = service.chat(request, null, null);

        assertThat(first.getAnswer()).isEqualTo("faq answer");
        assertThat(second.getAnswer()).isEqualTo("faq answer");
        verify(knowledgeAgent, times(1)).chat(anyString(), anyString());
    }

    @Test
    void chat_shouldExpireFaqCacheEntryAfterTtl() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setQuestion("what is the return policy");
        request.setSessionId("session-cache-ttl");

        ChatServiceImpl service = createService(knowledgeQueryClassifier);
        when(knowledgeQueryClassifier.classify(request.getQuestion())).thenReturn(KnowledgeQueryRoute.RAG_FAQ);
        Result<String> firstAgentResult = agentResult("faq answer-1");
        Result<String> secondAgentResult = agentResult("faq answer-2");
        when(knowledgeAgent.chat(anyString(), anyString()))
                .thenReturn(firstAgentResult)
                .thenReturn(secondAgentResult);
        setField(service, "platformAgent", knowledgeAgent);
        setField(service, "faqCacheTtlMs", 5L);

        ChatResponse first = service.chat(request, null, null);
        Thread.sleep(10L);
        ChatResponse second = service.chat(request, null, null);

        assertThat(first.getAnswer()).isEqualTo("faq answer-1");
        assertThat(second.getAnswer()).isEqualTo("faq answer-2");
        verify(knowledgeAgent, times(2)).chat(anyString(), anyString());
    }

    @Test
    void chat_shouldEvictOldestFaqCacheEntryWhenCapacityExceeded() throws Exception {
        ChatRequest firstRequest = new ChatRequest();
        firstRequest.setQuestion("what is the return policy");
        firstRequest.setSessionId("session-cache-cap-1");
        ChatRequest secondRequest = new ChatRequest();
        secondRequest.setQuestion("how to refund");
        secondRequest.setSessionId("session-cache-cap-2");
        ChatRequest thirdRequest = new ChatRequest();
        thirdRequest.setQuestion("what is the return policy");
        thirdRequest.setSessionId("session-cache-cap-3");

        ChatServiceImpl service = createService(knowledgeQueryClassifier);
        when(knowledgeQueryClassifier.classify(anyString())).thenReturn(KnowledgeQueryRoute.RAG_FAQ);
        Result<String> firstAgentResult = agentResult("faq answer-1");
        Result<String> secondAgentResult = agentResult("faq answer-2");
        Result<String> thirdAgentResult = agentResult("faq answer-3");
        when(knowledgeAgent.chat(anyString(), anyString()))
                .thenReturn(firstAgentResult)
                .thenReturn(secondAgentResult)
                .thenReturn(thirdAgentResult);
        setField(service, "platformAgent", knowledgeAgent);
        setField(service, "faqCacheMaxEntries", 1);

        ChatResponse first = service.chat(firstRequest, null, null);
        ChatResponse second = service.chat(secondRequest, null, null);
        ChatResponse third = service.chat(thirdRequest, null, null);

        assertThat(first.getAnswer()).isEqualTo("faq answer-1");
        assertThat(second.getAnswer()).isEqualTo("faq answer-2");
        assertThat(third.getAnswer()).isEqualTo("faq answer-3");
        verify(knowledgeAgent, times(3)).chat(anyString(), anyString());
    }

    @Test
    void chat_shouldReuseDistributedFaqCacheFromRedisBeforeAgent() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setQuestion("what is the return policy");
        request.setSessionId("session-cache-redis-hit");

        ChatServiceImpl service = createService(knowledgeQueryClassifier, stringRedisTemplate);
        when(knowledgeQueryClassifier.classify(request.getQuestion())).thenReturn(KnowledgeQueryRoute.RAG_FAQ);
        when(stringRedisTemplate.opsForValue()).thenReturn(stringValueOperations);
        when(stringValueOperations.get("knowledge:faq:platform:null:what is the return policy"))
                .thenReturn(JSON_MAPPER.writeValueAsString(new FaqCacheEntry("faq from redis", List.of())));
        setField(service, "platformAgent", knowledgeAgent);

        ChatResponse response = service.chat(request, null, null);

        assertThat(response.getAnswer()).isEqualTo("faq from redis");
        verify(knowledgeAgent, never()).chat(anyString(), anyString());
    }

    @Test
    void chat_shouldFallbackToLocalFaqCacheWhenRedisUnavailable() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setQuestion("what is the return policy");
        request.setSessionId("session-cache-redis-fallback");

        ChatServiceImpl service = createService(knowledgeQueryClassifier, stringRedisTemplate);
        when(knowledgeQueryClassifier.classify(request.getQuestion())).thenReturn(KnowledgeQueryRoute.RAG_FAQ);
        when(stringRedisTemplate.opsForValue()).thenReturn(stringValueOperations);
        when(stringValueOperations.get(anyString())).thenThrow(new RuntimeException("redis down"));
        org.mockito.Mockito.doThrow(new RuntimeException("redis down"))
                .when(stringValueOperations)
                .set(anyString(), anyString(), any(java.time.Duration.class));
        Result<String> faqResult = agentResult("faq via local fallback");
        when(knowledgeAgent.chat(anyString(), anyString())).thenReturn(faqResult);
        setField(service, "platformAgent", knowledgeAgent);

        ChatResponse first = service.chat(request, null, null);
        ChatResponse second = service.chat(request, null, null);

        assertThat(first.getAnswer()).isEqualTo("faq via local fallback");
        assertThat(second.getAnswer()).isEqualTo("faq via local fallback");
        verify(knowledgeAgent, times(1)).chat(anyString(), anyString());
    }

    @Test
    void chat_shouldStoreFaqAnswerIntoRedisWithTtl() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setQuestion("what is the return policy");
        request.setSessionId("session-cache-redis-store");

        ChatServiceImpl service = createService(knowledgeQueryClassifier, stringRedisTemplate);
        when(knowledgeQueryClassifier.classify(request.getQuestion())).thenReturn(KnowledgeQueryRoute.RAG_FAQ);
        when(stringRedisTemplate.opsForValue()).thenReturn(stringValueOperations);
        Result<String> faqResult = agentResult("faq from agent");
        when(knowledgeAgent.chat(anyString(), anyString())).thenReturn(faqResult);
        setField(service, "platformAgent", knowledgeAgent);
        setField(service, "faqCacheTtlMs", 1234L);

        ChatResponse response = service.chat(request, null, null);

        assertThat(response.getAnswer()).isEqualTo("faq from agent");
        verify(stringValueOperations).set(
                eq("knowledge:faq:platform:null:what is the return policy"),
                anyString(),
                eq(java.time.Duration.ofMillis(1234L)));
    }

    @Test
    void chat_shouldLogFastPathLightRouteForStructuredQuestion() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setQuestion("show my order list");
        request.setSessionId("session-9");

        ChatServiceImpl service = createService(new KnowledgeQueryClassifier());
        when(orderQueryTool.queryCurrentUserOrderSummaries(5)).thenReturn(List.of(
                orderSummary("ORD-9", 1, "PAID", "Phone", new BigDecimal("199.00"))
        ));
        setField(service, "platformAgent", knowledgeAgent);

        Logger logger = (Logger) LoggerFactory.getLogger(ChatServiceImpl.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            ChatResponse response = service.chat(request, 1001L, "USER");

            assertThat(response.getAnswer()).contains("ORD-9");
            assertThat(findStageMessage(appender.list, "lightRoute"))
                    .contains("lightRoute=" + KnowledgeLightRoute.FAST_PATH_CHANNEL.name());
            assertThat(findStageMessage(appender.list, "routeSelection"))
                    .contains("route=ORDER_LIST");
            assertThat(findStageMessage(appender.list, "execution"))
                    .contains("agentCalls=0")
                    .contains("degraded=false");
            verify(knowledgeAgent, never()).chat(anyString(), anyString());
        } finally {
            logger.detachAppender(appender);
        }
    }

    @Test
    void chat_shouldRecordDownstreamOperationMetricsForOrderList() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setQuestion("show my order list");
        request.setSessionId("session-metrics-1");

        ChatServiceImpl service = createService(new KnowledgeQueryClassifier());
        when(orderQueryTool.queryCurrentUserOrderSummaries(5)).thenReturn(List.of(
                orderSummary("ORD-M1", 1, "PAID", "Phone", new BigDecimal("199.00"))
        ));
        setField(service, "platformAgent", knowledgeAgent);

        ChatResponse response = service.chat(request, 1001L, "USER");

        assertThat(response.getAnswer()).contains("ORD-M1");
        verify(chatExecutionMetricsRecorder).recordDownstream(
                eq("order"),
                eq("queryCurrentUserOrderSummaries"),
                eq(KnowledgeQueryRoute.ORDER_LIST),
                any(),
                eq(false));
    }

    @Test
    void chat_shouldReturnDegradedAnswerWhenRealtimeFastPathExceedsBudget() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setQuestion("show my order list");
        request.setSessionId("session-timeout-1");

        ChatServiceImpl service = createService(new KnowledgeQueryClassifier());
        when(orderQueryTool.queryCurrentUserOrderSummaries(5)).thenAnswer(invocation -> {
            Thread.sleep(30L);
            return List.of(orderSummary("ORD-SLOW", 1, "PAID", "Phone", new BigDecimal("199.00")));
        });
        setField(service, "platformAgent", knowledgeAgent);
        setField(service, "structuredQueryTimeoutMs", 1L);

        Logger logger = (Logger) LoggerFactory.getLogger(ChatServiceImpl.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            ChatResponse response = service.chat(request, 1001L, "USER");

            assertThat(response.getAnswer()).contains("响应较慢");
            assertThat(response.getAnswer()).doesNotContain("ORD-SLOW");
            assertThat(findStageMessage(appender.list, "execution"))
                    .contains("agentCalls=0")
                    .contains("degraded=true");
            verify(knowledgeAgent, never()).chat(anyString(), anyString());
        } finally {
            logger.detachAppender(appender);
        }
    }

    @Test
    void stream_shouldEmitUnifiedEventsForFastPath() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setQuestion("show my order list");
        request.setSessionId("stream-session-1");

        TestableChatServiceImpl service = createTestableService(knowledgeQueryClassifier);
        when(knowledgeQueryClassifier.classify(request.getQuestion())).thenReturn(KnowledgeQueryRoute.ORDER_LIST);
        when(orderQueryTool.queryCurrentUserOrderSummaries(5)).thenReturn(List.of(
                orderSummary("ORD-STREAM", 1, "PAID", "Phone", new BigDecimal("199.00"))
        ));
        setField(service, "platformAgent", knowledgeAgent);

        service.stream(request, 1001L, "USER");

        waitForCapturedEvents(service, 6);
        List<String> payloads = service.events();

        assertThat(payloads).hasSizeGreaterThanOrEqualTo(6);
        assertThat(payloads.get(0)).isEqualTo("start=stream-session-1");
        assertThat(payloads.get(1)).isEqualTo("lightRoute=FAST_PATH_CHANNEL");
        assertThat(payloads.get(2)).isEqualTo("route=ORDER_LIST");
        assertThat(payloads.get(3)).contains("chunk=").contains("ORD-STREAM");
        assertThat(payloads.get(4)).contains("answer=").contains("ORD-STREAM");
        assertThat(payloads.get(5)).isEqualTo("done=stream-session-1");
    }

    @Test
    void stream_shouldEmitChunksFromStreamingAgentAndFinalAnswer() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setQuestion("平台退货规则是什么");
        request.setSessionId("stream-session-agent");

        TestableChatServiceImpl service = createTestableService(knowledgeQueryClassifier);
        when(knowledgeQueryClassifier.classify(request.getQuestion())).thenReturn(KnowledgeQueryRoute.RAG_FAQ);
        when(streamingKnowledgeAgent.chat(anyString(), anyString()))
                .thenReturn(new TestTokenStream(List.of("第一段", "第二段"), "第一段第二段"));
        setField(service, "platformAgent", knowledgeAgent);
        setField(service, "streamingPlatformAgent", streamingKnowledgeAgent);

        service.stream(request, 1001L, "USER");

        waitForCapturedEvents(service, 7);
        List<String> payloads = service.events();

        assertThat(payloads.get(0)).isEqualTo("start=stream-session-agent");
        assertThat(payloads.get(1)).isEqualTo("lightRoute=RAG_FAQ_CHANNEL");
        assertThat(payloads.get(2)).isEqualTo("route=RAG_FAQ");
        assertThat(payloads.get(3)).isEqualTo("chunk=第一段");
        assertThat(payloads.get(4)).isEqualTo("chunk=第二段");
        assertThat(payloads.get(5)).contains("answer=").contains("第一段第二段");
        assertThat(payloads.get(6)).isEqualTo("done=stream-session-agent");
        verify(knowledgeAgent, never()).chat(anyString(), anyString());
    }

    private ChatServiceImpl createService(KnowledgeQueryClassifier classifier) {
        return createService(classifier, null);
    }

    private ChatServiceImpl createService(KnowledgeQueryClassifier classifier, StringRedisTemplate redisTemplate) {
        return new ChatServiceImpl(
                chatModel,
                streamingChatModel,
                embeddingModel,
                embeddingStore,
                productQueryTool,
                orderQueryTool,
                inventoryQueryTool,
                couponQueryTool,
                cartQueryTool,
                addressQueryTool,
                notificationQueryTool,
                paymentQueryTool,
                chatSessionMapper,
                chatExecutionMetricsRecorder,
                classifier,
                redisTemplate,
                JSON_MAPPER
        );
    }

    private TestableChatServiceImpl createTestableService(KnowledgeQueryClassifier classifier) {
        return createTestableService(classifier, null);
    }

    private TestableChatServiceImpl createTestableService(KnowledgeQueryClassifier classifier,
                                                          StringRedisTemplate redisTemplate) {
        return new TestableChatServiceImpl(
                chatModel,
                streamingChatModel,
                embeddingModel,
                embeddingStore,
                productQueryTool,
                orderQueryTool,
                inventoryQueryTool,
                couponQueryTool,
                cartQueryTool,
                addressQueryTool,
                notificationQueryTool,
                paymentQueryTool,
                chatSessionMapper,
                chatExecutionMetricsRecorder,
                classifier,
                redisTemplate,
                JSON_MAPPER
        );
    }

    private OrderVO order(String orderNo, int status, String statusText, String itemName, BigDecimal amount) {
        OrderItemVO item = new OrderItemVO();
        item.setName(itemName);
        item.setQuantity(1);

        OrderVO order = new OrderVO();
        order.setOrderNo(orderNo);
        order.setStatus(status);
        order.setStatusText(statusText);
        order.setTotalAmount(amount);
        order.setItems(List.of(item));
        order.setCreatedAt(LocalDateTime.of(2026, 5, 20, 10, 30));
        return order;
    }

    private OrderSummaryVO orderSummary(String orderNo, int status, String statusText, String itemSummary, BigDecimal amount) {
        OrderSummaryVO summary = new OrderSummaryVO();
        summary.setOrderNo(orderNo);
        summary.setStatus(status);
        summary.setStatusText(statusText);
        summary.setItemSummary(itemSummary);
        summary.setTotalAmount(amount);
        summary.setCreatedAt(LocalDateTime.of(2026, 5, 20, 10, 30));
        return summary;
    }

    @SuppressWarnings("unchecked")
    private Result<String> agentResult(String answer) {
        Result<String> result = org.mockito.Mockito.mock(Result.class);
        org.mockito.Mockito.doReturn(answer).when(result).content();
        org.mockito.Mockito.doReturn(List.of()).when(result).sources();
        return result;
    }

    private static String findStageMessage(List<ILoggingEvent> events, String stage) {
        Optional<String> message = events.stream()
                .filter(event -> event.getLevel() == Level.INFO)
                .map(ILoggingEvent::getFormattedMessage)
                .filter(text -> text.contains("stage=" + stage))
                .findFirst();
        return message.orElse(null);
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = findField(target.getClass(), fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static boolean setFieldIfPresent(Object target, String fieldName, Object value) {
        try {
            setField(target, fieldName, value);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static Field findField(Class<?> type, String fieldName) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }

    private static void waitForCapturedEvents(TestableChatServiceImpl service, int expectedSize) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (System.nanoTime() < deadline) {
            if (service.events().size() >= expectedSize) {
                return;
            }
            Thread.sleep(20L);
        }
        assertThat(service.events().size()).isGreaterThanOrEqualTo(expectedSize);
    }

    private static Object getField(Class<?> type, Object target, String fieldName) throws Exception {
        Field field = type.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private static final class TestableChatServiceImpl extends ChatServiceImpl {

        private final List<String> events = new ArrayList<>();

        private TestableChatServiceImpl(ChatModel chatModel,
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
                                        StringRedisTemplate redisTemplate,
                                        JsonMapper jsonMapper) {
            super(chatModel, streamingChatModel, embeddingModel, embeddingStore, productQueryTool, orderQueryTool, inventoryQueryTool,
                    couponQueryTool, cartQueryTool, addressQueryTool, notificationQueryTool, paymentQueryTool,
                    chatSessionMapper, chatExecutionMetricsRecorder, knowledgeQueryClassifier, redisTemplate, jsonMapper);
        }

        @Override
        protected void sendStreamEvent(SseEmitter emitter, String eventName, Object data) {
            events.add(eventName + "=" + String.valueOf(data));
        }

        private List<String> events() {
            return events;
        }
    }

    private static final class TestTokenStream implements TokenStream {

        private final List<String> chunks;
        private final String answer;
        private Consumer<String> partialResponseHandler;
        private Consumer<dev.langchain4j.model.chat.response.ChatResponse> completeResponseHandler;
        private Consumer<Throwable> errorHandler;

        private TestTokenStream(List<String> chunks, String answer) {
            this.chunks = chunks;
            this.answer = answer;
        }

        @Override
        public TokenStream onPartialResponse(Consumer<String> partialResponseHandler) {
            this.partialResponseHandler = partialResponseHandler;
            return this;
        }

        @Override
        public TokenStream onRetrieved(Consumer<List<Content>> contentsHandler) {
            return this;
        }

        @Override
        public TokenStream beforeToolExecution(Consumer<BeforeToolExecution> beforeToolExecutionHandler) {
            return this;
        }

        @Override
        public TokenStream onToolExecuted(Consumer<ToolExecution> toolExecutionHandler) {
            return this;
        }

        @Override
        public TokenStream onCompleteResponse(Consumer<dev.langchain4j.model.chat.response.ChatResponse> completeResponseHandler) {
            this.completeResponseHandler = completeResponseHandler;
            return this;
        }

        @Override
        public TokenStream onError(Consumer<Throwable> errorHandler) {
            this.errorHandler = errorHandler;
            return this;
        }

        @Override
        public TokenStream ignoreErrors() {
            return this;
        }

        @Override
        public void start() {
            try {
                for (String chunk : chunks) {
                    if (partialResponseHandler != null) {
                        partialResponseHandler.accept(chunk);
                    }
                }
                if (completeResponseHandler != null) {
                    completeResponseHandler.accept(dev.langchain4j.model.chat.response.ChatResponse.builder()
                            .aiMessage(dev.langchain4j.data.message.AiMessage.from(answer))
                            .build());
                }
            } catch (Throwable throwable) {
                if (errorHandler != null) {
                    errorHandler.accept(throwable);
                }
            }
        }
    }
}
