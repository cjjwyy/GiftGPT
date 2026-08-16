# GiftGPT 后续优化执行方案（竞赛版 · 审阅稿）

> 这份文档是把 `README.md` 里“后面怎么优化（竞赛实用版）”的建议拆成可以照着做的任务。
> 状态：**待审阅**。先看阶段总览，觉得哪块不合适直接标出来，我再改。
>
> 范围：**不含**你们正在做的多跳映射（兴趣→品类、性格→品类、品类→opt_name、性别→品类、关系→品类），那块按现有映射表继续推进。

---

## 0. 怎么用这份文档

- 每个任务都写了：**目标 / 改哪里 / 具体步骤 / 怎么验证 / 做完长这样**。
- 任务分三档：
  - **A 档：答辩前做**，目标是演示不翻车。
  - **B 档：加分项**，目标是让推荐和搜索更像一个真产品。
  - **C 档：有余力再做**，目标是补全链路和 B 端。
- 不需要全做完。按比赛剩余时间从 A 档往下挑即可。
- 所有改动继续遵守仓库约定：
  - 后端改完跑 `cd backend && mvn compile -q`。
  - 前端改完跑 `cd frontend/giftgpt-web && npm run lint && npx tsc --noEmit`。
  - 最小 diff，一个任务一个 commit，commit 用中文 `feat:/fix:/chore:`。
- **动手前先和做多跳映射的同学同步一下**：A1/A2 会改 `RecommendationService.java`，如果对方也在改，先合一边，避免白干。

---

## 1. 阶段总览

| 档位 | 任务 | 预计工作量 | 对答辩的价值 | 依赖 |
|------|------|-----------|--------------|------|
| A1 | 统一 Deepseek 调用 + 重试 + 兜底 | 0.5 天 | 断网/没 key 都不崩 | 无 |
| A2 | 推荐打分去随机化 | 0.5 天 | 结果可复现，评委不会看到两次排序不同 | A1 后更顺 |
| A3 | 前端边界页面巡检 | 0.5 天 | 点不存在的 ID 不再卡 Loading | 无 |
| A4 | 接口越权检查 | 0.5 天 | 安全问题现场被问不慌 | 无 |
| A5 | CI 质量门禁 | 0.5-1 天 | 提交不会带着低级错误部署 | 无 |
| A6 | 密钥/配置小加固 | 0.5 天 | 评审看配置不扣分 | A5 后 |
| B1 | 推荐点击事件 + 数据飞轮 | 1 天 | 能讲“越用越准” | A2 |
| B2 | 商品搜索 Provider + PDD 缓存 | 1 天 | 省 API 额度，以后好扩展 | 无 |
| B3 | 搜索 total 和相关性过滤 | 0.5 天 | 分页正确、结果不跑偏 | B2 |
| B4 | 空结果引导 + 价格/去重兜底 | 0.5 天 | 边界体验完整 | A2 |
| B5 | 推荐卡片展示推荐原因 + 简化权重 | 0.5 天 | 可解释推荐，评委直接看到“为什么推荐” | A2 |
| C1 | 物流标记 + 订单状态机 | 0.5-1 天 | 演示更真实 | 无 |
| C2 | 贺卡二维码/语音 + 收礼方访问 | 1-2 天 | 全链路亮点 | 无 |
| C3 | 日历定时提醒 | 1 天 | 提醒功能不再只是 CRUD | 无 |
| C4 | 企业批量下单真实落库 | 1 天 | B 端演示有东西看 | 无 |
| C5 | AI 调用日志/成本看板 | 0.5-1 天 | 有可靠性截图可讲 | A1 |

---

## 2. A 档：答辩前把演示做稳

### A1. 统一 Deepseek 调用 + 重试 + 兜底

**目标**

现在 `RecommendationService` 自己写了一套 Deepseek DTO 和 `callDeepseek()`，跟 `giftgpt-common` 里的 `DeepseekClient` 重复。统一之后只维护一处，并且加重试，AI 挂了也能走 fallback。

**改哪里**

- `backend/giftgpt-common/src/main/java/com/giftgpt/common/ai/DeepseekClient.java`
- `backend/giftgpt-recommendation/src/main/java/com/giftgpt/recommendation/service/RecommendationService.java`

**具体步骤**

1. 给 `DeepseekClient` 增加重试，不动现有方法签名：

```java
private static final int MAX_RETRIES = 2;
private static final long RETRY_BASE_MS = 500;

public String chat(String system, String prompt, int maxTokens) throws IOException {
    IOException last = null;
    for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
        try {
            return doChat(system, prompt, maxTokens);   // 把现在的请求逻辑搬进 doChat
        } catch (IOException e) {
            last = e;
            if (attempt == MAX_RETRIES || !retryable(e)) {
                break;
            }
            log.warn("Deepseek call failed, retry {}/{}: {}", attempt + 1, MAX_RETRIES, e.getMessage());
            sleepBeforeRetry(attempt);
        }
    }
    throw last;
}
```

2. `doChat` 里顺手做两件事：
   - 响应解析前检查 `choices` 非空，空数组直接抛 `IOException("empty choices")`。
   - 保留 `stripMarkdown()`，模型偶尔包代码块也能解析。

