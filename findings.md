# Findings & Decisions

## Requirements
- Controller 层不得包含任何业务逻辑（JWT 解析、类型转换、多步编排）
- 禁止在循环内执行数据库查询
- 禁止返回 Map 给前端，全部改为 VO/DTO 类
- 禁止接收 Map 作为 @RequestBody，全部改为带 @Valid 的 Request DTO

## Research Findings
- 代码审查发现 6 个 Controller 中共 11 处违规
- 最严重的是 ProductController：Map 入参 + Map 返回值 + 手动解析业务逻辑
- AddressController：JWT userId 提取在 4 个方法中重复
- StockController：3 个方法接收 raw Map，手动 Long.valueOf/Integer.parseInt
- AuthController：1 处 Bearer 前缀剥离

## Technical Decisions
| Decision | Rationale |
|----------|-----------|
| ProductController `create` 改用嵌套 DTO | Jackson 自动反序列化，无需手动 parseSpu/parseSkus |
| 创建 `ProductDetailVO` 替换 `Map<String, Object>` | 前端能直接拿到类型安全的响应 |
| JWT userId 由 Gateway AuthFilter 注入 X-User-Id Header | 复用已有 Filter，减少 Controller 耦合 |
| 所有请求 DTO 添加 @Valid + Jakarta Validation 注解 | 替代手动 null 检查和类型转换 |

## Issues Encountered
| Issue | Resolution |
|-------|------------|
|       |            |

## Resources
- 代码审查报告中有完整的违例清单（11 个问题）
- DTO 目录约定：`dto/request/`、`dto/response/`
- VO 目录约定：`dto/response/` 或单独的 `vo/` 包
