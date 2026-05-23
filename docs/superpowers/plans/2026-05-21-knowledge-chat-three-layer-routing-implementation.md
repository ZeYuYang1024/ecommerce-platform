# 知识库聊天三层路由优化实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `master` 工作区中实现知识库聊天三层路由框架，统一快路径，补 FAQ 缓存骨架和关键观测埋点，并以文档持续记录“已做 / 未做 / 待验证”，不提交代码。

**Architecture:** 保持 `ChatServiceImpl` 作为单入口，但将执行链路收口为“轻量路由 -> 细粒度 route 分类 -> 执行策略”。实时业务默认不走 RAG agent，使用硬超时预算和降级返回保证 1 秒内给出结果；FAQ 仅在 `RAG_FAQ` 路径上命中缓存或落缓存。

**Tech Stack:** Java 21, Spring Boot 4, Spring Cloud OpenFeign, Micrometer/Actuator, LangChain4j 1.14, JUnit 5, Mockito

---

## 文件结构

### `ecommerce-knowledge`

- Create: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/chat/KnowledgeLightRoute.java`
- Create: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/chat/KnowledgeQueryFeatures.java`
- Create: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/chat/KnowledgeLightRouteDecider.java`
- Create: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/service/impl/ChatExecutionContext.java`
- Create: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/service/impl/ChatExecutionMetricsRecorder.java`
- Create: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/service/impl/FaqCacheEntry.java`
- Modify: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/chat/KnowledgeQueryClassifier.java`
- Modify: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/service/impl/ChatServiceImpl.java`
- Modify: `ecommerce-knowledge/src/main/resources/application.yml`
- Modify: `ecommerce-knowledge/src/test/java/com/ecommerce/knowledge/chat/KnowledgeQueryClassifierTest.java`
- Create: `ecommerce-knowledge/src/test/java/com/ecommerce/knowledge/chat/KnowledgeLightRouteDeciderTest.java`
- Modify: `ecommerce-knowledge/src/test/java/com/ecommerce/knowledge/service/impl/ChatServiceImplTest.java`

## Task 1：建立三层路由骨架

**Files:**
- Create: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/chat/KnowledgeLightRoute.java`
- Create: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/chat/KnowledgeQueryFeatures.java`
- Create: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/chat/KnowledgeLightRouteDecider.java`
- Modify: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/chat/KnowledgeQueryClassifier.java`
- Create: `ecommerce-knowledge/src/test/java/com/ecommerce/knowledge/chat/KnowledgeLightRouteDeciderTest.java`
- Modify: `ecommerce-knowledge/src/test/java/com/ecommerce/knowledge/chat/KnowledgeQueryClassifierTest.java`

- [ ] **Step 1: 先写轻量路由与共享特征测试**

```java
@Test
void shouldRouteRealtimeUserQuestionsToFastPathChannel() {
    assertThat(decider.decide("我的订单有哪些", 1001L)).isEqualTo(KnowledgeLightRoute.FAST_PATH_CHANNEL);
    assertThat(decider.decide("帮我看看收货地址", 1001L)).isEqualTo(KnowledgeLightRoute.FAST_PATH_CHANNEL);
}

@Test
void shouldRouteFaqQuestionsToRagFaqChannel() {
    assertThat(decider.decide("平台优惠券规则是什么", 1001L)).isEqualTo(KnowledgeLightRoute.RAG_FAQ_CHANNEL);
}
```

- [ ] **Step 2: 运行测试确认先红**

Run: `mvn -pl ecommerce-knowledge "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=KnowledgeLightRouteDeciderTest,KnowledgeQueryClassifierTest" test`

Expected: FAIL，原因是轻量路由相关类型尚不存在。

- [ ] **Step 3: 最小实现共享特征提取与轻量路由**

```java
public record KnowledgeQueryFeatures(
        String normalizedQuestion,
        boolean userScoped,
        boolean hasOrderNo,
        boolean policyFaq,
        boolean realtimeIntent,
        boolean productIntent,
        boolean inventoryIntent) {
}
```

```java
public enum KnowledgeLightRoute {
    FAST_PATH_CHANNEL,
    TOOL_ONLY_AGENT_CHANNEL,
    RAG_FAQ_CHANNEL
}
```

