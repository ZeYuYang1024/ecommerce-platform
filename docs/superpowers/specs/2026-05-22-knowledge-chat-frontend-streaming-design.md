# 知识库聊天前端双端流式接入设计

## 1. 背景

`ecommerce-knowledge` 后端已经完成统一流式协议升级，当前 `stream()` 事件顺序为：

`start -> lightRoute -> route -> chunk... -> answer -> done`

并且仍保留最终 `answer` 事件，兼容旧调用方。

本轮新增需求不是继续优化后端路由，而是把这套流式能力真正接到两个前端：

1. 用户端 `ecommerce-web`
2. 管理端 `ecommerce-admin`

用户要求：

- 两个前端都要做
- 在 `master` 工作区继续推进
- 不提交代码
- 先做分析和评估
- 必须把“准备做什么、为什么这么做、哪些没做”写进文档

## 2. 本轮目标

本轮只做前端接入分析与实施评估，不做代码实现。

需要明确：

1. 两个前端当前是否已有知识问答入口
2. 现有入口是否已经消费流式接口
3. 用户端和管理端分别适合怎么接
4. 管理端要不要新建独立聊天页
5. 后端是否还缺少支撑管理端流式接入的接口
6. 后续真正落代码时的推荐路径、风险和测试边界

## 3. 非目标

本轮明确不做以下内容：

- 不改任何前端页面代码
- 不补任何前端测试
- 不调整现有后端流式协议
- 不引入 Grafana / Prometheus
- 不扩展到小程序端
- 不提交 git commit

## 4. 当前现状调研

### 4.1 用户端 `ecommerce-web`

用户端已经存在知识问答页面：

- 页面文件：`ecommerce-web/pages/knowledge.vue`
- 导航入口：`ecommerce-web/components/AppHeader.vue`

当前页面行为：

- 已有完整聊天 UI
- 当前调用同步接口 `POST /api/v1/knowledge/chat`
- 前端等待完整结果返回后，一次性渲染助手消息
- 已通过 `localStorage` 保存 `kb_session`
- 已有登录态错误提示

结论：

- 用户端**不是从零开始**
- 最合适的方式是**升级现有页面**
- 没必要新建第二个聊天页

### 4.2 管理端 `ecommerce-admin`

管理端当前只有知识库文档管理页：

- 路由：
  - `/knowledge`
  - `/merchant/knowledge`
- 页面文件：`ecommerce-admin/src/views/knowledge/documents.vue`

当前页面职责非常明确：

- 文档列表
- 分类筛选
- 新建 / 编辑 / 删除 / 重建索引

当前没有任何聊天入口，也没有消费知识问答流式事件的逻辑。

结论：

- 管理端目前**没有聊天工作台**
- 现有 `documents.vue` 是纯 CRUD 页面，不适合直接塞入大段聊天与调试逻辑

### 4.3 后端接口现状

后端当前接口分布如下：

- 用户同步聊天：`POST /api/v1/knowledge/chat`
- 用户流式聊天：`POST /api/v1/knowledge/chat/stream`
- 平台管理端同步聊天：`POST /api/v1/admin/knowledge/chat`
- 商家管理端同步聊天：`POST /api/v1/admin/merchant/knowledge/chat`

当前缺口：

- **没有**平台管理端流式接口 `/api/v1/admin/knowledge/chat/stream`
- **没有**商家管理端流式接口 `/api/v1/admin/merchant/knowledge/chat/stream`

这意味着：

- 用户端已经具备前端接入流式的后端条件
- 管理端如果要做“真正的 `chunk` 流式显示”，后端还需要补两个 stream 入口

### 4.4 认证与请求方式现状

用户端：

- 通过 `useApi.ts` 统一注入 `Authorization`
- token 来源是 Cookie
- 现有 `useApi.post()` 是“完整响应后再解析”的同步模型

管理端：

- axios 请求拦截器自动注入 `Authorization`
- token 来源是 `localStorage`
- 现有 axios 使用方式适合同步 JSON，不适合浏览器端逐段消费 `text/event-stream`

结论：

- 两个前端如果要接流式，**都不适合直接复用现有同步请求封装**
- 更合适的方案是分别新增轻量流式请求工具，基于浏览器原生 `fetch + ReadableStream`

