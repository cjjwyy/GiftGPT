'use client';

import { useState, useEffect } from 'react';
import { useSearchParams } from 'next/navigation';
import { Suspense } from 'react';
import { packagingApi, greetingApi } from '@/lib/api';
import { Loading } from '@/components/Loading';
import { Sparkles, Gift, History, ArrowLeft } from 'lucide-react';
import { toast } from 'react-hot-toast';

const GIFT_BOXES = [
  { id: 'classic', name: '经典缎面礼盒', desc: '硬质磁吸礼盒，缎面蝴蝶结，丝绒内衬', price: 29.9, svg: '/packaging/box-classic.svg' },
  { id: 'korean', name: '韩式极简礼盒', desc: '哑光质感，单色丝带，简约标签', price: 19.9, svg: '/packaging/box-korean.svg' },
  { id: 'kraft', name: '牛皮纸自然风', desc: '牛皮纸+麻绳+干花点缀，环保自然', price: 9.9, svg: '/packaging/box-kraft.svg' },
  { id: 'luxury', name: '轻奢烫金礼盒', desc: '烫金封面，双层蝴蝶结，珠光内衬', price: 49.9, svg: '/packaging/box-luxury.svg' },
  { id: 'acrylic', name: '透明亚克力盒', desc: '透明展示盒，内填拉菲草，丝带装饰', price: 24.9, svg: '/packaging/box-acrylic.svg' },
];

const CUSTOMIZATIONS = [
  { id: 'ribbon_text', name: '礼带烫金字', price: 9.9, svg: '/packaging/opt-ribbon-text.svg' },
  { id: 'greeting_card', name: '手写贺卡', price: 5.0, svg: '/packaging/opt-greeting-card.svg' },
  { id: 'dried_flower', name: '干花装饰', price: 12.0, svg: '/packaging/opt-dried-flower.svg' },
  { id: 'polaroid', name: '拍立得照片夹', price: 8.0, svg: '/packaging/opt-polaroid.svg' },
  { id: 'scent', name: '香薰加香', price: 6.0, svg: '/packaging/opt-scent.svg' },
  { id: 'band_wrap', name: '定制腰封', price: 7.0, svg: '/packaging/opt-band-wrap.svg' },
];

const RIBBON_STYLES = [
  { id: 'cross', name: '经典交叉', svg: '/packaging/ribbon-cross.svg' },
  { id: 'side', name: '单侧斜绑', svg: '/packaging/ribbon-side.svg' },
  { id: 'double_bow', name: '双层蝴蝶结', svg: '/packaging/ribbon-double-bow.svg' },
  { id: 'furoshiki', name: '日式风吕敷', svg: '/packaging/ribbon-furoshiki.svg' },
];

const SCENTS = ['玫瑰', '白茶', '雪松'];

const BOX_MAP = Object.fromEntries(GIFT_BOXES.map(b => [b.id, b]));
const RIBBON_MAP = Object.fromEntries(RIBBON_STYLES.map(r => [r.id, r]));