3. `RecommendationService` 里：
   - 注入 `private final DeepseekClient deepseekClient;`
   - 删除本地 `DeepseekMessage / DeepseekRequest / DeepseekChoice / DeepseekResponse / DeepseekUsage` 和 `callDeepseek()`。
   - `generateAiGifts()` 改成：

```java
try {
    String content = deepseekClient.chat(SYSTEM_PROMPT, prompt, 4096);
    String json = DeepseekClient.stripMarkdown(content);
    AiGiftResult aiResult = objectMapper.readValue(json, AiGiftResult.class);
    resp.setGifts(aiResult.getGifts());
    resp.setSummary(aiResult.getSummary());
} catch (Exception e) {
    log.error("Deepseek API call failed, using tag-based fallback", e);
    resp.setGifts(fallbackAiGifts(request, tagNames));
    resp.setSummary("AI 服务暂时不可用，以下为基于标签的推荐结果");
}
```

4. 解析 JSON 再做一层保护：`readValue` 失败时尝试截取第一个 `{...}` 再解析，仍失败再 fallback。可以先只在日志里记录，不一次改太散。

**怎么验证**

```bash
cd backend && mvn compile -q
```

然后本地跑起来，把 `application-local.yml` 里的 key 临时删掉，调用：

```bash
curl -X POST http://localhost:8080/api/v1/recommendations/search \
  -H "Authorization: <token>" -H "Content-Type: application/json" \
  -d '{"recipientId":1,"occasion":"birthday","budget":300}'
```

**做完长这样**：没 key、key 错、超时，推荐接口都返回 fallback 清单，不 500。

---

### A2. 推荐打分去随机化

**目标**

现在 `buildItemFromAiGift()` 里是 `0.90 + Math.random() * 0.10`，同样请求两次排序不同，评委可能看出来。改成稳定、能解释的分数。

**改哪里**

- `backend/giftgpt-recommendation/src/main/java/com/giftgpt/recommendation/service/RecommendationService.java`

**具体步骤**

1. `buildItemFromAiGift(AiGift gi)` 加一个预算参数：`buildItemFromAiGift(AiGift gi, BigDecimal budget)`，调用处传 `request.getBudget()`。

2. 增加两个小工具函数：

```java
private double keywordScore(String productName, String giftName) {
    String name = productName == null ? "" : productName.toLowerCase();
    String gift = stripParentheses(giftName).toLowerCase();
    String[] kws = gift.split("[\\s,，、]+");
    int hit = 0;
    int weight = 0;
    for (String k : kws) {
        if (k.length() >= 2 && name.contains(k)) { hit++; weight += k.length(); }
    }
    return kws.length == 0 ? 0.0 : Math.min(1.0, (double) hit / kws.length);
}

private double priceFit(BigDecimal productPrice, BigDecimal budget) {
    if (productPrice == null || budget == null || budget.doubleValue() <= 0) return 0.5;
    double diff = Math.abs(productPrice.doubleValue() - budget.doubleValue());
    return Math.max(0.0, 1.0 - diff / budget.doubleValue());
}

private double salesScore(Product p) {
    int sales = p == null || p.getSalesCount() == null ? 0 : p.getSalesCount();
    return Math.min(1.0, sales / 10000.0);   // 1 万件销量算满分，具体阈值按商品库调整
}
```

3. 找到商品后替换打分：

```java
double kw = keywordScore(matched.getName(), gi.getName());
double fit = priceFit(matched.getPrice(), budget);
double sales = salesScore(matched);
double score = 0.50 * kw + 0.30 * fit + 0.20 * sales;
item.setScore(Math.min(1.0, Math.max(0.0, score)));
```

4. 没匹配到真实商品时，score 给固定值（比如 `0.50`），不要随机。

**怎么验证**

- 后端 `mvn compile -q`。
- 同一个收礼人、同一个场景和预算，连续调两次 `POST /recommendations/search`，排序一致（LLM 返回内容一致的前提下）。
- 前端推荐页分数变化能对应到关键词/价格/销量。

**做完长这样**：可以跟评委说“分数 = 兴趣/关键词命中 50% + 价格贴近 30% + 销量热度 20%”，而不是“调了 AI 排的”。

---

### A3. 前端边界页面巡检

**目标**

把无效 ID、空列表、接口失败、未登录这些边界都过一遍，不白屏、不永远转圈。

**改哪里**

主要看这些文件：

- `frontend/giftgpt-web/src/app/gifts/[id]/page.tsx`
- `frontend/giftgpt-web/src/app/products/[id]/page.tsx`
- `frontend/giftgpt-web/src/app/recipients/[id]/page.tsx`
- `frontend/giftgpt-web/src/app/recommend/page.tsx`
- `frontend/giftgpt-web/src/app/recommend/history/page.tsx`
- `frontend/giftgpt-web/src/app/products/page.tsx`
- `frontend/giftgpt-web/src/app/gifts/page.tsx`
- `frontend/giftgpt-web/src/app/packaging/page.tsx`
- `frontend/giftgpt-web/src/app/calendar/page.tsx`
- `frontend/giftgpt-web/src/app/stories/page.tsx`

**具体步骤**

