'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { storyApi } from '@/lib/api';
import { toast } from 'react-hot-toast';

export default function NewStoryPage() {
  const router = useRouter();
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [isAnonymous, setIsAnonymous] = useState(false);
  const [loading, setLoading] = useState(false);

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim()) { toast.error('请输入故事标题'); return; }
    if (!content.trim()) { toast.error('请输入故事内容'); return; }
    setLoading(true);
    try {
      await storyApi.create({ title, content, isAnonymous: isAnonymous ? 1 : 0 });
      toast.success('故事发布成功');
      router.push('/stories');
    } catch (err: any) {
      toast.error(err.message || '发布失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-2xl mx-auto px-4 py-10">
      <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100 mb-6">分享我的故事</h1>
      <form onSubmit={onSubmit} className="card space-y-5">
        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">标题 *</label>
          <input className="input-field" value={title} onChange={e => setTitle(e.target.value)} placeholder="给这个故事起个标题" />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">故事内容 *</label>
          <textarea className="input-field" rows={8} value={content} onChange={e => setContent(e.target.value)} placeholder="分享你的送礼故事...选了什么礼物？TA的反应如何？" />
        </div>
        <div className="flex items-center gap-3">
          <input type="checkbox" id="anon" checked={isAnonymous} onChange={e => setIsAnonymous(e.target.checked)} className="w-4 h-4 rounded border-gray-300 text-primary-500 focus:ring-primary-400" />
          <label htmlFor="anon" className="text-sm text-gray-600 dark:text-gray-400">匿名发布（其他人看不到你的身份）</label>
        </div>
        <button type="submit" disabled={loading} className="btn-primary w-full">
          {loading ? '发布中...' : '发布故事'}
        </button>
      </form>
    </div>
  );
}
