'use client';

import { ChangeEvent, useCallback, useEffect, useState } from 'react';
import { giftApi } from '@/lib/api';
import { useParams } from 'next/navigation';
import { Loading } from '@/components/Loading';
import Link from 'next/link';
import Image from 'next/image';
import { Copy, Link2, Mic, PackageCheck, Truck } from 'lucide-react';
import toast from 'react-hot-toast';
import { OCCASION_LABELS } from '@/lib/occasions';

interface FeedbackLink {
  url: string;
  qrCodeUrl?: string;
  expiresAt: string;
}

export default function GiftDetailPage() {
  const params = useParams();
  const id = Number(params.id);
  const [gift, setGift] = useState<any>(null);
  const [err, setErr] = useState(false);
  const [logistics, setLogistics] = useState<any>(null);
  const [greeting, setGreeting] = useState<any>(null);
  const [feedbackContent, setFeedbackContent] = useState('');
  const [feedbacks, setFeedbacks] = useState<any[]>([]);
  const [feedbackLink, setFeedbackLink] = useState<FeedbackLink | null>(null);
  const [working, setWorking] = useState('');

  const loadFeedbacks = useCallback(() => {
    if (!Number.isFinite(id)) return;
    giftApi.feedbackList(id).then(setFeedbacks)
      .catch((error: Error) => toast.error(error.message || '加载反馈失败'));
  }, [id]);

  useEffect(() => {
    if (!Number.isFinite(id) || id <= 0) {
      setErr(true);
      return;
    }
    giftApi.get(id).then(setGift).catch(() => setErr(true));
    giftApi.logistics(id).then(setLogistics).catch(() => setLogistics(null));
    giftApi.greeting(id).then(setGreeting).catch(() => setGreeting(null));
    loadFeedbacks();
  }, [id, loadFeedbacks]);

  const submitFeedback = async () => {
    if (!feedbackContent.trim()) {
      toast.error('请输入反馈内容');
      return;
    }
    setWorking('feedback');
    try {
      await giftApi.feedback(id, { content: feedbackContent, type: 'comment' });
      setFeedbackContent('');
      loadFeedbacks();
      toast.success('送礼方反馈已提交');
    } catch (error: any) {
      toast.error(error?.message || '提交失败');
    } finally {
      setWorking('');
    }
  };

  const createOrder = async () => {
    setWorking('order');
    try {
      await giftApi.createOrder(id, {});
      const data = await giftApi.logistics(id);
      setLogistics(data);
      setGift((current: any) => current ? { ...current, status: data.status } : current);
      toast.success('订单已创建');
    } catch (error: any) {
      toast.error(error?.message || '下单失败');
    } finally {
      setWorking('');
    }
  };

  const createFeedbackLink = async () => {
    setWorking('link');
    try {
      const data = await giftApi.feedbackLink(id);
      setFeedbackLink(data);
      toast.success(data.qrCodeUrl ? '反馈链接和二维码已生成' : '反馈链接已生成，二维码服务暂不可用');
    } catch (error: any) {
      toast.error(error?.message || '生成反馈链接失败');
    } finally {
      setWorking('');
    }
  };

  const uploadVoice = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;
    if (file.size > 5 * 1024 * 1024) {
      toast.error('语音文件不能超过5MB');
      return;
    }
    setWorking('voice');
    try {
      const data = await giftApi.uploadVoice(id, file);
      setGreeting(data);
      toast.success('语音贺卡已保存');
    } catch (error: any) {
      toast.error(error?.message || '语音上传失败');
    } finally {
      setWorking('');
    }
  };

  if (err) {
    return (
      <div className="max-w-2xl mx-auto px-4 py-20 text-center text-gray-400">
        送礼记录不存在、已删除或不属于当前账号
        <br />
        <Link href="/gifts" className="text-primary-500">返回列表</Link>
      </div>
    );
  }
  if (!gift) return <Loading />;

  return (
    <div className="max-w-2xl mx-auto px-4 py-10">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">送礼详情</h1>
        {!logistics && (
          <button type="button" onClick={createOrder} disabled={working === 'order'}
            className="btn-primary flex items-center gap-2">
            <PackageCheck className="w-4 h-4" /> {working === 'order' ? '创建中...' : '确认下单'}
          </button>
        )}
      </div>

      <div className="card space-y-3">
        <div className="flex justify-between">
          <span className="text-gray-500 dark:text-gray-400">场景</span>
          <span className="font-medium">{OCCASION_LABELS[gift.occasion] || gift.occasion}</span>
        </div>
        <div className="flex justify-between">
          <span className="text-gray-500 dark:text-gray-400">商品预算</span>
          <span className="font-medium">¥{gift.budget}</span>
        </div>
        <div className="flex justify-between">
          <span className="text-gray-500 dark:text-gray-400">状态</span>
          <span className="tag">{gift.status}</span>
        </div>
        <div className="flex justify-between">
          <span className="text-gray-500 dark:text-gray-400">创建时间</span>
          <span>{gift.createTime?.replace('T', ' ')}</span>
        </div>
      </div>

      {logistics?.events?.length > 0 && (
        <div className="card mt-4">
          <div className="flex items-center justify-between mb-4">
            <h2 className="font-semibold text-gray-900 dark:text-white flex items-center gap-2">
              <Truck className="w-4 h-4" /> 物流追踪
            </h2>
            {logistics.simulated && (
              <span className="text-xs rounded-full bg-amber-50 text-amber-700 dark:bg-amber-900/20 dark:text-amber-300 px-2 py-1">演示模拟数据</span>
            )}
          </div>
          <ol className="relative border-l border-gray-200 dark:border-gray-700 ml-3 space-y-4">
            {logistics.events.map((event: any, index: number) => (
              <li key={index} className="ml-4">
                <p className="text-sm font-medium text-gray-800 dark:text-gray-100">{event.description}</p>
                <p className="text-xs text-gray-400">{event.eventTime?.replace('T', ' ')} · {event.location} · {event.status}</p>
              </li>
            ))}
          </ol>
        </div>
      )}

      <div className="card mt-4">
        <h2 className="font-semibold text-gray-900 dark:text-white mb-2">贺卡与收礼方反馈</h2>
        <p className="text-sm text-gray-500 dark:text-gray-400 mb-4">生成一次性链接给收礼人，链接有效期7天且提交后失效。</p>
        {greeting?.content && <p className="rounded-lg bg-gray-50 dark:bg-gray-800 p-3 text-sm whitespace-pre-line mb-3">{greeting.content}</p>}
        {greeting?.voiceUrl && (
          <audio controls src={greeting.voiceUrl} className="w-full mb-3">
            您的浏览器不支持音频播放。
          </audio>
        )}
        <div className="flex flex-wrap gap-2">
          <label className="btn-outline text-sm py-2 px-4 flex items-center gap-2 cursor-pointer">
            <Mic className="w-4 h-4" /> {working === 'voice' ? '上传中...' : '上传语音贺卡'}
            <input type="file" accept="audio/webm,audio/ogg,audio/mpeg,audio/wav,audio/mp4" className="hidden"
              disabled={working === 'voice'} onChange={uploadVoice} />
          </label>
          <button type="button" onClick={createFeedbackLink} disabled={working === 'link'}
            className="btn-primary text-sm py-2 px-4 flex items-center gap-2">
            <Link2 className="w-4 h-4" /> {working === 'link' ? '生成中...' : '生成收礼方链接'}
          </button>
        </div>
        {feedbackLink && (
          <div className="mt-4 rounded-xl border border-primary-100 dark:border-primary-900 p-3">
            {feedbackLink.qrCodeUrl && (
              <Image src={feedbackLink.qrCodeUrl} alt="收礼方反馈二维码" width={144} height={144}
                unoptimized className="w-36 h-36 mx-auto mb-3" />
            )}
            <div className="flex gap-2">
              <input readOnly value={feedbackLink.url} className="input-field text-xs flex-1" />
              <button type="button" className="btn-outline px-3" title="复制链接"
                onClick={() => navigator.clipboard.writeText(feedbackLink.url).then(() => toast.success('链接已复制')).catch(() => toast.error('复制失败'))}>
                <Copy className="w-4 h-4" />
              </button>
            </div>
            <p className="text-xs text-gray-400 mt-2">有效至 {feedbackLink.expiresAt?.replace('T', ' ')}</p>
          </div>
        )}
      </div>

      <div className="card mt-4">
        <h2 className="font-semibold text-gray-900 dark:text-white mb-3">反馈记录</h2>
        <p className="text-xs text-gray-400 mb-3">这里提交的是送礼方备注；收礼方身份只能通过上方一次性链接写入。</p>
        <textarea value={feedbackContent} onChange={event => setFeedbackContent(event.target.value.slice(0, 1000))}
          className="input-field mb-3" rows={2} placeholder="记录这次送礼体验..." />
        <button type="button" onClick={submitFeedback} disabled={working === 'feedback'}
          className="btn-primary text-sm py-1.5 px-6">{working === 'feedback' ? '提交中...' : '提交反馈'}</button>
        {feedbacks.length > 0 && (
          <div className="mt-4 space-y-2">
            {feedbacks.map((feedback: any) => (
              <div key={feedback.id} className="border-t border-gray-100 dark:border-gray-800 pt-2">
                <span className={`text-xs px-2 py-0.5 rounded-full mr-2 ${feedback.role === 'receiver' ? 'bg-rose-50 text-rose-600' : 'bg-primary-50 text-primary-600'}`}>
                  {feedback.role === 'receiver' ? '收礼方' : '送礼方'}
                </span>
                <span className="text-sm text-gray-700 dark:text-gray-200">{feedback.content}</span>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