1. 先扫一遍被吞掉的错误：

```bash
grep -R "catch(() => {})" frontend/giftgpt-web/src
grep -R "catch (() => {})" frontend/giftgpt-web/src
```

2. 每个页面确认三种状态都有：
   - 加载中：转圈或骨架屏。
   - 有数据：正常列表/详情。
   - 出错/空数据：明确文案 + 返回链接或刷新按钮。

3. 建议抽一个小组件，别每页复制：

```tsx
// frontend/giftgpt-web/src/components/EmptyState.tsx
export function EmptyState({ title, action }: { title: string; action?: React.ReactNode }) {
  return (
    <div className="max-w-2xl mx-auto px-4 py-16 text-center text-gray-400">
      <p className="mb-3">{title}</p>
      {action}
    </div>
  );
}
```

4. 重点用例：
   - `/products/999999`、`/gifts/999999`：显示“不存在/已删除”，不 Loading。
   - 商品搜索无结果：显示“没有找到相关商品”。
   - 推荐结果为空：显示“没有匹配的礼物，试试放宽预算或换个场景”。
   - 未登录访问需要登录的页面：统一跳登录或给“去登录”按钮。

**怎么验证**

```bash
cd frontend/giftgpt-web && npm run lint && npx tsc --noEmit
```

浏览器手点一遍，再用 Playwright 的 `TC-006/TC-007` 更新断言：无效 ID 应该出现错误页，而不是 Loading。

**做完长这样**：现场随便点，任何页面都有反馈，不会卡在加载动画。

---

### A4. 接口越权检查

**目标**

目前有几处只查了资源存不存在，没查是不是当前用户的。堵上之后，两个账号之间互相访问会被拒绝。

**改哪里**

- `backend/giftgpt-user/src/main/java/com/giftgpt/user/service/GiftMemoryService.java`
- `backend/giftgpt-order/src/main/java/com/giftgpt/order/service/OrderService.java`

**具体步骤**

1. `GiftMemoryService.getById()`：

```java
public GiftRecord getById(Long id) {
    Long userId = StpUtil.getLoginIdAsLong();
    GiftRecord record = giftRecordMapper.selectById(id);
    if (record == null) {
        throw new BusinessException(ResultCode.GIFT_RECORD_NOT_FOUND);
    }
    if (!record.getUserId().equals(userId)) {
        throw new BusinessException(ResultCode.FORBIDDEN);
    }
    return record;
}
```

2. `OrderService` 增加一个私有工具方法，后面所有跟送礼记录有关的接口都先过它：

```java
private GiftRecord loadOwnGiftRecord(Long giftRecordId) {
    Long userId = StpUtil.getLoginIdAsLong();
    GiftRecord record = giftRecordMapper.selectById(giftRecordId);
    if (record == null) {
        throw new BusinessException(ResultCode.GIFT_RECORD_NOT_FOUND);
    }
    if (!record.getUserId().equals(userId)) {
        throw new BusinessException(ResultCode.FORBIDDEN);
    }
    return record;
}
```

3. 在以下方法里调用：
   - `getOrderDetail(Long id)`：查到 order 后，`loadOwnGiftRecord(order.getGiftRecordId())`。
   - `getLogistics(Long giftRecordId)`：开头先 `loadOwnGiftRecord(giftRecordId)`。
   - `submitFeedback(Long giftRecordId, Feedback feedback)`：开头先 `loadOwnGiftRecord(giftRecordId)`。
   - `listFeedback(Long giftRecordId)`：开头先 `loadOwnGiftRecord(giftRecordId)`。

4. 收礼方反馈先不放开。以后做“扫码反馈”时，再用一次性 token 单独鉴权，不要直接放行登录用户。

**怎么验证**

注册两个账号 A、B：

- A 创建画像和送礼记录。
- B 用自己 token 访问 A 的 `/gifts/{id}`、`/gifts/{id}/logistics`、`/gifts/{id}/feedback`，应返回 403/404。
- A 自己访问仍正常。

**做完长这样**：横向越权这条常见扣分点没有了。

---

### A5. CI 质量门禁

**目标**

提交代码后先跑检查，再部署，不让低级错误上线。

**改哪里**

- `frontend/giftgpt-web/package.json`
- `.github/workflows/deploy-frontend.yml`
- `.github/workflows/deploy.yml`
- 新建少量后端单测（可选但推荐）
- `tests/e2e/`（清理 + 更新断言）

**具体步骤**

1. 前端加 typecheck：

```json
"scripts": {
  "dev": "next dev -p 3000",
  "build": "next build",
  "start": "next start -p 3000",
  "lint": "next lint",
  "typecheck": "tsc --noEmit"
}
```

2. `deploy-frontend.yml` 在 build 前加：

```yaml
      - name: Lint and typecheck
        shell: powershell
        working-directory: frontend/giftgpt-web
        run: |
          npm run lint
          npm run typecheck
```

3. `deploy.yml` 把打包那一步拆开：

```yaml
      - run: mvn test -q
        working-directory: backend
      - run: mvn package -DskipTests -q
        working-directory: backend
```

4. 后端最小单测先写 2-3 个，不需要 Spring 上下文，直接 Mockito：

