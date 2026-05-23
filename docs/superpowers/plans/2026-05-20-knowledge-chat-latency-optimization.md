# Knowledge Chat Latency Optimization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reduce `ecommerce-knowledge` chat latency across the common request classes by routing structured queries away from the heavy agent path, shrinking downstream payload, and improving visibility.

**Architecture:** Keep `ChatServiceImpl` as the single entry point, but split its execution into explicit request classes. Structured personal-data questions should resolve through deterministic service code and lightweight downstream contracts. Only policy, FAQ, and mixed reasoning questions should use the retrieval-enabled LangChain agent.

**Tech Stack:** Java 21, Spring Boot 4, Spring Cloud OpenFeign, LangChain4j 1.14, MyBatis-Plus, JUnit 5, Mockito, Spring MVC test

---

## File Structure

### `ecommerce-knowledge`

- Create: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/chat/KnowledgeQueryRoute.java`
- Create: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/chat/KnowledgeQueryClassifier.java`
- Create: `ecommerce-knowledge/src/test/java/com/ecommerce/knowledge/chat/KnowledgeQueryClassifierTest.java`
- Create: `ecommerce-knowledge/src/test/java/com/ecommerce/knowledge/service/impl/ChatServiceImplTest.java`
- Modify: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/service/impl/ChatServiceImpl.java`
- Modify: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/agent/KnowledgeAgent.java`
- Modify: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/config/Langchain4jConfig.java`
- Modify: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/controller/ChatController.java`
- Modify: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/tool/OrderQueryTool.java`
- Modify: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/tool/AddressQueryTool.java`
- Modify: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/tool/CouponQueryTool.java`
- Create: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/client/dto/OrderSummaryVO.java`
- Create: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/client/dto/AddressSummaryVO.java`
- Create: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/client/dto/CouponSummaryVO.java`
- Modify: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/client/OrderClient.java`
- Modify: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/client/AddressClient.java`
- Modify: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/client/CouponClient.java`

### `ecommerce-order`

- Modify: `ecommerce-order/src/main/java/com/ecommerce/order/controller/OrderController.java`
- Modify: `ecommerce-order/src/main/java/com/ecommerce/order/service/OrderService.java`
- Modify: `ecommerce-order/src/main/java/com/ecommerce/order/service/impl/OrderServiceImpl.java`
- Modify: `ecommerce-order/src/test/java/com/ecommerce/order/controller/OrderControllerTest.java`
- Modify: `ecommerce-order/src/test/java/com/ecommerce/order/service/impl/OrderServiceImplTest.java`

### `ecommerce-user`

- Modify: `ecommerce-user/src/main/java/com/ecommerce/user/controller/AddressController.java`
- Modify: `ecommerce-user/src/main/java/com/ecommerce/user/service/AddressService.java`
- Modify: `ecommerce-user/src/main/java/com/ecommerce/user/service/impl/AddressServiceImpl.java`
- Modify: `ecommerce-user/src/test/java/com/ecommerce/user/controller/AddressControllerTest.java`
- Modify: `ecommerce-user/src/test/java/com/ecommerce/user/service/impl/AddressServiceImplTest.java`

### `ecommerce-coupon`

- Modify: `ecommerce-coupon/src/main/java/com/ecommerce/coupon/controller/CouponController.java`
- Modify: `ecommerce-coupon/src/main/java/com/ecommerce/coupon/service/CouponService.java`
- Modify: `ecommerce-coupon/src/main/java/com/ecommerce/coupon/service/impl/CouponServiceImpl.java`
- Create: `ecommerce-coupon/src/test/java/com/ecommerce/coupon/controller/CouponControllerTest.java`

## Task 1: Add Route Classification And Stage Timing

**Files:**
- Create: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/chat/KnowledgeQueryRoute.java`
- Create: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/chat/KnowledgeQueryClassifier.java`
- Create: `ecommerce-knowledge/src/test/java/com/ecommerce/knowledge/chat/KnowledgeQueryClassifierTest.java`
- Modify: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/service/impl/ChatServiceImpl.java`
- Create: `ecommerce-knowledge/src/test/java/com/ecommerce/knowledge/service/impl/ChatServiceImplTest.java`

- [ ] **Step 1: Write the failing classifier tests**

