# 知识库聊天三层路由优化设计

**日期**：2026-05-21  
**状态**：待评审

## 目标

优化 `ecommerce-knowledge` 的聊天执行链路，优先消灭无效 agent 调用，在不提交代码的前提下完成设计、计划、实现和验证记录，最终达到以下目标：

1. 将当前分散的快路径逻辑收口为统一框架
2. 引入三层路由模型：轻量路由 -> 细粒度 route 分类 -> 执行策略
3. 对实时业务问题提供明确的 1 秒 SLA：**1 秒内必须返回任何结果**
4. 仅为稳定 FAQ 增加缓存骨架，不缓存用户实时业务答案
5. 补齐 route、fast path、agent、下游工具耗时等关键观测信息
6. 在文档中持续记录“已做 / 未做 / 待验证 / 下次继续点”

## 范围

### 本次范围内

- `ecommerce-knowledge` 聊天主链路优化
- 三层路由框架设计与实现
- 统一快路径决策
- FAQ 缓存骨架
- 实时业务 1 秒 SLA 控制
- route 分布、agent 命中与耗时、下游工具耗时等埋点
- 单元测试补充
- 进度文档、设计文档、实施计划文档

### 本次范围外

- 不新增业务写操作能力
- 不缓存订单、支付、购物车、地址、优惠券、通知等实时业务答案
- 不引入新的独立聚合服务
- 不做与本次目标无关的大规模重构
- 不提交 `git commit`
- 不切换分支，继续在 `master` 上开发

## 当前现状

当前 `ChatServiceImpl` 中同时存在两套快路径逻辑：

1. `tryHandleFastPath(question, userId)`
2. `determineFastPath(route, question, userId)`

现有执行链路大致是：

1. 请求进入 `executeChat`
2. 先尝试旧 `tryHandleFastPath`
3. 再调用 `KnowledgeQueryClassifier.classify`
4. 再调用 `determineFastPath`
5. 再选择 `toolOnlyAgent` 或 `ragAgent`
6. 最终执行 agent 或返回快路径结果

这会带来几个问题：

- 快路径判断前后分裂，维护成本高
- 指标难以统一统计
- 同类规则可能在两处重复维护
- 后续加入 FAQ 缓存、轻量路由、SLA 预算时容易继续堆逻辑

另外，当前配置中：

- `knowledge.chat.structured-query-timeout-ms=1500`
- 未看到 `ecommerce-knowledge` 显式 Feign 超时配置
- 已引入 `spring-boot-starter-actuator`

这意味着当前链路还不满足“1 秒内必须返回任何结果”的目标，但项目具备补 Micrometer 指标的基础。

## 核心设计

### 一、三层路由模型

本次采用三层模型，但三层是**统一框架内部的职责拆分**，不是在现有链路外再包一层补丁。

#### 第一层：轻量路由

职责：只做粗粒度通道判断。

输出三个通道之一：

- `FAST_PATH_CHANNEL`
- `TOOL_ONLY_AGENT_CHANNEL`
- `RAG_FAQ_CHANNEL`

这一层只回答一个问题：

> 这条请求应该优先进入哪条大通道？

这一层**不允许**：

- 直接拼接订单、地址、支付等业务答案
- 替代细粒度 route 分类
- 自己长成第二套 `tryHandleFastPath`

#### 第二层：细粒度 route 分类

职责：做唯一权威业务意图判断。

输出现有或扩展后的 route，例如：

- `AFTER_SALE`
- `ORDER_LIST`
- `CART`
- `ADDRESS`
- `COUPON`
- `NOTIFICATION`
- `PAYMENT_BY_ORDER_NO`
- `PRODUCT`
- `INVENTORY`
- `RAG_FAQ`

这一层回答的问题是：

> 这条请求具体属于哪种业务意图？

轻量路由和细粒度 route 分类必须共享底层特征提取逻辑，避免各自维护一套关键词体系。

#### 第三层：执行策略

职责：根据“通道 + route”执行最终处理。

- `FAST_PATH_CHANNEL`
  - 执行确定性快路径
  - 适用于实时业务问题
  - 目标是在 1 秒内返回真实答案或降级答案

- `TOOL_ONLY_AGENT_CHANNEL`
  - 走 tool-only agent
  - 适用于需要工具但不需要 FAQ/RAG 的问题

- `RAG_FAQ_CHANNEL`
  - 先查 FAQ 缓存
  - 缓存未命中再走 RAG agent

## 二、实时业务 1 秒 SLA 设计

本次 SLA 定义不是“1 秒内必须拿到完整业务数据”，而是：