```java
// PackagingService 示例：AI 不可用时返回 fallback
class PackagingServiceTest {
    @Test
    void shouldFallbackWhenAiThrows() {
        DeepseekClient client = mock(DeepseekClient.class);
        when(client.isConfigured()).thenReturn(true);
        when(client.chat(any(), any(), anyInt())).thenThrow(new IOException("timeout"));

        PackagingService service = new PackagingService(
            mock(PackagingMapper.class), mock(GiftRecordMapper.class),
            mock(RecipientMapper.class), client);

        AiPackagingRequest req = new AiPackagingRequest();
        req.setProductName("测试礼物");
        AiPackagingResult result = service.aiRecommend(req);

        assertNotNull(result);
        assertNotNull(result.getPackagingType());
    }
}
```

5. Playwright：
   - 删掉 `tests/example.spec.js`。
   - 把 `TC-006/TC-007` 的断言从“永远 Loading”改成“显示错误页和返回链接”。
   - 本地跑：`npx playwright test --project=chromium`。
   - CI 可以先只跑 chromium，不追求三浏览器。

**怎么验证**

推一次代码，看 Actions 里 lint/typecheck/test 是否都过，再触发部署。

**做完长这样**：低级错误在 CI 拦住，部署日志有明确检查步骤。

---

### A6. 密钥/配置小加固

**目标**

把默认 JWT 密钥、CORS 全放开这些点处理一下，评审看配置文件不扣分。

**改哪里**

- `backend/giftgpt-server/src/main/resources/application.yml`
- `backend/giftgpt-common/src/main/java/com/giftgpt/common/config/WebConfig.java`
- `.github/workflows/deploy.yml`

**具体步骤**

1. `application.yml`：

```yaml
sa-token:
  jwt-secret-key: ${JWT_SECRET:giftgpt-dev-only-secret-change-me}
```

2. `deploy.yml` 生成 `application-prod.yml` 时把 `JWT_SECRET` 也注入：

```powershell
env:
  JWT_SECRET: ${{ secrets.JWT_SECRET }}
  DEEPSEEK_API_KEY: ${{ secrets.DEEPSEEK_API_KEY }}
  PDD_CLIENT_ID: ${{ secrets.PDD_CLIENT_ID }}
  PDD_CLIENT_SECRET: ${{ secrets.PDD_CLIENT_SECRET }}
```

生成配置时追加：

```yaml
sa-token:
  jwt-secret-key: <来自 Secrets>
```

3. CORS 改成配置化白名单：

```java
@Value("${giftgpt.cors.allowed-origins:http://localhost:3000}")
private String allowedOrigins;

// 用 split(",") 逐个 addAllowedOrigin
```

`application.yml` 增加：

```yaml
giftgpt:
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:3000}
```

4. H2 console 按 profile 控制：

```yaml
spring:
  h2:
    console:
      enabled: ${H2_CONSOLE_ENABLED:true}
```

服务器 `application-prod.yml` 里设 `false`。

**怎么验证**

- 本地照常能登录、能跨域调用。
- 服务器部署后，`/h2-console` 不对外，JWT 密钥不是仓库里的默认值。

**做完长这样**：安全和配置评审项少两个扣分点。

---

## 3. B 档：加分项，让推荐更“像样”

### B1. 推荐点击事件 + 数据飞轮

**目标**

记录“查看详情 / 去购买 / 去包装”的点击，攒一段时间后就能算 CTR，答辩时讲“推荐效果在持续优化”。

**改哪里**

- `backend/giftgpt-server/src/main/resources/schema.sql`
- `backend/giftgpt-recommendation/.../entity/` 新建 `RecommendEvent.java`
- `backend/giftgpt-recommendation/.../mapper/` 新建 `RecommendEventMapper.java`
- `RecommendationController.java` + `RecommendationService.java`
- `frontend/giftgpt-web/src/lib/api.ts`
- `frontend/giftgpt-web/src/components/GiftCard.tsx`

**具体步骤**

1. `schema.sql` 末尾追加：