## 5. 可选方案

### 方案 A：最小改动方案

做法：

- 用户端升级现有 `knowledge.vue` 到流式
- 管理端不新建页面，只在 `documents.vue` 里塞一个聊天抽屉或调试面板
- 管理端第一阶段甚至可以先走同步接口，暂时不追求真流式

优点：

- 初始改动最少
- 页面数最少

缺点：

- `documents.vue` 职责会变混乱
- 文档 CRUD 和聊天调试混在一起，长期维护差
- 管理端如果先走同步，就和“两个端都接 `chunk`”的目标不完全一致

### 方案 B：平衡方案

做法：

- 用户端复用现有 `knowledge.vue`，升级为流式消费
- 管理端新增独立聊天工作台页
- 管理端保留文档管理页原有职责，不在 `documents.vue` 内硬塞聊天能力
- 后端补管理端两个流式接口后，再让管理端也消费 `chunk`

优点：

- 用户端改动聚焦
- 管理端页面职责清晰
- 更适合后续排查 route、lightRoute、fast path、FAQ cache、agent 路径问题
- 和当前后台双路由模式一致，天然支持平台 / 商家双模式

缺点：

- 比方案 A 多一个新页面和新路由
- 管理端需要同时考虑平台态和商家态接口差异

### 方案 C：重抽象方案

做法：

- 同时在两个前端里抽统一的流式协议解析器、消息状态机、聊天组件
- 用户端和管理端都在这一层上复用

优点：

- 长期复用性最好
- 协议处理更统一

缺点：

- 首轮改动范围偏大
- 两个前端技术栈和封装风格不同，强行抽共性容易过度设计

## 6. 推荐方案

推荐采用 **方案 B**。

原因：

1. 用户端已经有成熟聊天页，直接升级最自然
2. 管理端当前完全没有聊天入口，更适合新增独立工作台，而不是污染 `documents.vue`
3. 管理端聊天页除了“能聊”，还应该承担调试观测价值，这和文档管理页不是一个职责
4. 方案 B 改动规模可控，但不会把结构做坏

## 7. 设计结论

### 7.1 用户端设计

用户端继续复用：

- `ecommerce-web/pages/knowledge.vue`

升级方向：

1. 从同步 `POST /api/v1/knowledge/chat` 升级为流式 `POST /api/v1/knowledge/chat/stream`
2. 使用 `fetch` 直接发送 POST 请求，并逐段解析 SSE 帧
3. 页面消费以下事件：
   - `start`
   - `lightRoute`
   - `route`
   - `chunk`
   - `answer`
   - `done`
   - `error`
4. 聊天气泡优先用 `chunk` 逐步渲染，最终以 `answer` 进行收口
5. 保留现有 `sessionId` 存储逻辑

用户端不建议新增复杂调试面板，只保留轻量用户体验信号，例如：

- 加载态
- 正在回复中的增量文字
- 失败提示

是否展示 `lightRoute / route` 可作为可选项，不强制暴露给普通用户。

### 7.2 管理端设计

管理端建议新增独立页面，而不是改造 `documents.vue`：

- 平台页：`/knowledge/chat`
- 商家页：`/merchant/knowledge/chat`

页面职责：

1. 作为知识问答调试工作台
2. 支持直接提问并观察增量返回
3. 展示本次请求的 `lightRoute`、`route`
4. 展示最终答案
5. 可选展示原始事件时间线，便于排查流式协议问题

管理端页面应该保持后台工具风格：

- 信息密度较高
- 明确区分输入区、会话区、事件区
- 不做营销风格展示

不推荐把聊天功能嵌入 `documents.vue` 的原因：

- 文档 CRUD 与聊天调试职责不同
- 页面会变成“大杂烩”
- 后续再加 route 调试信息会进一步挤压 CRUD 可用性

### 7.3 后端前置依赖

如果要让管理端也真正消费 `chunk`，后端必须先补两个流式接口：

- `POST /api/v1/admin/knowledge/chat/stream`
- `POST /api/v1/admin/merchant/knowledge/chat/stream`

其中商家端流式接口仍应沿用当前商家同步接口语义：

- 由网关注入 `X-User-Id`
- 由网关注入 `X-User-Type`
- 由网关注入 `X-Merchant-Id`

