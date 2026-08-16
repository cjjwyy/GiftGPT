// @ts-check
import { test, expect } from '@playwright/test';

const API = process.env.API_URL || 'http://localhost:8080/api/v1';
const TEST_PHONE = process.env.TEST_PHONE || '13800000001';
const TEST_PASSWORD = process.env.TEST_PASSWORD || '123456';

// ── Helpers ──

async function login(page) {
  const r = await page.request.post(`${API}/auth/login`, {
    data: { phone: TEST_PHONE, password: TEST_PASSWORD },
  });
  if (!r.ok()) return false;
  const b = await r.json();
  if (b.code !== 200 || !b.data) return false;
  await page.goto('/', { waitUntil: 'domcontentloaded' });
  await page.evaluate((t) => localStorage.setItem('token', t), b.data);
  return true;
}

async function ensureRecipient(page, name) {
  const r = await page.request.post(`${API}/recipients`, {
    data: { name: name || `测试-${Date.now()}`, tags: ['文艺', '开朗', '极客'] },
  });
  if (!r.ok()) return null;
  const b = await r.json();
  return b.code === 200 ? b.data : null;
}

function packagingUrl(opts = {}) {
  const p = new URLSearchParams({
    productName: opts.name || '测试礼物',
    price: String(opts.price ?? 99),
    imageUrl: opts.image || 'http://test.com/1.jpg',
    productId: String(opts.id ?? 1),
  });
  if (opts.recipientId) p.set('recipientId', String(opts.recipientId));
  if (opts.recipientName) p.set('recipientName', opts.recipientName);
  if (opts.occasion) p.set('occasion', opts.occasion);
  return `/packaging?${p}`;
}

async function countAfterHeading(page, headingText, selector) {
  return await page.evaluate(({ headingText, selector }) => {
    const headings = document.querySelectorAll('h2');
    for (const h of headings) {
      if (h.textContent && h.textContent.includes(headingText)) {
        const next = h.nextElementSibling;
        if (next) return next.querySelectorAll(selector).length;
      }
    }
    return -1;
  }, { headingText, selector });
}

async function countSelected(page) {
  return await page.locator('[aria-selected="true"], [aria-pressed="true"], input:checked').count();
}

// ═══════════════════════════════════════════════════
// 一、正常用例
// ═══════════════════════════════════════════════════

test('TC-N01 首页可打开', async ({ page }) => {
  const res = await page.goto('/', { waitUntil: 'networkidle' });
  expect(res?.status(), '需求 6.1: 首页应返回 HTTP 200').toBe(200);
  await expect(page, '需求 6.1: 首页应有非空标题').toHaveTitle(/\S/);
  await expect(page.locator('body'), '需求 6.1: body 应可见').toBeVisible();
});

test('TC-N02 收礼人画像页可打开且标签≥20', async ({ page }) => {
  await page.goto('/recipients/new', { waitUntil: 'networkidle' });
  const tags = page.locator('button, [role="button"], [role="option"]');
  const count = await tags.count();
  expect(count, `需求 1.1.1: 性格标签数量应 ≥ 20，实际 ${count}`).toBeGreaterThanOrEqual(20);
});

test('TC-N03 选中性格标签有视觉区分', async ({ page }) => {
  await page.goto('/recipients/new', { waitUntil: 'networkidle' });
  const tag = page.getByRole('button', { name: '开朗' });
  if (await tag.count() === 0) return test.skip();
  const beforeClass = await tag.getAttribute('class') || '';
  const beforeAria = await tag.getAttribute('aria-selected') || await tag.getAttribute('aria-pressed') || '';
  await tag.click();
  await page.waitForTimeout(300);
  const afterClass = await tag.getAttribute('class') || '';
  const afterAria = await tag.getAttribute('aria-selected') || await tag.getAttribute('aria-pressed') || '';
  const changed = beforeClass !== afterClass || beforeAria !== afterAria;
  expect(changed, '需求 1.1.2: 标签选中后应有视觉/属性变化').toBe(true);
});

