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

- **收礼人画像**：手动选择性格标签（开朗、文艺、极客等 20+ 维度），支持 MBTI 人格、性格描述与最近关注记录
- **AI 智能匹配**：`性格特征 × 场景 × 预算` → 三步式推荐（画像分析 → AI 礼物 → 真实商品匹配）+ 推荐理由；开启 Neo4j 后追加带推理链的 KG 增强结果
- **礼物包装定制**：5 种礼盒 + 6 种个性化定制（烫金礼带、手写贺卡、干花、香薰等）+ 4 种丝带绑法，AI 根据商品信息智能推荐包装方案，支持历史记录查看
- **全链路闭环**：购买跳转 → 包装定制 → AI 贺卡 → 物流追踪 → 双向反馈
- **礼物社区**：成功案例分享、日历提醒、礼物记忆库
- **B端服务**：企业批量团购、员工关怀日历

---

## 技术栈

| 层次 | 技术 |
|------|------|
| 前端 | React 18 + Next.js 14 + TypeScript + TailwindCSS（支持深色模式，部署构建为 standalone） |
| 后端 | Spring Boot 2.7.18 + Java 11（运行）/ JDK 17（编译）+ Maven 多模块（9 个模块，仅 `giftgpt-server` 可启动） |
| ORM | MyBatis-Plus |
| 认证 | Sa-Token（JWT 简单模式，`Authorization` 请求头无 `Bearer` 前缀） |
| 数据库 | H2 嵌入式文件数据库（`data/giftgpt.mv.db`，持久化存储；以后需要扩展可切 MySQL） |
| API 文档 | Knife4j (Swagger) |
| AI 推理 | Deepseek-chat（OpenAI 兼容 API）；Java 直连 + 可选 Python FastAPI/LangChain AI 服务 |
| 知识图谱 | Neo4j（可选，`KG_ENABLED=false` 默认关闭）+ `kg_taxonomy.json` 分类体系 |
| 商品数据源 | 拼多多多多进宝 官方 API（结果自动 upsert 到本地商品表） |

> **注意**：后端用 JDK 17 编译为 Java 11 字节码，运行时使用 JDK 11。<br>
> JDK 17 在部分 Windows 11 上存在 WEPoll bug，导致嵌入式服务器无法启动；JDK 11 的 WindowsSelectorProvider 可正常工作。<br>
> 数据库文件 `backend/giftgpt-server/data/giftgpt.mv.db` 在服务器重启后依然存在，数据不会丢失。<br>
> SaToken 使用 JWT 简单模式（`StpLogicJwtForSimple`）+ 内存 token DAO：**后端进程重启会使已签发 token 失效**，前端需重新登录；业务数据（用户、画像、故事等）不受影响。请求头 `Authorization` 直接携带 token，不加 `Bearer` 前缀。<br>
> 知识图谱默认关闭（`giftgpt.kg.enabled=false`）；开启后若 Neo4j 不可用，推荐主链路不受影响，KG 增强结果会被跳过。<br>
> `schema.sql` 以 `CREATE TABLE IF NOT EXISTS` + `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` 方式执行，自动修补旧库缺失的列（如 `story_reply.update_time`、`recipient.mbti/personality/recent_purchases`），`continue-on-error` 保证幂等。<br>
> `start-server.bat` 启动时若发现 jar 缺失会自动 `mvn package`，并以 `--spring.profiles.active=local` 启动，加载 `application-local.yml` 中的电商 API 密钥（该文件已 gitignore，需自备）。

---

## 第三方平台 API 配置

GiftGPT 通过拼多多多多进宝联盟 API 获取真实商品数据（商品名、价格、图片、购买链接）。API 密钥为**可选配置**——未配置时搜索功能不可用，但不影响其他功能。

> **本项目为商业竞赛用途。** 拼多多"多多进宝"是面向推广者的 CPS（按成交付费）接口，个人注册即可，不要求企业资质。

### 推荐流程

```
用户发起推荐 → Deepseek 生成礼物建议 → 逐条调用拼多多搜索 → 未命中再查本地商品库
     ↑                                      ↓
  选品搜索/商品详情 ←── upsert 缓存到本地 DB ←── 返回真实商品+链接
```

