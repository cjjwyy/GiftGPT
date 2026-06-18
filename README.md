# GiftGPT — AI 驱动的全链路礼物推荐平台

> **Slogan：不猜，更懂TA的心意**
>
> 消除"不知道送什么"的焦虑，让每一份礼物都恰到好处。

---

## 一、项目背景与商业价值

### 1.1 痛点洞察

在注重仪式感的当代社交生活中，"挑礼物"是高频刚需，但多数人面临严重的决策困难：

| 痛点 | 具体表现 |
|------|---------|
| **认知盲区** | 对收礼人真实喜好缺乏系统把握，反复询问失去惊喜感 |
| **转化断层** | 无法将"模糊的性格印象"转化为"具体的商品清单" |
| **推荐错位** | 现有平台推荐以"我"为中心，缺乏"为他人选购"的引擎 |

### 1.2 市场机会

- **核心用户**：18-30 岁学生及职场新人，可支配收入有限但社交支出占比持续增长
- **扩展用户**：30-45 岁中青年（代际送礼场景：母亲节、教师节）
- **B端长尾**：企业 HR（团建福利、员工生日、客户礼品）

千禧一代与 Z 世代追求**情感价值 > 实用价值**，愿为"恰当的礼物 + 精美包装 + 个性化祝福"支付溢价。

### 1.3 竞争优势（SWOT 提炼）

| 优势 | 劣势 |
|------|------|
| 全链路闭环（选品→包装→贺卡→物流） | 数据隐私授权门槛高 |
| AI 性格匹配 + 知识图谱推荐 | 前期多端对接投入大 |
| 双端价值（C端 + B端） | 用户付费习惯待培养 |
| 礼物故事社区增强粘性 | |

| 机会 | 威胁 |
|------|------|
| Z世代仪式感消费持续增长 | 电商平台内置AI礼物推荐（淘宝/京东） |
| B端企业福利定制需求升级 | 独立AI选礼小程序（礼选AI等） |
| LLM技术成熟降低开发成本 | KOL种草内容即推荐，转化路径短 |
| 社交电商与内容种草联动 | 微信/抖音社交礼物直送功能 |

---

## 二、核心功能

```
┌─────────────────────────────────────────────────────────────┐
│                      GiftGPT 功能全景                        │
├───────────────┬───────────────┬───────────────┬─────────────┤
│  收礼人画像   │  自我消费洞察  │  AI 智能匹配  │  全链路闭环  │
│  Recipient    │  Self-        │  AI Matching  │  Full-      │
│  Profile      │  Insight      │               │  Chain      │
├───────────────┼───────────────┼───────────────┼─────────────┤
│ · 性格标签    │ · 浏览记录分析 │ · 性格×场景   │ · 跳转购买   │
│ · 社交媒体    │ · 购买记录分析 │   ×预算 匹配  │ · 礼物包装   │
│   公开信息    │ · 价格偏好    │ · 知识图谱    │ · 电子贺卡   │
│ · 画像可      │   提取        │   推理        │ · 物流追踪   │
│   手动修正    │ · 品味圈层    │ · 解释性推荐  │ · 双向互动   │
│               │   推断        │               │             │
└───────────────┴───────────────┴───────────────┴─────────────┘
```

### 2.1 收礼人画像构建
- 用户可新建收礼人画像，手动选择**性格标签**（开朗、文艺、极客、养生派等 20+ 维度）
- 经授权后读取收礼人社交媒体的**公开信息**（头像、简介、公开动态），多模态分析提取偏好
- 经授权后接入电商平台的**购买记录**，分析品类倾向
- 画像存入**透明记忆库**，用户可随时查看、修正、或撤回授权

### 2.2 自我消费洞察
- 授权后抓取送礼人自身在主流购物平台的浏览、加购、购买记录
- 分析价格偏好区间、品类倾向、品牌偏好
- 反推用户所处**消费层级与品味圈层**，作为礼物筛选的约束条件

### 2.3 AI 礼物智能匹配
- 输入：`收礼人性格特征 + 送礼场景（生日/纪念日/节庆/求婚...） + 预算`
- 引擎：`LLM 推理 + 礼物知识图谱 + 协同过滤`
- 输出：**排序礼物清单**，附带推荐理由解释
- 支持对话式交互："TA喜欢摄影和户外，预算500左右，下个月生日"

