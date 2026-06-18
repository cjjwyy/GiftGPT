'use client';

import { useState, useEffect } from 'react';
import { giftApi } from '@/lib/api';
import { GiftRecord } from '@/types';
import { Loading } from '@/components/Loading';
import Link from 'next/link';

const STATUS_MAP: Record<string, string> = {
  draft: '草稿', recommended: '已推荐', ordered: '已下单', shipped: '已发货', delivered: '已送达',
};

export default function GiftsPage() {
  const [gifts, setGifts] = useState<GiftRecord[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    giftApi.list().then(d => { setGifts(d.records || []); setLoading(false); }).catch(() => setLoading(false));
  }, []);

  if (loading) return <Loading />;

  return (
    <div className="max-w-4xl mx-auto px-4 py-10">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">我的送礼记录</h1>
      {gifts.length === 0 ? (
        <div className="card text-center py-20 text-gray-400">还没有送礼记录，开始你的第一次AI推荐吧！</div>
      ) : (
        <div className="space-y-3">
          {gifts.map(g => (
            <Link key={g.id} href={`/gifts/${g.id}`} className="card flex items-center justify-between hover:shadow-md transition-all">
              <div>
                <span className="font-medium text-gray-800">{g.occasion} · 预算¥{g.budget}</span>
              </div>
              <span className="tag">{STATUS_MAP[g.status] || g.status}</span>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