### 配置方式

在 `backend/giftgpt-server/src/main/resources/application-local.yml` 中填入密钥（该文件已加入 `.gitignore`，不会提交到仓库）：

```yaml
giftgpt:
  ai:
    deepseek:
      api-key: sk-xxx    # Deepseek API 密钥
  commerce:
    pinduoduo:
      client-id:          # 多多进宝 Client ID
      client-secret:      # 多多进宝 Client Secret
```

启动时需激活 `local` profile：`--spring.profiles.active=local`

### 拼多多多多进宝 — 个人注册流程

> **个人可以注册，不需要营业执照。** 选择"个人开发者"身份，应用审核秒过。

**网址**：https://open.pinduoduo.com（拼多多开放平台）+ https://jinbao.pinduoduo.com（多多进宝）

**Step 1 — 注册拼多多开放平台**
1. 打开 https://open.pinduoduo.com → 点击"注册"
2. 使用手机号注册，填写验证码、密码
3. 选择 **"我是第三方开发者"** → 身份类型选择 **"个人开发者"**（不需要营业执照！）
4. 填写个人身份信息（姓名、手机号、身份证号）
5. 点击"提交审核"，等待审核通过（通常几小时）

**Step 2 — 注册多多进宝**
1. 打开 https://jinbao.pinduoduo.com
2. 使用**同一手机号**登录，手机验证码即可自动注册
3. 完成实名认证

**Step 3 — 创建应用，获取 client_id / client_secret**
1. 回到拼多多开放平台 → "我的应用" → "创建应用"
2. 应用类型选择 **"多多客联盟"**
3. 填写应用名称（如 GiftGPT）和说明
4. PRD/MRD 文档：下载模板，导出为 PDF 上传即可（**目前自动审核，基本秒过**）
5. 回调地址填写：`http://www.pinduoduo.com` 或 `http://localhost:3000`
6. 提交后一般**秒通过**审核
7. 在应用详情中获取 **client_id** 和 **client_secret**

**Step 4 — 绑定并创建推广位（关键！）**
1. 进入多多进宝后台（jinbao.pinduoduo.com）→ "API权限"菜单
2. 将上一步获取的 **client_id** 填入，点击**绑定**（这一步必须做，否则 API 不可用！）
3. 在多多进宝后台创建推广位（至少一个）

**Step 5 — 申请接口权限**
1. 在拼多多开放平台应用详情 → "接口权限"
2. 搜索 `pdd.ddk.goods.search` → 申请
3. 审核通过（通常 1-2 个工作日）

**Step 6 — 填入配置**
```yaml
pinduoduo:
  client-id: <你的 Client ID>
  client-secret: <你的 Client Secret>
```

> **限额**：个人开发者每日 2,000 次。拼多多的价格单位是**分**（如 `29900` = ¥299），代码已自动转换。

#### 拼多多常见问题
- **Q: client_id 必须绑定？** A: 是的，必须在多多进宝后台的"API权限"中绑定 client_id，否则调用 API 会失败
- **Q: PRD/MRD 文档不会写？** A: 下载模板后直接导出为空白 PDF 上传即可，目前自动审核秒过，不会有人看内容
- **Q: 返回的价格不对？** A: `min_group_price` 的单位是分（如 `29900` = ¥299.00），`PddService` 已自动转换
- **Q: 签名错误？** A: 拼多多的签名规则是参数值直接拼接无分隔符（`key1value1key2value2`），`PlatformApiSigner` 已封装
- **Q: 商品链接打不开/提示商品不存在？** A: `PddService` 优先使用 `goods_id`（数字ID）拼接 URL，兼容性最好；`goods_sign` 作为 fallback

### 测试 API 是否配置成功

启动后端后，用 curl 测试：

```bash
# 搜索拼多多商品
curl "http://localhost:8080/api/v1/products/search?keyword=耳机"

# 查看拼多多授权状态
curl "http://localhost:8080/api/v1/products/platforms/pinduoduo/authority"
```