```sql
-- Recommend click events (for CTR / data flywheel)
CREATE TABLE IF NOT EXISTS recommend_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    recipient_id BIGINT,
    occasion VARCHAR(50),
    product_id BIGINT,
    product_name VARCHAR(200),
    event_type VARCHAR(20) NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

2. 实体和 mapper 照抄现有 `RecommendationHistory` 的写法。

3. 后端加一个上报接口：

```java
@PostMapping("/events")
public Result<Void> trackEvent(@RequestBody RecommendEventRequest request) {
    recommendationService.trackEvent(request);
    return Result.ok();
}
```

`event_type` 只允许 `detail / buy / packaging` 三种，其他直接忽略。

4. `RecommendEventRequest` 字段：`recipientId`、`occasion`、`productId`、`productName`、`eventType`。用户 ID 从登录态取。

5. `api.ts` 加：

```ts
trackEvent: (data: {
  recipientId?: number;
  occasion?: string;
  productId?: number;
  productName?: string;
  eventType: 'detail' | 'buy' | 'packaging';
}) => request<void>('/recommendations/events', { method: 'POST', body: JSON.stringify(data) }),
```

6. `GiftCard.tsx` 三个按钮的 `onClick` 里调用 `recommendApi.trackEvent(...)`，不 `await` 也不阻塞跳转，失败静默即可。

7. 简单统计 SQL 示例（以后做周报/看板用）：

```sql
SELECT occasion, event_type, COUNT(*) AS cnt
FROM recommend_event
GROUP BY occasion, event_type
ORDER BY cnt DESC;
```

**怎么验证**

- 前端点“查看详情/去购买/包装”，后端 `recommend_event` 表新增对应行。
- 登录用户 A 的记录带 A 的 user_id；未登录不会调用成功。

**做完长这样**：PPT 能放一张“点击事件 → CTR → 排序权重”的数据飞轮图。

---

### B2. 商品搜索 Provider + PDD 查询缓存

**目标**

现在 `CommerceService` 直接依赖 `PddService`，以后接京东/淘宝要改调用方。抽个接口，并给 PDD 加个 5 分钟缓存，省每日 2000 次限额。

**改哪里**

- 新建 `backend/giftgpt-goods/src/main/java/com/giftgpt/goods/service/ProductSearchProvider.java`
- `backend/giftgpt-goods/src/main/java/com/giftgpt/goods/service/PddService.java`
- `backend/giftgpt-goods/src/main/java/com/giftgpt/goods/service/CommerceService.java`

**具体步骤**

1. 接口：

```java
public interface ProductSearchProvider {
    String platformName();
    List<Product> search(String keyword, int page, int size);
    boolean isConfigured();
}
```

2. `PddService implements ProductSearchProvider`，把现有 `searchGoods` 变成 `search` 的实现，`platformName()` 返回“拼多多”。

3. `CommerceService` 改成依赖接口：

```java
private final List<ProductSearchProvider> providers;

// 构造时注入所有 provider，主搜索里遍历：
for (ProductSearchProvider provider : providers) {
    if (!provider.isConfigured()) continue;
    List<Product> results = provider.search(keyword, page, size);
    ...
}
```

4. 缓存放在 `CommerceService` 或 Provider 里，简单版：

```java
private static class CacheEntry {
    final List<Product> products;
    final long expireAt;
}

private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
private static final long CACHE_TTL_MS = 5 * 60 * 1000;

public List<Product> search(String keyword, int page, int size) {
    String key = keyword + "|" + page + "|" + size;
    CacheEntry hit = cache.get(key);
    if (hit != null && hit.expireAt > System.currentTimeMillis()) {
        return hit.products;
    }
    List<Product> result = doSearch(keyword, page, size);
    cache.put(key, new CacheEntry(result, System.currentTimeMillis() + CACHE_TTL_MS));
    return result;
}
```

5. 缓存只缓存成功且非空的结果；空结果可以缓存短一点（比如 1 分钟）。

**怎么验证**

- 同一关键词连续搜索两次，日志里 PDD HTTP 调用只出现一次。
- 京东/淘宝以后接入时，只新增一个 Provider 类，`CommerceService` 不用改主逻辑。

**做完长这样**：PDD 限额消耗明显下降，答辩可以讲“供应商可插拔”。

---

### B3. 搜索 total 和相关性过滤

**目标**

现在合并搜索的 total 是“外部本页数 + DB total”，分页不准确；PDD 返回的商品也可能跟关键词无关。

**改哪里**

- `backend/giftgpt-goods/src/main/java/com/giftgpt/goods/service/PddService.java`
- `backend/giftgpt-goods/src/main/java/com/giftgpt/goods/service/ProductService.java`
- `backend/giftgpt-goods/src/main/java/com/giftgpt/goods/service/CommerceService.java`

**具体步骤**

1. `PddService.parseResponse()` 尝试读真实 total：

```java
JsonNode totalNode = resp.get("total_count");
if (totalNode != null && totalNode.canConvertToLong()) {
    // 把 total 存到字段或返回对象里，供 ProductService 使用
}
```

PDD 不同接口版本字段名可能不一样，先打印一次完整响应确认是 `total_count` 还是 `total`。

2. 读不到真实 total 时，保留现在的估算，但注释写清楚是临时方案。

3. 相关性过滤放在 `CommerceService` 里，核心逻辑：

```java
private boolean keywordMatches(Product p, String keyword) {
    String name = p.getName() == null ? "" : p.getName().toLowerCase();
    String kw = keyword.toLowerCase();
    String[] parts = kw.split("[\\s,，、]+");
    for (String part : parts) {
        if (part.length() >= 2 && name.contains(part)) return true;
    }
    return false;
}
```

4. 对 PDD 实时结果：
   - 命中关键词 → 展示 + 缓存。
   - 没命中 → 只缓存不展示，避免首页全是“不相关但销量高”的商品。

**怎么验证**

- 搜索“耳机”，结果标题至少命中“耳机”或相关分词。
- 分页 total 在 PDD 返回真实值时正确；未返回时回退估算。

**做完长这样**：搜索和推荐的商品不会明显跑偏，分页不再乱跳。

---

### B4. 空结果引导 + 价格/去重兜底

**目标**

推荐结果为空时给引导；超过预算的商品过滤；同一个商品别出现两次。

**改哪里**

- `backend/giftgpt-recommendation/src/main/java/com/giftgpt/recommendation/service/RecommendationService.java`
- `frontend/giftgpt-web/src/app/recommend/page.tsx`
- `frontend/giftgpt-web/src/components/GiftCard.tsx`（可选）

**具体步骤**

1. 后端在 `matchAndSearch()` 组装完 `items` 后加后置处理：

```java
// 1. 过滤超预算
items.removeIf(item -> item.getPrice() != null
        && request.getBudget() != null
        && item.getPrice().compareTo(request.getBudget()) > 0);

