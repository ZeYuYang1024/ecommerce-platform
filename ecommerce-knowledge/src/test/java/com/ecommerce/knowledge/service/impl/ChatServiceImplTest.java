package com.ecommerce.knowledge.service.impl;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.ecommerce.knowledge.agent.KnowledgeAgent;
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
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.service.Result;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    private ChatModel chatModel;

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

    private ChatServiceImpl createService(KnowledgeQueryClassifier classifier) {
        return new ChatServiceImpl(
                chatModel,
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
                classifier
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

    private static String findStageMessage(List<ILoggingEvent> events, String stage) {
        Optional<String> message = events.stream()
                .filter(event -> event.getLevel() == Level.INFO)
                .map(ILoggingEvent::getFormattedMessage)
                .filter(text -> text.contains("stage=" + stage))
                .findFirst();
        return message.orElse(null);
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
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
}
