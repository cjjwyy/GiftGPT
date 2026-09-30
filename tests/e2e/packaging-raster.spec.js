const { test, expect } = require('@playwright/test');
const fs = require('node:fs/promises');

const boxes = ['classic', 'korean', 'kraft', 'luxury', 'acrylic'];
const boxNames = ['经典缎面礼盒', '韩式极简礼盒', '牛皮纸自然风', '轻奢烫金礼盒', '透明亚克力盒'];
const addonIds = ['ribbon_text', 'greeting_card', 'dried_flower', 'polaroid', 'scent', 'band_wrap'];
const addonNames = ['礼带烫金字', '手写贺卡', '干花装饰', '拍立得照片夹', '香薰加香', '定制腰封'];
const plan = {
  id: 1, theme: 'kraft', wrappingStyle: 'furoshiki', productName: '历史包装测试',
  customizationsJson: JSON.stringify(['greeting_card', 'scent']), customText: '生日快乐', scent: '白茶',
};

test.beforeEach(async ({ page }) => {
  // No real accounts, AI calls, database writes or production traffic in these UI tests.
  await page.route('**/api/v1/**', async route => {
    const path = new URL(route.request().url()).pathname;
    let data;
    if (path.endsWith('/packaging/themes')) data = boxes.map(id => ({ id, price: 20 }));
    else if (path.endsWith('/packaging/addon-prices')) data = Object.fromEntries(addonIds.map(id => [id, 5]));
    else if (path.endsWith('/packaging/list')) data = { records: [plan] };
    else if (path.endsWith('/packaging/save')) data = { id: 2, version: 1, giftRecordId: 123 };
    else throw new Error(`Unexpected API call: ${path}`);
    await route.fulfill({ json: { code: 200, message: 'ok', data } });
  });
});

async function start(page) {
  await page.goto('/packaging?productName=' + encodeURIComponent('送给你的生日礼物'));
  await expect(page.getByRole('button', { name: /^经典缎面礼盒/ })).toContainText('¥20.00', { timeout: 15000 });
}

test('all 15 choices use loadable JPEGs; preview follows box and wrapping selections', async ({ page }) => {
  await start(page);
  const preview = page.getByRole('region', { name: '包装搭配方案' });
  await expect(preview.getByRole('button', { name: '导出方案 PNG' })).toBeDisabled();
  const images = page.locator('img[src^="/packaging/"]');
  await expect(images).toHaveCount(15);
  for (const img of await images.all()) {
    await img.scrollIntoViewIfNeeded();
    await expect(img).toHaveAttribute('src', /\.jpg$/);
    await expect.poll(() => img.evaluate(el => el.complete && el.naturalWidth > 0)).toBeTruthy();
  }
  for (let i = 0; i < boxes.length; i++) {
    await page.getByRole('button', { name: new RegExp(boxNames[i]) }).click();
    await expect(preview.getByRole('img', { name: `${boxNames[i]}款式参考` })).toHaveAttribute('src', `/packaging/box-${boxes[i]}.jpg`);
  }
  for (const [name, file] of [['经典交叉', 'cross'], ['单侧斜绑', 'side'], ['双层蝴蝶结', 'double-bow'], ['日式风吕敷', 'furoshiki']]) {
    await page.getByRole('button', { name: new RegExp(`^${name}`) }).click();
    await expect(preview.getByRole('img', { name: `${name}绑法参考` })).toHaveAttribute('src', `/packaging/ribbon-${file}.jpg`);
  }
  await expect(preview.locator('svg')).toHaveCount(0);
  await page.getByRole('button', { name: /轻奢烫金礼盒/ }).click();
  await page.getByRole('button', { name: /^双层蝴蝶结/ }).click();
  await page.evaluate(() => window.scrollTo(0, 0));
  await page.screenshot({ path: test.info().outputPath('packaging-desktop.png'), fullPage: true });
});

test('PNG export includes selections and retains save payload', async ({ page }) => {
  await start(page);
  await page.getByRole('button', { name: /透明亚克力盒/ }).click();
  await page.getByRole('button', { name: /单侧斜绑/ }).click();
  for (const name of addonNames) await page.getByRole('checkbox', { name, exact: true }).check();
  await page.getByPlaceholder('烫金文字（最多10字）').fill('最好的你');
  await page.getByPlaceholder('贺卡文案（50字以内）').fill('愿每一个平凡的日子，都有值得珍藏的小美好。\n生日快乐！');
  await page.locator('select').filter({ has: page.locator('option[value="银色"]') }).selectOption('银色');
  await page.locator('select').filter({ has: page.locator('option[value="白茶"]') }).selectOption('白茶');
  const preview = page.getByRole('region', { name: '包装搭配方案' });
  await expect(preview).toContainText('烫字颜色：银色');
  await expect(preview).toContainText('香型：白茶');
  const downloaded = page.waitForEvent('download');
  await preview.getByRole('button', { name: '导出方案 PNG' }).click();
  const download = await downloaded;
  expect(download.suggestedFilename()).toBe('gift-packaging.png');
  const target = test.info().outputPath('gift-packaging.png');
  await download.saveAs(target);
  const bytes = await fs.readFile(target);
  expect(bytes.subarray(0, 8).toString('hex')).toBe('89504e470d0a1a0a');
  expect(bytes.readUInt32BE(16)).toBe(1200);
  expect(bytes.length).toBeGreaterThan(100000);
  const savedRequest = page.waitForRequest(req => req.url().endsWith('/packaging/save'));
  await page.getByRole('button', { name: '确认包装方案', exact: true }).click();
  expect((await savedRequest).postDataJSON()).toMatchObject({
    packagingType: 'acrylic', wrappingStyle: 'side', ribbonColor: '银色', ribbonText: '最好的你', scent: '白茶',
    customizations: addonIds,
  });
  await expect(page.getByRole('button', { name: '重新保存', exact: true })).toBeVisible();
});