> **1 秒内必须返回任何结果**

因此本次设计保证的是：

- 下游快：返回真实业务答案
- 下游慢：返回降级答案

而不是无限等待下游结果。

### 时间预算建议

- 总预算：`1000ms`
- 轻量路由 + 细分类：`< 20ms`
- 本地决策与格式化：`< 50ms`
- 单次下游调用预算：`300ms - 500ms`
- 预留调度和波动缓冲：剩余预算

### 适用范围

以下实时业务 route 一律不走 RAG agent：

- `AFTER_SALE`
- `ORDER_LIST`
- `CART`
- `ADDRESS`
- `COUPON`
- `NOTIFICATION`
- `PAYMENT_BY_ORDER_NO`

### 超时与降级

知识服务必须对慢下游有上界控制：

- 订单服务慢：返回“当前订单服务响应较慢，请稍后重试”
- 地址服务慢：返回“当前地址服务响应较慢，请稍后重试”
- 优惠券服务慢：返回“当前优惠券服务响应较慢，请稍后重试”

本次设计要求同时考虑两层超时控制：

1. Feign 客户端显式超时
2. 知识服务本地超时兜底

这样即使 Feign 行为不稳定，也能保证知识服务不被无限拖住。

## 三、FAQ 缓存设计

### 缓存范围

只缓存稳定 FAQ：

- 仅当轻量路由结果为 `RAG_FAQ_CHANNEL`
- 且细粒度 route 为 `RAG_FAQ`

时，才允许进入 FAQ 缓存。

### 不缓存的内容

以下答案不允许进入缓存：

- 当前订单列表
- 售后可退换结果
- 购物车状态
- 用户地址
- 用户优惠券
- 用户通知
- 用户支付状态

原因很简单：这些数据是用户态实时业务数据，直接缓存回答有一致性和越权风险。

### 缓存形态

本次只做缓存骨架，优先满足：

- 可开关
- 可替换
- 可观测

第一版可先用内存级缓存骨架，后续再决定是否升级到更明确的缓存实现。

## 四、指标与观测设计

本次重点不是把所有日志都变成复杂监控，而是先把链路观测补完整。

### 必须补齐的指标

- route 分布
- 轻量路由分布
- fast path 命中率
- tool-only agent 命中率
- `RAG_FAQ` 占比
- `agentExecution` p50 / p95
- 下游工具接口耗时分布
- 单请求 agent 调用次数分布：
  - `0 次`
  - `1 次`
  - `多次`
- 实时业务 route 超时率
- 实时业务 route 降级率
- 1 秒 SLA 命中率

### 分层记录原则

为了避免数据失真，三层各自只记录自己的指标：

- 轻量路由层：记录 `lightRoute`
- 细分类层：记录 `route`
- 执行层：记录 `fastPathHit`、`toolOnlyAgentHit`、`ragFaqHit`、`agentCallCount`、`elapsedMs`
- 下游工具层：记录单次工具调用耗时和异常/超时

### 埋点实现建议

项目已引入 actuator，因此本次可以采用：

- 日志埋点：便于快速落地与现有测试兼容
- Micrometer 指标：用于后续汇总和观察分布

第一轮不要求把所有指标一次性做成复杂仪表盘，但必须把数据源先打出来。

## 五、代码结构设计

### 目标

避免 `ChatServiceImpl` 继续失控膨胀，但本轮也不做过度拆分。

### 建议的最小结构收口

可以在 `ChatServiceImpl` 内或邻近类中引入以下局部对象：

- `LightRouteDecision`
- `ExecutionPlan`
- `FastPathDecision`
- `FaqCacheKey`
- `ChatExecutionContext`
- `ChatMetricsRecorder`

这样至少让三层职责清楚，而不是继续堆 if/else。

### 统一执行上下文

`chat()` 和 `stream()` 必须共用同一份执行上下文，避免：

- SSE 先发出的 route 与真实执行路径不一致
- 观测数据无法对应

上下文中至少应包含：

- `sessionId`
- `lightRoute`
- `route`
- `executionMode`
- `fastPathHit`
- `agentCallCount`
- `degraded`
- `elapsedMs`

## 六、风险与规避

### 风险 1：轻量路由再次长成第二套规则系统

规避：

- 轻量路由只做粗分流
- 不允许直接返回业务答案

### 风险 2：轻量路由与细 route 分类不一致

规避：

- 共用底层特征提取
- 细 route 分类作为唯一权威判断

### 风险 3：三层只是叠加到旧链路上

规避：