### 2.4 全链路闭环服务
- **购买**：直接跳转合作电商完成下单（CPS佣金模式）
- **包装**：选择主题礼盒、定制包装纸、手写字体祝福卡
- **贺卡**：AI 生成个性化祝福文案，支持语音留言二维码
- **物流**：一站式物流追踪，送达通知
- **双向互动**：收礼人扫描贺卡二维码 → 生成感谢视频/惊喜反馈 → 形成社交闭环

### 2.5 附加功能
- **礼物记忆库**：保存所有送礼记录，形成历史情感图谱
- **日历提醒**：同步重要日期，提前推送选礼建议
- **礼物故事社区**：用户晒出成功送礼案例，获得社交认同
- **B端企业服务**：团建福利批量定制、员工生日自动提醒+选品

---

## 三、技术架构

```
┌──────────────────────────────────────────────────────────────────┐
│                         前端层 (Web → 小程序)                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐    │
│  │  React 18    │  │  Next.js     │  │  TailwindCSS +       │    │
│  │  TypeScript  │  │  (SSR/SSG)   │  │  shadcn/ui           │    │
│  └──────────────┘  └──────────────┘  └──────────────────────┘    │
├──────────────────────────────────────────────────────────────────┤
│                        API 网关 (Nginx / Kong)                     │
├────────────────────────────┬─────────────────────────────────────┤
│     Java 主服务 (Spring Boot 3)    │    Python AI 服务 (FastAPI)     │
│  ┌──────────────────────────┐      │  ┌──────────────────────────┐  │
│  │ · 用户认证与授权 (JWT)    │      │  │ · LLM 推理服务            │  │
│  │ · 画像管理 CRUD          │      │  │ · 多模态分析              │  │
│  │ · 订单/物流管理          │      │  │ · 推荐算法引擎            │  │
│  │ · 社区内容管理           │      │  │ · 知识图谱查询            │  │
│  │ · B端企业服务            │      │  │ · NL2SQL 自然语言查询     │  │
│  │ · 支付/电商对接          │      │  │ · 贺卡文案生成            │  │
│  └──────────────────────────┘      │  └──────────────────────────┘  │
├───────────────────────────────────┴──────────────────────────────┤
│                          数据层                                    │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────────┐  │
│  │ MySQL    │  │ Redis    │  │ Neo4j    │  │ Elasticsearch    │  │
│  │ 业务数据  │  │ 缓存/会话 │  │ 知识图谱  │  │ 商品搜索/向量检索  │  │
│  └──────────┘  └──────────┘  └──────────┘  └──────────────────┘  │
├──────────────────────────────────────────────────────────────────┤
│                      基础设施 (Docker + K8s)                       │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────────┐  │
│  │ MinIO    │  │ RabbitMQ │  │ Prometheus│  │ GitHub Actions   │  │
│  │ 对象存储  │  │ 消息队列  │  │ + Grafana │  │ CI/CD            │  │
│  └──────────┘  └──────────┘  └──────────┘  └──────────────────┘  │
└──────────────────────────────────────────────────────────────────┘
```

### 3.1 技术选型

| 层次 | 技术 | 选型理由 |
|------|------|---------|
| **前端** | React 18 + Next.js 14 + TypeScript | 生态成熟、SSR 友好、后续可迁移小程序（Taro） |
| **UI 框架** | TailwindCSS + shadcn/ui | 开发效率高、组件可定制、暗色模式支持 |
| **主后端** | Spring Boot 3.4 + Java 17 | 企业级稳定性、安全框架成熟、比赛认可度高 |
| **AI 后端** | Python FastAPI + LangChain | AI/LLM 生态最强、模型调用便捷 |
| **关系数据库** | H2 (嵌入式) / MySQL 8.0 | 零配置快速启动，生产可切换 MySQL |
| **图数据库** | Neo4j (预留) | 礼物知识图谱：商品-属性-场景-性格多跳推理 |
| **缓存** | Redis (预留) | 会话管理、热点数据缓存、排行榜 |
| **搜索引擎** | Elasticsearch (预留) | 商品全文搜索 + 向量检索（语义相似度） |
| **LLM** | 多模型可插拔（Qwen/DeepSeek/GPT-4o） (预留) | 成本可控，支持本地/云端切换 |
| **消息队列** | RabbitMQ (预留) | AI任务异步处理、订单状态变更通知 |
| **对象存储** | MinIO / 阿里云OSS (预留) | 贺卡图片、包装预览图、用户头像 |
| **容器化** | 本地直接运行 | 无需 Docker，开箱即用 |
| **CI/CD** | GitHub Actions | 自动构建、测试、部署 |