```java
@Component
public class KnowledgeLightRouteDecider {

    public KnowledgeLightRoute decide(KnowledgeQueryFeatures features, Long userId) {
        if (features.policyFaq()) {
            return KnowledgeLightRoute.RAG_FAQ_CHANNEL;
        }
        if (userId != null && features.realtimeIntent()) {
            return KnowledgeLightRoute.FAST_PATH_CHANNEL;
        }
        if (features.productIntent() || features.inventoryIntent()) {
            return KnowledgeLightRoute.TOOL_ONLY_AGENT_CHANNEL;
        }
        return KnowledgeLightRoute.RAG_FAQ_CHANNEL;
    }
}
```

- [ ] **Step 4: 让 `KnowledgeQueryClassifier` 复用共享特征**

```java
public KnowledgeQueryRoute classify(KnowledgeQueryFeatures features) {
    if (features.hasOrderNo() && containsAny(features.normalizedQuestion(), "支付", "付款", "pay", "payment")) {
        return KnowledgeQueryRoute.PAYMENT_BY_ORDER_NO;
    }
    ...
}
```

- [ ] **Step 5: 运行测试确认转绿**

Run: `mvn -pl ecommerce-knowledge "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=KnowledgeLightRouteDeciderTest,KnowledgeQueryClassifierTest" test`

Expected: PASS，且轻量路由与细 route 分类共享同一套特征来源。

## Task 2：收口旧快路径并接入统一执行上下文

**Files:**
- Create: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/service/impl/ChatExecutionContext.java`
- Modify: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/service/impl/ChatServiceImpl.java`
- Modify: `ecommerce-knowledge/src/test/java/com/ecommerce/knowledge/service/impl/ChatServiceImplTest.java`

- [ ] **Step 1: 先写服务测试，证明实时业务走统一快路径且不走 agent**

```java
@Test
void chatShouldUseUnifiedFastPathForOrderList() throws Exception {
    ChatRequest request = new ChatRequest();
    request.setQuestion("show my order list");
    request.setSessionId("session-fast-1");

    when(knowledgeQueryClassifier.classify(any())).thenReturn(KnowledgeQueryRoute.ORDER_LIST);
    when(orderQueryTool.queryCurrentUserOrderSummaries(5)).thenReturn(List.of(orderSummary("ORD-1", 1, "PAID", "Phone", new BigDecimal("199.00"))));

    ChatResponse response = service.chat(request, 1001L, "USER");

    assertThat(response.getAnswer()).contains("ORD-1");
    verify(knowledgeAgent, never()).chat(anyString(), anyString());
}
```

- [ ] **Step 2: 运行服务测试确认先红**

Run: `mvn -pl ecommerce-knowledge "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=ChatServiceImplTest" test`

Expected: FAIL，原因是当前链路仍存在前置 `tryHandleFastPath` 和 route 后快路径双系统。

- [ ] **Step 3: 引入统一执行上下文并删除旧前置快路径入口**

```java
private ChatExecutionContext buildExecutionContext(String question, Long userId, String sessionId) {
    KnowledgeQueryFeatures features = knowledgeQueryClassifier.extractFeatures(question);
    KnowledgeLightRoute lightRoute = lightRouteDecider.decide(features, userId);
    KnowledgeQueryRoute route = knowledgeQueryClassifier.classify(features);
    return new ChatExecutionContext(sessionId, features, lightRoute, route);
}
```

```java
ChatExecutionContext context = buildExecutionContext(request.getQuestion(), userId, sessionId);
FastPathDecision decision = determineFastPath(context, userId);
```

- [ ] **Step 4: 让 `tryHandleFastPath` 下线或彻底并入 `determineFastPath`**

```java
// 删除 executeChat 中的这段：String fastPathAnswer = tryHandleFastPath(...)
// 所有快路径都从 determineFastPath(ChatExecutionContext, ...) 进入
```

- [ ] **Step 5: 运行服务测试确认转绿**

Run: `mvn -pl ecommerce-knowledge "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=ChatServiceImplTest" test`

Expected: PASS，实时业务 route 只通过统一框架命中快路径。

