# AGENTS.md — GiftGPT

## 架构概览

三个服务，默认开发链路只用前两个：

| 服务 | 技术栈 | 端口 | 入口 |
|------|--------|------|------|
| 后端 | Spring Boot 2.7.18 + MyBatis-Plus + Sa-Token + H2 | 8080 | `backend/giftgpt-server`（唯一可启动模块） |
| 前端 | Next.js 14 App Router + React 18 + TailwindCSS | 3000 | `frontend/giftgpt-web` |
| AI 服务（可选） | FastAPI + LangChain | 8000 | `backend/giftgpt-ai/main.py` |

后端是 Maven 多模块项目（9 个模块），但只有 `giftgpt-server` 可启动——它的 `spring-boot-maven-plugin` 设了 `skip=false`，父 POM 和其他模块都是 `skip=true`。`GiftGptApplication` 用 `scanBasePackages = "com.giftgpt"` 扫描全部模块，无需额外配置。

## 开发命令

### 后端

```bash
# 构建（在 backend/ 下，用 JDK 17 编译）
cd backend && mvn package -DskipTests -q

# 运行（用 JDK 11，在 backend/giftgpt-server/ 下）
java -Djava.net.preferIPv4Stack=true -jar target/giftgpt-server-1.0.0-SNAPSHOT.jar --spring.profiles.active=local

# 或用 Maven 直接跑
cd backend/giftgpt-server && mvn spring-boot:run
```

Windows 一键启动：`setup.bat`（同时拉起前后端）。`start-server.bat` 在 jar 缺失时自动 `mvn package`。

### 前端

```bash
cd frontend/giftgpt-web
npm install
npm run dev      # 开发服务器 http://localhost:3000
npm run build    # 生产构建
npm run lint     # next lint（唯一的代码检查命令）
```

### Python AI 服务（可选）

```bash
cd backend/giftgpt-ai
pip install -r requirements.txt
uvicorn main:app --port 8000 --reload
```

## 关键约束与陷阱

### JDK 版本（重要）

- **编译用 JDK 17**，`maven-compiler-plugin` 的 source/target 是 11，但 `java.version` 属性是 17。
- **运行必须用 JDK 11**（Windows 上 JDK 17 有 WEPoll bug，会导致嵌入式服务器无法启动）。`start-server.bat` 按顺序探测 `%USERPROFILE%\jdk-11` → Eclipse Adoptium JDK 11 → 回退到 JDK 17 并告警。
- 改后端代码后，用 JDK 17 重新 `mvn package`，再用 JDK 11 运行 jar。

### 数据库（H2 嵌入式）

- 数据库文件：`backend/giftgpt-server/data/giftgpt.mv.db`，持久化，已 gitignore（`**/data/`）。
- `schema.sql` 以 `CREATE TABLE IF NOT EXISTS` + `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` 执行，`continue-on-error: true`，幂等。改表结构直接加 `ALTER` 语句，不用手写迁移。
- H2 控制台：http://localhost:8080/h2-console（用户 `sa`，无密码）。
- `docker-compose.yml` 定义了 MySQL/Redis/Neo4j/ES/MinIO/RabbitMQ，但**默认开发不用 Docker**——H2 嵌入式是默认数据源。Docker 仅用于未来扩展。

### Sa-Token 认证

- **内存 token-store**：后端重启后所有已签发 token 失效，前端需重新登录。业务数据不受影响。
- Token 通过 `Authorization` 请求头发送，**无 `Bearer` 前缀**（直接是 token 字符串）。
- 前端将 token 存在 `localStorage`，见 `frontend/giftgpt-web/src/lib/api.ts`。

### API 响应格式

所有后端 API 返回统一结构：`{ code: number, message: string, data: T }`。`code === 200` 表示成功，其他值抛错。前端 `api.ts` 的 `request()` 函数已封装此逻辑。

### 配置文件

- `application.yml`：主配置，用环境变量占位（`${DEEPSEEK_API_KEY:}` 等）。
- `application-local.yml`：**已 gitignore**，存放 Deepseek API key 和京东/淘宝/拼多多联盟密钥。启动时用 `--spring.profiles.active=local` 加载。
- 未配置某平台密钥时，该平台被**跳过而非报错**（日志显示 `XX keys not configured, skipping`）。

### 前端 API 基址

`http://localhost:8080/api/v1`（默认），可用 `NEXT_PUBLIC_API_URL` 环境变量覆盖。所有 API 调用集中在 `src/lib/api.ts`。

## 代码检查

- **前端**：`npm run lint`（next lint）。无独立的 typecheck 脚本，但 `tsconfig.json` 存在，可手动 `npx tsc --noEmit`。
- **后端**：无测试文件（`src/test/` 为空），无 checkstyle/spotless 配置。`mvn package` 默认跳过测试（`-DskipTests`）。
- **Python**：无测试配置。

## 变更与测试约束

### 最小化变动原则

- **只改必须改的**：每次变更限定在完成任务所需的最小范围内，不夹带重构、不顺手改无关代码、不新增投机性抽象。
- **优先复用**：改动前先查本仓库是否已有可复用的工具函数、类型、组件或模式，能复用就不重写。
- **最短 diff**：在正确理解问题全流程的前提下，选择能解决问题的最短改动；不理解问题就动手的"小 diff"是第二个 bug。
- **根因优先**：bug 修复改在所有调用方共同经过的共享位置，不在单个调用路径打补丁。

### 变更说明

每次输出代码后，必须列出本次改动的清单：

- **改了哪些文件**：文件路径。
- **改了哪些逻辑**：每个文件中变更的具体逻辑点（函数/字段/分支等），一句话说明改了什么、为什么。

格式示例：

```
变更清单：
- backend/giftgpt-server/.../UserController.java：login() 增加空指针校验，防止 token 为 null 时 NPE。
- frontend/giftgpt-web/src/lib/api.ts：request() 统一处理 401 跳转登录页。
```

### 测试与修复闭环

- **改完必测**：每次变更后运行对应的检查命令——前端 `npm run lint`（必要时 `npx tsc --noEmit`），后端 `mvn package -DskipTests`（或 `mvn compile`），Python 按需。
- **测试不通过则修复**：lint/编译/类型检查失败时，不得交付，必须定位并修复根因后重新运行，直到通过。
- **无法验证时说明**：若改动无法用现有命令验证（如纯文档），明确说明"无对应检查命令，未运行测试"。

## .gitignore 特殊项

- `package-lock.json` 和 `yarn.lock` 被 gitignore——只保留 `package.json`。
- `README(detailed).md` 被 gitignore。
- 根目录的 `test-*.py` 和 `*.json` 被 gitignore（测试脚本和临时数据不入库）。
- `application-local.yml` 和 `application-prod.yml` 被 gitignore。

## 改文字的快速定位

| 改什么 | 看哪里 |
|--------|--------|
| 前端页面文案 | `frontend/giftgpt-web/src/app/**/page.tsx` |
| 前端公共组件 | `frontend/giftgpt-web/src/components/*.tsx` |
| 后端 API 返回消息 | 各模块 `controller/` 和 `service/` 下的 `.java` 文件 |
| 数据库表结构 | `backend/giftgpt-server/src/main/resources/schema.sql` |
| 后端配置常量 | `backend/giftgpt-server/src/main/resources/application.yml` |
