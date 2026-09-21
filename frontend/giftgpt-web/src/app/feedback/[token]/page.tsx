'use client';

import { useEffect, useState } from 'react';
import { useParams } from 'next/navigation';
import { publicFeedbackApi } from '@/lib/api';
import { Loading } from '@/components/Loading';
import { Gift, Heart } from 'lucide-react';
import toast from 'react-hot-toast';
import { OCCASION_LABELS } from '@/lib/occasions';

export default function ReceiverFeedbackPage() {
  const params = useParams();
  const token = typeof params.token === 'string' ? params.token : '';
  const [gift, setGift] = useState<any>(null);
  const [error, setError] = useState('');
  const [content, setContent] = useState('');
  const [type, setType] = useState('thanks');
  const [isPublic, setIsPublic] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!token) {
      setError('反馈链接无效');
      return;
    }
    publicFeedbackApi.get(token).then(setGift)
      .catch((err: Error) => setError(err.message || '反馈链接无效或已过期'));
  }, [token]);

  const submit = async () => {
    if (!content.trim()) {
      toast.error('请写下你的反馈');
      return;
    }
    setSubmitting(true);
    try {
      await publicFeedbackApi.submit(token, { content, type, isPublic: isPublic ? 1 : 0 });
      setGift((current: any) => ({ ...current, submitted: true }));
      toast.success('谢谢，你的心意已送达');
    } catch (err: any) {
      toast.error(err?.message || '提交失败');
    } finally {
      setSubmitting(false);
    }
  };

  if (error) {
    return (
      <div className="max-w-lg mx-auto px-4 py-24 text-center">
        <Gift className="w-12 h-12 mx-auto text-gray-300 mb-4" />
        <h1 className="text-xl font-semibold">无法打开这份心意</h1>
        <p className="text-gray-500 mt-2">{error}</p>
      </div>
    );
  }
  if (!gift) return <Loading />;

  return (
    <div className="max-w-lg mx-auto px-4 py-12">
      <div className="card text-center">
        <Heart className="w-12 h-12 text-rose-500 mx-auto mb-3" />
        <p className="text-sm text-gray-400">{OCCASION_LABELS[gift.occasion] || gift.occasion || '一份特别的礼物'}</p>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white mt-1">送给 {gift.recipientName} 的心意</h1>
        {gift.productName && <p className="text-sm text-gray-500 mt-2">礼物：{gift.productName}</p>}
        {gift.greetingContent && (
          <p className="mt-5 rounded-xl bg-rose-50 dark:bg-rose-900/20 px-4 py-4 text-left whitespace-pre-line text-gray-700 dark:text-gray-200">
            {gift.greetingContent}
          </p>
        )}
        {gift.greetingVoiceUrl && (
          <audio controls src={gift.greetingVoiceUrl} className="w-full mt-4">
            您的浏览器不支持音频播放。
          </audio>
        )}
      </div>

      {gift.submitted ? (
        <div className="card mt-4 text-center py-10">
          <Heart className="w-10 h-10 text-rose-400 mx-auto mb-3" />
          <p className="font-semibold text-gray-800 dark:text-gray-100">反馈已提交，谢谢你的回应</p>
          <p className="text-sm text-gray-400 mt-1">为保护隐私，这个链接不能再次提交。</p>
        </div>
      ) : (
        <div className="card mt-4">
          <h2 className="font-semibold mb-3">写下你的感受</h2>
          <select value={type} onChange={event => setType(event.target.value)} className="input-field mb-3">
            <option value="thanks">感谢</option>
            <option value="like">很喜欢</option>
            <option value="suggestion">建议</option>
            <option value="comment">其他</option>
          </select>
          <textarea value={content} onChange={event => setContent(event.target.value.slice(0, 1000))}
            className="input-field" rows={5} placeholder="这份礼物给你的感受是..." />
          <label className="flex items-center gap-2 text-sm text-gray-500 mt-3">
            <input type="checkbox" checked={isPublic} onChange={event => setIsPublic(event.target.checked)} />
            允许匿名展示在礼物故事中
          </label>
          <button type="button" onClick={submit} disabled={submitting} className="btn-primary w-full mt-4">
            {submitting ? '提交中...' : '提交反馈'}
          </button>
        </div>
      )}
    </div>
  );
}
