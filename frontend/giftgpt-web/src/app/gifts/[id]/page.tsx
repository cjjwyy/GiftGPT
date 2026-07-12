'use client';

import { useState, useEffect } from 'react';
import { giftApi } from '@/lib/api';
import { useParams } from 'next/navigation';
import { Loading } from '@/components/Loading';
import Link from 'next/link';

export default function GiftDetailPage() {
  const params = useParams();
  const id = Number(params.id);
  const [gift, setGift] = useState<any>(null);
  const [err, setErr] = useState(false);
  const [logi, setLogi] = useState<any>(null);

  useEffect(() => {
    giftApi.get(id).then(setGift).catch(() => setErr(true));
  }, [id]);

  useEffect(() => {
    giftApi.logistics(id).then(setLogi).catch(() => setLogi(null));
  }, [id]);

  if (err) return (
    <div className="max-w-2xl mx-auto px-4 py-20 text-center text-gray-400">
      送礼记录不存在或已删除<br/>
      <Link href="/gifts" className="text-primary-500">返回列表</Link>
    </div>
  );
  if (!gift) return <Loading />;

  return (
    <div className="max-w-2xl mx-auto px-4 py-10">
      <h1 className="text-2xl font-bold text-gray-900 dark:text-white mb-6">送礼详情</h1>
      <div className="card space-y-3">
        <div className="flex justify-between">
          <span className="text-gray-500 dark:text-gray-400">场景</span>
          <span className="font-medium">{gift.occasion}</span>
        </div>
        <div className="flex justify-between">
          <span className="text-gray-500 dark:text-gray-400">预算</span>
          <span className="font-medium">¥{gift.budget}</span>
        </div>
        <div className="flex justify-between">
          <span className="text-gray-500 dark:text-gray-400">状态</span>
          <span className="tag">{gift.status}</span>
        </div>
        <div className="flex justify-between">
          <span className="text-gray-500 dark:text-gray-400">创建时间</span>
          <span>{gift.createTime}</span>
        </div>
      </div>
      {logi && logi.events && logi.events.length > 0 && (
        <div className="card mt-4">
          <h2 className="font-semibold text-gray-900 dark:text-white mb-4">物流追踪</h2>
          <ol className="relative border-l border-gray-200 dark:border-gray-700 ml-3 space-y-4">
            {logi.events.map((e: any, i: number) => (
              <li key={i} className="ml-4">
                <p className="text-sm font-medium text-gray-800 dark:text-gray-100">{e.description}</p>
                <p className="text-xs text-gray-400">{e.eventTime} · {e.location} · {e.status}</p>
              </li>
            ))}
          </ol>
        </div>
      )}
    </div>
  );
}