如果未配置拼多多密钥，后端日志会显示：`Pinduoduo keys not configured, skipping PDD search`——这意味着搜索被跳过，不属于报错。

---

## AI 服务与知识图谱配置（可选）

### Python AI 服务（FastAPI + LangChain）

```bash
cd backend/giftgpt-ai
pip install -r requirements.txt
cp .env.example .env        # 填入 DEEPSEEK_API_KEY
uvicorn main:app --port 8000 --reload
```

已提供 5 组路由：

| 路径 | 说明 | 当前接入状态 |
|------|------|--------------|
| `POST /api/v1/ai/chat` | LLM 多轮对话（内存保留最近 5 轮） | 独立可用 |
| `POST /api/v1/ai/profile/analyze` | 画像结构化分析 | 独立可用 |
| `POST /api/v1/ai/recommend` | LangChain 结构化推荐 | 独立可用 |
| `POST /api/v1/ai/kg/query` `/kg/sync` | KG 查询 / taxonomy 同步 | 独立可用 |
| `POST /api/v1/ai/greeting/generate` | 贺卡文案生成 | **已接入 Java**（失败自动模板兜底） |

> 当前 Java 推荐主链路仍直连 Deepseek；Python 侧 LangChain 链已具备，后续逐步切换。

### Neo4j 知识图谱

1. 启动 Neo4j（compose 文件在 `backend/docker-compose.yml`）：

```bash
cd backend
docker compose up -d neo4j
```

2. 在 `application-local.yml` 或环境变量中开启并连接：

```yaml
giftgpt:
  kg:
    enabled: true                 # 或环境变量 KG_ENABLED=true
    uri: bolt://localhost:7687
    user: neo4j
    password: giftgpt123
```

3. 验证与重建：

```bash
curl http://localhost:8080/api/v1/kg/status
curl -X POST http://localhost:8080/api/v1/kg/rebuild        # 加载 taxonomy + H2 数据重建
curl -X POST http://localhost:8080/api/v1/kg/sync-products  # 商品增量同步
```

> 分类知识源：`backend/giftgpt-recommendation/src/main/resources/kg_taxonomy.json` 与根目录 `GiftGPT知识图谱分类映射表.md`。KG 不可用时，推荐接口自动跳过增强结果，不影响主流程。


---

## 项目结构

```
GiftGPT/
├── README.md                        # 本文件
├── AGENTS.md                        # 开发约定与命令速查
├── GiftGPT知识图谱分类映射表.md       # 知识图谱分类/映射知识源
├── setup.bat                        # 一键启动（自动拉起前后端）
├── start-server.bat                 # 启动后端（jar 缺失时自动构建）
├── start-frontend.bat               # 启动前端
├── .github/workflows/               # 前后端自动部署（self-hosted runner）
├── backend/                         # 后端
│   ├── pom.xml                      # Maven 父 POM（9 个模块）
│   ├── docker-compose.yml           # MySQL/Redis/Neo4j/ES/MinIO/RabbitMQ（预留）
│   ├── giftgpt-server/              # 唯一可启动模块 + schema.sql + application.yml
│   ├── giftgpt-common/              # 公共组件、AI 客户端（DeepseekClient/PythonAiClient）
│   ├── giftgpt-auth/                # 认证授权、数据授权面板
│   ├── giftgpt-user/                # 用户与收礼人画像、礼物记忆库
│   ├── giftgpt-recommendation/      # AI 推荐引擎 + Java 侧 KG 同步/查询
│   ├── giftgpt-goods/               # 商品服务、拼多多 API 对接
│   ├── giftgpt-order/               # 订单、包装、贺卡、物流、反馈
│   ├── giftgpt-content/             # 社区、点赞/回复、日历
│   ├── giftgpt-enterprise/          # B端企业服务
│   └── giftgpt-ai/                  # FastAPI + LangChain（llm/profile/recommender/kg/content）
└── frontend/
    └── giftgpt-web/                 # Next.js 14 App Router 前端
```

---

## API 概览

Base URL: `/api/v1`