// 2. 按 productId 去重（productId <= 0 的保留第一个）
Map<Long, RecommendItem> seen = new LinkedHashMap<>();
for (RecommendItem item : items) {
    Long key = item.getProductId() != null && item.getProductId() > 0
            ? item.getProductId() : (long) -System.identityHashCode(item);
    seen.putIfAbsent(key, item);
}
items = new ArrayList<>(seen.values());

// 3. 排序
items.sort(Comparator.comparing(RecommendItem::getScore, Comparator.nullsLast(Double::compareTo)).reversed());
```

2. 前端 `recommend/page.tsx` 结果区加空状态：

```tsx
{items.length === 0 && (
  <EmptyState
    title="没有找到匹配的礼物"
    action={<><button onClick={() => setBudget(budget + 200)}>放宽预算</button><Link href="/recipients">检查画像</Link></>}
  />
)}
```

3. 预算输入继续做前端校验：0 或负数直接提示，不发请求。

**怎么验证**

- 预算设很小（如 1 元），不崩，显示空状态和引导。
- 相同 `productId` 的结果只出现一次。
- 所有展示商品价格 ≤ 预算。

**做完长这样**：边界用例全部有合理反馈。


### B5. 推荐卡片展示推荐原因 + 简化权重

**目标**

现在卡片只显示一个“匹配 95%”和一句推荐理由，看不出分数怎么来的。把分数拆成“兴趣命中 / 价格贴近 / 销量热度”三块展示，评委一眼就能看懂“为什么推荐”。

**改哪里**

- `backend/giftgpt-recommendation/src/main/java/com/giftgpt/recommendation/dto/RecommendItem.java`
- `backend/giftgpt-recommendation/src/main/java/com/giftgpt/recommendation/service/RecommendationService.java`
- `frontend/giftgpt-web/src/components/GiftCard.tsx`
- `frontend/giftgpt-web/src/types/index.ts`

**具体步骤**

1. `RecommendItem` 加一个嵌套 DTO：

```java
@Data
public static class ScoreFactor {
    private String label;     // 比如：兴趣命中
    private Double weight;    // 权重：0.50 / 0.30 / 0.20
    private Double score;     // 单项得分：0-1
}

private List<ScoreFactor> scoreFactors;
```

2. A2 算分时同时填充：

```java
item.setScoreFactors(List.of(
    buildFactor("兴趣/关键词命中", 0.50, kw),
    buildFactor("价格贴近预算", 0.30, fit),
    buildFactor("销量热度", 0.20, sales)
));
```

3. 推荐理由跟最高分项挂钩，避免“理由”和“分数”打架：

```java
String topReason = kw >= fit && kw >= sales
        ? "命中TA的兴趣/关键词：" + String.join("、", gi.getTags())
        : fit >= sales
            ? "价格最贴近预算"
            : "同类中销量热度高";
```

已有 `reason` 时保留 AI 写的理由；没有时才用上面的自动理由。

4. KG 增强项不套用这个三因子模型，继续展示 `reasoningChain`，把 `scoreFactors` 留空或只放一条“兴趣标签命中”。

5. 前端 `types/index.ts` 加：

```ts
export interface ScoreFactor {
  label: string;
  weight: number;
  score: number;
}
```

并在 `RecommendItem` 里加 `scoreFactors?: ScoreFactor[];`。

6. `GiftCard.tsx` 在“匹配 92%”徽章下面加简化权重条：

```tsx
{scoreFactors && scoreFactors.length > 0 && (
  <div className="flex gap-1 mt-1">
    {scoreFactors.map(f => (
      <span key={f.label}
        className="text-[10px] px-1.5 py-0.5 rounded-full bg-gray-100 dark:bg-gray-800 text-gray-500"
        title={`权重 ${(f.weight * 100).toFixed(0)}%，得分 ${(f.score * 100).toFixed(0)}%`}>
        {f.label} {(f.score * 100).toFixed(0)}%
      </span>
    ))}
  </div>
)}
```

样式不用花，三个小标签即可。想要更直观，可以把文字换成三段式进度条：

```tsx
<div className="flex h-1.5 rounded-full overflow-hidden bg-gray-100 dark:bg-gray-800">
  <div style={{ width: `${f.score * f.weight * 100}%` }} className="bg-rose-400" />
  <div style={{ width: `${f.score * f.weight * 100}%` }} className="bg-amber-400" />
  <div style={{ width: `${f.score * f.weight * 100}%` }} className="bg-emerald-400" />