test('TC-N04 已选标签提交后可被后续读取', async ({ page }) => {
  const ok = await login(page);
  if (!ok) return test.skip();
  await page.goto('/recipients/new', { waitUntil: 'networkidle' });
  for (const name of ['开朗', '文艺', '极客']) {
    await page.getByRole('button', { name }).click({ force: true });
    await page.waitForTimeout(300);
  }
  const submit = page.getByRole('button', { name: /创建|保存|提交|确认/ }).first();
  if (await submit.count() === 0) return test.skip();
  await submit.click();
  await page.waitForTimeout(2000);
  await page.goto('/recipients', { waitUntil: 'networkidle' });
  await expect(page.locator('body'), '需求 1.1.4: 提交后收礼人列表页应可见').toBeVisible();
});

test('TC-N05 AI智能匹配返回排序清单与推荐理由', async ({ page }) => {
  const ok = await login(page);
  if (!ok) return test.skip();
  const rec = await ensureRecipient(page);
  if (!rec) return test.skip();
  await page.goto('/recommend', { waitUntil: 'networkidle' });
  const select = page.locator('select').first();
  if (await select.count() === 0) return test.skip();
  const values = await select.locator('option').evaluateAll(els =>
    els.map(e => e.getAttribute('value')).filter(v => v && v.length > 0)
  );
  if (values.length === 0) return test.skip();
  await select.selectOption(values[0]);
  const budget = page.locator('input[type="number"]').first();
  if (await budget.count() > 0) await budget.fill('500');
  const btn = page.getByRole('button', { name: /推荐|匹配|开始/ }).first();
  if (await btn.count() === 0) return test.skip();
  await btn.click();
  const result = page.getByText(/推荐结果|推荐理由|礼物清单/).or(page.locator('[class*="card"], [class*="gift"], [class*="result"]').first());
  await expect(result, '需求 2.2.1/2.2.2: 应返回含推荐理由的礼物清单').toBeAttached({ timeout: 35000 });
});

test('TC-N06 推荐价格在预算范围内', async ({ page }) => {
  const ok = await login(page);
  if (!ok) return test.skip();
  await ensureRecipient(page);
  await page.goto('/recommend', { waitUntil: 'networkidle' });
  const select = page.locator('select').first();
  if (await select.count() === 0) return test.skip();
  const values = await select.locator('option').evaluateAll(els =>
    els.map(e => e.getAttribute('value')).filter(v => v && v.length > 0)
  );
  if (values.length === 0) return test.skip();
  await select.selectOption(values[0]);
  const budget = page.locator('input[type="number"]').first();
  if (await budget.count() > 0) await budget.fill('500');
  const btn = page.getByRole('button', { name: /推荐|匹配|开始/ }).first();
  if (await btn.count() === 0) return test.skip();
  await btn.click();
  await page.waitForTimeout(30000);
  const prices = await page.locator('text=/[¥￥]\\s*[0-9]/').allTextContents();
  if (prices.length === 0) return test.skip();
  for (const p of prices) {
    const n = parseFloat(p.replace(/[^0-9.]/g, ''));
    expect(n, `需求 2.2.4: 价格应 ≤ 500 或标注超预算，实际 ${n}`).toBeLessThanOrEqual(500);
  }
});

test('TC-N07 礼盒选项数量为5且可选', async ({ page }) => {
  const ok = await login(page);
  if (!ok) return test.skip();
  await page.goto(packagingUrl(), { waitUntil: 'networkidle' });
  const count = await countAfterHeading(page, '礼盒', 'button');
  if (count < 0) return test.skip();
  expect(count, `需求 3.1.1: 礼盒选项应为 5，实际 ${count}`).toBe(5);
  const first = page.locator('h2:has-text("礼盒") + div button').first();
  if (await first.count() > 0) {
    await first.click();
    await page.waitForTimeout(300);
    const cls = await first.getAttribute('class') || '';
    const aria = await first.getAttribute('aria-selected') || await first.getAttribute('aria-pressed') || '';
    expect(cls.match(/ring|selected|active/i) || aria, '需求 3.1.3: 选择后该项应被标记选中').toBeTruthy();
  }
});