```java
// ecommerce-knowledge/src/test/java/com/ecommerce/knowledge/chat/KnowledgeQueryClassifierTest.java
@ExtendWith(MockitoExtension.class)
class KnowledgeQueryClassifierTest {

    private final KnowledgeQueryClassifier classifier = new KnowledgeQueryClassifier();

    @Test
    void classifyReturnsAfterSaleForReturnAndRefundQuestions() {
        assertThat(classifier.classify("如何退换货？")).isEqualTo(KnowledgeQueryRoute.AFTER_SALE);
        assertThat(classifier.classify("订单可以提现吗")).isEqualTo(KnowledgeQueryRoute.ORDER_DETAIL);
    }

    @Test
    void classifyReturnsStructuredForMyOrderQuestions() {
        assertThat(classifier.classify("我的订单有哪些")).isEqualTo(KnowledgeQueryRoute.ORDER_LIST);
        assertThat(classifier.classify("我的收货地址")).isEqualTo(KnowledgeQueryRoute.ADDRESS);
    }

    @Test
    void classifyReturnsRagForFaqStyleQuestions() {
        assertThat(classifier.classify("平台优惠券规则是什么")).isEqualTo(KnowledgeQueryRoute.RAG_FAQ);
    }
}
```

- [ ] **Step 2: Run the classifier test to verify it fails**

Run: `mvn -pl ecommerce-knowledge -Dtest=KnowledgeQueryClassifierTest test`

Expected: FAIL with missing classifier types and route enum.

- [ ] **Step 3: Add a small route enum and classifier**

```java
// ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/chat/KnowledgeQueryRoute.java
public enum KnowledgeQueryRoute {
    AFTER_SALE,
    ORDER_LIST,
    ORDER_DETAIL,
    PAYMENT,
    CART,
    ADDRESS,
    COUPON,
    NOTIFICATION,
    PRODUCT_SEARCH,
    RAG_FAQ
}

// ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/chat/KnowledgeQueryClassifier.java
@Component
public class KnowledgeQueryClassifier {

    public KnowledgeQueryRoute classify(String question) {
        String normalized = StrUtil.trimToEmpty(question);
        if (StrUtil.containsAny(normalized, "退货", "退款", "换货", "售后")) {
            return KnowledgeQueryRoute.AFTER_SALE;
        }
        if (StrUtil.containsAny(normalized, "购物车")) {
            return KnowledgeQueryRoute.CART;
        }
        if (StrUtil.containsAny(normalized, "订单")) {
            return KnowledgeQueryRoute.ORDER_LIST;
        }
        if (StrUtil.containsAny(normalized, "收货地址", "地址")) {
            return KnowledgeQueryRoute.ADDRESS;
        }
        if (StrUtil.containsAny(normalized, "优惠券", "券")) {
            return KnowledgeQueryRoute.COUPON;
        }
        if (StrUtil.containsAny(normalized, "通知", "消息")) {
            return KnowledgeQueryRoute.NOTIFICATION;
        }
        return KnowledgeQueryRoute.RAG_FAQ;
    }
}
```

- [ ] **Step 4: Add timing logs around route, fast path, and agent execution**

```java
// inside ChatServiceImpl.executeChat(...)
KnowledgeQueryRoute route = classifier.classify(request.getQuestion());
long startNs = System.nanoTime();
log.info("knowledge.chat.route sessionId={}, route={}, ownerType={}", sessionId, route, ownerType);

String fastPathAnswer = tryHandleFastPath(route, request.getQuestion(), userId);
if (fastPathAnswer != null) {
    log.info("knowledge.chat.fast-path sessionId={}, route={}, costMs={}",
            sessionId, route, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs));
    ...
}

long agentStart = System.nanoTime();
Result<String> result = agent.chat(memoryId, promptInput);
log.info("knowledge.chat.agent sessionId={}, route={}, costMs={}",
        sessionId, route, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - agentStart));
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `mvn -pl ecommerce-knowledge -Dtest=KnowledgeQueryClassifierTest,ChatServiceImplTest test`

Expected: PASS with route classification and stage logs wired into `ChatServiceImpl`.

- [ ] **Step 6: Commit**

```bash
git add ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/chat/KnowledgeQueryRoute.java ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/chat/KnowledgeQueryClassifier.java ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/service/impl/ChatServiceImpl.java ecommerce-knowledge/src/test/java/com/ecommerce/knowledge/chat/KnowledgeQueryClassifierTest.java ecommerce-knowledge/src/test/java/com/ecommerce/knowledge/service/impl/ChatServiceImplTest.java
git commit -m "feat: classify knowledge chat routes"
```

## Task 2: Expand Deterministic Fast Paths For Structured Queries

**Files:**
- Modify: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/service/impl/ChatServiceImpl.java`
- Modify: `ecommerce-knowledge/src/test/java/com/ecommerce/knowledge/service/impl/ChatServiceImplTest.java`

