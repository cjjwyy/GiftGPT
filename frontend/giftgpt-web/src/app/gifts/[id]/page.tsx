'use client';

import { useState, useEffect } from 'react';
import { giftApi } from '@/lib/api';
import { useParams } from 'next/navigation';
import { Loading } from '@/components/Loading';
import Link from 'next/link';
import toast from 'react-hot-toast';

export default function GiftDetailPage() {
  const params = useParams();
  const id = Number(params.id);
  const [gift, setGift] = useState<any>(null);
  const [err, setErr] = useState(false);
  const [logi, setLogi] = useState<any>(null);
  const [fRole, setFRole] = useState<'sender' | 'receiver'>('sender');
  const [fContent, setFContent] = useState('');
  const [feedbacks, setFeedbacks] = useState<any[]>([]);

  useEffect(() => {
    giftApi.get(id).then(setGift).catch(() => setErr(true));
  }, [id]);

  useEffect(() => {
    giftApi.logistics(id).then(setLogi).catch(() => setLogi(null));
  }, [id]);

  const loadFeedbacks = () => { giftApi.feedbackList(id).then(setFeedbacks).catch(() => {}); };
  useEffect(() => { loadFeedbacks(); }, [id]);
  const submitFeedback = async () => {
    if (!fContent.trim()) { toast.error('请输入反馈内容'); return; }
    try {
      await giftApi.feedback(id, { content: fContent, role: fRole });
      setFContent('');
      loadFeedbacks();
      toast.success('反馈已提交');
    } catch (e: any) { toast.error(e?.message || '提交失败'); }
  };

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
      <div className="card mt-4">
        <h2 className="font-semibold text-gray-900 dark:text-white mb-3">双向反馈</h2>
        <div className="flex gap-2 mb-3">
          <button onClick={() => setFRole('sender')}
            className={fRole === 'sender' ? 'btn-primary text-sm py-1.5 px-4' : 'btn-outline text-sm py-1.5 px-4'}>
            送礼方
          </button>
          <button onClick={() => setFRole('receiver')}
            className={fRole === 'receiver' ? 'btn-primary text-sm py-1.5 px-4' : 'btn-outline text-sm py-1.5 px-4'}>
            收礼方
          </button>
        </div>
        <textarea value={fContent} onChange={e => setFContent(e.target.value)} className="input-field mb-3" rows={2}
          placeholder="输入反馈内容..." />
        <button onClick={submitFeedback} className="btn-primary text-sm py-1.5 px-6">提交反馈</button>
        {feedbacks.length > 0 && (
          <div className="mt-4 space-y-2">
            {feedbacks.map((f: any) => (
              <div key={f.id} className="border-t border-gray-100 dark:border-gray-800 pt-2">
                <span className={`text-xs px-2 py-0.5 rounded-full mr-2 ${f.role === 'receiver' ? 'bg-rose-50 text-rose-600' : 'bg-primary-50 text-primary-600'}`}>
                  {f.role === 'receiver' ? '收礼方' : '送礼方'}
                </span>
                <span className="text-sm text-gray-700 dark:text-gray-200">{f.content}</span>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