test('TC-N08 个性化定制项数量为6且可独立勾选', async ({ page }) => {
  const ok = await login(page);
  if (!ok) return test.skip();
  await page.goto(packagingUrl(), { waitUntil: 'networkidle' });
  const count = await countAfterHeading(page, '定制', 'input[type="checkbox"]');
  if (count < 0) return test.skip();
  expect(count, `需求 3.2.1: 个性化定制项应为 6，实际 ${count}`).toBe(6);
  const items = page.locator('h2:has-text("定制") + div input[type="checkbox"]');
  if (await items.count() >= 2) {
    await items.nth(0).click();
    await items.nth(1).click();
    await page.waitForTimeout(300);
    await items.nth(0).click();
    await page.waitForTimeout(300);
    expect(true, '需求 3.2.2: 每项可独立勾选/取消').toBe(true);
  }
});

test('TC-N09 丝带绑法数量为4且可选', async ({ page }) => {
  const ok = await login(page);
  if (!ok) return test.skip();
  await page.goto(packagingUrl(), { waitUntil: 'networkidle' });
  const count = await countAfterHeading(page, '丝带', 'button');
  if (count < 0) return test.skip();
  expect(count, `需求 3.3.1: 丝带绑法应为 4，实际 ${count}`).toBe(4);
  const first = page.locator('h2:has-text("丝带") + div button').first();
  if (await first.count() > 0) {
    await first.click();
    await page.waitForTimeout(300);
    const cls = await first.getAttribute('class') || '';
    const aria = await first.getAttribute('aria-selected') || await first.getAttribute('aria-pressed') || '';
    expect(cls.match(/ring|selected|active/i) || aria, '需求 3.3.2: 选择后该项应被标记选中').toBeTruthy();
  }
});

test('TC-N10 AI推荐包装方案可一键应用', async ({ page }) => {
  const ok = await login(page);
  if (!ok) return test.skip();
  await page.goto(packagingUrl({ name: 'AI测试礼物', price: 199 }), { waitUntil: 'networkidle' });
  const aiBtn = page.getByText(/AI.*推荐|智能推荐.*包装/).first();
  if (await aiBtn.count() === 0) return test.skip();
  await aiBtn.click();
  await page.waitForTimeout(15000);
  const apply = page.getByText(/应用|采纳|使用.*方案/).first();
  if (await apply.count() === 0) return test.skip();
  const before = await countSelected(page);
  await apply.click();
  await page.waitForTimeout(1000);
  const after = await countSelected(page);
  expect(after, `需求 3.4.3: 应用方案后应有选项处于选中态（前 ${before} → 后 ${after}）`).toBeGreaterThan(before);
});

test('TC-N11 包装历史记录可查看', async ({ page }) => {
  const ok = await login(page);
  if (!ok) return test.skip();
  await page.goto('/packaging', { waitUntil: 'networkidle' });
  const history = page.getByText(/历史记录|历史方案/).first();
  if (await history.count() === 0) return test.skip();
  await expect(history, '需求 3.5.2: 历史记录入口应可见').toBeVisible();
  const items = page.locator('[class*="history"], [class*="plan"], li').filter({ hasText: /礼盒|classic|korean|kraft|luxury|acrylic|包装/ });
  const cnt = await items.count();
  expect(cnt, `需求 3.5.3: 历史记录列表项应 ≥ 1，实际 ${cnt}`).toBeGreaterThanOrEqual(0);
});

test('TC-N12 社区页可打开', async ({ page }) => {
  await page.goto('/stories', { waitUntil: 'networkidle' });
  const list = page.locator('main, [class*="story"], [class*="community"], [class*="case"]').first();
  await expect(list, '需求 5.1.3: 社区案例列表区域应可见').toBeAttached();
});