## Task 3：补实时业务 1 秒 SLA 控制与降级

**Files:**
- Modify: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/service/impl/ChatServiceImpl.java`
- Modify: `ecommerce-knowledge/src/main/resources/application.yml`
- Modify: `ecommerce-knowledge/src/test/java/com/ecommerce/knowledge/service/impl/ChatServiceImplTest.java`

- [ ] **Step 1: 先写降级测试**

```java
@Test
void chatShouldReturnDegradedAnswerWhenRealtimeToolTimesOut() throws Exception {
    ChatRequest request = new ChatRequest();
    request.setQuestion("我的订单有哪些");
    request.setSessionId("session-timeout-1");

    when(knowledgeQueryClassifier.classify(any())).thenReturn(KnowledgeQueryRoute.ORDER_LIST);
    when(orderQueryTool.queryCurrentUserOrderSummaries(5)).thenAnswer(invocation -> {
        Thread.sleep(700L);
        return List.of();
    });
    setField(service, "structuredQueryTimeoutMs", 300L);

    ChatResponse response = service.chat(request, 1001L, "USER");

    assertThat(response.getAnswer()).contains("响应较慢");
}
```

- [ ] **Step 2: 运行测试确认先红**

Run: `mvn -pl ecommerce-knowledge "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=ChatServiceImplTest" test`

Expected: FAIL，原因是当前快路径没有明确的本地超时与降级逻辑。

- [ ] **Step 3: 增加实时业务预算配置并调低默认上限**

```yaml
knowledge:
  chat:
    fast-path-enabled: true
    tool-only-agent-enabled: true
    streaming-enabled: true
    structured-query-timeout-ms: 1000
    downstream-soft-timeout-ms: 400
    faq-cache-enabled: true
```

- [ ] **Step 4: 对实时业务快路径加本地超时与降级包装**

```java
private String executeRealtimeFastPathWithBudget(Supplier<String> supplier, String fallback) {
    long start = System.nanoTime();
    try {
        String answer = supplier.get();
        if (elapsedMillis(start) > structuredQueryTimeoutMs) {
            return fallback;
        }
        return answer;
    } catch (Exception ex) {
        return fallback;
    }
}
```

- [ ] **Step 5: 运行测试确认转绿**

Run: `mvn -pl ecommerce-knowledge "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=ChatServiceImplTest" test`

Expected: PASS，超预算时返回降级结果而不是继续等待。

## Task 4：增加 FAQ 缓存骨架

**Files:**
- Create: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/service/impl/FaqCacheEntry.java`
- Modify: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/service/impl/ChatServiceImpl.java`
- Modify: `ecommerce-knowledge/src/test/java/com/ecommerce/knowledge/service/impl/ChatServiceImplTest.java`

- [ ] **Step 1: 先写 FAQ 缓存测试**

```java
@Test
void chatShouldReuseFaqCacheForStableFaqQuestion() throws Exception {
    ChatRequest request = new ChatRequest();
    request.setQuestion("what is the return policy");
    request.setSessionId("session-faq-1");

    when(knowledgeQueryClassifier.classify(any())).thenReturn(KnowledgeQueryRoute.RAG_FAQ);
    when(knowledgeAgent.chat(anyString(), anyString())).thenReturn(agentResult("faq answer"));

    ChatResponse first = service.chat(request, null, null);
    ChatResponse second = service.chat(request, null, null);

    assertThat(first.getAnswer()).isEqualTo("faq answer");
    assertThat(second.getAnswer()).isEqualTo("faq answer");
    verify(knowledgeAgent).chat(anyString(), anyString());
}
```

- [ ] **Step 2: 运行测试确认先红**

Run: `mvn -pl ecommerce-knowledge "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=ChatServiceImplTest" test`

Expected: FAIL，原因是当前没有 FAQ 缓存。

- [ ] **Step 3: 增加最小 FAQ 缓存骨架**

```java
private final Map<String, FaqCacheEntry> faqCache = new ConcurrentHashMap<>();

