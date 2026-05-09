# Task Plan: P0 收尾 — 5项修复

## Goal
1. Spu 列表不再返回 deleted 字段
2. 新增商品详情展示页 (view-only)
3. 补后端 admin users 接口
4. Gateway 收紧 admin 路径的 GET 权限
5. 列表页展示第一张图片，详情页展示全部

## Current Phase
Phase 1

## Phases

### Phase 1: ProductController 返回 SpuVO (修复 #1)
- [ ] SpuVO 改为 list 也能用（已有所有字段）
- [ ] ProductController.list() / adminList() 返回 Page<SpuVO>
- [ ] ProductServiceImpl 加 toSpuVO() 转换方法
- **Status:** in_progress

### Phase 2: 商品详情展示页 (修复 #2 + #5)
- [ ] 创建 views/product/detail.vue 纯展示页
- [ ] 路由注册 /products/:id 指向 detail
- [ ] 列表页商品名改成链接跳转详情
- **Status:** pending

### Phase 3: Admin 用户列表接口 (修复 #3)
- [ ] Auth Service 加 GET /api/v1/admin/users 接口
- [ ] 查询 ecommerce_auth.user 表返回列表
- **Status:** pending

### Phase 4: Gateway 收紧权限 (修复 #4)
- [ ] AuthFilter 对 /api/v1/admin/** 所有方法都要鉴权
- **Status:** pending

### Phase 5: 验证
- [ ] mvn build pass + vite build pass
- **Status:** pending
