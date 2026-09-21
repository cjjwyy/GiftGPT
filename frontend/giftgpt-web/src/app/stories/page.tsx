'use client';

import { useState, useEffect } from 'react';
import { storyApi } from '@/lib/api';
import { StoryItem } from '@/types';
import { Loading } from '@/components/Loading';
import { Heart, MessageSquare, Send } from 'lucide-react';
import { toast } from 'react-hot-toast';
import Link from 'next/link';
import { relativeTime } from '@/lib/utils';
import ReportStory from '@/components/ReportStory';

interface Reply {
  id: number;
  storyId: number;
  userId: number;
  content: string;
  createTime?: string;
  nickname?: string;
}

function avatarText(s: StoryItem) {
  if (s.isAnonymous) return '匿';
  return (s.nickname || `用户${s.userId}`)?.[0]?.toUpperCase() || 'U';
}

export default function StoriesPage() {
  const [canModerate, setCanModerate] = useState(false);
  useEffect(() => { storyApi.moderationCapabilities().then(r => setCanModerate(r.canModerate)).catch(() => {}); }, []);
  const [stories, setStories] = useState<StoryItem[]>([]);
  const [page, setPage] = useState(1);
  const [hasMore, setHasMore] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [loading, setLoading] = useState(true);
  const [replyOpen, setReplyOpen] = useState<number | null>(null);
  const [replyText, setReplyText] = useState('');
  const [replies, setReplies] = useState<Record<number, Reply[]>>({});
  const [replyPages, setReplyPages] = useState<Record<number, { current: number; pages: number; total: number }>>({});
  const [moreRepliesBusy, setMoreRepliesBusy] = useState(false);
  const [replyLoading, setReplyLoading] = useState(false);
  const [busy, setBusy] = useState<number | null>(null);

  const fetchStories = () => {
    storyApi.list()
      .then(d => { setStories(d.records || []); setPage(1); setHasMore(d.pages > 1); setLoading(false); })
      .catch((err: any) => { setStories([]); setLoading(false); toast.error(err?.message || '加载故事失败'); });
  };

  useEffect(() => { fetchStories(); }, []);
  const loadMore = async () => {
    if (loadingMore) return;
    setLoadingMore(true);
    try {
      const data = await storyApi.list(page + 1);
      setStories(prev => Array.from(new Map([...prev, ...data.records].map(s => [s.id, s])).values()));
      setPage(page + 1); setHasMore(page + 1 < data.pages);
    } catch { toast.error('加载失败，请重试'); }
    finally { setLoadingMore(false); }
  };

  const handleLike = async (s: StoryItem) => {
    if (busy) return;
    setBusy(s.id);
    const wasLiked = !!s.liked;
    try {
      const res = wasLiked
        ? await storyApi.unlike(s.id)
        : await storyApi.like(s.id);
      setStories(prev => prev.map(x => x.id === s.id
        ? { ...x, likes: (res as any).likes ?? x.likes, liked: wasLiked ? 0 : 1 }
        : x));
    } catch (err: any) {
      toast.error(err.message?.includes('未登录') ? '请先登录' : (err.message || '操作失败'));
    } finally {
      setBusy(null);
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
        setReplies(prev => ({ ...prev, [storyId]: data.records || [] }));
        setReplyPages(prev => ({ ...prev, [storyId]: { current: data.current, pages: data.pages, total: data.total } }));
      } catch { toast.error('加载评论失败'); }
    }
  };

  const submitReply = async (storyId: number) => {
    if (replyLoading) return;
    if (!replyText.trim()) { toast.error('请输入评论内容'); return; }
    setReplyLoading(true);
    try {
      await storyApi.addReply(storyId, { content: replyText.trim() });
      const data = await storyApi.getReplies(storyId);
      setReplies(prev => ({ ...prev, [storyId]: data.records || [] }));
      setReplyPages(prev => ({ ...prev, [storyId]: { current: data.current, pages: data.pages, total: data.total } }));
      setReplyText('');
      toast.success('评论成功');
    } catch (err: any) {
      toast.error(err.message?.includes('未登录') ? '请先登录后再评论' : (err.message || '评论失败'));
    } finally {
      setReplyLoading(false);
    }
  };

  if (loading) return <Loading />;

  return (
    <div className="max-w-3xl mx-auto px-4 py-10">
      <div className="flex items-end justify-between mb-8">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">礼物故事社区</h1>
          <p className="text-gray-500 dark:text-gray-400 mt-1 text-sm">看看别人如何用礼物表达心意</p>
        </div>
        <Link href="/stories/new" className="btn-primary text-sm">分享我的故事</Link>
      </div>

      {canModerate && <Link href="/stories/moderation" className="btn-outline inline-block mb-4">处理社区举报</Link>}
      <div className="space-y-5">
        {stories.map(s => {
          const name = s.isAnonymous ? '匿名用户' : (s.nickname || `用户${s.userId}`);
          const list = replies[s.id] || [];
          return (
            <article key={s.id} className="card hover:shadow-lift">
              <div className="flex items-center gap-3 mb-4">
                <div className="w-10 h-10 bg-gradient-to-br from-primary-400 to-rose-400 rounded-full flex items-center justify-center text-white font-semibold">
                  {avatarText(s)}
                </div>
                <div className="min-w-0">
                  <p className="font-medium text-gray-800 dark:text-gray-100 truncate">{name}</p>
                  <p className="text-xs text-gray-400 dark:text-gray-500">{relativeTime(s.createTime)}</p>
                </div>
              </div>

              <h3 className="font-semibold text-gray-900 dark:text-white mb-1.5">{s.title}</h3>
              {(s as StoryItem & { canDelete?: boolean }).canDelete && <button className="text-xs text-rose-500" onClick={async () => {
                if (!confirm('删除这篇故事？')) return;
                try { await storyApi.delete(s.id); setStories(prev => prev.filter(x => x.id !== s.id)); }
                catch { toast.error('删除失败'); }
              }}>删除我的故事</button>}
              <p className="text-gray-600 dark:text-gray-300 leading-relaxed whitespace-pre-wrap">{s.content}</p>
              <ReportStory storyId={s.id} />

              <div className="flex items-center gap-5 mt-5 pt-4 border-t border-gray-100 dark:border-gray-800">
                <button
                  onClick={() => handleLike(s)}
                  disabled={busy === s.id}
                  className={`flex items-center gap-1.5 text-sm transition-colors disabled:opacity-50 ${s.liked ? 'text-rose-500' : 'text-gray-400 dark:text-gray-500 hover:text-rose-500'}`}
                >
                  <Heart className={`w-4 h-4 ${s.liked ? 'fill-rose-500' : ''}`} />
                  <span>{s.likes}</span>
                </button>
                <button
                  onClick={() => toggleReplies(s.id)}
                  className="flex items-center gap-1.5 text-sm text-gray-400 dark:text-gray-500 hover:text-primary-500 transition-colors"
                >
                  <MessageSquare className="w-4 h-4" />
                  <span>评论{replyPages[s.id] ? ` ${replyPages[s.id].total}` : ''}</span>
                </button>
              </div>

              {replyOpen === s.id && (
                <div className="mt-4 pt-4 border-t border-gray-100 dark:border-gray-800">
                  {list.length > 0 ? (
                    <div className="space-y-2.5 mb-4">
                      {list.map(r => (
                        <div key={r.id} className="flex gap-2.5">
                          <div className="w-7 h-7 shrink-0 bg-gray-100 dark:bg-gray-800 rounded-full flex items-center justify-center text-xs font-medium text-gray-500 dark:text-gray-400">
                            {(r.nickname || `用户${r.userId}`)?.[0]?.toUpperCase() || 'U'}
                          </div>
                          <div className="bg-gray-50 dark:bg-gray-800/60 rounded-xl px-3.5 py-2 flex-1">
                            <div className="flex items-center gap-2">
                              <span className="text-xs font-medium text-gray-600 dark:text-gray-300">{r.nickname || `用户${r.userId}`}</span>
                              <span className="text-[11px] text-gray-400 dark:text-gray-500">{relativeTime(r.createTime)}</span>
                            </div>
                            <p className="text-sm text-gray-700 dark:text-gray-300 mt-0.5">{r.content}</p>
                          </div>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <p className="text-sm text-gray-400 dark:text-gray-500 mb-4">暂无评论，来做第一个评论者吧</p>
                  )}
                  {replyPages[s.id]?.current < replyPages[s.id]?.pages && <button className="text-sm text-primary-500 mb-3" disabled={moreRepliesBusy} onClick={async () => {
                    setMoreRepliesBusy(true);
                    try {
                      const data = await storyApi.getReplies(s.id, replyPages[s.id].current + 1);
                      setReplies(prev => ({ ...prev, [s.id]: Array.from(new Map([...(prev[s.id] || []), ...data.records].map(r => [r.id, r])).values()) }));
                      setReplyPages(prev => ({ ...prev, [s.id]: { current: data.current, pages: data.pages, total: data.total } }));
                    } catch { toast.error('加载评论失败'); } finally { setMoreRepliesBusy(false); }
                  }}>加载更多评论</button>}
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
            </article>
          );
        })}

        {hasMore && <button className="btn-outline w-full" disabled={loadingMore} onClick={loadMore}>{loadingMore ? '加载中…' : '加载更多'}</button>}
        {stories.length === 0 && (
          <div className="card text-center py-20 text-gray-400 dark:text-gray-500">
            <MessageSquare className="w-12 h-12 mx-auto mb-3 opacity-40" />
            <p>还没有故事，来做第一个分享者吧</p>
          </div>
        )}
      </div>
    </div>
  );
}