- 三层是替换旧链路，不是套娃
- `tryHandleFastPath` 必须删除或彻底并入新框架

### 风险 4：1 秒 SLA 因层级增加而被拖慢

规避：

- 每层只做本层职责
- 粗分流和细分类必须轻量
- 实时业务避免串行多次下游调用

### 风险 5：`stream` 和普通 `chat` 行为不一致

规避：

- 两者共用统一执行上下文

### 风险 6：FAQ 缓存误命中

规避：

- 只有 `RAG_FAQ_CHANNEL + RAG_FAQ route` 才能查/写 FAQ 缓存

### 风险 7：指标重复统计导致判断失真

规避：

- 指标按层记录，责任清晰

### 风险 8：SSE 协议升级后旧调用方不兼容

规避：

- 统一流式协议新增 `start` 和 `chunk` 事件
- 保留最终 `answer` 事件，继续输出完整 `ChatResponse`
- `done` 和 `error` 语义保持不变

## 七、统一流式协议补充设计

### 目标

将 `stream()` 从“先同步执行 `chat()` 再一次性回包”升级为统一事件流，但不破坏现有依赖最终 `answer` 事件的调用方。

### 事件模型

统一事件顺序定义为：

1. `start`
2. `lightRoute`
3. `route`
4. `chunk`（0 次或多次）
5. `answer`
6. `done`

异常路径定义为：

1. `start`
2. `lightRoute`
3. `route`
4. `error`

其中：

- `start`：输出 `sessionId`
- `lightRoute`：输出一级轻量路由
- `route`：输出细粒度业务 route
- `chunk`：输出增量文本片段
- `answer`：输出完整 `ChatResponse`
- `done`：输出 `sessionId`
- `error`：输出可读错误信息

### 各执行路径的流式策略

- 实时业务快路径：
  - 不做 token 级流式
  - 直接发送一次 `chunk`
  - 然后发送完整 `answer`

- FAQ 缓存命中：
  - 不走 agent
  - 直接发送一次 `chunk`
  - 然后发送完整 `answer`

- agent 路径：
  - 使用 LangChain4j `StreamingChatModel` + `TokenStream`
  - `onPartialResponse` 持续发 `chunk`
  - `onCompleteResponse` 汇总为最终 `answer`

### agent 侧设计

需要新增一套流式 agent 接口，与同步 `KnowledgeAgent` 并存：

- 同步接口继续服务普通 `chat()`
- 流式接口仅服务 `stream()`

流式 agent 仍需复用现有：

- RAG 检索增强
- tool-only agent 工具集
- chat memory
- 统一系统提示词

### 兼容策略

- 旧调用方如果只消费 `answer`，行为不变
- 新调用方可以消费 `chunk` 做增量渲染
- 本轮不强制前端改造为只消费 `chunk`

### 上下文与工具调用

当前用户上下文依赖 `AgentUserContextHolder`。

为避免真流式时工具调用线程拿不到用户上下文，流式 agent 执行时必须：

- 在 `TokenStream.start()` 前设置上下文
- 在 `beforeToolExecution` 时补设上下文
- 在 `onToolExecuted`、`onCompleteResponse`、`onError` 后清理上下文

这样可以保证用户态工具调用仍然拿到当前会话用户。

## 八、实施原则

本次实施必须遵守以下约束：

1. 在 `master` 上开发
2. 不提交代码
3. 文档持续记录已做 / 未做 / 待验证
4. 不回滚当前工作区中的无关改动
5. 优先保证统一框架成立，再考虑局部优化

## 九、验收标准

本次设计完成后，实施应满足以下验收标准：

1. `tryHandleFastPath` 与 `determineFastPath` 不再并存为两套分流系统
2. 实时业务 route 默认不走 RAG agent
3. FAQ 缓存只作用于稳定 FAQ
4. 实时业务请求能够在 1 秒内返回真实结果或降级结果
5. route / fast path / agent / 下游工具耗时指标可观测
6. `chat` 与 `stream` 路由和执行结果一致
7. `stream` 对 agent 路径支持真流式 `chunk` 输出
8. 旧调用方继续可通过 `answer` 事件拿到完整结果
7. 文档中明确标记：
   - 本轮已完成
   - 本轮未完成
   - 已验证
   - 未验证
   - 下次继续点

## 十、本轮文档输出要求

除了代码实现，本轮还必须保留以下文档记录：

- 设计文档：本文件
- 实施计划文档
- `.planning` 目录中的任务计划、调研记录、进度记录

这些文档必须能支持下一次会话直接续做，而不需要重新整理上下文。
