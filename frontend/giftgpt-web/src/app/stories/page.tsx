'use client';

import { useState, useEffect } from 'react';
import { storyApi } from '@/lib/api';
import { StoryItem } from '@/types';
import { Loading } from '@/components/Loading';
import { Heart, MessageSquare, Send, X } from 'lucide-react';
import { toast } from 'react-hot-toast';
import Link from 'next/link';

interface Reply {
  id: number;
  storyId: number;
  userId: number;
  content: string;
  createTime?: string;
}

export default function StoriesPage() {
  const [stories, setStories] = useState<StoryItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [replyOpen, setReplyOpen] = useState<number | null>(null);
  const [replyText, setReplyText] = useState('');
  const [replies, setReplies] = useState<Record<number, Reply[]>>({});
  const [replyLoading, setReplyLoading] = useState(false);

  useEffect(() => {
    storyApi.list().then(d => { setStories(d.records || []); setLoading(false); }).catch(() => setLoading(false));
  }, []);

  const handleLike = async (id: number) => {
    try {
      const res = await storyApi.like(id);
      setStories(prev => prev.map(s => s.id === id ? { ...s, likes: (res as any).likes } : s));
      toast.success('已点赞');
    } catch (err: any) {
      if (err.message?.includes('already liked') || err.message?.includes('已经')) {
        // Unlike instead
        try {
          const res = await storyApi.unlike(id);
          setStories(prev => prev.map(s => s.id === id ? { ...s, likes: (res as any).likes } : s));
          toast.success('已取消点赞');
        } catch { toast.error('操作失败'); }
      } else {
        toast.error('请先登录');
      }
    }
  };

  const toggleReplies = async (storyId: number) => {
    if (replyOpen === storyId) {
      setReplyOpen(null);
      return;
    }
    setReplyOpen(storyId);
    if (!replies[storyId]) {
      try {
        const data = await storyApi.getReplies(storyId);
        setReplies(prev => ({ ...prev, [storyId]: data as Reply[] || [] }));
      } catch { toast.error('加载回复失败'); }
    }
  };

  const submitReply = async (storyId: number) => {
    if (!replyText.trim()) { toast.error('请输入回复内容'); return; }
    setReplyLoading(true);
    try {
      const newReply = await storyApi.addReply(storyId, { content: replyText.trim() });
      setReplies(prev => ({
        ...prev,
        [storyId]: [...(prev[storyId] || []), newReply as Reply],
      }));
      setReplyText('');
      toast.success('回复成功');
    } catch (err: any) {
      toast.error(err.message || '回复失败');
    } finally {
      setReplyLoading(false);
    }
  };

  if (loading) return <Loading />;

  return (
    <div className="max-w-4xl mx-auto px-4 py-10">
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">礼物故事社区</h1>
          <p className="text-gray-500 dark:text-gray-400 mt-1">看看别人如何用礼物表达心意</p>
        </div>
        <Link href="/stories/new" className="btn-primary">分享我的故事</Link>
      </div>
      <div className="space-y-4">
        {stories.map(s => (
          <div key={s.id} className="card">
            <div className="flex items-center gap-3 mb-3">
              <div className="w-10 h-10 bg-primary-100 dark:bg-primary-900/30 rounded-full flex items-center justify-center text-primary-600 dark:text-primary-400 font-bold">
                {s.isAnonymous ? '匿' : (s as any).nickname?.[0] || 'U'}
              </div>
              <div>
                <p className="font-medium text-gray-800 dark:text-gray-100">
                  {s.isAnonymous ? '匿名用户' : (s as any).nickname || `用户${s.userId}`}
                </p>
                <p className="text-xs text-gray-400 dark:text-gray-500">{s.createTime}</p>
              </div>
            </div>
            <h3 className="font-semibold text-gray-900 dark:text-white mb-2">{s.title}</h3>
            <p className="text-gray-600 dark:text-gray-300 leading-relaxed">{s.content}</p>
            <div className="flex items-center gap-4 mt-4 pt-3 border-t border-gray-50 dark:border-gray-800">
              <button onClick={() => handleLike(s.id)} className="flex items-center gap-1 text-gray-400 dark:text-gray-500 hover:text-rose-500 transition-colors">
                <Heart className="w-4 h-4" /> {s.likes}
              </button>
              <button onClick={() => toggleReplies(s.id)} className="flex items-center gap-1 text-gray-400 dark:text-gray-500 hover:text-primary-500 transition-colors">
                <MessageSquare className="w-4 h-4" /> 评论 {(replies[s.id] || []).length > 0 ? `(${replies[s.id].length})` : ''}
              </button>
            </div>

            {replyOpen === s.id && (
              <div className="mt-4 pt-4 border-t border-gray-50 dark:border-gray-800">
                {(replies[s.id] || []).length > 0 ? (
                  <div className="space-y-2 mb-4">
                    {(replies[s.id] || []).map(r => (
                      <div key={r.id} className="bg-gray-50 dark:bg-gray-800 rounded-lg px-4 py-2">
                        <span className="text-xs text-gray-500 dark:text-gray-400">用户{r.userId}</span>
                        <p className="text-sm text-gray-700 dark:text-gray-300 mt-0.5">{r.content}</p>
                      </div>
                    ))}
                  </div>
                ) : (
                  <p className="text-sm text-gray-400 dark:text-gray-500 mb-4">暂无评论，来做第一个评论者吧</p>
                )}
                <div className="flex gap-2">
                  <input
                    className="input-field flex-1 text-sm"
                    placeholder="写下你的评论..."
                    value={replyText}
                    onChange={e => setReplyText(e.target.value)}
                    onKeyDown={e => { if (e.key === 'Enter') submitReply(s.id); }}
                  />
                  <button
                    onClick={() => submitReply(s.id)}
                    disabled={replyLoading}
                    className="btn-primary text-sm py-2 px-4 flex items-center gap-1"
                  >
                    <Send className="w-4 h-4" />
                  </button>
                </div>
              </div>
            )}
          </div>
        ))}
        {stories.length === 0 && (
          <div className="card text-center py-20 text-gray-400 dark:text-gray-500">还没有故事，来做第一个分享者吧！</div>
        )}
      </div>
    </div>
  );
}