这两个接口是管理端前端流式接入的**前置条件**。

### 7.4 传输与解析设计

由于当前接口是：

- `POST`
- 需要请求体 `question + sessionId`

所以前端不应使用浏览器原生 `EventSource`，而应采用：

- `fetch`
- `ReadableStream`
- 自定义 SSE 帧解析

解析规则要兼容后端当前 `event:` / `data:` 结构。

### 7.5 测试与验证边界

后续真正实现时，需要至少覆盖：

1. 用户端：
   - 发送问题后能逐段看到 `chunk`
   - 最终 `answer` 能正确收口
   - 401 和网络异常提示正常
2. 管理端：
   - 平台态路由走 `/api/v1/admin/knowledge/...`
   - 商家态路由走 `/api/v1/admin/merchant/knowledge/...`
   - 聊天工作台不会影响现有文档管理页
3. 后端联动：
   - 管理端流式接口存在且网关可达

## 8. 风险评估

### 风险 1：管理端当前没有 stream 接口

影响：

- 管理端无法直接接入真正流式

处理：

- 作为实现前置任务，先补后端管理端 stream 接口

### 风险 2：现有前端请求封装不适合流式

影响：

- `useApi.post()` 和 axios 默认模式都只能拿完整响应

处理：

- 分别在两个前端新增轻量流式请求工具
- 不强行改造现有同步请求封装为“既同步又流式”的混合层

### 风险 3：把聊天塞进管理端文档页会破坏页面职责

影响：

- CRUD 和调试混在一起，后续维护困难

处理：

- 坚持独立工作台页

### 风险 4：流式异常处理容易和同步错误处理分叉

影响：

- 401、超时、网络断开、SSE 解析失败处理容易不一致

处理：

- 在实现阶段定义统一的前端流式状态机：
  - `idle`
  - `connecting`
  - `streaming`
  - `completed`
  - `failed`

## 9. 实施建议

建议后续按以下顺序真正落代码：

1. 先补管理端后端流式接口
2. 再升级用户端现有聊天页
3. 然后新增管理端聊天工作台页
4. 最后补前端测试与文档回写

## 10. 本轮结论

本轮评估后的明确结论如下：

1. 两个前端都应该做
2. 用户端直接升级现有 `knowledge.vue`
3. 管理端应该新增独立聊天工作台页
4. 管理端要做真流式，后端必须先补 admin / merchant 的 stream 接口
5. 两个前端都应使用 `fetch + ReadableStream`，而不是继续依赖现有同步请求封装
6. 本轮只完成分析和文档，不做代码实现，不提交代码

## 11. 当前实现状态（2026-05-22 晚）

### 已完成

1. 后端已补齐管理端流式入口：
   - `POST /api/v1/admin/knowledge/chat/stream`
   - `POST /api/v1/admin/merchant/knowledge/chat/stream`
2. 用户端已升级现有 `ecommerce-web/pages/knowledge.vue`，改为消费 `/api/v1/knowledge/chat/stream`
3. 用户端已新增轻量流式请求工具：`ecommerce-web/composables/useChatStream.ts`
4. 管理端已新增独立知识问答工作台页：
   - `/knowledge/chat`
   - `/merchant/knowledge/chat`
5. 管理端已新增轻量流式请求工具：`ecommerce-admin/src/utils/chatStream.js`
6. 管理端侧边栏已新增“知识问答”入口
7. 管理端事件时间线已从原始 `event/data` 展示升级为中文标签和摘要展示
8. 用户端和管理端都已补充 401 异常回归测试
9. 管理端已补充原知识库文档页回归，确认新增工作台未破坏文档管理页
10. 用户端全量 Playwright 已通过：`58/58`
11. 管理端全量 Playwright 已通过：`223/223`
12. 全量回归中暴露的旧测试口径问题已完成本轮必要收口，未扩大修改无关业务页面
13. 用户端与管理端已共享同一套 SSE 协议解析实现：`frontend-shared/chatSse.mjs`
14. 共享解析层已补独立回归测试：`frontend-shared/chatSse.test.mjs`
15. 用户端已支持显式调试模式：仅当 `localStorage.kb_debug = '1'` 时展示 `lightRoute / route`

### 未完成
