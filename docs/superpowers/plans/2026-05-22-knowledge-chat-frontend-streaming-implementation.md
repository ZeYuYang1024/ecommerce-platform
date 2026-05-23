# 知识库聊天前端双端流式接入实施规划

## 1. 范围

本规划对应两个前端：

1. `ecommerce-web`
2. `ecommerce-admin`

当前状态：

- **已完成分析、实现与回归收口**
- **用户端与管理端全量 Playwright 均已通过**
- **不提交代码**

## 2. 总体方案

采用“用户端升级现有聊天页 + 管理端新增独立工作台页 + 后端先补管理端流式接口”的路线。

## 3. 拆分任务

### 任务 1：补齐管理端流式后端入口

目标：

- 新增 `POST /api/v1/admin/knowledge/chat/stream`
- 新增 `POST /api/v1/admin/merchant/knowledge/chat/stream`

状态：`已完成`

说明：

- 这是管理端前端真流式接入的前置条件
- 不完成这一步，管理端只能继续走同步 chat

### 任务 2：升级用户端现有聊天页

目标：

- 修改 `ecommerce-web/pages/knowledge.vue`
- 把同步 `POST /knowledge/chat` 升级为流式 `POST /knowledge/chat/stream`
- 支持 `chunk` 增量渲染
- 保留最终 `answer` 收口

状态：`已完成`

涉及文件：

- `ecommerce-web/pages/knowledge.vue`
- 可能新增：`ecommerce-web/composables/useChatStream.ts`

### 任务 3：新增管理端聊天工作台页

目标：

- 新增平台态工作台页
- 新增商家态工作台页
- 新增对应路由与侧边栏入口
- 支持 `lightRoute / route / chunk / answer / done` 展示

状态：`已完成`

涉及文件：

- `ecommerce-admin/src/router/index.js`
- `ecommerce-admin/src/layout/index.vue`
- 新增：`ecommerce-admin/src/views/knowledge/chat.vue`
- 可能新增：`ecommerce-admin/src/utils/chatStream.js`

### 任务 4：联调与回归

目标：

- 验证平台态和商家态路由分流
- 验证用户端流式增量显示
- 验证 401、超时、解析失败提示

状态：`已完成（聚焦验证 + 全量回归）`

## 4. 推荐实现顺序

### 顺序 A

1. 先补后端 admin / merchant stream 接口
2. 再做用户端
3. 再做管理端

这是推荐顺序。

原因：

- 用户端已经有现成页面，改造快
- 管理端依赖后端 stream 入口，先补接口更稳

## 5. 关键设计约束

### 约束 1：用户端复用现有页面

不新建第二个用户聊天页。

### 约束 2：管理端新建独立工作台

不把聊天调试逻辑直接塞进 `documents.vue`。

### 约束 3：不使用 EventSource

因为当前接口是带请求体的 `POST`，前端统一使用：

- `fetch`
- `ReadableStream`
- 自定义 SSE 帧解析

### 约束 4：保留 `answer` 兼容收口

即便前端消费 `chunk`，最终仍以完整 `answer` 作为收尾，兼容后端当前协议设计。

### 约束 5：不引入 Grafana / Prometheus

项目当前不考虑该技术栈，本轮前端规划不依赖这两个体系。

## 6. 风险与处理

| 风险 | 影响 | 处理 |
|------|------|------|
| 管理端无 stream 接口 | 无法真流式 | 先补后端接口 |
| 流式解析复杂度高于同步请求 | 前端状态容易混乱 | 单独抽轻量流式工具 |
| 管理端聊天与文档管理耦合 | 页面职责失控 | 坚持独立工作台 |
| 用户端直接暴露调试信息 | 影响普通用户体验 | 用户端只保留必要状态，调试信息主要放管理端 |

## 7. 本轮已完成 / 未完成

### 已完成

- 已确认两个前端的现有入口和路由结构
- 已确认用户端已有聊天页
- 已确认管理端尚无聊天页
- 已确认管理端流式接入前缺少后端 stream 接口
- 已产出本实施规划文档
- 已补齐管理端后端 stream 入口
- 已完成用户端流式接入
- 已完成管理端知识问答工作台
- 已完成聚焦验证
- 已完成管理端事件时间线中文标签与摘要展示
- 已补用户端 401 流式异常回归
- 已补管理端 401 流式异常回归
- 已跑管理端原知识库文档页回归
- 已修正本轮全量回归中暴露的一批既有测试口径问题
- 已完成用户端全量 Playwright：`58/58` 通过
- 已完成管理端全量 Playwright：`223/223` 通过
- 已抽取双端共享的 SSE 协议解析层：`frontend-shared/chatSse.mjs`
- 已补共享解析层回归测试：`frontend-shared/chatSse.test.mjs`
- 已完成用户端调试模式路由展示：默认隐藏，仅 `localStorage.kb_debug = '1'` 时展示 `lightRoute / route`

### 未完成

## 8. 执行前提

后续如果继续扩展，需要用户继续确认以下口径：

1. 用户端是否展示 `lightRoute / route`
2. 管理端是否需要展示原始事件时间线
3. 管理端平台态和商家态是否共用一个组件

当前实现已按本规划落地，上述内容仅作为后续可选增强项。

## 9. 当前验证结果

已通过：

1. 后端：`ChatControllerTest`
2. 用户端：`ecommerce-web/tests/knowledge-stream.spec.ts`
3. 管理端：`ecommerce-admin/tests/knowledge-chat-stream.spec.ts`
4. 管理端原知识库页：`knowledge-tenant.spec.ts`、`knowledge-tenant-code0.spec.ts`

最近一次聚焦验证结果：

- 后端 `ChatControllerTest`：3 个测试通过
- 用户端 `knowledge-stream.spec.ts`：3 个测试通过
- 管理端知识相关测试：6 个测试通过
- 共享解析层 `chatSse.test.mjs`：4 个测试通过

知识问答相关组合验证：

- 用户端：
  - `tests/knowledge-stream.spec.ts`
  - `tests/e2e/knowledge-real.spec.ts`
  - 结果：4 个测试通过
- 管理端：
  - `tests/knowledge-chat-stream.spec.ts`
  - `tests/knowledge-tenant.spec.ts`
  - `tests/knowledge-tenant-code0.spec.ts`
  - 结果：6 个测试通过

两个前端全量回归执行结果：

- `ecommerce-web`：58 个用例全部通过
- `ecommerce-admin`：223 个用例全部通过

当前判断：

- 知识问答相关改动链路已经单独验证通过
- 两个前端全量 Playwright 已收敛为全绿
- 本轮额外修正的是测试口径、mock 数据结构和等待条件问题，不扩大修改无关业务页面

全量回归收口时处理的主要既有问题：

- 用户端 `home.spec.ts`：搜索跳转断言更新为当前 `/search?keyword=` 路由
- 用户端 `checkout.spec.ts`：断言改为当前默认地址卡片 / 支付跳转流程
- 用户端 `user-orders.spec.ts`：mock 响应结构改为当前分页结构
- 用户端真实全流程与知识问答真实用例：补稳定等待与超时预算
- 管理端商家列表、用户列表、类目、商家商品等测试：修正 mock 响应结构与等待条件
- 管理端真实商家审核链路：从固定等待改为等待审核接口返回和列表状态变化
