'use client';

import { useState, useEffect, Suspense } from 'react';
import { useSearchParams } from 'next/navigation';
import { recommendApi, recipientApi } from '@/lib/api';
import { GiftCard } from '@/components/GiftCard';
import { Loading } from '@/components/Loading';
import { Sparkles, Check, Loader2, User, BrainCircuit, Search, History } from 'lucide-react';
import { toast } from 'react-hot-toast';
import Link from 'next/link';

const OCCASIONS = [
  { value: 'birthday', label: '生日' }, { value: 'anniversary', label: '纪念日' },
  { value: 'valentines', label: '情人节' }, { value: 'festival', label: '节庆' },
  { value: 'graduation', label: '毕业' }, { value: 'proposal', label: '求婚' },
  { value: 'thank_you', label: '感谢' }, { value: 'other', label: '其他' },
];

type StepStatus = 'pending' | 'active' | 'done';
interface Step { key: string; label: string; desc: string; icon: any; status: StepStatus; }

const INITIAL_STEPS: Step[] = [
  { key: 'analyze', label: '分析性格', desc: '解读收礼人 MBTI、兴趣与画像', icon: User, status: 'pending' },
  { key: 'ai', label: 'AI 智能判断礼物', desc: '基于画像与场景生成候选清单', icon: BrainCircuit, status: 'pending' },
  { key: 'search', label: '拼多多搜索', desc: '在拼多多匹配真实商品', icon: Search, status: 'pending' },
];

