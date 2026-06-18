'use client';

import { useState, useEffect, Suspense } from 'react';
import { useSearchParams } from 'next/navigation';
import { recommendApi, recipientApi } from '@/lib/api';
import { GiftCard } from '@/components/GiftCard';
import { Loading } from '@/components/Loading';
import { Sparkles, Send } from 'lucide-react';
import { toast } from 'react-hot-toast';
import Link from 'next/link';

const OCCASIONS = [
  { value: 'birthday', label: '生日' }, { value: 'anniversary', label: '纪念日' },
  { value: 'valentines', label: '情人节' }, { value: 'festival', label: '节庆' },
  { value: 'graduation', label: '毕业' }, { value: 'proposal', label: '求婚' },
  { value: 'thank_you', label: '感谢' }, { value: 'other', label: '其他' },
];

function RecommendContent() {
  const searchParams = useSearchParams();
  const [recipients, setRecipients] = useState<any[]>([]);
  const [recipientId, setRecipientId] = useState(searchParams.get('recipientId') || '');
  const [occasion, setOccasion] = useState('birthday');
  const [budget, setBudget] = useState('300');
  const [result, setResult] = useState<any>(null);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');

  useEffect(() => {
    recipientApi.list().then(d => setRecipients(d.records || [])).catch(() => {});
  }, []);

  const onSearch = async () => {
    if (!recipientId) { toast.error('请选择一位收礼人'); return; }
    setLoading(true);
    try {
      const res = await recommendApi.search({ recipientId: Number(recipientId), occasion, budget: Number(budget) });
      setResult(res);
    } catch (err: any) {
      toast.error(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-6xl mx-auto px-4 py-10">
      <div className="text-center mb-10">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">AI 智能礼物推荐</h1>
        <p className="text-gray-500">选择收礼人、场景和预算，AI 为你精准匹配</p>
      </div>

      <div className="card mb-8">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 items-end">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">收礼人</label>
            <select className="input-field" value={recipientId} onChange={e => setRecipientId(e.target.value)}>
              <option value="">请选择</option>
              {recipients.map((r: any) => <option key={r.id} value={r.id}>{r.name} {r.relation ? `(${r.relation})` : ''}</option>)}
            </select>
            {recipients.length === 0 && <p className="text-xs text-gray-400 mt-1">还没有画像？<Link href="/recipients/new" className="text-primary-500">去创建</Link></p>}
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">场景</label>
            <select className="input-field" value={occasion} onChange={e => setOccasion(e.target.value)}>
              {OCCASIONS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">预算 ¥</label>
            <input className="input-field" type="number" value={budget} onChange={e => setBudget(e.target.value)} />
          </div>
          <button onClick={onSearch} disabled={loading} className="btn-primary flex items-center justify-center gap-2">
            <Sparkles className="w-4 h-4" /> {loading ? '分析中...' : '开始推荐'}
          </button>
        </div>
      </div>

      {result && (
        <div>
          <div className="mb-6">
            <p className="text-lg font-medium text-gray-800 mb-1">为 {result.recipientName} 推荐的礼物</p>
            <p className="text-sm text-gray-500">{result.summary}</p>
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            {result.items?.map((item: any, i: number) => (
              <GiftCard key={i} productId={item.productId} productName={item.productName}
                price={item.price} imageUrl={item.imageUrl} platform={item.platform}
                reason={item.reason} matchTags={item.matchTags} score={item.score} />
            ))}
          </div>
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