### 3.2 为什么混合架构？

- **Java Spring Boot**：承担核心业务逻辑、用户体系、交易流程 — 需要强类型、事务保障、安全框架
- **Python FastAPI**：承担 AI 推理、模型调用、知识图谱操作 — Python 是 AI 生态的事实标准
- 两者通过 **REST API** 通信，职责清晰、独立部署、互不影响

---

## 四、项目模块划分

```
giftgpt/
├── giftgpt-common/              # 公共模块
│   ├── 统一返回格式
│   ├── 异常处理
│   ├── 工具类
│   └── 注解定义
├── giftgpt-auth/                # 认证授权模块
│   ├── JWT 登录/注册
│   ├── OAuth2 第三方登录（微信/支付宝）
│   ├── RBAC 权限控制
│   └── 隐私授权面板 API（数据授权/撤回）
├── giftgpt-user/                # 用户服务模块
│   ├── 用户基础信息管理
│   ├── 收礼人画像 CRUD
│   ├── 消费洞察分析
│   └── 礼物记忆库
├── giftgpt-recommendation/      # 推荐引擎模块（核心）
│   ├── 性格-场景-预算 匹配算法
│   ├── 知识图谱查询接口
│   ├── LLM 对话式推荐
│   ├── 推荐解释生成
│   └── 冷启动与反馈学习
├── giftgpt-goods/               # 商品服务模块
│   ├── 商品搜索（ES）
│   ├── 商品详情聚合
│   ├── 合作电商 CPS 对接
│   └── 商品库管理
├── giftgpt-order/               # 订单服务模块
│   ├── 订单创建与管理
│   ├── 包装定制服务
│   ├── 电子贺卡生成
│   ├── 物流追踪
│   └── 双向互动（感谢/反馈）
├── giftgpt-content/             # 内容社区模块
│   ├── 礼物故事发布/互动
│   ├── 日历提醒服务
│   ├── 个性化推送
│   └── KOL 内容聚合
├── giftgpt-enterprise/          # B端企业服务模块
│   ├── 企业注册与认证
│   ├── 团购批量下单
│   ├── 员工关怀日历
│   └── SaaS 订阅管理
├── giftgpt-ai/                  # Python AI 子项目
│   ├── llm_service/             # LLM 推理服务
│   ├── profile_analyzer/        # 画像分析（多模态）
│   ├── recommender/             # 推荐算法引擎
│   ├── kg_service/              # 知识图谱服务
│   └── content_generator/       # 贺卡文案/祝福语生成
└── giftgpt-web/                 # 前端项目
    ├── src/
    │   ├── app/                 # Next.js App Router
    │   ├── components/          # UI 组件
    │   ├── hooks/               # 自定义 Hooks
    │   ├── lib/                 # 工具函数
    │   └── styles/              # 全局样式
    └── public/                  # 静态资源
```

---

## 五、数据库设计概要

### 5.1 核心实体关系

```
┌──────────┐     ┌──────────────┐     ┌──────────────┐
│   User   │────→│  Recipient   │────→│   Profile    │
│  (用户)   │     │  (收礼人)     │     │  (画像标签)   │
└──────────┘     └──────────────┘     └──────────────┘
     │                   │
     ↓                   ↓
┌──────────┐     ┌──────────────┐     ┌──────────────┐
│  Order   │←────│ GiftRecord   │────→│   Product    │
│  (订单)   │     │  (送礼记录)   │     │  (商品)       │
└──────────┘     └──────────────┘     └──────────────┘
     │                                       ↑
     ↓                                       │
┌──────────┐     ┌──────────────┐     ┌──────┴───────┐
│ Packaging│     │  GreetingCard│     │  Knowledge   │
│  (包装)   │     │  (贺卡)       │     │  Graph(Neo4j)│
└──────────┘     └──────────────┘     └──────────────┘
```

### 5.2 MySQL 核心表

