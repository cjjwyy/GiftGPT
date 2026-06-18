'use client';

import { useState, useEffect } from 'react';
import { recipientApi } from '@/lib/api';
import { Recipient } from '@/types';
import Link from 'next/link';
import { Plus, User, Pencil, Trash2 } from 'lucide-react';
import { Loading } from '@/components/Loading';
import { toast } from 'react-hot-toast';

export default function RecipientsPage() {
  const [recipients, setRecipients] = useState<Recipient[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    recipientApi.list()
      .then(d => { setRecipients(d.records || []); setLoading(false); })
      .catch(() => { setLoading(false); toast.error('加载失败，请先登录'); });
  }, []);

  const handleDelete = async (id: number) => {
    if (!confirm('确定删除该收礼人画像吗？')) return;
    await recipientApi.delete(id);
    setRecipients(prev => prev.filter(r => r.id !== id));
    toast.success('已删除');
  };

  if (loading) return <Loading />;

  return (
    <div className="max-w-5xl mx-auto px-4 py-10">
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">收礼人画像</h1>
          <p className="text-gray-500 mt-1">管理你的收礼人，让AI更精准推荐</p>
        </div>
        <Link href="/recipients/new" className="btn-primary inline-flex items-center gap-2">
          <Plus className="w-4 h-4" /> 新建画像
        </Link>
      </div>

      {recipients.length === 0 ? (
        <div className="card text-center py-20">
          <User className="w-16 h-16 text-gray-300 mx-auto mb-4" />
          <h3 className="text-lg font-medium text-gray-500 mb-2">还没有收礼人画像</h3>
          <p className="text-gray-400 mb-4">添加第一位收礼人，开始AI选礼之旅</p>
          <Link href="/recipients/new" className="btn-primary">创建第一个画像</Link>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {recipients.map(r => (
            <div key={r.id} className="card hover:shadow-md transition-all">
              <div className="flex items-start justify-between mb-3">
                <div className="w-12 h-12 bg-gradient-to-br from-primary-400 to-rose-400 rounded-xl flex items-center justify-center text-white font-bold text-lg">
                  {r.name[0]}
                </div>
                <div className="flex gap-1">
                  <Link href={`/recipients/${r.id}`} className="p-1.5 hover:bg-gray-100 rounded-lg"><Pencil className="w-4 h-4 text-gray-400" /></Link>
                  <button onClick={() => handleDelete(r.id)} className="p-1.5 hover:bg-red-50 rounded-lg"><Trash2 className="w-4 h-4 text-gray-400" /></button>
                </div>
              </div>
              <h3 className="font-semibold text-gray-800">{r.name}</h3>
              {r.relation && <p className="text-sm text-gray-500">{r.relation}</p>}
              <Link href={`/recommend?recipientId=${r.id}`} className="btn-primary block text-center mt-4 text-sm">
                为此人选礼物
              </Link>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