- [ ] **Step 1: Write the failing service tests for after-sale and structured questions**

```java
@Test
void chatReturnsAfterSaleTemplateWithoutCallingAgent() {
    ChatRequest request = new ChatRequest();
    request.setQuestion("如何退换货？");
    request.setSessionId("s-1");

    when(orderQueryTool.queryCurrentUserOrders()).thenReturn(List.of(paidOrder(), unpaidOrder()));

    ChatResponse response = service.chat(request, 1001L, "USER");

    assertThat(response.getAnswer()).contains("可退换货");
    verify(platformAgent, never()).chat(anyString(), anyString());
}

@Test
void chatReturnsOrderListFastPathWithoutCallingAgent() {
    ChatRequest request = new ChatRequest();
    request.setQuestion("我的订单有哪些");
    request.setSessionId("s-2");

    when(orderQueryTool.queryCurrentUserOrders()).thenReturn(List.of(paidOrder()));

    ChatResponse response = service.chat(request, 1001L, "USER");

    assertThat(response.getAnswer()).contains("订单号");
    verify(platformAgent, never()).chat(anyString(), anyString());
}
```

- [ ] **Step 2: Run the service test to verify it fails**

Run: `mvn -pl ecommerce-knowledge -Dtest=ChatServiceImplTest test`

Expected: FAIL because after-sale questions still fall through to the agent.

- [ ] **Step 3: Change `tryHandleFastPath` to accept the classified route and cover after-sale**

```java
private String tryHandleFastPath(KnowledgeQueryRoute route, String question, Long userId) {
    if (userId == null || route == KnowledgeQueryRoute.RAG_FAQ) {
        return null;
    }
    return switch (route) {
        case AFTER_SALE -> formatAfterSaleOrders(orderQueryTool.queryCurrentUserOrders());
        case ORDER_LIST -> formatOrders(orderQueryTool.queryCurrentUserOrders(), extractRequestedLimit(question, 3, 10));
        case PAYMENT -> handlePaymentQuestion(question);
        case ADDRESS -> handleAddressQuestion(question);
        case COUPON -> handleCouponQuestion(question);
        case NOTIFICATION -> formatNotifications(notificationQueryTool.queryCurrentUserNotifications());
        case CART -> handleCartQuestion(question);
        default -> null;
    };
}
```

- [ ] **Step 4: Add a dedicated formatter for after-sale eligibility**

```java
private String formatAfterSaleOrders(List<OrderVO> orders) {
    List<OrderVO> eligible = orders.stream()
            .filter(order -> Integer.valueOf(1).equals(order.getStatus()))
            .limit(5)
            .toList();
    if (eligible.isEmpty()) {
        return "当前没有可申请退换货的已支付订单。";
    }
    String body = eligible.stream()
            .map(order -> "| " + order.getOrderNo() + " | "
                    + firstItemName(order) + " | "
                    + amountText(order.getTotalAmount()) + " | "
                    + StrUtil.blankToDefault(order.getStatusText(), "已支付") + " |")
            .collect(Collectors.joining("\n"));
    return "目前可以申请退换货的订单如下：\n\n| 订单号 | 商品 | 金额 | 状态 |\n|---|---|---:|---|\n"
            + body
            + "\n\n请回复订单号，我可以继续帮你查看。";
}
```

- [ ] **Step 5: Run the service tests to verify they pass**

Run: `mvn -pl ecommerce-knowledge -Dtest=ChatServiceImplTest test`

Expected: PASS with structured questions returning without invoking the agent.

- [ ] **Step 6: Commit**

```bash
git add ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/service/impl/ChatServiceImpl.java ecommerce-knowledge/src/test/java/com/ecommerce/knowledge/service/impl/ChatServiceImplTest.java
git commit -m "feat: add deterministic fast paths for knowledge chat"
```

## Task 3: Split Tool-Only And RAG Agent Execution