</div>
```

7. 总匹配度仍显示现有徽章，数字来自 `score`。

**怎么验证**

- 前端 `npm run lint` + `npx tsc --noEmit`。
- 推荐一次后，卡片同时出现：推荐理由、总分徽章、三个权重小标签。
- 三个标签的权重固定为 50/30/20；单项得分和总分一致。
- 没匹配到真实商品的兜底卡片显示固定总分，权重标签可隐藏。

**做完长这样**：评委问“为什么推荐这个”，可以直接指着卡片讲“兴趣命中 50%、价格 30%、销量 20%”，而且分数是稳定的。

---

## 4. C 档：有余力再补全闭环

### C1. 物流标记 + 订单状态机

**目标**

物流现在是模拟数据，先标注清楚，再把订单状态收敛到统一方法。

**改哪里**

- `backend/giftgpt-server/src/main/resources/schema.sql`
- `backend/giftgpt-order/src/main/java/com/giftgpt/order/entity/LogisticsEvent.java`
- `backend/giftgpt-order/src/main/java/com/giftgpt/order/service/OrderService.java`

**具体步骤**

1. `logistics_event` 加列：

```sql
ALTER TABLE logistics_event ADD COLUMN IF NOT EXISTS source VARCHAR(20) DEFAULT 'simulated';
```

2. `LogisticsEvent` 加 `private String source;`。

3. `insertMockLogisticsEvents()` 里每条事件设 `source = "simulated"`。

4. 状态值统一常量：

```java
public static final String STATUS_PENDING = "pending";
public static final String STATUS_ORDERED = "ordered";
public static final String STATUS_PACKAGED = "packaged";
public static final String STATUS_SHIPPED = "shipped";
public static final String STATUS_DELIVERED = "delivered";
```

5. 所有改状态的地方走 `changeOrderStatus(orderId, newStatus)`，方法里写日志和更新。

**怎么验证**

- 新订单物流接口返回 `source=simulated`。
- 状态流转只能走 service 方法。

**做完长这样**：评委问“物流是真的吗”时，界面和文档都明确说明，不虚。

---

### C2. 贺卡二维码/语音 + 收礼方扫码反馈

**目标**

现在贺卡只有文字，`qr_code_url` 和语音是空的。补上之后全链路闭环更有说服力。

**改哪里**

- `backend/giftgpt-ai/content_generator/router.py`（**先跟做多跳映射的同学确认，避免冲突**）
- `backend/giftgpt-ai/requirements.txt`
- `backend/giftgpt-order/src/main/java/com/giftgpt/order/service/OrderService.java`
- `backend/docker-compose.yml` 的 MinIO（已预留）

**具体步骤**

1. Python 加依赖 `qrcode[pil]>=7.4`，新增 `/api/v1/ai/greeting/qrcode`：
   - 入参：贺卡文本、收礼人、场景。
   - 生成二维码图片，上传 MinIO 或先写 `backend/giftgpt-ai/data/qrcodes/`。
   - 返回可访问 URL。

2. Java 生成贺卡后，若 Python 服务可用，再调二维码接口拿 URL，失败就 `qr_code_url=null`，不影响文字贺卡。

3. 语音先做简单版：调浏览器/前端录音，上传到 MinIO，保存 URL。语音合成（TTS）可以后补。

4. 收礼方反馈入口：
   - 给每个送礼记录生成一个短期随机 token（存表 `feedback_access_token`）。
   - 二维码内容指向 `/feedback/{token}`。
   - 这个页面只允许提交收礼方反馈，不要求登录，token 过期或已用即失效。

**怎么验证**

- 生成贺卡后能扫码打开收礼方反馈页。
- Python 服务挂了，文字贺卡仍然正常。

**做完长这样**：包装 → 贺卡 → 扫码 → 收礼方反馈的闭环完整了。

---

### C3. 日历定时提醒

**目标**

日历现在只有 CRUD，加个每天扫描的定时任务，让“提醒”真的发生。

**改哪里**

- `backend/giftgpt-server/src/main/resources/schema.sql`
- 新建 `backend/giftgpt-content/.../entity/Notification.java` + mapper
- `backend/giftgpt-content/src/main/java/com/giftgpt/content/service/ContentService.java`
- 新建 `backend/giftgpt-content/src/main/java/com/giftgpt/content/task/CalendarRemindTask.java`
- `frontend/giftgpt-web/src/lib/api.ts` + 页面轮询

**具体步骤**

1. 建通知表：

```sql
CREATE TABLE IF NOT EXISTS notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    calendar_event_id BIGINT,
    title VARCHAR(200),
    content VARCHAR(500),
    is_read TINYINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

2. 定时任务每天扫描未来 3 天的事件：

```java
@Scheduled(cron = "0 0 8 * * ?")  // 每天 8 点
public void scanAndNotify() {
    // 查 event_date 在 [today, today+3] 且 remind_before_days 命中的事件
    // 每个事件一条 notification，避免重复
}
```

3. 前端导航或首页加“提醒”小红点，每 30 秒轮询一次未读数；比赛演示也可以手动触发“立即检查”。

**怎么验证**

- 建一个 3 天后的日历事件，手动跑一次任务，通知表新增记录，前端能看到未读提醒。

**做完长这样**：日历功能从“记事本”变成“真的会提醒”。

---

### C4. 企业批量下单真实落库