| 表名 | 说明 | 关键字段 |
|------|------|---------|
| `user` | 用户表 | id, phone, email, password_hash, auth_provider |
| `user_consume_profile` | 消费画像 | user_id, price_min, price_max, category_prefs(JSON), brand_prefs |
| `recipient` | 收礼人 | id, user_id, name, relation, gender, age_range |
| `recipient_tag` | 收礼人性格标签 | recipient_id, tag_code (开朗/文艺/极客/...) |
| `recipient_profile` | 收礼人画像详情 | recipient_id, personality_desc, hobby_list(JSON), social_analysis(JSON) |
| `gift_record` | 送礼记录 | id, user_id, recipient_id, occasion, budget, product_id, greeting_card_id |
| `product` | 商品 | id, name, price, category, platform, platform_url, image_url |
| `order` | 订单 | id, gift_record_id, status, logistics_no, logistics_company |
| `packaging` | 包装服务 | id, order_id, theme, custom_text, preview_image |
| `greeting_card` | 电子贺卡 | id, content, voice_url, qr_code_url, style_template |
| `feedback` | 收礼人反馈 | id, gift_record_id, type(文字/视频/语音), content, is_public |
| `story` | 社区故事 | id, user_id, gift_record_id, content, likes, is_anonymous |
| `calendar_event` | 日历提醒 | id, user_id, recipient_id, occasion, remind_date, remind_before_days |
| `data_authorization` | 数据授权记录 | id, user_id, data_type, authorized_scope, status, expire_at |

### 5.3 Neo4j 知识图谱模型

```
(Product)-[:BELONGS_TO]->(Category)
(Product)-[:HAS_ATTRIBUTE]->(Attribute{name, value})
(Product)-[:SUITABLE_FOR]->(Personality{type})
(Product)-[:FIT_OCCASION]->(Occasion{name})
(Product)-[:IN_PRICE_RANGE]->(PriceTier{level})
(Category)-[:RELATED_TO]->(Category)
(Personality)-[:COMPATIBLE_WITH]->(Personality)
(User)-[:PURCHASED]->(Product)-[:ALSO_BOUGHT]->(Product)
```

---

## 六、API 设计概要

### 6.1 RESTful API 规范

```
Base URL: /api/v1

认证相关:
  POST   /auth/register               # 注册
  POST   /auth/login                  # 登录
  POST   /auth/oauth/{provider}       # 第三方登录
  GET    /auth/authorizations         # 获取数据授权列表
  PUT    /auth/authorizations/{id}    # 修改授权范围
  DELETE /auth/authorizations/{id}    # 撤回授权

收礼人画像:
  POST   /recipients                  # 创建收礼人画像
  GET    /recipients                  # 获取收礼人列表
  GET    /recipients/{id}             # 获取收礼人详情+画像
  PUT    /recipients/{id}             # 更新画像
  DELETE /recipients/{id}             # 删除
  POST   /recipients/{id}/analyze     # 触发AI画像分析

礼物推荐:
  POST   /recommendations/search       # 条件检索推荐
  POST   /recommendations/chat         # 对话式推荐(SSE流式)
  GET    /recommendations/history      # 历史推荐记录
  POST   /recommendations/{id}/feedback # 推荐反馈(喜欢/不喜欢)

商品:
  GET    /products/search             # 商品搜索(ES)
  GET    /products/{id}              # 商品详情

订单与送礼:
  POST   /gifts                       # 创建送礼记录
  GET    /gifts                       # 送礼记录列表
  GET    /gifts/{id}                  # 送礼详情
  POST   /gifts/{id}/order            # 下单
  GET    /gifts/{id}/logistics        # 物流追踪
  POST   /gifts/{id}/feedback         # 收礼人反馈(通过二维码入口)

包装与贺卡:
  POST   /greetings/generate          # AI生成贺卡文案
  POST   /greetings/preview           # 贺卡+包装预览
  PUT    /greetings/{id}              # 修改贺卡

社区:
  GET    /stories                     # 社区故事列表
  POST   /stories                     # 发布故事
  POST   /stories/{id}/like           # 点赞

B端:
  POST   /enterprise/register         # 企业注册
  POST   /enterprise/orders/batch     # 批量下单
  GET    /enterprise/calendar         # 员工关怀日历
```

### 6.2 AI 服务内部接口 (Python → Java 互调)

| 接口 | 方向 | 说明 |
|------|------|------|
| `POST /ai/profile/analyze` | Java→Python | 分析收礼人社交媒体数据，生成画像 |
| `POST /ai/consume/insight` | Java→Python | 分析送礼人消费记录，提取偏好 |
| `POST /ai/recommend` | Java→Python | 基于画像+场景+预算返回推荐清单 |
| `POST /ai/chat/recommend` | Java→Python | 对话式推荐（SSE 流式返回） |
| `POST /ai/greeting/generate` | Java→Python | 生成个性化贺卡文案 |
| `POST /ai/kg/query` | Java→Python | 知识图谱多跳推理查询 |
| `POST /callback/recommend/feedback` | Python→Java | 推荐反馈回调（用于模型微调） |

