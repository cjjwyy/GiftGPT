'use client';

import { useState, useEffect } from 'react';
import { giftApi, recipientApi } from '@/lib/api';
import { GiftRecord } from '@/types';
import { Loading } from '@/components/Loading';
import Link from 'next/link';

const STATUS_MAP: Record<string, string> = {
  draft: '草稿', recommended: '已推荐', ordered: '已下单', shipped: '已发货', delivered: '已送达',
};

export default function GiftsPage() {
  const [gifts, setGifts] = useState<GiftRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [recipients, setRecipients] = useState<any[]>([]);
  const [fRecipientId, setFRecipientId] = useState('');
  const [fOccasion, setFOccasion] = useState('');
  const [fStatus, setFStatus] = useState('');

  const loadGifts = () => {
    setLoading(true);
    giftApi.list({
      recipientId: fRecipientId ? Number(fRecipientId) : undefined,
      occasion: fOccasion || undefined,
      status: fStatus || undefined,
    }).then(d => { setGifts(d.records || []); setLoading(false); }).catch(() => setLoading(false));
  };

  useEffect(() => {
    recipientApi.list(1, 100).then(d => setRecipients(d.records || [])).catch(() => {});
    loadGifts();
  }, []);

  if (loading) return <Loading />;

  return (
    <div className="max-w-4xl mx-auto px-4 py-10">
      <h1 className="text-2xl font-bold text-gray-900 dark:text-white mb-6">我的送礼记录</h1>

      <div className="card mb-6">
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 items-end">
          <div>
            <label className="block text-xs font-medium text-gray-500 mb-1">收礼人</label>
            <select className="input-field" value={fRecipientId} onChange={e => setFRecipientId(e.target.value)}>
              <option value="">全部</option>
              {recipients.map((r: any) => <option key={r.id} value={r.id}>{r.name}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-500 mb-1">场景</label>
            <select className="input-field" value={fOccasion} onChange={e => setFOccasion(e.target.value)}>
              <option value="">全部</option>
              <option value="birthday">生日</option>
              <option value="anniversary">纪念日</option>
              <option value="valentines">情人节</option>
              <option value="festival">节庆</option>
              <option value="graduation">毕业</option>
              <option value="proposal">求婚</option>
              <option value="thank_you">感谢</option>
              <option value="other">其他</option>
            </select>
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-500 mb-1">状态</label>
            <select className="input-field" value={fStatus} onChange={e => setFStatus(e.target.value)}>
              <option value="">全部</option>
              <option value="draft">草稿</option>
              <option value="recommended">已推荐</option>
              <option value="ordered">已下单</option>
              <option value="packaged">已包装</option>
              <option value="shipped">已发货</option>
              <option value="delivered">已送达</option>
            </select>
          </div>
          <button onClick={loadGifts} className="btn-primary text-sm py-2">查询</button>
        </div>
      </div>

      {gifts.length === 0 ? (
        <div className="card text-center py-20 text-gray-400 dark:text-gray-500">还没有送礼记录，开始你的第一次AI推荐吧！</div>
      ) : (
        <div className="space-y-3">
          {gifts.map(g => (
            <Link key={g.id} href={`/gifts/${g.id}`} className="card flex items-center justify-between hover:shadow-md transition-all">
              <div>
                <span className="font-medium text-gray-800 dark:text-gray-100">{g.occasion} · 预算¥{g.budget}</span>
              </div>
              <span className="tag">{STATUS_MAP[g.status] || g.status}</span>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