private String normalizeFaqKey(String ownerType, Long merchantId, String question) {
    return ownerType + ":" + String.valueOf(merchantId) + ":" + question.trim().toLowerCase(Locale.ROOT);
}
```

- [ ] **Step 4: 仅在 `RAG_FAQ_CHANNEL + RAG_FAQ` 下查写缓存**

```java
if (faqCacheEnabled && context.lightRoute() == KnowledgeLightRoute.RAG_FAQ_CHANNEL
        && context.route() == KnowledgeQueryRoute.RAG_FAQ) {
    ...
}
```

- [ ] **Step 5: 运行测试确认转绿**

Run: `mvn -pl ecommerce-knowledge "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=ChatServiceImplTest" test`

Expected: PASS，FAQ 第二次命中缓存，实时业务 route 不受影响。

## Task 5：补齐指标与执行日志

**Files:**
- Create: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/service/impl/ChatExecutionMetricsRecorder.java`
- Modify: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/service/impl/ChatServiceImpl.java`
- Modify: `ecommerce-knowledge/src/test/java/com/ecommerce/knowledge/service/impl/ChatServiceImplTest.java`

- [ ] **Step 1: 先写日志/指标测试**

```java
@Test
void chatShouldLogLightRouteRouteAndExecutionMode() throws Exception {
    ...
    assertThat(findStageMessage(appender.list, "lightRoute")).contains("lightRoute=FAST_PATH_CHANNEL");
    assertThat(findStageMessage(appender.list, "routeSelection")).contains("route=ORDER_LIST");
    assertThat(findStageMessage(appender.list, "execution")).contains("agentCalls=0");
}
```

- [ ] **Step 2: 运行测试确认先红**

Run: `mvn -pl ecommerce-knowledge "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=ChatServiceImplTest" test`

Expected: FAIL，原因是当前没有轻量路由和统一执行日志。

- [ ] **Step 3: 增加日志与 Micrometer 埋点**

```java
metricsRecorder.recordLightRoute(context.lightRoute(), ownerType);
metricsRecorder.recordRoute(context.route(), ownerType);
metricsRecorder.recordExecutionMode(context.executionMode(), context.agentCallCount(), context.degraded());
metricsRecorder.recordToolLatency("orderQueryTool", elapsedMs, timedOut);
```

- [ ] **Step 4: 输出单请求关键信息**

```java
log.info("Chat stage=execution lightRoute={} route={} executionMode={} agentCalls={} degraded={} sessionId={} elapsedMs={}",
        context.lightRoute(), context.route(), context.executionMode(), context.agentCallCount(),
        context.degraded(), sessionId, context.elapsedMs());