---

## 七、开发路线图

### Phase 0：基础设施搭建（第 1-2 周）
- [ ] 初始化 Spring Boot 多模块项目
- [ ] Docker Compose 开发环境（MySQL + Redis + Neo4j + ES + MinIO + RabbitMQ）
- [ ] CI/CD 流水线（GitHub Actions）
- [ ] 统一日志、监控、异常处理
- [ ] 代码规范与 Git 工作流约定

### Phase 1：用户体系 + 画像系统（第 3-5 周）
- [ ] 用户注册/登录（JWT + OAuth2 微信登录）
- [ ] 隐私授权面板（细粒度数据授权/撤回）
- [ ] 收礼人画像 CRUD + 手动标签系统
- [ ] Python AI 服务：画像自动分析（基于公开信息的性格推断）
- [ ] 消费洞察：对接电商授权数据，分析价格偏好

### Phase 2：推荐引擎 MVP（第 6-8 周）⭐ 核心
- [ ] 礼物知识图谱构建（Neo4j）：商品-属性-场景-性格关系网
- [ ] 规则引擎：`性格特征 × 场景 × 预算` → 候选商品集
- [ ] LLM 推理层：对候选集进行语义排序 + 推荐理由生成
- [ ] 对话式推荐：SSE 流式返回，支持多轮交互

### Phase 3：商品与电商对接（第 9-10 周）
- [ ] 商品数据聚合（爬虫 + API 接入主流电商）
- [ ] Elasticsearch 商品搜索（全文 + 向量语义检索）
- [ ] CPS 佣金链接生成
- [ ] 商品库管理后台

### Phase 4：订单与全链路闭环（第 11-12 周）
- [ ] 订单创建与管理
- [ ] 包装定制服务（主题模板 + 预览）
- [ ] AI 贺卡文案生成 + 语音留言
- [ ] 物流追踪对接
- [ ] 收礼人反馈（扫描二维码 → 感谢/反馈）

### Phase 5：社区与运营（第 13-14 周）
- [ ] 礼物故事社区（发布/互动/点赞）
- [ ] 日历提醒（重要日期 + 提前推送）
- [ ] 个性化推送（基于画像的节日/场景推送）
- [ ] 数据看板（用户画像分析、推荐准确率）

### Phase 6：B端 + 打磨（第 15-16 周）
- [ ] 企业注册与认证
- [ ] 批量团购下单
- [ ] 员工关怀日历 SaaS
- [ ] 全链路压测与性能优化
- [ ] UI/UX 细节打磨
- [ ] 答辩/路演材料准备

---

## 八、商业模式

### 8.1 收入模型

| 收入来源 | 模式 | 说明 |
|---------|------|------|
| **CPS 佣金** | 按成交额抽佣 5%-15% | 与电商平台/品牌方合作协议 |
| **包装增值** | 按单收费 ¥9.9-¥49.9 | 主题礼盒、定制包装、贺卡印刷 |
| **B端SaaS** | 按年/按人数订阅 | 企业团购管理、员工关怀日历 |
| **广告/推广** | CPC/CPT | 品牌新品首发推广位 |
| **数据服务** | 脱敏后的消费趋势报告 | 面向品牌方的品类洞察 |

### 8.2 蓝海战略 — 四步动作框架

```
  ┌─ 消除 ──────────────────────┐    ┌─ 减少 ──────────────────────┐
  │ · "不知道送什么"的焦虑时间    │    │ · 对礼物价格的过度关注       │
  │ · 反复比价的精力消耗          │    │ · 物流信息的反复查询         │
  │ · 选择困难导致的放弃购买      │    │ · 包装简陋的负面体验         │
  └────────────────────────────┘    └────────────────────────────┘

  ┌─ 提升 ──────────────────────┐    ┌─ 创造 ──────────────────────┐
  │ · 礼物情感传递效率            │    │ · 送礼者-收礼人双向互动闭环  │
  │ · 包装审美与个性化            │    │ · 礼物记忆库（情感图谱）      │
  │ · 推荐透明度（可解释AI）       │    │ · 性格驱动的礼物匹配范式     │
  │ · 一站式全链路体验            │    │ · 隐私透明授权体系           │
  └────────────────────────────┘    └────────────────────────────┘
```

---

## 九、团队分工

