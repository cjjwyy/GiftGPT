'use client';

import { useState, useEffect } from 'react';
import { storyApi } from '@/lib/api';
import { StoryItem } from '@/types';
import { Loading } from '@/components/Loading';
import { Heart, MessageSquare } from 'lucide-react';
import { toast } from 'react-hot-toast';
import Link from 'next/link';

export default function StoriesPage() {
  const [stories, setStories] = useState<StoryItem[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    storyApi.list().then(d => { setStories(d.records || []); setLoading(false); }).catch(() => setLoading(false));
  }, []);

  const handleLike = async (id: number) => {
    try {
      const res = await storyApi.like(id);
      setStories(prev => prev.map(s => s.id === id ? { ...s, likes: (res as any).likes } : s));
      toast.success('已点赞');
    } catch { toast.error('请先登录'); }
  };

  if (loading) return <Loading />;

  return (
    <div className="max-w-4xl mx-auto px-4 py-10">
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">礼物故事社区</h1>
          <p className="text-gray-500 mt-1">看看别人如何用礼物表达心意</p>
        </div>
        <Link href="/stories/new" className="btn-primary">分享我的故事</Link>
      </div>
      <div className="space-y-4">
        {stories.map(s => (
          <div key={s.id} className="card">
            <div className="flex items-center gap-3 mb-3">
              <div className="w-10 h-10 bg-primary-100 rounded-full flex items-center justify-center text-primary-600 font-bold">
                {s.isAnonymous ? '匿' : 'U'}
              </div>
              <div>
                <p className="font-medium text-gray-800">{s.isAnonymous ? '匿名用户' : `用户${s.userId}`}</p>
                <p className="text-xs text-gray-400">{s.createTime}</p>
              </div>
            </div>
            <h3 className="font-semibold text-gray-900 mb-2">{s.title}</h3>
            <p className="text-gray-600 leading-relaxed">{s.content}</p>
            <div className="flex items-center gap-4 mt-4 pt-3 border-t border-gray-50">
              <button onClick={() => handleLike(s.id)} className="flex items-center gap-1 text-gray-400 hover:text-rose-500 transition-colors">
                <Heart className="w-4 h-4" /> {s.likes}
              </button>
              <button className="flex items-center gap-1 text-gray-400">
                <MessageSquare className="w-4 h-4" /> 评论
              </button>
            </div>
          </div>
        ))}
        {stories.length === 0 && (
          <div className="card text-center py-20 text-gray-400">还没有故事，来做第一个分享者吧！</div>
        )}
      </div>
    </div>
  );
}
