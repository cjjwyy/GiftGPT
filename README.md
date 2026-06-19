# GiftGPT — AI 驱动的全链路礼物推荐平台

> 不猜，更懂TA的心意

消除"不知道送什么"的焦虑，让每一份礼物都恰到好处。

---

## 项目简介

GiftGPT 是一个 AI 驱动的礼物推荐平台，基于收礼人性格画像、送礼场景和预算，智能匹配最合适的礼物，并提供从选品、包装、贺卡到物流的全链路闭环服务。

- **核心用户**：18-30 岁学生及职场新人
- **扩展用户**：30-45 岁中青年（母亲节、教师节等代际送礼）
- **B端场景**：企业团建福利、员工生日、客户礼品

---

## 核心功能

- **收礼人画像**：手动选择性格标签（开朗、文艺、极客等 20+ 维度）+ AI 多模态分析
- **AI 智能匹配**：`性格特征 × 场景 × 预算` → 排序礼物清单 + 推荐理由
- **全链路闭环**：购买跳转 → 包装定制 → AI 贺卡 → 物流追踪 → 双向反馈
- **礼物社区**：成功案例分享、日历提醒、礼物记忆库
- **B端服务**：企业批量团购、员工关怀日历

---

## 技术栈

| 层次 | 技术 |
|------|------|
| 前端 | React 18 + Next.js 14 + TypeScript + TailwindCSS（支持深色模式） |
| 后端 | Spring Boot 2.7 + Java 11（运行）/ JDK 17（编译）+ Maven 多模块 |
| ORM | MyBatis-Plus |
| 认证 | Sa-Token (JWT) |
| 数据库 | H2 嵌入式文件数据库（`data/giftgpt.mv.db`，持久化存储） |
| API 文档 | Knife4j (Swagger) |
| AI 服务 | Python FastAPI + LangChain（可选） |

> **注意**：后端用 JDK 17 编译为 Java 11 字节码，运行时使用 JDK 11。<br>
> JDK 17 在部分 Windows 11 上存在 WEPoll bug，导致嵌入式服务器无法启动；JDK 11 的 WindowsSelectorProvider 可正常工作。<br>
> 数据库文件 `backend/giftgpt-server/data/giftgpt.mv.db` 在服务器重启后依然存在，数据不会丢失。

---

## 项目结构

```
GiftGPT/
├── README.md
├── setup.bat                  # 一键启动
├── start-server.bat           # 启动后端
├── start-frontend.bat         # 启动前端
├── backend/                   # 后端
│   ├── pom.xml                # Maven 父 POM
│   ├── giftgpt-server/        # 启动入口
│   ├── giftgpt-common/        # 公共组件
│   ├── giftgpt-auth/          # 认证授权
│   ├── giftgpt-user/          # 用户与收礼人画像
│   ├── giftgpt-recommendation/ # AI 推荐引擎
│   ├── giftgpt-goods/         # 商品服务
│   ├── giftgpt-order/         # 订单与全链路
│   ├── giftgpt-content/       # 社区与日历
│   ├── giftgpt-enterprise/    # B端企业服务
│   └── giftgpt-ai/            # Python AI 服务
└── frontend/
    └── giftgpt-web/           # Next.js 前端
```

---

## API 概览

Base URL: `/api/v1`

| 模块 | 路径 | 说明 |
|------|------|------|
| 认证 | `POST /auth/register` `POST /auth/login` | 注册 / 登录 |
| 收礼人 | `GET/POST /recipients` `GET/PUT/DELETE /recipients/{id}` | 画像 CRUD |
| 推荐 | `POST /recommendations/search` `GET /recommendations/history` | AI 推荐 / 历史 |
| 商品 | `GET /products/search` `GET /products/{id}` | 搜索 / 详情 |
| 送礼 | `GET/POST /gifts` `POST /gifts/{id}/order` `GET /gifts/{id}/logistics` | 记录 / 下单 / 物流 |
| 贺卡 | `POST /greetings/generate` | AI 生成贺卡文案 |
| 社区 | `GET/POST /stories` `POST /stories/{id}/like` | 故事 / 点赞 |
| 企业 | `POST /enterprise/register` `POST /enterprise/orders/batch` | 注册 / 批量下单 |

---

## 快速开始

### 环境要求

- JDK 11（运行）/ JDK 17（编译）
- Node.js 20+
- Maven 3.9+
- Python 3.11+ (可选)
- **Windows**: 需 JDK 11 运行（JDK 17 在部分 Windows 版本上有 WEPoll 兼容性 bug）

### 启动

```bash
# 自动构建并启动（首次运行推荐）
build-and-run.bat

# 或手动：
# 1. 启动后端
start-server.bat

# 2. 启动前端
start-frontend.bat
```

### 访问地址

| 服务 | 地址 |
|------|------|
| 前端 | http://localhost:3000 |
| API 文档 | http://localhost:8080/swagger-ui.html |
| H2 控制台 | http://localhost:8080/h2-console |
| Python AI (可选) | http://localhost:8000/docs |

> 数据库使用 H2 嵌入式数据库，数据存储在 `backend/giftgpt-server/data/`，无需额外安装。

---

## 数据库

### 核心表

| 表名 | 说明 |
|------|------|
| `user` | 用户表 |
| `recipient` + `recipient_tag` + `recipient_profile` | 收礼人画像 |
| `user_consume_profile` | 消费画像 |
| `gift_record` | 送礼记录 |
| `product` | 商品 |
| `order` | 订单 |
| `packaging` | 包装服务 |
| `greeting_card` | 电子贺卡 |
| `feedback` | 收礼人反馈 |
| `story` | 社区故事 |
| `calendar_event` | 日历提醒 |
| `data_authorization` | 数据授权记录 |
| `recommendation_history` | 推荐历史 |
| `enterprise` | 企业 |

---

## 开发路线图

- [x] Spring Boot 多模块项目骨架
- [x] 用户注册/登录 (JWT)
- [x] 收礼人画像 CRUD + 标签系统
- [x] AI 推荐引擎 (规则匹配)
- [x] 商品搜索与详情
- [x] 订单、贺卡、物流全链路
- [x] 社区故事与日历提醒
- [x] B端企业服务
- [x] 社区故事（发布、浏览、点赞/取消点赞、评论回复）
- [x] 日历提醒（添加事件、查看列表、历史记录）
- [x] 深色模式（右上角月亮/太阳图标切换）
- [x] 手机号唯一性校验（注册时提示"该手机号已被注册"）
- [x] 数据库持久化（H2 文件数据库 `data/giftgpt.mv.db`，重启数据不丢失）
- [x] 用户名持久化（JWT token 中携带昵称，页面刷新不丢失登录状态）
- [ ] LLM 推理集成
- [ ] 知识图谱多跳推理
- [ ] 第三方电商对接

---

## 商业价值

| 收入来源 | 模式 |
|---------|------|
| CPS 佣金 | 按成交额抽佣 5%-15% |
| 包装增值 | 按单收费 ¥9.9-¥49.9 |
| B端SaaS | 按年/按人数订阅 |
| 广告推广 | 品牌新品首发推广 |
| 数据服务 | 脱敏消费趋势报告 |
