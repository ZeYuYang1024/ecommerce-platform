# 小红书评论机器人 — 设计规格

**日期**: 2026-05-01  
**目标**: 在计算机毕设相关帖子下自动评论，推广引流  
**账号**: 1-2 个自有账号  
**技术栈**: Python + DrissionPage + Flask + MySQL  

---

## 1. 架构

```
┌─────────────────────────────────────────────────────┐
│                    Web 管理界面                       │
│          (Flask Jinja2 + HTMX + Tailwind)            │
│  ┌──────────┐ ┌──────────┐ ┌───────────────────┐    │
│  │ 关键词管理 │ │ 评论记录  │ │ 启动/停止/看状态   │    │
│  └──────────┘ └──────────┘ └───────────────────┘    │
├─────────────────────────────────────────────────────┤
│                   Flask 后端                         │
│  ┌──────────┐ ┌──────────┐ ┌───────────────────┐    │
│  │ 配置路由  │ │ 任务调度  │ │ 帖子搜索 + 过滤    │    │
│  └──────────┘ └──────────┘ └───────────────────┘    │
│  ┌──────────┐ ┌──────────┐ ┌───────────────────┐    │
│  │ 评论生成  │ │ 账号管理  │ │ 反检测/限速       │    │
│  └──────────┘ └──────────┘ └───────────────────┘    │
├─────────────────────────────────────────────────────┤
│                  DrissionPage                        │
│           (接管本机 Chrome，保持真实指纹)              │
├─────────────────────────────────────────────────────┤
│              MySQL 数据库                             │
│   posts(已评帖子) | comments(评论记录) |               │
│   keywords(关键词) | accounts(账号信息)               │
└─────────────────────────────────────────────────────┘
```

## 2. 目录结构

```
xhs-comment-bot/
├── app.py                 # Flask 入口
├── config.py              # 配置管理
├── browser/
│   ├── __init__.py
│   └── chrome_manager.py  # DrissionPage 管理
├── search/
│   ├── __init__.py
│   └── searcher.py        # 搜索+话题
├── comment/
│   ├── __init__.py
│   ├── generator.py       # 评论生成（模板+AI）
│   └── poster.py          # 评论发布
├── storage/
│   ├── __init__.py
│   ├── models.py          # SQLAlchemy 模型
│   └── db.py              # 数据库初始化
├── web/
│   ├── __init__.py
│   ├── routes.py          # API 路由
│   ├── templates/         # Jinja2 模板
│   └── static/            # CSS/JS
└── requirements.txt
```

## 3. 数据模型

```sql
-- 关键词/话题配置
CREATE TABLE keywords (
    id INT AUTO_INCREMENT PRIMARY KEY,
    keyword VARCHAR(100) NOT NULL,
    type ENUM('search','topic') NOT NULL DEFAULT 'search',
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 已评帖子去重
CREATE TABLE posts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    post_id VARCHAR(100) NOT NULL UNIQUE,
    post_url VARCHAR(500) NOT NULL,
    post_title VARCHAR(500),
    post_author VARCHAR(100),
    post_content TEXT,
    commented_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 评论记录
CREATE TABLE comments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    post_url VARCHAR(500) NOT NULL,
    post_title VARCHAR(500),
    post_author VARCHAR(100),
    content TEXT NOT NULL,
    source ENUM('template','ai') NOT NULL DEFAULT 'template',
    status ENUM('success','failed','skipped') NOT NULL DEFAULT 'success',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 账号配置
CREATE TABLE accounts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    daily_limit INT NOT NULL DEFAULT 30,
    commented_today INT NOT NULL DEFAULT 0,
    last_reset DATE
);
```

## 4. 核心流程

单次执行流程：

1. 用户点击"开始" → Flask 后台线程启动
2. 遍历启用的关键词/话题
3. DrissionPage 打开搜索页/话题页 → 获取前 N 条帖子列表
4. 去重：查询 posts 表，过滤已评帖子
5. 对每条新帖子：
   - 相关性判断（标题/内容匹配"毕设/计算机/毕业"）
   - 优先 AI 生成评论 → 失败用模板兜底
   - 打开帖子 → 随机等待 5-20s → 输入评论 → 发布
   - 记录到 posts + comments 表
   - 随机间隔 30-90s 看下一篇
6. 任务结束，前端显示执行结果

## 5. 评论生成策略

- **AI 生成**: 传帖子标题 + 正文摘要给 LLM，prompt 为"写一句自然真诚的中文评论，不要营销腔，30字以内"
- **模板兜底**: AI 调用失败时从模板库随机选一条
- **模板库**: 初始内置 10-15 条，Web 界面可增删改
- **LLM**: 支持 DeepSeek / 通义千问等国产 API

## 6. 反检测措施

- 接管本机 Chrome（`ChromiumOptions().set_local_port()`），浏览器指纹和登录态完全真实
- 所有操作间隔加随机抖动（不固定延时）
- 单账号每日评论上限（默认 30 条），到量自动停
- 模拟人类打字速度
- 每个账号独立限速

## 7. Web 管理界面

三个 Tab 页：

**仪表盘**: 账号状态 → 今日已评/上限 → 运行状态（空闲/运行中）→ 开始/停止按钮 → 最近5条评论记录

**关键词管理**: 表格列（关键词、类型搜索/话题、启用状态、删除操作）→ 添加按钮

**评论记录 & 模板**: 评论历史分页列表（可筛选状态、导出 CSV）→ 模板库列表（添加/删除/编辑）→ AI 开关

**前端技术**: Jinja2 模板 + HTMX 无刷新交互 + Tailwind CSS CDN

## 8. 启动方式

```bash
cp config.example.py config.py
# 编辑 config.py: MySQL连接串、LLM API key、Chrome路径、日评论上限
python app.py
# 浏览器打开 http://127.0.0.1:5800
```

## 9. 不做的

- 不自动注册/养号
- 不做多账号并发
- 不做定时自动运行（需手动点开始）
- 不做图片/视频评论
- 不做私信功能