function RecommendContent() {
  const searchParams = useSearchParams();
  const [recipients, setRecipients] = useState<any[]>([]);
  const [recipientId, setRecipientId] = useState(searchParams.get('recipientId') || '');
  const [occasion, setOccasion] = useState('birthday');
  const [budget, setBudget] = useState('300');
  const [result, setResult] = useState<any>(null);
  const [steps, setSteps] = useState<Step[]>(INITIAL_STEPS);
  const [running, setRunning] = useState(false);
  const [analysis, setAnalysis] = useState('');

  useEffect(() => {
    recipientApi.list().then(d => setRecipients(d.records || [])).catch(() => {});
    const saved = sessionStorage.getItem('lastRecommendation');
    if (saved) {
      try { setResult(JSON.parse(saved)); } catch {}
    }
  }, []);

  const setStep = (key: string, status: StepStatus) => {
    setSteps(prev => prev.map(s => s.key === key ? { ...s, status } : s));
  };

  const onSearch = async () => {
    if (!recipientId) { toast.error('请选择一位收礼人'); return; }
    setRunning(true);
    setResult(null);
    setAnalysis('');
    setSteps(INITIAL_STEPS.map(s => ({ ...s, status: 'pending' })));

    try {
      // Step 1: analyze personality
      setStep('analyze', 'active');
      const snapshot = await recommendApi.analyze(Number(recipientId));
      setAnalysis(snapshot?.analysis || '');
      setStep('analyze', 'done');

      // Step 2: AI judges gifts
      setStep('ai', 'active');
      const ai = await recommendApi.aiGifts({ recipientId: Number(recipientId), occasion, budget: Number(budget) });
      setStep('ai', 'done');

      // Step 3: search platforms & assemble
      setStep('search', 'active');
      const final = await recommendApi.match({
        recipientId: Number(recipientId),
        occasion,
        budget: Number(budget),
        gifts: ai?.gifts || [],
        summary: ai?.summary,
      });
      setStep('search', 'done');
      setResult(final);
      sessionStorage.setItem('lastRecommendation', JSON.stringify(final));
    } catch (err: any) {
      toast.error(err.message || '推荐生成失败');
    } finally {
      setRunning(false);
    }
  };

  return (
    <div className="max-w-6xl mx-auto px-4 py-10">
      <div className="text-center mb-10">
        <h1 className="text-3xl font-bold text-gray-900 dark:text-white mb-2">AI 智能礼物推荐</h1>
        <p className="text-gray-500 dark:text-gray-400">选择收礼人、场景和预算，AI 为你精准匹配</p>
        <div className="mt-3">
          <Link href="/recommend/history" className="text-sm text-primary-500 hover:text-primary-600 inline-flex items-center gap-1">
            <History className="w-4 h-4" /> 查看历史记录
          </Link>
        </div>
      </div>

      <div className="card mb-8">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 items-end">
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">收礼人</label>
            <select className="input-field" value={recipientId} onChange={e => setRecipientId(e.target.value)} disabled={running}>
              <option value="">请选择</option>
              {recipients.map((r: any) => <option key={r.id} value={r.id}>{r.name} {r.relation ? `(${r.relation})` : ''}</option>)}
            </select>
            {recipients.length === 0 && <p className="text-xs text-gray-400 dark:text-gray-500 mt-1">还没有画像？<Link href="/recipients/new" className="text-primary-500">去创建</Link></p>}
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">场景</label>
            <select className="input-field" value={occasion} onChange={e => setOccasion(e.target.value)} disabled={running}>
              {OCCASIONS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">预算 ¥</label>
            <input className="input-field" type="number" value={budget} onChange={e => setBudget(e.target.value)} disabled={running} />
          </div>
          <button onClick={onSearch} disabled={running} className="btn-primary flex items-center justify-center gap-2">
            <Sparkles className="w-4 h-4" /> {running ? '生成中...' : '开始推荐'}
          </button>
        </div>
      </div>

      {running && (
        <div className="card mb-8">
          <div className="space-y-4">
            {steps.map((s, i) => (
              <div key={s.key} className="flex items-center gap-4">
                <div className={`w-10 h-10 rounded-full flex items-center justify-center shrink-0 transition-colors ${
                  s.status === 'done' ? 'bg-green-500 text-white'
                    : s.status === 'active' ? 'bg-primary-500 text-white'
                    : 'bg-gray-100 dark:bg-gray-800 text-gray-400'
                }`}>
                  {s.status === 'done' ? <Check className="w-5 h-5" />
                    : s.status === 'active' ? <Loader2 className="w-5 h-5 animate-spin" />
                    : <s.icon className="w-5 h-5" />}
                </div>
                <div className="flex-1">
                  <p className={`font-medium ${s.status === 'pending' ? 'text-gray-400 dark:text-gray-500' : 'text-gray-800 dark:text-gray-100'}`}>
                    {i + 1}. {s.label}
                  </p>
                  <p className="text-sm text-gray-400 dark:text-gray-500">{s.desc}</p>
                  {s.key === 'analyze' && s.status === 'done' && analysis && (
                    <p className="text-xs text-primary-600 dark:text-primary-400 mt-1 leading-relaxed">{analysis}</p>
                  )}
                </div>
                {s.status === 'done' && <span className="text-xs text-green-500 font-medium">已完成</span>}
              </div>
            ))}
          </div>
        </div>
      )}

      {result && (
        <div>
          <div className="mb-6">
            <p className="text-lg font-medium text-gray-800 dark:text-gray-100 mb-1">为 {result.recipientName} 推荐的礼物</p>
            <p className="text-sm text-gray-500 dark:text-gray-400">{result.summary}</p>
          </div>
              {result.items?.length ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
              {result.items.map((item: any, i: number) => (
                <GiftCard key={i} productId={item.productId} productName={item.productName}
                  price={item.price} imageUrl={item.imageUrl} platform={item.platform}
                  platformUrl={item.platformUrl}
                  reason={item.reason} matchTags={item.matchTags} score={item.score}
                  recipientName={result.recipientName} recipientId={result.recipientId} occasion={result.occasion} />
              ))}
            </div>
          ) : (
            <div className="card text-center py-16 text-gray-400 dark:text-gray-500">暂无匹配商品，可尝试调整预算或场景</div>
          )}
        </div>
      )}
    </div>
  );
}

export default function RecommendPage() {
  return (
    <Suspense fallback={<Loading />}>
      <RecommendContent />
    </Suspense>
  );
}