| 角色 | 职责 | 建议人数 |
|------|------|---------|
| PM / 产品 | 需求管理、竞品跟踪、路演答辩 | 1 |
| 后端 (Java) | Spring Boot 微服务、API 设计、数据库 | 2 |
| AI / 算法 (Python) | 推荐引擎、LLM 集成、知识图谱 | 1-2 |
| 前端 | React/Next.js Web 端开发 | 1-2 |
| 设计 | UI/UX 设计、品牌视觉、路演 PPT | 1 |
| 数据/运维 | 数据采集、Docker/K8s、CI/CD | 1 |

---

## 十、快速启动

### 10.1 环境要求

- JDK 17+
- Node.js 20+
- Maven 3.9+
- Python 3.11+ (可选，AI 服务)

### 10.2 项目结构

```
GiftGPT/                    # 项目根目录
├── README.md
├── setup.bat                # 一键启动脚本
├── start-server.bat         # 单独启动后端
├── start-frontend.bat       # 单独启动前端
├── backend/                 # 后端 (Java + Python AI)
│   ├── pom.xml
│   ├── giftgpt-server/      # 启动入口
│   ├── giftgpt-common/      # 公共组件
│   ├── giftgpt-auth/        # 认证授权
│   ├── giftgpt-user/        # 用户与收礼人画像
│   ├── giftgpt-recommendation/  # AI 推荐引擎
│   ├── giftgpt-goods/       # 商品服务
│   ├── giftgpt-order/       # 订单与全链路
│   ├── giftgpt-content/     # 社区与日历
│   ├── giftgpt-enterprise/  # B端企业服务
│   └── giftgpt-ai/          # Python AI 服务
└── frontend/
    └── giftgpt-web/         # Next.js 前端
```

### 10.3 一键启动

```bash
# 双击运行或命令行执行
setup.bat
```

脚本会自动同时启动前后端，并打开浏览器访问 http://localhost:3000。

### 10.4 分别启动

```bash
# 1. 安装依赖
cd backend && mvn clean install -DskipTests
cd ../frontend/giftgpt-web && npm install

# 2. 启动后端
start-server.bat          # 或 cd backend/giftgpt-server && mvn spring-boot:run

# 3. 启动前端
start-frontend.bat        # 或 cd frontend/giftgpt-web && npm run dev
```

# 2. 启动后端
start-server.bat          # 或 mvn spring-boot:run -pl giftgpt-server

# 3. 启动前端
start-frontend.bat        # 或 cd giftgpt-web && npm run dev
```

### 10.5 访问地址

| 服务 | 地址 |
|------|------|
| 前端页面 | http://localhost:3000 |
| Java API | http://localhost:8080 |
| API 文档 (Swagger) | http://localhost:8080/swagger-ui.html |
| H2 数据库控制台 | http://localhost:8080/h2-console |
| Python AI (如果启动) | http://localhost:8000/docs |

> 数据库使用 H2 嵌入式数据库，数据文件存储在 `backend/giftgpt-server/data/`，无需额外安装。Python AI 服务为可选组件。

---

## 十一、关键风险与缓解

| 风险 | 等级 | 缓解措施 |
|------|------|---------|
| 大厂复制产品模式 | 🔴 高 | 深耕细分场景 + 独家供应商 + B端差异化 |
| 用户隐私授权率低 | 🔴 高 | 最小必要授权 + 透明化面板 + 可随时撤回 |
| 电商平台限制数据抓取 | 🟡 中 | 优先对接开放API + CPS合作模式 |
| LLM 推理成本过高 | 🟡 中 | 多模型可插拔（开源模型兜底）+ 缓存策略 |
| 推荐冷启动效果差 | 🟡 中 | 基于规则引擎兜底 + 画像手动标签快速启动 |
| 供应链不可控 | 🟢 低 | 与品牌直签 + 多供应商备选 |

---

## 十二、竞赛策略建议

1. **突出技术壁垒**：重点展示知识图谱多跳推理 + LLM可解释推荐，这比简单的"调用AI接口"有技术深度
2. **强调隐私设计**：自主设计的授权面板是差异化亮点，契合《个人信息保护法》合规要求
3. **展示数据闭环**：推荐→购买→反馈→画像更新→更准推荐的完整数据飞轮
4. **Demo 场景化**：准备 3-5 个典型用户故事（如：程序员男友给文艺女友选生日礼物），展示整个流程
5. **商业模式清晰**：CPS+B端SaaS双引擎，不依赖单一收入来源
6. **技术选型务实**：混合架构体现工程判断力，承认AI生态现实而非强行全Java

---

> 📧 联系方式 | 📄 许可证 MIT | 🏆 为比赛而生，追求卓越