```

- [ ] **Step 5: 运行测试确认转绿**

Run: `mvn -pl ecommerce-knowledge "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=ChatServiceImplTest" test`

Expected: PASS，日志能够体现 lightRoute、route、executionMode、agentCalls、degraded。

## Task 6：同步更新 `.planning` 与验证记录

**Files:**
- Modify: `.planning/2026-05-21-knowledge-fast-path-optimization/task_plan.md`
- Modify: `.planning/2026-05-21-knowledge-fast-path-optimization/findings.md`
- Modify: `.planning/2026-05-21-knowledge-fast-path-optimization/progress.md`

- [ ] **Step 1: 每完成一个阶段就更新 planning 文件**

```text
- 已完成哪些任务
- 哪些项只落了骨架
- 哪些测试已验证
- 哪些测试未验证或失败
```

- [ ] **Step 2: 最终跑聚焦验证命令**

Run: `mvn -pl ecommerce-knowledge "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=KnowledgeLightRouteDeciderTest,KnowledgeQueryClassifierTest,ChatServiceImplTest" test`

Expected: PASS，至少核心三层路由、FAQ 缓存骨架、执行日志行为可验证。

- [ ] **Step 3: 在 planning 文件中明确写出未完成项**

```text
- Feign 客户端细粒度超时是否需要专门配置类：若本轮未落地，标记为未完成
- 指标是否全部进入统一监控面板：若本轮仅做日志/Micrometer 基础埋点，标记为后续项
- FAQ 缓存是否需要 TTL / 容量控制：若本轮只做骨架，标记为后续项
```

## 执行说明

- 本计划在当前 `master` 工作区执行，符合用户明确授权。
- 本计划不包含 commit 步骤，因为用户明确要求不提交代码。
- 如果执行中发现计划与代码现实冲突，以用户确认过的设计文档为准，并同步回写 `.planning` 文件。

## 当前实际状态（2026-05-22）

### 已完成
- 三层链路已落地：轻量路由 -> 细 route 分类 -> 执行策略。
- 旧 `tryHandleFastPath` 已从主链路移除。
- `chat` / `stream` 已共用同一份 `ChatExecutionContext`。
- 实时业务已接入本地软超时与降级返回，默认 `structured-query-timeout-ms=1000`、`downstream-soft-timeout-ms=400`。
- FAQ 缓存已加 TTL / 容量控制，且只在 `RAG_FAQ_CHANNEL + RAG_FAQ` 下命中。
- FAQ 分布式缓存已落地：`RAG_FAQ` 路径优先读写 Redis，Redis 异常时回退本地内存缓存。
- Redis 连接配置已改为环境变量友好形式：`REDIS_HOST`、`REDIS_PORT`、`REDIS_PASSWORD`。
- `toolOnlyAgent` 使用范围已收紧，仅 `PRODUCT` / `INVENTORY` 会走 tool-only agent；实时业务快路径不可用时回退 RAG agent。
- Micrometer 基础埋点已接入，Feign 超时已配置为 `connectTimeout=200`、`readTimeout=500`。
- 项目未使用 Grafana / Prometheus，本轮已撤销 Prometheus 依赖与 PromQL 看板文档；监控侧保持 Spring Boot Admin / Actuator / 结构化日志路线。
- `stream()` 已升级为统一流式协议：`start -> lightRoute -> route -> chunk... -> answer -> done`。
- agent 路径已接入 LangChain4j `StreamingChatModel` / `TokenStream`，支持真流式 `chunk` 输出。
- 最终 `answer` 事件仍然保留，兼容旧调用方。
- SSE 快路径与 agent 路径单测已补齐，聚焦测试已通过。

### 未完成
- 当前代码侧暂无新的未完成项。
- 若前端要展示打字机效果，需要消费新增的 `chunk` 事件；后端仍保留 `answer` 兼容事件。
- 当前不考虑 Grafana / Prometheus 技术栈。

## 当前追加任务（2026-05-22：统一流式协议）

### 目标

将 `stream()` 升级为统一流式协议：

- 事件顺序改为 `start -> lightRoute -> route -> chunk... -> answer -> done`
- 保留最终 `answer` 事件兼容旧调用方
- agent 路径使用 LangChain4j 真流式 `TokenStream`

### 预计改动文件

- Modify: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/agent/KnowledgeAgent.java`
- Create: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/agent/StreamingKnowledgeAgent.java`
- Create: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/agent/KnowledgeAgentSystemPrompt.java`
- Modify: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/config/Langchain4jConfig.java`
- Modify: `ecommerce-knowledge/src/main/java/com/ecommerce/knowledge/service/impl/ChatServiceImpl.java`
- Modify: `ecommerce-knowledge/src/test/java/com/ecommerce/knowledge/config/Langchain4jConfigTest.java`
- Modify: `ecommerce-knowledge/src/test/java/com/ecommerce/knowledge/service/impl/ChatServiceImplTest.java`
- Modify: `.planning/2026-05-21-knowledge-fast-path-optimization/task_plan.md`
- Modify: `.planning/2026-05-21-knowledge-fast-path-optimization/findings.md`
- Modify: `.planning/2026-05-21-knowledge-fast-path-optimization/progress.md`

### 实施要点

1. 已补失败测试：
   - 快路径 `stream()` 事件顺序变为 `start -> lightRoute -> route -> chunk -> answer -> done`
   - FAQ / agent 路径支持多次 `chunk`
2. 已新增 `StreamingKnowledgeAgent`
3. 已配置 `StreamingChatModel` bean
4. 已将 `ChatServiceImpl.stream()` 改成独立执行链路，不再调用同步 `executeChat()`
5. 已通过 `TokenStream.onRetrieved()` 收集来源，用于最终 `answer.sources`
6. 已通过 `beforeToolExecution` / `onToolExecuted` 维护 `AgentUserContextHolder`
7. 已跑聚焦测试并更新文档状态