test('saved plan shows the same raster assets and actual customization text', async ({ page }) => {
  await page.goto('/packaging');
  await page.getByRole('button', { name: /历史包装测试/ }).click();
  const preview = page.getByRole('region', { name: '包装搭配方案' });
  await expect(preview.getByRole('img', { name: '牛皮纸自然风款式参考' })).toBeVisible();
  await expect(preview.getByRole('img', { name: '日式风吕敷绑法参考' })).toBeVisible();
  await expect(preview).toContainText('贺卡内容：生日快乐');
  await expect(preview).toContainText('香型：白茶');
  await expect(page.getByRole('checkbox', { name: '手写贺卡', exact: true })).toBeDisabled();
});

test('save works without randomUUID and reuses its request key on retry', async ({ page }) => {
  await page.addInitScript(() => {
    Object.defineProperty(crypto, 'randomUUID', { value: undefined, configurable: true });
  });
  await start(page);
  await page.getByRole('button', { name: /经典缎面礼盒/ }).click();
  const firstRequest = page.waitForRequest(req => req.url().endsWith('/packaging/save'));
  await page.getByRole('button', { name: '确认包装方案', exact: true }).click();
  const key = (await firstRequest).postDataJSON().requestKey;
  expect(key).toMatch(/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/);
  const retryRequest = page.waitForRequest(req => req.url().endsWith('/packaging/save'));
  await page.getByRole('button', { name: '重新保存', exact: true }).click();
  expect((await retryRequest).postDataJSON().requestKey).toBe(key);
});

test('request key generation failure releases the save lock for retry', async ({ page }) => {
  await start(page);
  await page.getByRole('button', { name: /经典缎面礼盒/ }).click();
  await page.evaluate(() => {
    const original = crypto.getRandomValues.bind(crypto);
    let failOnce = true;
    Object.defineProperty(crypto, 'randomUUID', { value: undefined, configurable: true });
    Object.defineProperty(crypto, 'getRandomValues', {
      configurable: true,
      value(array) {
        if (failOnce) {
          failOnce = false;
          throw new Error('测试随机数生成失败');
        }
        return original(array);
      },
    });
  });
  const button = page.getByRole('button', { name: '确认包装方案', exact: true });
  await button.click();
  await expect(page.getByText('测试随机数生成失败', { exact: true })).toBeVisible();
  await expect(button).toBeEnabled();
  const savedRequest = page.waitForRequest(req => req.url().endsWith('/packaging/save'));
  await button.click();
  expect((await savedRequest).postDataJSON().requestKey).toBeTruthy();
  await expect(page.getByRole('button', { name: '重新保存', exact: true })).toBeVisible();
});

test('mobile and dark mode do not overflow after selecting all extras', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await start(page);
  await page.getByRole('button', { name: /经典缎面礼盒/ }).click();
  for (const name of addonNames) await page.getByRole('checkbox', { name, exact: true }).check();
  await page.getByPlaceholder('贺卡文案（50字以内）').fill('祝你开心快乐，愿每一天都充满惊喜！');
  expect((await page.getByPlaceholder('贺卡文案（50字以内）').boundingBox()).width).toBeGreaterThan(280);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBeTruthy();
  await page.evaluate(() => window.scrollTo(0, 0));
  await page.screenshot({ path: test.info().outputPath('packaging-mobile.png'), fullPage: true, animations: 'disabled' });
  await page.evaluate(() => document.documentElement.classList.add('dark'));
  await page.screenshot({ path: test.info().outputPath('packaging-dark.png'), fullPage: true, animations: 'disabled' });
});

test('a missing raster asset produces a retryable export error', async ({ page }) => {
  await page.route('**/packaging/box-classic.jpg', route => route.abort());
  await start(page);
  await page.getByRole('button', { name: /经典缎面礼盒/ }).click();
  const button = page.getByRole('button', { name: '导出方案 PNG' });
  await button.click();
  await expect(page.getByText('图片加载失败，请稍后重试', { exact: true })).toBeVisible();
  await expect(button).toBeEnabled();
});