**Files:**
- Modify: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/service/impl/ChatServiceImpl.java`
- Modify: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/agent/KnowledgeAgent.java`
- Modify: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/config/Langchain4jConfig.java`
- Modify: `ecommerce-knowledge/src/test/java/com/ecommerce/knowledge/service/impl/ChatServiceImplTest.java`

- [ ] **Step 1: Write the failing service test that structured routes must not use the RAG agent**

```java
@Test
void chatUsesToolOnlyAgentForProductAndStructuredQueries() {
    ChatRequest request = new ChatRequest();
    request.setQuestion("帮我看看 iPhone 15");
    request.setSessionId("s-3");

    when(toolOnlyAgent.chat(anyString(), anyString())).thenReturn(Result.from("ok"));

    service.chat(request, 1001L, "USER");

    verify(toolOnlyAgent).chat(anyString(), anyString());
    verify(ragAgent, never()).chat(anyString(), anyString());
}
```

- [ ] **Step 2: Run the service test to verify it fails**

Run: `mvn -pl ecommerce-knowledge -Dtest=ChatServiceImplTest test`

Expected: FAIL because only one retrieval-enabled agent exists today.

- [ ] **Step 3: Build two distinct agents in `ChatServiceImpl`**

```java
private KnowledgeAgent buildToolOnlyAgent() {
    return AiServices.builder(KnowledgeAgent.class)
            .chatModel(chatModel)
            .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                    .id(String.valueOf(memoryId))
                    .maxMessages(8)
                    .build())
            .tools(productQueryTool, orderQueryTool, inventoryQueryTool, couponQueryTool,
                    cartQueryTool, addressQueryTool, notificationQueryTool, paymentQueryTool)
            .build();
}

private KnowledgeAgent buildRagAgent(Filter filter) {
    ContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
            .embeddingStore(embeddingStore)
            .embeddingModel(embeddingModel)
            .maxResults(3)
            .minScore(0.60)
            .filter(filter)
            .build();
    return AiServices.builder(KnowledgeAgent.class)
            .chatModel(chatModel)
            .retrievalAugmentor(DefaultRetrievalAugmentor.builder().contentRetriever(retriever).build())
            .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                    .id(String.valueOf(memoryId))
                    .maxMessages(12)
                    .build())
            .tools(productQueryTool, inventoryQueryTool)
            .build();
}
```

- [ ] **Step 4: Route only FAQ-style questions to the RAG agent**

```java
KnowledgeAgent selectedAgent = route == KnowledgeQueryRoute.RAG_FAQ ? ragAgent : toolOnlyAgent;
Result<String> result = selectedAgent.chat(memoryId, promptInput);
```

- [ ] **Step 5: Lower the model budget for the chat model**

```java
// Langchain4jConfig.java
@Value("${langchain4j.open-ai.chat-model.max-tokens:512}")
private int maxTokens;

@Value("${langchain4j.open-ai.chat-model.temperature:0.2}")
private double temperature;
```

- [ ] **Step 6: Run the tests to verify they pass**

Run: `mvn -pl ecommerce-knowledge -Dtest=ChatServiceImplTest,KnowledgeQueryClassifierTest test`

Expected: PASS with structured routes bypassing retrieval and smaller memory windows configured.

- [ ] **Step 7: Commit**

```bash
git add ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/service/impl/ChatServiceImpl.java ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/agent/KnowledgeAgent.java ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/config/Langchain4jConfig.java ecommerce-knowledge/src/test/java/com/ecommerce/knowledge/service/impl/ChatServiceImplTest.java
git commit -m "refactor: split knowledge tool and rag paths"
```

## Task 4: Add Lightweight Summary Contracts For Hot Queries

**Files:**
- Create: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/client/dto/OrderSummaryVO.java`
- Create: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/client/dto/AddressSummaryVO.java`
- Create: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/client/dto/CouponSummaryVO.java`
- Modify: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/client/OrderClient.java`
- Modify: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/client/AddressClient.java`
- Modify: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/client/CouponClient.java`
- Modify: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/tool/OrderQueryTool.java`
- Modify: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/tool/AddressQueryTool.java`
- Modify: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/tool/CouponQueryTool.java`
- Modify: `ecommerce-order/src/main/java/com/ecommerce/order/controller/OrderController.java`
- Modify: `ecommerce-order/src/main/java/com/ecommerce/order/service/OrderService.java`
- Modify: `ecommerce-order/src/main/java/com/ecommerce/order/service/impl/OrderServiceImpl.java`
- Modify: `ecommerce-order/src/test/java/com/ecommerce/order/controller/OrderControllerTest.java`
- Modify: `ecommerce-order/src/test/java/com/ecommerce/order/service/impl/OrderServiceImplTest.java`
- Modify: `ecommerce-user/src/main/java/com/ecommerce/user/controller/AddressController.java`
- Modify: `ecommerce-user/src/main/java/com/ecommerce/user/service/AddressService.java`
- Modify: `ecommerce-user/src/main/java/com/ecommerce/user/service/impl/AddressServiceImpl.java`
- Modify: `ecommerce-user/src/test/java/com/ecommerce/user/controller/AddressControllerTest.java`
- Modify: `ecommerce-user/src/test/java/com/ecommerce/user/service/impl/AddressServiceImplTest.java`
- Modify: `ecommerce-coupon/src/main/java/com/ecommerce/coupon/controller/CouponController.java`
- Modify: `ecommerce-coupon/src/main/java/com/ecommerce/coupon/service/CouponService.java`
- Modify: `ecommerce-coupon/src/main/java/com/ecommerce/coupon/service/impl/CouponServiceImpl.java`
- Create: `ecommerce-coupon/src/test/java/com/ecommerce/coupon/controller/CouponControllerTest.java`

