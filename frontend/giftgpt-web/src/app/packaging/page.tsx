'use client';

import { useState, useEffect, useRef } from 'react';
import { useSearchParams } from 'next/navigation';
import { Suspense } from 'react';
import { packagingApi, greetingApi } from '@/lib/api';
import { Loading } from '@/components/Loading';
import { Sparkles, Gift, History, ArrowLeft } from 'lucide-react';
import { toast } from 'react-hot-toast';
import Link from 'next/link';
import Image from 'next/image';
import PackagingPreview from '@/components/PackagingPreview';
import { GIFT_BOXES, CUSTOMIZATIONS, RIBBON_STYLES } from '@/lib/packagingCatalog';

const SCENTS = ['玫瑰', '白茶', '雪松'];


const BOX_MAP = Object.fromEntries(GIFT_BOXES.map(b => [b.id, b]));
const RIBBON_MAP = Object.fromEntries(RIBBON_STYLES.map(r => [r.id, r]));

function PackagingContent() {
  const requestKey = useRef('');
  const saveBusy = useRef(false);
  const [BOX_PRICES, setBoxPrices] = useState<Record<string, number>>({});
  const [CUSTOMIZATION_PRICES, setAddonPrices] = useState<Record<string, number>>({});
  const [pricesReady, setPricesReady] = useState(false);
  const searchParams = useSearchParams();
  const productName = searchParams.get('productName') || '';
  const imageUrl = searchParams.get('imageUrl') || '';
  const productId = searchParams.get('productId') || '';
  const productPrice = Number(searchParams.get('price') || 0);
  const recipientId = searchParams.get('recipientId') || '';
  const recipientName = searchParams.get('recipientName') || '';
  const occasion = searchParams.get('occasion') || '';

  const hasProduct = !!productName;
  const hasRecipient = !!recipientId;

  const [selectedBox, setSelectedBox] = useState('');
  const [customs, setCustoms] = useState<Set<string>>(new Set());
  const [ribbonText, setRibbonText] = useState('');
  const [ribbonColor, setRibbonColor] = useState('金色');
  const [cardText, setCardText] = useState('');
  const [scent, setScent] = useState('');

  const [ribbonStyle, setRibbonStyle] = useState('');
  const [aiLoading, setAiLoading] = useState(false);
  const [aiGreetingLoading, setAiGreetingLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  const [history, setHistory] = useState<any[]>([]);
  const [viewingPlan, setViewingPlan] = useState<any>(null);
  const [savedPlan, setSavedPlan] = useState<any>(null);

  const readOnly = !hasProduct || !!viewingPlan;

  useEffect(() => {
    Promise.all([packagingApi.themes(), packagingApi.addonPrices()]).then(([themes, addons]) => {
      setBoxPrices(Object.fromEntries(themes.map(t => [t.id, Number(t.price)])));
      setAddonPrices(addons); setPricesReady(true);
    }).catch(() => toast.error('价格配置加载失败，请刷新后重试'));
    if (!hasProduct) {
      packagingApi.list(1, 20).then(d => setHistory(d.records || []))
        .catch((err: any) => toast.error(err?.message || '加载包装历史失败'));
    }
  }, [hasProduct]);

  const toggleCustom = (id: string) => {
    if (readOnly) return;
    setCustoms(prev => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id); else next.add(id);
      return next;
    });
  };

  const aiRecommend = async () => {
    setAiLoading(true);
    try {
      const res = await packagingApi.aiRecommend({
        productName,
        productPrice: productPrice > 0 ? productPrice : undefined,
      });
      setSelectedBox('');
      setCustoms(new Set());
      setRibbonText('');
      setRibbonColor('金色');
      setCardText('');
      setScent('');
      setRibbonStyle('');
      const newCustoms = new Set<string>();
      if (res.packagingType) setSelectedBox(res.packagingType);
      if (res.ribbonText) { setRibbonText(res.ribbonText); newCustoms.add('ribbon_text'); }
      if (res.ribbonColor) setRibbonColor(res.ribbonColor);
      if (res.scent) { setScent(res.scent); newCustoms.add('scent'); }
      if (res.wrappingStyle) setRibbonStyle(res.wrappingStyle);
      setCustoms(newCustoms);
      toast.success(res.aiGenerated ? 'AI 包装推荐已完成' : 'AI 暂不可用，已使用默认包装方案');
    } catch (e: any) { toast.error(e?.message || 'AI推荐失败'); }
    setAiLoading(false);
  };

  const onAiGreeting = async () => {
    setAiGreetingLoading(true);
    try {
      const res = await greetingApi.generate({
        recipientName: recipientName || '朋友',
        relation: '',
        occasion: occasion || '生日',
        senderName: '我',
      });
      setCardText((res.content || '').slice(0, 50));
      toast.success(res.aiGenerated ? 'AI 贺卡已生成（由 AI 生成）' : 'AI 服务不可用，已用默认文案');
    } catch (e: any) {
      toast.error(e?.message || '生成失败');
    }
    setAiGreetingLoading(false);
  };

  const onSave = async () => {
    if (saveBusy.current || !pricesReady) return;
    if (!selectedBox) { toast.error('请选择礼盒'); return; }
    saveBusy.current = true;
    if (!requestKey.current) requestKey.current = crypto.randomUUID();
    setSaving(true);
    try {
      const saved = await packagingApi.save({
        planId: savedPlan?.id, version: savedPlan?.version, requestKey: requestKey.current,
        productName, productImageUrl: imageUrl,
        productPrice: productPrice > 0 ? productPrice : undefined,
        productId: productId ? Number(productId) : undefined,
        packagingType: selectedBox,
        ribbonText: customs.has('ribbon_text') ? ribbonText : undefined,
        ribbonColor: customs.has('ribbon_text') ? ribbonColor : undefined,
        scent: customs.has('scent') ? scent : undefined,
        customText: customs.has('greeting_card') ? cardText : undefined,
        customizations: Array.from(customs),
        wrappingStyle: ribbonStyle,
        recipientId: recipientId ? Number(recipientId) : undefined,
        occasion: occasion || undefined,
      });
      setSavedPlan(saved);
      toast.success('包装方案已保存');
    } catch (e: any) { toast.error(e?.message || '保存失败'); }
    setSaving(false);
    saveBusy.current = false;
  };

  const viewPlan = (plan: any) => {
    setViewingPlan(plan);
    setSelectedBox(plan.theme || '');
    setRibbonText(plan.ribbonText || '');
    setRibbonColor(plan.ribbonColor || '金色');
    setCardText(plan.customText || '');
    setScent(plan.scent || '');
    setRibbonStyle(plan.wrappingStyle || '');
    const c = new Set<string>();
    if (plan.ribbonText) c.add('ribbon_text');
    if (plan.customText) c.add('greeting_card');
    if (plan.scent) c.add('scent');
    try { setCustoms(plan.customizationsJson ? new Set<string>(JSON.parse(plan.customizationsJson)) : c); }
    catch { setCustoms(c); }
  };

  const backToBrowse = () => {
    setViewingPlan(null);
    setSelectedBox('');
    setCustoms(new Set());
    setRibbonText('');
    setRibbonColor('金色');
    setCardText('');
    setScent('');
    setRibbonStyle('');
  };

  const dispProductName = viewingPlan?.productName || productName;
  const dispImageUrl = viewingPlan?.productImageUrl || imageUrl;
  const packagingTotal = (BOX_PRICES[selectedBox] || 0)
    + Array.from(customs).reduce((sum, item) => sum + (CUSTOMIZATION_PRICES[item] || 0), 0);

  return (
    <div className="max-w-5xl mx-auto px-4 py-10">
      <h1 className="text-2xl font-bold text-gray-900 dark:text-white mb-2">礼物包装</h1>
      <p className="text-sm text-gray-500 dark:text-gray-400 mb-6">从一只礼盒，到一个温柔的细节，让心意有迹可循。图片由 AI 生成，仅作款式参考。</p>
      {(hasProduct || viewingPlan) && <PackagingPreview theme={selectedBox} ribbonColor={ribbonColor} ribbonStyle={ribbonStyle} ribbonText={ribbonText} cardText={cardText} scent={scent} customs={customs} productName={dispProductName} />}

      {viewingPlan && (
        <button onClick={backToBrowse} className="btn-outline text-sm py-2 px-4 mb-4 flex items-center gap-2">
          <ArrowLeft className="w-4 h-4" /> 返回
        </button>
      )}

      {/* Product info (create mode or viewing a saved plan) */}
      {(hasProduct || viewingPlan) && (
        <div className="card mb-6 flex items-center gap-4 p-4">
          {dispImageUrl ? (
            <Image src={dispImageUrl} alt={dispProductName} width={64} height={64} unoptimized referrerPolicy="no-referrer"
              className="w-16 h-16 rounded-lg object-cover" />
          ) : (
            <div className="w-16 h-16 rounded-lg bg-gray-100 dark:bg-gray-800 flex items-center justify-center">
              <Gift className="w-8 h-8 text-gray-300" />
            </div>
          )}
          <div className="flex-1">
            <p className="font-medium text-gray-800 dark:text-gray-100">{dispProductName}</p>
          </div>
          {hasRecipient && !viewingPlan && (
            <div className="text-sm text-gray-500 dark:text-gray-400">
              <p>收礼人：{recipientName}</p>
              <p>场景：{occasion}</p>
            </div>
          )}
          {hasProduct && !viewingPlan && (
            <button onClick={aiRecommend} disabled={aiLoading}
              className="btn-primary text-sm py-2 px-4 flex items-center gap-2">
              <Sparkles className="w-4 h-4" /> {aiLoading ? 'AI推荐中...' : 'AI智能推荐包装'}
            </button>
          )}
        </div>
      )}

      {/* Gift box selection */}
      <h2 className="text-lg font-semibold text-gray-800 dark:text-gray-100 mb-3">选择礼盒</h2>
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3 mb-8">
        {GIFT_BOXES.map(box => (
          <button key={box.id} onClick={() => !readOnly && setSelectedBox(box.id)} disabled={readOnly}
            aria-pressed={selectedBox === box.id}
            className={`card p-3 text-center transition-all ${selectedBox === box.id ? 'ring-2 ring-primary-500' : 'hover:shadow-md'} ${readOnly ? 'cursor-default' : ''}`}>
            <Image src={box.image} alt={box.name} width={480} height={480} sizes="(max-width: 640px) 45vw, (max-width: 1024px) 30vw, 180px" unoptimized className="w-full aspect-square object-cover rounded-xl mb-3" />
            <p className="text-sm font-medium text-gray-800 dark:text-gray-100">{box.name}</p>
            <p className="text-xs text-gray-400 mt-0.5 line-clamp-2">{box.desc}</p>
            <p className="text-xs text-rose-500 mt-1">{pricesReady ? `¥${BOX_PRICES[box.id]?.toFixed(2)}` : '价格加载中'}</p>
          </button>
        ))}
      </div>

      {/* Customization */}
      <h2 className="text-lg font-semibold text-gray-800 dark:text-gray-100 mb-3">个性化定制</h2>
      <div className="space-y-3 mb-8">
        {CUSTOMIZATIONS.map(c => (
          <div key={c.id} className={`card p-4 grid grid-cols-[20px_64px_minmax(0,1fr)] sm:grid-cols-[20px_80px_minmax(0,1fr)] items-center gap-3 sm:gap-4 transition-all ${customs.has(c.id) ? 'ring-1 ring-primary-300' : ''}`}>
            <input type="checkbox" checked={customs.has(c.id)} onChange={() => toggleCustom(c.id)} disabled={readOnly}
              aria-label={c.name} className="w-5 h-5 shrink-0 rounded border-gray-300 text-primary-500 focus:ring-primary-500" />
            <Image src={c.image} alt={c.name} width={160} height={160} unoptimized className="w-16 h-16 sm:w-20 sm:h-20 shrink-0 rounded-xl object-cover" />
            <div className="min-w-0">
              <p className="font-medium text-gray-800 dark:text-gray-100">{c.name}</p>
              <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">{c.desc}</p>
              <p className="text-xs text-rose-500">{pricesReady ? `+¥${CUSTOMIZATION_PRICES[c.id]?.toFixed(2)}` : '价格加载中'}</p>
            </div>
              {c.id === 'ribbon_text' && customs.has('ribbon_text') && (
                <div className="col-span-3 sm:col-start-3 sm:col-span-1 flex flex-wrap items-center gap-2">
                  <input value={ribbonText} onChange={e => setRibbonText(e.target.value.slice(0, 10))} disabled={readOnly}
                    placeholder="烫金文字（最多10字）" className="input-field text-sm flex-1 min-w-0" />
                  <select value={ribbonColor} onChange={e => setRibbonColor(e.target.value)} disabled={readOnly} className="input-field text-sm w-24">
                    <option value="金色">金色</option>
                    <option value="银色">银色</option>
                  </select>
                </div>
              )}
              {c.id === 'greeting_card' && customs.has('greeting_card') && (
                <div className="col-span-3 sm:col-start-3 sm:col-span-1 flex flex-wrap items-center gap-2 w-full">
                  <textarea value={cardText} onChange={e => setCardText(e.target.value.slice(0, 50))} disabled={readOnly}
                    placeholder="贺卡文案（50字以内）" className="input-field text-sm w-full sm:flex-1 min-w-0" rows={2} />
                  <button type="button" disabled={readOnly || aiGreetingLoading} onClick={onAiGreeting}
                    className="btn-primary text-sm py-1.5 px-4 flex items-center gap-2 whitespace-nowrap">
                    <Sparkles className="w-3.5 h-3.5" /> {aiGreetingLoading ? '生成中' : 'AI 生成'}
                  </button>
                </div>
              )}
              {c.id === 'scent' && customs.has('scent') && (
                <select value={scent} onChange={e => setScent(e.target.value)} disabled={readOnly} className="col-span-3 sm:col-start-3 sm:col-span-1 input-field text-sm w-32">
                  <option value="">选择香型</option>
                  {SCENTS.map(s => <option key={s} value={s}>{s}</option>)}
                </select>
              )}
          </div>
        ))}
      </div>

      {/* Ribbon style */}
      <h2 className="text-lg font-semibold text-gray-800 dark:text-gray-100 mb-3">丝带绑法</h2>
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-8">
        {RIBBON_STYLES.map(r => (
          <button key={r.id} onClick={() => !readOnly && setRibbonStyle(r.id)} disabled={readOnly}
            aria-pressed={ribbonStyle === r.id}
            className={`card p-3 text-center transition-all ${ribbonStyle === r.id ? 'ring-2 ring-primary-500' : 'hover:shadow-md'} ${readOnly ? 'cursor-default' : ''}`}>
            <Image src={r.image} alt={r.name} width={480} height={480} sizes="(max-width: 640px) 45vw, 220px" unoptimized className="w-full aspect-square object-cover rounded-xl mb-3" />
            <p className="text-sm font-medium text-gray-800 dark:text-gray-100">{r.name}</p>
            <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">{r.desc}</p>
          </button>
        ))}
      </div>

      {/* Save (only in create mode) */}
      {!readOnly && (
        <div className="card flex items-center justify-between p-4 sticky bottom-4">
          <div>
            <p className="text-sm text-gray-400">包装费用由系统按所选项目计算</p>
            <p className="text-lg font-bold text-rose-500">¥{packagingTotal.toFixed(2)}</p>
          </div>
          <div className="flex items-center gap-2">
            {savedPlan?.giftRecordId && <Link href={`/gifts/${savedPlan.giftRecordId}`} className="btn-outline py-2.5 px-5">查看送礼记录</Link>}
            <button onClick={onSave} disabled={saving || !selectedBox || !pricesReady}
              className="btn-primary py-2.5 px-8 disabled:opacity-40">
              {saving ? '保存中...' : savedPlan ? '重新保存' : '确认包装方案'}
            </button>
          </div>
        </div>
      )}

      {/* History section (only in browse mode) */}
      {!hasProduct && (
        <div className="mt-8">
          <h2 className="text-lg font-semibold text-gray-800 dark:text-gray-100 mb-3 flex items-center gap-2">
            <History className="w-5 h-5" /> 历史记录
          </h2>
          {history.length === 0 ? (
            <div className="card text-center py-12 text-gray-400 dark:text-gray-500">暂无保存的包装方案</div>
          ) : (
            <div className="space-y-3">
              {history.map((p: any) => (
                <button key={p.id} onClick={() => viewPlan(p)}
                  className="card p-4 flex items-center gap-4 hover:shadow-md transition-all text-left w-full">
                  {p.productImageUrl ? (
                    <Image src={p.productImageUrl} alt={p.productName} width={48} height={48} unoptimized referrerPolicy="no-referrer"
                      className="w-12 h-12 rounded-lg object-cover" />
                  ) : (
                    <div className="w-12 h-12 rounded-lg bg-gray-100 dark:bg-gray-800 flex items-center justify-center">
                      <Gift className="w-6 h-6 text-gray-300" />
                    </div>
                  )}
                  <div className="flex-1 min-w-0">
                    <p className="font-medium text-gray-800 dark:text-gray-100 truncate">{p.productName || '未关联商品'}</p>
                    <p className="text-sm text-gray-400 truncate">
                      {BOX_MAP[p.theme]?.name || p.theme || '未选择礼盒'}
                      {p.wrappingStyle && ` · ${RIBBON_MAP[p.wrappingStyle]?.name || p.wrappingStyle}`}
                    </p>
                    <p className="text-xs text-gray-400 mt-0.5">{p.createTime?.substring(0, 16).replace('T', ' ')}</p>
                  </div>
                </button>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

export default function PackagingPage() {
  return (
    <Suspense fallback={<Loading />}>
      <PackagingContent />
    </Suspense>
  );
}
