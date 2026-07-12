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
- **礼物包装定制**：5 种礼盒 + 6 种个性化定制（烫金礼带、手写贺卡、干花、香薰等）+ 4 种丝带绑法，AI 根据商品信息智能推荐包装方案，支持历史记录查看
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
| AI 推理 | Deepseek-v4（通过 OpenAI 兼容 API） |
| 商品数据源 | 拼多多多多进宝 官方 API |

> **注意**：后端用 JDK 17 编译为 Java 11 字节码，运行时使用 JDK 11。<br>
> JDK 17 在部分 Windows 11 上存在 WEPoll bug，导致嵌入式服务器无法启动；JDK 11 的 WindowsSelectorProvider 可正常工作。<br>
> 数据库文件 `backend/giftgpt-server/data/giftgpt.mv.db` 在服务器重启后依然存在，数据不会丢失。<br>
> SaToken 默认使用内存 token-store，**后端进程重启会使已签发的 token 失效**，前端需重新登录；业务数据（用户、画像、故事等）不受影响。<br>
> `schema.sql` 以 `CREATE TABLE IF NOT EXISTS` + `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` 方式执行，自动修补旧库缺失的列（如 `story_reply.update_time`、`recipient.mbti/personality/recent_purchases`），`continue-on-error` 保证幂等。<br>
> `start-server.bat` 启动时若发现 jar 缺失会自动 `mvn package`，并以 `--spring.profiles.active=local` 启动，加载 `application-local.yml` 中的电商 API 密钥（该文件已 gitignore，需自备）。

---

## 第三方平台 API 配置

GiftGPT 通过拼多多多多进宝联盟 API 获取真实商品数据（商品名、价格、图片、购买链接）。API 密钥为**可选配置**——未配置时搜索功能不可用，但不影响其他功能。

> **本项目为商业竞赛用途。** 拼多多"多多进宝"是面向推广者的 CPS（按成交付费）接口，个人注册即可，不要求企业资质。

### 推荐流程

```
用户发起推荐 → Deepseek 生成礼物建议 → 按关键词在本地 DB 中匹配真实商品
     ↑                                                    ↓
  选品搜索 ←── 缓存到本地 DB ←── 调用拼多多多多进宝 API
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
| 推荐 | `POST /recommendations/search` `POST /recommendations/analyze` `POST /recommendations/ai-gifts` `POST /recommendations/match` `GET /recommendations/history` `GET /recommendations/history/{id}` `DELETE /recommendations/history` | 一次性推荐 / ①分析性格 / ②AI判断礼物 / ③拼多多搜索 / 历史列表 / 历史详情 / 批量删除 |
| 商品 | `GET /products/search` `GET /products/{id}` `GET /products/platforms/pinduoduo/authority` | 搜索 / 详情 / 拼多多授权状态 |
| 送礼 | `GET/POST /gifts` `POST /gifts/{id}/order` `GET /gifts/{id}/logistics` | 记录 / 下单 / 物流 |
| 包装 | `GET /packaging/themes` `POST /packaging/ai-recommend` `POST /packaging/save` `GET /packaging/list` | 礼盒列表 / AI智能推荐 / 保存方案 / 历史列表 |
| 贺卡 | `POST /greetings/generate` | AI 生成贺卡文案 |
| 社区 | `GET/POST /stories` `POST /stories/{id}/like` `POST /stories/{id}/unlike` `GET/POST /stories/{id}/replies` | 故事 / 点赞 / 取消点赞 / 评论 |
| 日历 | `GET/POST /calendar` | 日历提醒列表 / 创建 |
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

### 已完成
- [x] 项目骨架 & 用户认证 & 收礼人画像
- [x] AI 智能推荐引擎（Deepseek LLM + 真实商品匹配 + 分步骤进度展示）
- [x] 商品搜索与详情（拼多多多多进宝 API）
- [x] 全链路闭环：订单 / 包装定制 / AI 贺卡 / 物流
- [x] 礼物包装 AI 推荐 & 定制系统
- [x] 社区故事 & 日历提醒 & B端企业服务
- [x] 推荐历史压缩存储 & 批量管理
- [x] 基础体验：数据库持久化 / Token 持久化 / 深色模式 / 跨域修复 / UI 美化

### 待开发
- [ ] LLM 推理集成
- [ ] 知识图谱多跳推理

---

## 商业价值

| 收入来源 | 模式 |
|---------|------|
| CPS 佣金 | 按成交额抽佣 5%-15% |
| 包装增值 | 按单收费 ¥9.9-¥49.9 |
| B端SaaS | 按年/按人数订阅 |
| 广告推广 | 品牌新品首发推广 |
| 数据服务 | 脱敏消费趋势报告 |