- [ ] **Step 1: Write the failing tests for summary endpoints**

```java
// add to OrderControllerTest
@Test
void shouldReturnCurrentUserOrderSummaries() throws Exception {
    when(orderService.listOrderSummaries(1001L, 5)).thenReturn(List.of(summary("ORD-1")));

    mockMvc.perform(get("/api/v1/orders/summaries")
                    .header("X-User-Id", "1001")
                    .param("limit", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].orderNo").value("ORD-1"));
}

// add to AddressControllerTest
@Test
void shouldReturnDefaultAddressSummary() throws Exception {
    when(addressService.getDefaultSummary(1001L)).thenReturn(addressSummary());

    mockMvc.perform(get("/api/v1/users/addresses/default")
                    .header("X-User-Id", "1001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.receiverName").value("Alice"));
}
```

- [ ] **Step 2: Run the focused endpoint tests to verify they fail**

Run: `mvn -pl ecommerce-order,ecommerce-user,ecommerce-coupon -Dtest=OrderControllerTest,AddressControllerTest,CouponControllerTest test`

Expected: FAIL with missing summary contracts.

- [ ] **Step 3: Add summary endpoints instead of reusing heavy page payloads**

```java
// ecommerce-order/src/main/java/com/ecommerce/order/controller/OrderController.java
@GetMapping("/orders/summaries")
public Result<List<OrderSummaryVO>> summaries(@RequestHeader("X-User-Id") Long userId,
                                              @RequestParam(defaultValue = "5") int limit) {
    return Result.ok(orderService.listOrderSummaries(userId, limit));
}

// ecommerce-user/src/main/java/com/ecommerce/user/controller/AddressController.java
@GetMapping("/addresses/default")
public Result<AddressVO> defaultAddress(@RequestHeader("X-User-Id") Long userId) {
    return Result.ok(addressService.defaultAddress(userId));
}

// ecommerce-coupon/src/main/java/com/ecommerce/coupon/controller/CouponController.java
@GetMapping("/coupons/mine/summaries")
public Result<List<CouponVO>> myCouponSummaries(@RequestHeader("X-User-Id") Long userId) {
    return Result.ok(couponService.listUserCouponSummaries(userId));
}
```

- [ ] **Step 4: Update knowledge tools to use summary endpoints**