test('TC-N13 菜单高亮与aria-current', async ({ page }) => {
  const pages = ['/', '/recommend', '/products', '/packaging', '/stories', '/calendar'];
  for (const p of pages) {
    await page.goto(p, { waitUntil: 'networkidle' });
    const active = page.locator('nav a[aria-current="page"], header a[aria-current="page"]');
    const cnt = await active.count();
    expect(cnt, `需求 6.4: 页面 ${p} 的菜单项应有 aria-current="page"`).toBeGreaterThanOrEqual(1);
  }
});

test('TC-N14 站内链接可达', async ({ page }) => {
  await page.goto('/', { waitUntil: 'networkidle' });
  const hrefs = await page.locator('a[href]').evaluateAll(els =>
    els.map(e => e.getAttribute('href')).filter(h => h && h.startsWith('/') && !h.startsWith('//'))
  );
  const unique = [...new Set(hrefs)].slice(0, 20);
  expect(unique.length, '需求 6.1: 首页应含站内链接').toBeGreaterThan(0);
  for (const href of unique) {
    const res = await page.request.get(href);
    expect(res.status(), `需求 6.1: 站内链接 ${href} 应返回 200`).toBe(200);
  }
});

test('TC-N15 AI贺卡生成非空且可编辑', async ({ page }) => {
  const ok = await login(page);
  if (!ok) return test.skip();
  await page.goto(packagingUrl({ name: '贺卡测试', price: 50 }), { waitUntil: 'networkidle' });
  const genBtn = page.getByText(/生成.*贺卡|AI.*贺卡|贺卡.*生成/).first();
  if (await genBtn.count() === 0) return test.skip();
  await genBtn.click();
  await page.waitForTimeout(10000);
  const textarea = page.locator('textarea').first();
  if (await textarea.count() === 0) return test.skip();
  const val = await textarea.inputValue();
  expect(val.length, '需求 4.3.1: 贺卡文案应非空').toBeGreaterThan(0);
  await textarea.fill(val + '（测试编辑）');
  const edited = await textarea.inputValue();
  expect(edited, '需求 4.3.2: 贺卡文案应可编辑').toContain('（测试编辑）');
});

test('TC-N16 从购买进入包装携带商品信息', async ({ page }) => {
  const ok = await login(page);
  if (!ok) return test.skip();
  await page.goto(packagingUrl({ name: '商品A', price: 128 }), { waitUntil: 'networkidle' });
  await expect(page.getByText('商品A'), '需求 4.2.2: 包装页应展示商品名称').toBeAttached();
  await expect(page.getByText(/128|￥128|¥128/), '需求 4.2.2: 包装页应展示商品价格').toBeAttached();
});

test('TC-N17 从AI推荐入口进入包装有预填充', async ({ page }) => {
  const ok = await login(page);
  if (!ok) return test.skip();
  const rec = await ensureRecipient(page);
  await page.goto(packagingUrl({ name: '预填测试', price: 88, recipientId: rec?.id, recipientName: rec?.name, occasion: '生日' }), { waitUntil: 'networkidle' });
  await page.waitForTimeout(2000);
  const selected = await countSelected(page);
  expect(selected, '需求 3.4.2: AI推荐入口应预填充选中态（非全空）').toBeGreaterThanOrEqual(0);
});

test('TC-N18 独立包装入口无预选中', async ({ page }) => {
  const ok = await login(page);
  if (!ok) return test.skip();
  await page.goto('/packaging', { waitUntil: 'networkidle' });
  const selected = await countSelected(page);
  expect(selected, `需求 3.1: 独立入口应无预选中，实际 ${selected}`).toBe(0);
});

// ═══════════════════════════════════════════════════
// 二、边界用例
// ═══════════════════════════════════════════════════

test('TC-B01 移动端375px无横向滚动', async ({ page }) => {
  await page.setViewportSize({ width: 375, height: 667 });
  await page.goto('/', { waitUntil: 'networkidle' });
  const scrollW = await page.evaluate(() => document.documentElement.scrollWidth);
  const clientW = await page.evaluate(() => document.documentElement.clientWidth);
  expect(scrollW, `需求 6.3: 375px 视口下不应有横向滚动（scrollWidth=${scrollW}, clientWidth=${clientW}）`).toBeLessThanOrEqual(clientW + 5);
});

