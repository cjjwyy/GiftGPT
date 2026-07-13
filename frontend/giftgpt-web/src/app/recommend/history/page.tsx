'use client';

import { useState, useEffect } from 'react';
import { recommendApi } from '@/lib/api';
import { GiftCard } from '@/components/GiftCard';
import { Loading } from '@/components/Loading';
import { Gift, ChevronLeft, Calendar, Wallet, Pencil, Trash2, CheckSquare, X } from 'lucide-react';
import Link from 'next/link';
import { toast } from 'react-hot-toast';

const OCCASION_LABELS: Record<string, string> = {
  birthday: '生日', anniversary: '纪念日', valentines: '情人节',
  festival: '节庆', graduation: '毕业', proposal: '求婚', thank_you: '感谢', other: '其他',
};

export default function HistoryPage() {
  const [records, setRecords] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(1);
  const [total, setTotal] = useState(0);
  const [detail, setDetail] = useState<any>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [editMode, setEditMode] = useState(false);
  const [selected, setSelected] = useState<Set<number>>(new Set());

  const loadHistory = async (p: number) => {
    setLoading(true);
    setPage(p);
    try {
      const res = await recommendApi.history(p, 10);
      setRecords(res.records || []);
      setTotal(res.total || 0);
    } catch { setRecords([]); }
    setLoading(false);
  };

  useEffect(() => { loadHistory(1); }, []);

  const viewDetail = async (id: number) => {
    if (editMode) return;
    setDetailLoading(true);
    setDetail(null);
    try {
      const res = await recommendApi.historyDetail(id);
      setDetail(res);
    } catch { setDetail(null); }
    setDetailLoading(false);
  };

  const toggleSelect = (id: number) => {
    setSelected(prev => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id); else next.add(id);
      return next;
    });
  };

  const toggleSelectAll = () => {
    if (selected.size === records.length) {
      setSelected(new Set());
    } else {
      setSelected(new Set(records.map(r => r.id)));
    }
  };

  const deleteSelected = async () => {
    if (selected.size === 0) { toast.error('请先选择记录'); return; }
    try {
      await recommendApi.deleteHistory(Array.from(selected));
      toast.success(`已删除 ${selected.size} 条记录`);
      setSelected(new Set());
      setEditMode(false);
      loadHistory(1);
    } catch { toast.error('删除失败'); }
  };

  if (detailLoading || (detail && !detail.items)) {
    return (
      <div className="max-w-6xl mx-auto px-4 py-10">
        <Loading />
      </div>
    );
  }

  if (detail) {
    return (
      <div className="max-w-6xl mx-auto px-4 py-10">
        <button onClick={() => setDetail(null)} className="flex items-center gap-1 text-sm text-gray-500 hover:text-gray-700 dark:text-gray-400 mb-6">
          <ChevronLeft className="w-4 h-4" /> 返回历史记录
        </button>
        <div>
          <div className="mb-6">
            <p className="text-lg font-medium text-gray-800 dark:text-gray-100 mb-1">为 {detail.recipientName} 推荐的礼物</p>
            <p className="text-sm text-gray-500 dark:text-gray-400">{detail.summary}</p>
          </div>
          {detail.items?.length ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
              {detail.items.map((item: any, i: number) => (
                <GiftCard key={i} productId={item.productId} productName={item.productName}
                  price={item.price} imageUrl={item.imageUrl} platform={item.platform}
                  platformUrl={item.platformUrl}
                  reason={item.reason} matchTags={item.matchTags} score={item.score}
                  reasoningChain={item.reasoningChain} />
              ))}
            </div>
          ) : (
            <div className="card text-center py-16 text-gray-400 dark:text-gray-500">暂无匹配商品</div>
          )}
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto px-4 py-10">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">推荐历史记录</h1>
        <div className="flex items-center gap-3">
          {editMode ? (
            <>
              <button onClick={toggleSelectAll}
                className="text-sm text-primary-500 hover:text-primary-600 flex items-center gap-1">
                <CheckSquare className="w-4 h-4" /> {selected.size === records.length && records.length > 0 ? '取消全选' : '全选'}
              </button>
              <button onClick={deleteSelected} disabled={selected.size === 0}
                className="text-sm text-red-500 hover:text-red-600 flex items-center gap-1 disabled:opacity-40">
                <Trash2 className="w-4 h-4" /> 删除({selected.size})
              </button>
              <button onClick={() => { setEditMode(false); setSelected(new Set()); }}
                className="text-sm text-gray-500 hover:text-gray-700 dark:text-gray-400 flex items-center gap-1">
                <X className="w-4 h-4" /> 完成
              </button>
            </>
          ) : (
            <>
              <button onClick={() => setEditMode(true)}
                className="text-sm text-gray-500 hover:text-gray-700 dark:text-gray-400 flex items-center gap-1">
                <Pencil className="w-4 h-4" /> 编辑
              </button>
              <Link href="/recommend" className="text-sm text-primary-500 hover:text-primary-600">
                返回推荐
              </Link>
            </>
          )}
        </div>
      </div>

      {loading ? <Loading /> : records.length === 0 ? (
        <div className="text-center py-20 text-gray-400 dark:text-gray-500">
          <Gift className="w-16 h-16 mx-auto mb-4" />
          <p className="text-lg">暂无推荐记录</p>
          <p className="text-sm mt-1">去生成一次智能推荐吧</p>
        </div>
      ) : (
        <>
          <div className="space-y-3">
            {records.map((r: any) => (
              <div key={r.id}
                onClick={() => !editMode && viewDetail(r.id)}
                className={`card w-full text-left transition-all flex items-center gap-4 p-4 ${editMode ? 'cursor-default' : 'hover:shadow-md cursor-pointer'}`}>
                {editMode && (
                  <input
                    type="checkbox"
                    checked={selected.has(r.id)}
                    onChange={() => toggleSelect(r.id)}
                    onClick={e => e.stopPropagation()}
                    className="w-5 h-5 rounded border-gray-300 text-primary-500 focus:ring-primary-500 shrink-0 cursor-pointer"
                  />
                )}
                <div className="w-10 h-10 rounded-full bg-primary-50 dark:bg-primary-900/30 flex items-center justify-center shrink-0">
                  <Gift className="w-5 h-5 text-primary-500" />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="font-medium text-gray-800 dark:text-gray-100 truncate">
                    {OCCASION_LABELS[r.scene] || r.scene || '推荐'}
                  </p>
                  <div className="flex items-center gap-3 text-xs text-gray-400 dark:text-gray-500 mt-1">
                    <span className="flex items-center gap-1"><Calendar className="w-3 h-3" />{r.createTime?.substring(0, 16).replace('T', ' ')}</span>
                    <span className="flex items-center gap-1"><Wallet className="w-3 h-3" />¥{r.budget}</span>
                  </div>
                </div>
                {r.feedback && (
                  <span className="text-xs px-2 py-0.5 rounded-full bg-green-50 text-green-600 dark:bg-green-900/30 dark:text-green-400 shrink-0">
                    {r.feedback}
                  </span>
                )}
              </div>
            ))}
          </div>
          {total > 10 && (
            <div className="flex items-center justify-center gap-4 mt-6">
              <button onClick={() => loadHistory(page - 1)} disabled={page <= 1}
                className="btn-outline text-sm py-1.5 px-4 disabled:opacity-40">上一页</button>
              <span className="text-sm text-gray-400">{page} / {Math.ceil(total / 10)}</span>
              <button onClick={() => loadHistory(page + 1)} disabled={page * 10 >= total}
                className="btn-outline text-sm py-1.5 px-4 disabled:opacity-40">下一页</button>
            </div>
          )}
        </>
      )}
    </div>
  );
}