```java
// OrderQueryTool.java
public List<OrderSummaryVO> queryCurrentUserOrderSummaries(int limit) {
    Long userId = currentUserId();
    if (userId == null) {
        return Collections.emptyList();
    }
    var result = orderClient.listSummaries(userId, limit);
    return result != null && result.getData() != null ? result.getData() : Collections.emptyList();
}

// AddressQueryTool.java
public AddressSummaryVO queryCurrentUserDefaultAddressSummary() {
    ...
}
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `mvn -pl ecommerce-order,ecommerce-user,ecommerce-coupon,ecommerce-knowledge -Dtest=OrderControllerTest,AddressControllerTest,CouponControllerTest,ChatServiceImplTest test`

Expected: PASS with summary endpoints available and knowledge tools switched to lighter payloads.

- [ ] **Step 6: Commit**

```bash
git add ecommerce-order/src/main/java/com/ecommerce/order/controller/OrderController.java ecommerce-order/src/main/java/com/ecommerce/order/service/OrderService.java ecommerce-order/src/main/java/com/ecommerce/order/service/impl/OrderServiceImpl.java ecommerce-user/src/main/java/com/ecommerce/user/controller/AddressController.java ecommerce-user/src/main/java/com/ecommerce/user/service/AddressService.java ecommerce-user/src/main/java/com/ecommerce/user/service/impl/AddressServiceImpl.java ecommerce-coupon/src/main/java/com/ecommerce/coupon/controller/CouponController.java ecommerce-coupon/src/main/java/com/ecommerce/coupon/service/CouponService.java ecommerce-coupon/src/main/java/com/ecommerce/coupon/service/impl/CouponServiceImpl.java ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/client/OrderClient.java ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/client/AddressClient.java ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/client/CouponClient.java ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/tool/OrderQueryTool.java ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/tool/AddressQueryTool.java ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/tool/CouponQueryTool.java
git commit -m "feat: add lightweight summary contracts for knowledge queries"
```

## Task 5: Add Streaming And Rollout Guards

**Files:**
- Modify: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/controller/ChatController.java`
- Modify: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/service/impl/ChatServiceImpl.java`
- Modify: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/config/Langchain4jConfig.java`
- Modify: `ecommerce-knowledge/src/test/java/com/ecommerce/knowledge/service/impl/ChatServiceImplTest.java`

- [ ] **Step 1: Write the failing controller test for a streaming endpoint**

```java
@Test
void shouldExposeStreamingChatEndpoint() throws Exception {
    mockMvc.perform(post("/api/v1/knowledge/chat/stream")
                    .header("X-User-Id", "1001")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"question\":\"平台退货规则是什么\"}"))
            .andExpect(status().isOk());
}
```

- [ ] **Step 2: Run the controller test to verify it fails**

Run: `mvn -pl ecommerce-knowledge -Dtest=ChatControllerTest test`

Expected: FAIL because the streaming endpoint does not exist.

- [ ] **Step 3: Add a streaming endpoint for agent-backed requests only**

```java
@PostMapping(path = "/api/v1/knowledge/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter streamChat(@RequestHeader(value = "X-User-Id", required = false) Long userId,
                             @RequestHeader(value = "X-User-Type", required = false) String userType,
                             @RequestBody ChatRequest request) {
    return chatService.stream(request, userId, userType);
}
```

- [ ] **Step 4: Add feature flags and latency budgets**

```yaml
knowledge:
  chat:
    fast-path-enabled: true
    tool-only-agent-enabled: true
    streaming-enabled: true
    structured-query-timeout-ms: 1500
```

- [ ] **Step 5: Run focused verification**

Run: `mvn -pl ecommerce-knowledge,ecommerce-order,ecommerce-user,ecommerce-coupon -Dtest=ChatServiceImplTest,KnowledgeQueryClassifierTest,OrderControllerTest,AddressControllerTest,CouponControllerTest test`

Expected: PASS with route, fast path, summary contracts, and streaming support all covered.

- [ ] **Step 6: Commit**

```bash
git add ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/controller/ChatController.java ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/service/impl/ChatServiceImpl.java ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/config/Langchain4jConfig.java ecommerce-knowledge/src/test/java/com/ecommerce/knowledge/service/impl/ChatServiceImplTest.java
git commit -m "feat: add streaming and rollout guards for knowledge chat"
```

## Rollout Order

1. Ship Task 1 alone and observe real route distribution for one day.
2. Ship Task 2 next; this should remove the worst structured-query latency immediately.
3. Ship Task 3 after route metrics confirm the heavy agent is still overused.
4. Ship Task 4 only for the endpoints with clear payload waste in production traces.
5. Ship Task 5 last because it changes the API surface and client behavior.

## Acceptance Metrics

- Structured routes should show zero retrieval hits in logs.
- Fast-path hit ratio should exceed 70% for user-scoped support questions.
- `knowledge.chat.agent` p95 should fall after route split even before streaming.
- Downstream order summary payload should be smaller than current `OrderVO` list responses.

## Open Questions

- Whether product search should stay on the tool-only agent or gain its own lightweight endpoint.
- Whether notification answers need summary DTOs or the current payload is already small enough.
- Whether the frontend is ready to consume SSE immediately, or if streaming should stay backend-dark at first.