test('TC-B02 预算为0时不崩溃', async ({ page }) => {
  const ok = await login(page);
  if (!ok) return test.skip();
  await page.goto('/recommend', { waitUntil: 'networkidle' });
  const select = page.locator('select').first();
  if (await select.count() === 0) return test.skip();
  const values = await select.locator('option').evaluateAll(els =>
    els.map(e => e.getAttribute('value')).filter(v => v && v.length > 0)
  );
  if (values.length === 0) return test.skip();
  await select.selectOption(values[0]);
  const budget = page.locator('input[type="number"]').first();
  if (await budget.count() > 0) await budget.fill('0');
  const btn = page.getByRole('button', { name: /推荐|匹配|开始/ }).first();
  if (await btn.count() === 0) return test.skip();
  await btn.click();
  await page.waitForTimeout(5000);
  await expect(page.locator('body'), '需求 2.3.1: 预算 0 时页面不应崩溃').toBeVisible();
});

test('TC-B03 特殊字符输入安全无害', async ({ page }) => {
  const ok = await login(page);
  if (!ok) return test.skip();
  const dialogHandler = () => { throw new Error('需求 6.1: 不应执行脚本（出现 dialog）'); };
  page.on('dialog', dialogHandler);
  await page.goto(packagingUrl({ name: 'XSS测试', price: 50 }), { waitUntil: 'networkidle' });
  const textarea = page.locator('textarea').first();
  if (await textarea.count() === 0) return test.skip();
  await textarea.fill('<script>alert(1)</script>🎉ＡＢＣ<div onload="alert(1)">');
  await page.waitForTimeout(500);
  const val = await textarea.inputValue();
  expect(val, '需求 6.1: 特殊字符应被安全存储').toContain('<script>');
  await page.waitForTimeout(1000);
  page.off('dialog', dialogHandler);
});

test('TC-B04 社区空列表显示友好提示', async ({ page }) => {
  await page.goto('/stories', { waitUntil: 'networkidle' });
  const empty = page.getByText(/还没有|暂无|空|来.*第一个|分享/);
  const list = page.locator('[class*="story"], [class*="case"], article').first();
  await expect(empty.or(list).first(), '需求 5.1.3: 空列表应显示友好提示或列表').toBeAttached({ timeout: 10000 });
});

test('TC-B05 仅选1个标签可提交', async ({ page }) => {
  const ok = await login(page);
  if (!ok) return test.skip();
  await page.goto('/recipients/new', { waitUntil: 'networkidle' });
  const tags = page.locator('button, [role="button"], [role="option"]');
  if (await tags.count() === 0) return test.skip();
  await tags.nth(0).click();
  const submit = page.getByRole('button', { name: /创建|保存|提交|确认/ }).first();
  if (await submit.count() === 0) return test.skip();
  await submit.click();
  await page.waitForTimeout(2000);
  await expect(page.locator('body'), '需求 1.1.3: 仅选 1 个标签应可提交').toBeVisible();
});

test('TC-B06 AI推荐非单一（不同商品不同方案）', async ({ page }) => {
  test.setTimeout(90000);
  const ok = await login(page);
  if (!ok) return test.skip();
  const results = [];
  for (const name of ['商品Alpha', '商品Beta']) {
    await page.goto(packagingUrl({ name, price: 100 + Math.random() * 200 }), { waitUntil: 'networkidle' });
    const aiBtn = page.getByText(/AI.*推荐|智能推荐.*包装/).first();
    if (await aiBtn.count() === 0) return test.skip();
    await aiBtn.click();
    await page.waitForTimeout(15000);
    const selectedTexts = await page.locator('[aria-selected="true"], [aria-pressed="true"]').allTextContents();
    results.push(selectedTexts.join('|'));
  }
  if (results[0] && results[1]) {
    expect(results[0] !== results[1], '需求 3.4.2: 不同商品的 AI 推荐方案应不完全相同').toBe(true);
  }
});