| 模块 | 路径 | 说明 |
|------|------|------|
| 认证 | `POST /auth/register` `POST /auth/login` `POST /auth/logout` | 注册 / 登录 / 登出 |
| 数据授权 | `GET /auth/authorizations` `PUT/DELETE /auth/authorizations/{id}` | 授权列表 / 修改范围 / 撤回 |
| 收礼人 | `GET/POST /recipients` `GET/PUT/DELETE /recipients/{id}` | 画像 CRUD（含 MBTI、性格描述、最近关注、标签） |
| 推荐 | `POST /recommendations/search` `POST /recommendations/analyze` `POST /recommendations/ai-gifts` `POST /recommendations/match` `GET /recommendations/history` `GET /recommendations/history/{id}` `POST /recommendations/{id}/feedback` `DELETE /recommendations/history` | 一次性推荐 / ①分析画像 / ②AI判断礼物 / ③商品匹配+KG增强 / 历史列表 / 历史详情 / 推荐反馈 / 批量删除 |
| 知识图谱 | `GET /kg/status` `POST /kg/rebuild` `POST /kg/sync-products` | KG 状态 / 重建 taxonomy+图 / 商品增量同步 |
| 商品 | `GET /products/search` `GET /products/{id}` `GET /products/platforms/pinduoduo/authority` | 搜索（PDD 实时+本地库合并）/ 详情 / 拼多多推广位授权状态 |
| 送礼 | `GET /gifts` `GET /gifts/{id}` `POST /gifts/{id}/order` `GET /gifts/{id}/logistics` `POST /gifts/{id}/feedback` `GET /gifts/{id}/feedback` | 记录列表+筛选(收礼人/场景/状态) / 详情 / 下单 / 物流时间轴 / 提交反馈(送礼方/收礼方) / 反馈列表 |
| 包装 | `GET /packaging/themes` `POST /packaging/ai-recommend` `POST /packaging/save` `GET /packaging/list` | 礼盒列表 / AI智能推荐 / 保存方案 / 历史列表 |
| 贺卡 | `POST /greetings/generate` | AI 生成贺卡文案（调用 Python content_generator，失败自动模板兜底） |
| 社区 | `GET/POST /stories` `POST /stories/{id}/like` `POST /stories/{id}/unlike` `GET/POST /stories/{id}/replies` | 故事 / 点赞 / 取消点赞 / 回复 |
| 日历 | `GET/POST /calendar` `PUT/DELETE /calendar/{id}` | 日历提醒列表 / 创建 / 编辑 / 删除 |
| 企业 | `POST /enterprise/register` `GET /enterprise/{id}` `GET /enterprise/my` `POST /enterprise/orders/batch` | 注册 / 查询 / 我的企业 / 批量下单 |

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
# Windows 一键启动（自动拉起前后端；后端 jar 缺失时自动构建）
setup.bat

# 或手动：
# 1. 构建并启动后端（构建用 JDK 17，运行用 JDK 11）
cd backend && mvn package -DskipTests -q
cd giftgpt-server && java -Djava.net.preferIPv4Stack=true -jar target/giftgpt-server-1.0.0-SNAPSHOT.jar --spring.profiles.active=local

# 2. 启动前端
cd frontend/giftgpt-web && npm install && npm run dev
```

质量检查命令：

```bash
# 前端
cd frontend/giftgpt-web && npm run lint && npx tsc --noEmit

