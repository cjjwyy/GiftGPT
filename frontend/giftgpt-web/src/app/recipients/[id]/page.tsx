'use client';

import { useState, useEffect } from 'react';
import { recipientApi } from '@/lib/api';
import { useParams, useRouter } from 'next/navigation';
import { Loading } from '@/components/Loading';
import { toast } from 'react-hot-toast';
import Link from 'next/link';

const TAG_OPTIONS = [
  '开朗', '文艺', '极客', '养生派', '摄影', '户外', '音乐', '运动',
  '美食', '咖啡', '旅行', '阅读', '动漫', '游戏', '宠物', '科技',
  '时尚', '简约', '复古', '浪漫', '艺术', '理性',
];

export default function RecipientDetailPage() {
  const params = useParams();
  const router = useRouter();
  const id = Number(params.id);
  const [detail, setDetail] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState(false);
  const [name, setName] = useState('');
  const [relation, setRelation] = useState('');
  const [tags, setTags] = useState<string[]>([]);

  useEffect(() => {
    recipientApi.get(id).then(d => {
      setDetail(d);
      setName(d.name); setRelation(d.relation || ''); setTags(d.tags || []);
      setLoading(false);
    }).catch(() => setLoading(false));
  }, [id]);

  const toggleTag = (tag: string) => {
    setTags(prev => prev.includes(tag) ? prev.filter(t => t !== tag) : [...prev, tag]);
  };

  const onSave = async () => {
    try {
      await recipientApi.update(id, { name, relation, tags });
      setEditing(false);
      toast.success('已更新');
    } catch (err: any) { toast.error(err.message); }
  };

  if (loading) return <Loading />;
  if (!detail) return <div className="text-center py-20 text-gray-500 dark:text-gray-400">画像不存在</div>;

  return (
    <div className="max-w-2xl mx-auto px-4 py-10">
      <h1 className="text-2xl font-bold text-gray-900 dark:text-white mb-6">收礼人画像详情</h1>
      <div className="card space-y-4">
        <div>
          <label className="text-sm text-gray-500 dark:text-gray-400">姓名</label>
          {editing ? (
            <input className="input-field mt-1" value={name} onChange={e => setName(e.target.value)} />
          ) : (
            <p className="text-lg font-semibold">{detail.name}</p>
          )}
        </div>
        <div>
          <label className="text-sm text-gray-500 dark:text-gray-400">关系</label>
          {editing ? (
            <input className="input-field mt-1" value={relation} onChange={e => setRelation(e.target.value)} />
          ) : (
            <p>{detail.relation || '-'}</p>
          )}
        </div>
        <div>
          <label className="text-sm text-gray-500 dark:text-gray-400 mb-2 block">性格标签</label>
          {editing ? (
            <div className="flex flex-wrap gap-2">
              {TAG_OPTIONS.map(t => (
                <button key={t} type="button"
                  className={tags.includes(t) ? 'tag-selected' : 'tag cursor-pointer hover:bg-primary-100'}
                  onClick={() => toggleTag(t)}>{t}</button>
              ))}
            </div>
          ) : (
            <div className="flex flex-wrap gap-1">
              {detail.tags?.map((t: string) => <span key={t} className="tag">{t}</span>)}
              {(!detail.tags || detail.tags.length === 0) && <span className="text-gray-400 dark:text-gray-500">暂无标签</span>}
            </div>
          )}
        </div>
        {detail.personalityDesc && (
          <div>
            <label className="text-sm text-gray-500 dark:text-gray-400">AI 分析</label>
            <p className="text-gray-700 dark:text-gray-200 mt-1">{detail.personalityDesc}</p>
          </div>
        )}

        <div className="flex gap-3 pt-4 border-t">
          {editing ? (
            <>
              <button onClick={onSave} className="btn-primary">保存</button>
              <button onClick={() => setEditing(false)} className="btn-outline">取消</button>
            </>
          ) : (
            <>
              <button onClick={() => setEditing(true)} className="btn-primary">编辑画像</button>
              <Link href={`/recommend?recipientId=${id}`} className="btn-outline">为此人选礼物</Link>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