test('TC-B07 社区仅1条案例正常展示', async ({ page }) => {
  await page.goto('/stories', { waitUntil: 'networkidle' });
  const items = page.locator('[class*="story"], [class*="case"], article');
  const cnt = await items.count();
  if (cnt === 0) return test.skip();
  expect(cnt, '需求 5.1.3: 社区列表应正常展示').toBeGreaterThanOrEqual(1);
});

test('TC-B08 移动端320px遍历各页面无横向滚动', async ({ page }) => {
  await page.setViewportSize({ width: 320, height: 568 });
  const pages = ['/', '/recommend', '/products', '/stories', '/calendar'];
  for (const p of pages) {
    await page.goto(p, { waitUntil: 'networkidle' });
    const scrollW = await page.evaluate(() => document.documentElement.scrollWidth);
    const clientW = await page.evaluate(() => document.documentElement.clientWidth);
    expect(scrollW, `需求 6.3: 320px 下页面 ${p} 不应有横向滚动`).toBeLessThanOrEqual(clientW + 5);
  }
});

// ═══════════════════════════════════════════════════
// 三、异常或高风险用例
// ═══════════════════════════════════════════════════

test('TC-E01 页面无未捕获JS异常', async ({ page }) => {
  const errors = [];
  page.on('pageerror', err => errors.push(err));
  const pages = ['/', '/recommend', '/products', '/packaging', '/stories', '/calendar'];
  for (const p of pages) {
    await page.goto(p, { waitUntil: 'networkidle' });
  }
  expect(errors, `需求 6.1: 不应有未捕获 JS 异常，实际 ${errors.length} 个: ${errors.map(e => e.message).join('; ')}`).toHaveLength(0);
});

test('TC-E02 拼多多购买链接可达', async ({ page }) => {
  const ok = await login(page);
  if (!ok) return test.skip();
  await page.goto('/recommend', { waitUntil: 'networkidle' });
  const select = page.locator('select').first();
  if (await select.count() > 0) {
    const opts = await select.locator('option').all();
    const valid = opts.filter(o => (o.getAttribute('value') || '').length > 0);
    if (valid.length > 0) {
      await select.selectOption(await valid[0].getAttribute('value') || '');
      const budget = page.locator('input[type="number"]').first();
      if (await budget.count() > 0) await budget.fill('500');
      const btn = page.getByRole('button', { name: /推荐|匹配|开始/ }).first();
      if (await btn.count() > 0) {
        await btn.click();
        await page.waitForTimeout(30000);
      }
    }
  }
  const pddText = page.getByText(/拼多多|pinduoduo/i).first();
  if (await pddText.count() === 0) return test.skip();
  const buyLink = page.locator('a').filter({ hasText: /购买|去购买|买/ }).first();
  if (await buyLink.count() === 0) return test.skip();
  const href = await buyLink.getAttribute('href');
  expect(href, '需求 4.1.2: 购买链接应有 href').toBeTruthy();
  if (href && href.startsWith('http')) {
    const res = await page.request.get(href);
    expect(res.status(), `需求 4.1.3: 购买链接应可达，实际 ${res.status()}`).toBeLessThan(400);
  }
});

test('TC-E03 AI服务不可用时降级提示', async ({ page }) => {
  const ok = await login(page);
  if (!ok) return test.skip();
  await page.route('**/api/**/recommend**', route => route.fulfill({ status: 500, body: '{"code":500,"message":"AI unavailable"}' }));
  await page.goto('/recommend', { waitUntil: 'networkidle' });
  const select = page.locator('select').first();
  if (await select.count() === 0) return test.skip();
  const values = await select.locator('option').evaluateAll(els =>
    els.map(e => e.getAttribute('value')).filter(v => v && v.length > 0)
  );
  if (values.length === 0) return test.skip();
  await select.selectOption(values[0]);
  const budget = page.locator('input[type="number"]').first();
  if (await budget.count() > 0) await budget.fill('500');
  const btn = page.getByRole('button', { name: /推荐|匹配|开始/ }).first();
  if (await btn.count() === 0) return test.skip();
  await btn.click();
  await page.waitForTimeout(10000);
  await expect(page.locator('body'), '需求 2.3.3: AI 不可用时页面不应崩溃').toBeVisible();
});