# 后端（编译验证）
cd backend && mvn compile -q
```

### 服务器部署（比赛演示够用）

- 前端：GitHub Actions `deploy-frontend.yml` 构建 Next.js standalone 产物，部署到 Windows 服务器 `C:\giftgpt-web` 并注册计划任务；前端默认通过 Next rewrites 将 `/api/*` 代理到 `localhost:8080`。
- 后端：`deploy.yml` 使用服务器本机 Maven/JDK 17 打包，复制 jar 到 `C:\giftgpt`，用 GitHub Secrets 生成 `application-prod.yml` 后重启计划任务。
- 服务器上现在也用 H2 文件库，比赛演示够用；想更稳的话再切 `backend/docker-compose.yml` 里的 MySQL，切换前先备份。

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
| `feedback` | 收礼人反馈（role 字段区分送礼方/收礼方） |
| `logistics_event` | 物流事件节点 |
| `story` | 社区故事 |
| `calendar_event` | 日历提醒 |
| `data_authorization` | 数据授权记录 |
| `recommendation_history` | 推荐历史 |
| `enterprise` | 企业 |

---

## 开发路线图

### 已完成
- [x] 项目骨架 & 用户认证 & 收礼人画像（MBTI/性格/最近关注字段）
- [x] AI 智能推荐引擎（Deepseek LLM 三步式：画像分析 → AI 礼物 → 商品匹配）
- [x] 商品搜索与详情（拼多多多多进宝 API + 本地库缓存/合并）
- [x] 全链路闭环：订单 / 包装定制 / AI 贺卡 / 物流时间轴 / 双向反馈
- [x] 礼物包装 AI 推荐 & 定制系统（无 key/调用失败自动兜底）
- [x] 社区故事 & 点赞/回复 & 日历提醒（编辑/删除）& B端企业服务
- [x] 推荐历史 GZIP+Base64 压缩存储 & 批量管理 & 推荐反馈
- [x] 记忆库按收礼人/场景/状态筛选检索
- [x] 共享 AI 客户端提取（DeepseekClient + PythonAiClient）
- [x] 知识图谱基础设施：taxonomy 加载、Neo4j schema、收礼人/商品同步、KG 增强推荐与推理链展示、`/kg/*` 管理接口
- [x] Python giftgpt-ai 五大路由骨架 + LangChain 共享客户端；贺卡生成已接入 Java 主链路
- [x] CI/CD：GitHub Actions 前后端自动部署到 Windows 服务器
- [x] 基础体验：数据库持久化 / Token 持久化 / 深色模式 / 跨域修复 / UI 美化

### 开发中
- [ ] 知识图谱多跳推理映射完善（兴趣→品类、性格→品类、品类→opt_name、性别→品类、关系→品类；以 `GiftGPT知识图谱分类映射表.md` 为知识源，专项推进）
- [ ] Python giftgpt-ai 服务全量接入 LangChain 链（当前贺卡已接通，其余路由为独立可用状态）

### 接下来想做（不含多跳映射专项）
- [ ] 推荐链路更稳：统一走 DeepseekClient、加重试/超时/兜底
- [ ] CI 拦住低级错误：前端 lint/typecheck、后端测试、Playwright 冒烟
- [ ] 推荐结果可解释、可复现：确定性打分、去重、价格兜底
- [ ] 安全小补丁：JWT 密钥外部化、越权检查
- [ ] 全链路更实：物流状态机、二维码/语音、日历提醒、企业批量单
- [ ] 搜索快一点：全文索引/缓存/Provider 抽象

---

## 后面怎么优化（竞赛实用版）

> 说明：你们正在做的多跳映射（兴趣→品类、性格→品类、品类→opt_name、性别→品类、关系→品类）不在这节展开，按映射表继续推进就好。

现在这版功能已经比较全了，接下来的目标不是堆功能，而是：**答辩现场不翻车 + 让评委一眼看出“这不是只会调 API 的作品”**。

### 第一优先：让演示稳、别翻车

**为什么**：比赛演示最怕现场断网、没配 key、AI 超时、点一个无效 ID 白屏。

**做什么**
- [ ] 把 `RecommendationService` 里的 Deepseek 直连换成共享的 `DeepseekClient`，顺便加 2 次重试和超时；AI 挂了必须走 fallback，而不是把异常抛到前端。
- [ ] 把 `buildItemFromAiGift()` 里的 `Math.random()` 打分换掉——现在两次请求排序都不一样，评委一眼就能发现。改成“关键词命中 + 价格贴近度 + 销量”的稳定公式。
- [ ] 把“无推荐结果、商品不存在、接口报错、未登录”这些边界页都过一遍，统一给友好提示和返回按钮。
- [ ] 补几个越权检查：`GiftMemoryService.getById()`、订单详情/物流/反馈目前没校验是不是本人的，演示前最好堵上。
- [ ] CI 里加 `npm run lint` + `npx tsc --noEmit` 和后端 `mvn test`，提交不绿就别部署。

**怎么算完成**：拔掉 Deepseek key、模拟超时、乱传 ID，整个推荐和详情流程都不崩，还能看到兜底结果。

### 第二优先：让推荐“看起来更懂 TA”

- [ ] 推荐结果做价格和去重兜底：超过预算的过滤掉，同一个商品别出现两次。
- [ ] 推荐卡片把“匹配 92%”拆成“兴趣命中 50% · 价格贴近 30% · 销量热度 20%”三个小标签，推荐理由跟最高分项对应，评委一眼看懂“为什么推荐”。
- [ ] 空结果给引导（换场景/放宽预算），别让页面空着。
- [ ] 加一张 `recommend_event` 表记录“查看详情 / 去购买 / 去包装”点击，攒出 CTR 后再作为排序权重，答辩时可以讲“数据飞轮”。
- [ ] 商品搜索改成可替换的 Provider 接口，现在只有 PDD，以后接京东/淘宝不用改业务代码；PDD 查询加 5 分钟缓存，省每日 2000 次限额。
- [ ] 搜索结果先做相关性过滤，标题一个关键词都不中的商品别展示，只缓存。

**演示话术**：先讲画像→LLM→商品匹配主链路，再讲 KG 推理链增强，最后补一句“排序是可解释的，分数来自命中标签、价格和销量”，比纯随机分有说服力。

### 第三优先：把“演示闭环”做成“真闭环”

- [ ] 物流时间轴现在是模拟的，先加个 `source=simulated` 标记，答辩时主动说明；有机会再接真实物流 API。
- [ ] 贺卡二维码和语音 URL 目前是空的，可以先用 Python 生成二维码 + 语音文件放 MinIO，让页面真的能扫码/播放。
- [ ] 日历提醒做个 `@Scheduled` 每日扫描，哪怕先出站内通知，也比纯 CRUD 更像产品。
- [ ] 企业批量下单别只返回字符串，真正建 `gift_record/order`，哪怕只支持 3-5 个员工的小批量。
- [ ] 订单状态用一个简单的状态机管理，不要各处直接改 `status` 字段。

### 加分项：竞赛评委爱听的东西

- [ ] 推荐点击/反馈回流：哪怕先做 CSV/周报统计，也能讲“推荐效果在持续优化”。
- [ ] AI 调用日志：记录每次模型、耗时、token、是否降级，答辩可以放一张成本/可靠性截图。
- [ ] 隐私授权闭环：`data_authorization` 已经有表，演示时补一个“撤回授权 → 删除派生缓存”的动作，隐私分就有了。
- [ ] 搜索/推荐性能：本地库小没关系，讲清楚“换 MySQL 全文索引或 ES 的路径”即可，不一定要现场实现。

### 建议节奏（按比赛剩余时间倒推）

1. **还剩 1 周**：只做第一优先，再把三个典型演示用例（比如“给文艺女友选生日礼物”“给爸爸选父亲节礼物”“企业小批量团购”）录屏过 3 遍。
2. **还剩 2-3 周**：做第二优先，整理“画像 → 推荐 → 反馈”数据飞轮的一页 PPT。
3. **还剩 1 个月以上**：再做第三优先和加分项，丰富全链路和 B 端演示。

### 先别做

- 别急着拆微服务、上 K8s：现在模块化单体完全够用，评委更关心推荐准不准、演示顺不顺。
- 别急着做多模态/社交数据导入：授权和脱敏没做实之前，这是减分风险。
- 别自己训模型：先用好 prompt、候选集和反馈权重，成本和效果都更可控。

---

## 商业价值（怎么赚钱）

| 收入来源 | 模式 |
|---------|------|
| CPS 佣金 | 按成交额抽佣 5%-15% |
| 包装增值 | 按单收费 ¥9.9-¥49.9 |
| B端SaaS | 按年/按人数订阅 |
| 广告推广 | 品牌新品首发推广 |
| 数据服务 | 脱敏消费趋势报告 |