function PackagingContent() {
  const searchParams = useSearchParams();
  const productName = searchParams.get('productName') || '';
  const price = searchParams.get('price') || '';
  const imageUrl = searchParams.get('imageUrl') || '';
  const productId = searchParams.get('productId') || '';
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

  const readOnly = !hasProduct || !!viewingPlan;

  useEffect(() => {
    if (!hasProduct) {
      packagingApi.list(1, 20).then(d => setHistory(d.records || [])).catch(() => {});
    }
  }, [hasProduct]);

  const totalPrice = (GIFT_BOXES.find(b => b.id === selectedBox)?.price || 0)
    + Array.from(customs).reduce((sum, id) => sum + (CUSTOMIZATIONS.find(c => c.id === id)?.price || 0), 0);

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
        productPrice: Number(price),
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
      toast.success('AI智能包装推荐已完成');
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
    if (!selectedBox) { toast.error('请选择礼盒'); return; }
    setSaving(true);
    try {
      await packagingApi.save({
        productName, productPrice: Number(price), productImageUrl: imageUrl,
        productId: productId ? Number(productId) : undefined,
        packagingType: selectedBox,
        ribbonText: customs.has('ribbon_text') ? ribbonText : undefined,
        ribbonColor: customs.has('ribbon_text') ? ribbonColor : undefined,
        scent: customs.has('scent') ? scent : undefined,
        customText: customs.has('greeting_card') ? cardText : undefined,
        wrappingStyle: ribbonStyle,
        price: totalPrice,
        recipientId: recipientId ? Number(recipientId) : undefined,
        occasion: occasion || undefined,
      });
      toast.success('包装方案已保存');
    } catch (e: any) { toast.error(e?.message || '保存失败'); }
    setSaving(false);
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
    setCustoms(c);
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
  const dispPrice = viewingPlan?.productPrice ?? price;
  const dispImageUrl = viewingPlan?.productImageUrl || imageUrl;

  return (
    <div className="max-w-5xl mx-auto px-4 py-10">
      <h1 className="text-2xl font-bold text-gray-900 dark:text-white mb-6">礼物包装</h1>

      {viewingPlan && (
        <button onClick={backToBrowse} className="btn-outline text-sm py-2 px-4 mb-4 flex items-center gap-2">
          <ArrowLeft className="w-4 h-4" /> 返回
        </button>
      )}

      {/* Product info (create mode or viewing a saved plan) */}
      {(hasProduct || viewingPlan) && (
        <div className="card mb-6 flex items-center gap-4 p-4">
          {dispImageUrl ? (
            <img src={dispImageUrl} alt={dispProductName} referrerPolicy="no-referrer"
              className="w-16 h-16 rounded-lg object-cover" />
          ) : (
            <div className="w-16 h-16 rounded-lg bg-gray-100 dark:bg-gray-800 flex items-center justify-center">
              <Gift className="w-8 h-8 text-gray-300" />
            </div>
          )}
          <div className="flex-1">
            <p className="font-medium text-gray-800 dark:text-gray-100">{dispProductName}</p>
            <p className="text-sm text-gray-400">¥{dispPrice}</p>
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
            className={`card p-3 text-center transition-all ${selectedBox === box.id ? 'ring-2 ring-primary-500' : 'hover:shadow-md'} ${readOnly ? 'cursor-default' : ''}`}>
            <img src={box.svg} alt={box.name} className="w-full aspect-square object-contain mb-2" />
            <p className="text-sm font-medium text-gray-800 dark:text-gray-100">{box.name}</p>
            <p className="text-xs text-gray-400 mt-0.5 line-clamp-2">{box.desc}</p>
            <p className="text-sm font-bold text-rose-500 mt-1">¥{box.price}</p>
          </button>
        ))}
      </div>

      {/* Customization */}
      <h2 className="text-lg font-semibold text-gray-800 dark:text-gray-100 mb-3">个性化定制</h2>
      <div className="space-y-3 mb-8">
        {CUSTOMIZATIONS.map(c => (
          <div key={c.id} className={`card p-4 flex items-center gap-4 transition-all ${customs.has(c.id) ? 'ring-1 ring-primary-300' : ''}`}>
            <input type="checkbox" checked={customs.has(c.id)} onChange={() => toggleCustom(c.id)} disabled={readOnly}
              className="w-5 h-5 rounded border-gray-300 text-primary-500 focus:ring-primary-500" />
            <img src={c.svg} alt={c.name} className="w-12 h-12 object-contain" />
            <div className="flex-1">
              <p className="font-medium text-gray-800 dark:text-gray-100">{c.name}</p>
              {c.id === 'ribbon_text' && customs.has('ribbon_text') && (
                <div className="flex items-center gap-2 mt-2">
                  <input value={ribbonText} onChange={e => setRibbonText(e.target.value.slice(0, 10))} disabled={readOnly}
                    placeholder="烫金文字（最多10字）" className="input-field text-sm flex-1" />
                  <select value={ribbonColor} onChange={e => setRibbonColor(e.target.value)} disabled={readOnly} className="input-field text-sm w-24">
                    <option value="金色">金色</option>
                    <option value="银色">银色</option>
                  </select>
                </div>
              )}
              {c.id === 'greeting_card' && customs.has('greeting_card') && (
                <div className="flex items-center gap-2 mt-2 w-full">
                  <textarea value={cardText} onChange={e => setCardText(e.target.value.slice(0, 50))} disabled={readOnly}
                    placeholder="贺卡文案（50字以内）" className="input-field text-sm flex-1" rows={2} />
                  <button type="button" disabled={readOnly || aiGreetingLoading} onClick={onAiGreeting}
                    className="btn-primary text-sm py-1.5 px-4 flex items-center gap-2 whitespace-nowrap">
                    <Sparkles className="w-3.5 h-3.5" /> {aiGreetingLoading ? '生成中' : 'AI 生成'}
                  </button>
                </div>
              )}
              {c.id === 'scent' && customs.has('scent') && (
                <select value={scent} onChange={e => setScent(e.target.value)} disabled={readOnly} className="input-field text-sm mt-2 w-32">
                  <option value="">选择香型</option>
                  {SCENTS.map(s => <option key={s} value={s}>{s}</option>)}
                </select>
              )}

            </div>
            <span className="text-sm font-bold text-rose-500">¥{c.price}</span>
          </div>
        ))}
      </div>

      {/* Ribbon style */}
      <h2 className="text-lg font-semibold text-gray-800 dark:text-gray-100 mb-3">丝带绑法</h2>
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-8">
        {RIBBON_STYLES.map(r => (
          <button key={r.id} onClick={() => !readOnly && setRibbonStyle(r.id)} disabled={readOnly}
            className={`card p-3 text-center transition-all ${ribbonStyle === r.id ? 'ring-2 ring-primary-500' : 'hover:shadow-md'} ${readOnly ? 'cursor-default' : ''}`}>
            <img src={r.svg} alt={r.name} className="w-full aspect-square object-contain mb-2" />
            <p className="text-sm font-medium text-gray-800 dark:text-gray-100">{r.name}</p>
          </button>
        ))}
      </div>

      {/* Total + save (only in create mode) */}
      {!readOnly && (
        <div className="card flex items-center justify-between p-4 sticky bottom-4">
          <div>
            <p className="text-sm text-gray-400">包装总计</p>
            <p className="text-2xl font-bold text-rose-500">¥{totalPrice.toFixed(1)}</p>
          </div>
          <button onClick={onSave} disabled={saving || !selectedBox}
            className="btn-primary py-2.5 px-8 disabled:opacity-40">
            {saving ? '保存中...' : '确认包装方案'}
          </button>
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
                    <img src={p.productImageUrl} alt={p.productName} referrerPolicy="no-referrer"
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
                  <span className="text-lg font-bold text-rose-500 shrink-0">¥{p.price}</span>
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