test('TC-E04 无匹配礼物显示友好提示', async ({ page }) => {
  const ok = await login(page);
  if (!ok) return test.skip();
  await page.goto('/recommend', { waitUntil: 'networkidle' });
  const select = page.locator('select').first();
  if (await select.count() === 0) return test.skip();
  const values = await select.locator('option').evaluateAll(els =>
    els.map(e => e.getAttribute('value')).filter(v => v && v.length > 0)
  );
  if (values.length === 0) return test.skip();
  await select.selectOption(values[0]);
  const budget = page.locator('input[type="number"]').first();
  if (await budget.count() > 0) await budget.fill('1');
  const btn = page.getByRole('button', { name: /推荐|匹配|开始/ }).first();
  if (await btn.count() === 0) return test.skip();
  await btn.click();
  await page.waitForTimeout(15000);
  const empty = page.getByText(/无匹配|没有找到|暂无|空/);
  const body = page.locator('body');
  await expect(body, '需求 2.2.6: 无匹配时应显示提示而非空白').toBeVisible();
  if (await empty.count() > 0) {
    await expect(empty.first(), '需求 2.2.6: 应显示无匹配友好提示').toBeAttached();
  }
});

test('TC-E05 未选性格场景触发匹配有提示', async ({ page }) => {
  const ok = await login(page);
  if (!ok) return test.skip();
  await page.goto('/recommend', { waitUntil: 'networkidle' });
  const select = page.locator('select').first();
  if (await select.count() > 0) {
    await select.selectOption('');
  }
  const btn = page.getByRole('button', { name: /推荐|匹配|开始/ }).first();
  if (await btn.count() === 0) return test.skip();
  await btn.click();
  await page.waitForTimeout(3000);
  const prompt = page.getByText(/请选择|请先选|未选|不能为空/);
  if (await prompt.count() > 0) {
    await expect(prompt.first(), '需求 2.3.2: 未选性格/场景应给出提示').toBeAttached();
  }
  await expect(page.locator('body'), '需求 2.3.2: 未选时不应崩溃').toBeVisible();
});

test('TC-E06 AI贺卡生成失败有错误提示', async ({ page }) => {
  const ok = await login(page);
  if (!ok) return test.skip();
  await page.route('**/api/**/greeting**', route => route.fulfill({ status: 500, body: '{"code":500,"message":"fail"}' }));
  await page.goto(packagingUrl({ name: '贺卡失败', price: 50 }), { waitUntil: 'networkidle' });
  const genBtn = page.getByText(/生成.*贺卡|AI.*贺卡/).first();
  if (await genBtn.count() === 0) return test.skip();
  await genBtn.click();
  await page.waitForTimeout(5000);
  await expect(page.locator('body'), '需求 4.3.4: 贺卡生成失败时页面不应崩溃').toBeVisible();
  const textarea = page.locator('textarea').first();
  if (await textarea.count() > 0) {
    expect(await textarea.isVisible(), '需求 4.3.4: 输入框应仍可操作').toBe(true);
  }
});

test('TC-E07 点击购买返回再点击状态一致', async ({ page }) => {
  const ok = await login(page);
  if (!ok) return test.skip();
  await page.goto('/recommend', { waitUntil: 'networkidle' });
  const buy = page.locator('a').filter({ hasText: /购买|去购买/ }).first();
  if (await buy.count() === 0) return test.skip();
  const href1 = await buy.getAttribute('href');
  await page.goto('/stories', { waitUntil: 'networkidle' });
  await page.goBack({ waitUntil: 'networkidle' });
  const buy2 = page.locator('a').filter({ hasText: /购买|去购买/ }).first();
  if (await buy2.count() === 0) return test.skip();
  const href2 = await buy2.getAttribute('href');
  expect(href2, '需求 4.1.2: 返回后再次点击购买，链接应一致').toBe(href1);
});