**目标**

现在 `EnterpriseService.createBatchOrder()` 只返回字符串“批量下单已受理”，演示没有实际效果。改成真的创建送礼记录和订单。

**改哪里**

- `backend/giftgpt-enterprise/src/main/java/com/giftgpt/enterprise/service/EnterpriseService.java`
- 可能复用 `backend/giftgpt-order/src/main/java/com/giftgpt/order/service/OrderService.java`

**具体步骤**

1. 给 `EnterpriseService` 注入 `GiftRecordMapper` 和 `OrderMapper`（或调 `OrderService` 的公共方法）。

2. `createBatchOrder()` 流程：

```java
@Transactional
public Object createBatchOrder(BatchOrderRequest request) {
    Long userId = StpUtil.getLoginIdAsLong();
    Enterprise enterprise = enterpriseMapper.selectById(request.getEnterpriseId());
    if (enterprise == null || !enterprise.getUserId().equals(userId)) {
        throw new BusinessException(ResultCode.FORBIDDEN);
    }

    List<Long> orderIds = new ArrayList<>();
    for (BatchOrderRequest.EmployeeGift emp : request.getEmployees()) {
        // 1. 用 emp.recipientId 找/建收礼人，校验归属
        // 2. 创建 gift_record，状态 pending/ordered
        // 3. 创建 order，金额用 emp.budget
        orderIds.add(order.getId());
    }
    return Map.of("total", orderIds.size(), "orderIds", orderIds);
}
```

3. 员工列表先用 3-5 人的小批量演示，CSV 导入以后再加。

4. 前端企业页显示“已创建 N 个订单”，并跳转送礼记录页。

**怎么验证**

- 批量下单后，`gift_record` 和 `order` 表新增对应行，送礼记录页能看到每个员工订单。

**做完长这样**：B 端演示从“返回一句话”变成“能看见批量订单”。

---

### C5. AI 调用日志 / 成本小看板

**目标**

记录每次 Deepseek 调用的耗时、token、是否降级，答辩可展示，也能估算成本。

**改哪里**

- `backend/giftgpt-server/src/main/resources/schema.sql`
- `backend/giftgpt-common/src/main/java/com/giftgpt/common/ai/DeepseekClient.java`
- 可选：`frontend/giftgpt-web/src/app/profile/page.tsx` 或管理页

**具体步骤**

1. 建表：

```sql
CREATE TABLE IF NOT EXISTS ai_invocation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    scene VARCHAR(50),
    model VARCHAR(50),
    prompt_tokens INT,
    completion_tokens INT,
    latency_ms BIGINT,
    success TINYINT,
    fallback TINYINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

2. `DeepseekClient` 每次调用后写一条日志。为了让 common 模块能落库，可以先只打结构化日志，再由 `RecommendationService` 落表，避免 common 模块依赖 mapper。

3. 简单的 SQL 周报：

```sql
SELECT DATE(create_time) d, COUNT(*), SUM(fallback), AVG(latency_ms)
FROM ai_invocation_log
GROUP BY DATE(create_time)
ORDER BY d DESC;
```

**怎么验证**

- 跑几次推荐，表里有记录。
- 拔 key 跑一次，`fallback=1`。

**做完长这样**：答辩可以放一页“AI 调用可靠性/成本”数据。

---

## 5. 建议执行顺序

```
A3(边界页) ──► A4(越权) ──► A1(AI 统一) ──► A2(去随机) ──► B5(原因+权重) ──► B4(兜底) ──► B1(点击事件)
                                        └──────► A5(CI) ──► A6(配置)
B2(Provider+缓存) ──► B3(total/相关性)
C1/C3 可并行
C2 建议最后做，并先跟 Python 侧同步
C4 单独一个人做
```

按时间建议：

- **只剩 1 周**：只做 A 档里的 A3、A4、A1、A2，反复跑三套演示脚本。
- **剩 2-3 周**：补 A5、A6，再做 B5、B4、B1。
- **剩 1 个月以上**：B 档做完，再挑 C 档里 2-3 个最能出效果的（推荐 C1、C4，其次 C2、C3）。

---

## 6. 风险与注意事项

| 风险 | 怎么避免 |
|------|----------|
| 和 KG 多跳映射团队同时改 `RecommendationService` | 开工前 git status/pull + 群里同步；能拆 commit 就拆 |
| `mvn package` 在服务器运行时因 jar 锁失败 | 日常验证用 `mvn compile -q`；确需 jar 先停服务 |
| LLM 返回不稳定 | 所有 AI 调用必须有 fallback；排序用确定性分数兜底 |
| PDD 接口字段变化 | 改前打印一次真实响应；解析都用 `get/optText` 空值保护 |
| 比赛现场没网 | 提前准备一份本地商品数据 + 不带 AI 的兜底推荐路径 |
| 功能越做越多 | 每做完一个任务就问一句“这个对答辩有没有用”，没用就砍 |

---

## 7. 审阅记录

| 日期 | 审阅意见 | 结论（通过/修改/删除） |
|------|----------|------------------------|
|      |          |                        |
|      |          |                        |

> 你标完意见后我来改文档；确认没问题再按 A 档开始排任务。
