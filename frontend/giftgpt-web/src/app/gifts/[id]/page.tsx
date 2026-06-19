'use client';

import { useState, useEffect } from 'react';
import { giftApi } from '@/lib/api';
import { useParams } from 'next/navigation';
import { Loading } from '@/components/Loading';

export default function GiftDetailPage() {
  const params = useParams();
  const id = Number(params.id);
  const [gift, setGift] = useState<any>(null);

  useEffect(() => {
    giftApi.get(id).then(setGift).catch(() => {});
  }, [id]);

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
    </div>
  );
}
