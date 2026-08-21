'use client';

import { useState, useEffect } from 'react';
import { recipientApi } from '@/lib/api';
import { useParams, useRouter } from 'next/navigation';
import { Loading } from '@/components/Loading';
import { toast } from 'react-hot-toast';
import Link from 'next/link';
import TagPicker from '@/components/TagPicker';
import {
  SUPPLEMENT_TAGS,
  buildTagSupplements,
  formatSupplementText,
} from '@/lib/tagOptions';

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
  const [supplementTexts, setSupplementTexts] = useState<Record<string, string>>({});

  useEffect(() => {
    recipientApi.get(id).then(d => {
      setDetail(d);
      setName(d.name); setRelation(d.relation || ''); setTags(d.tags || []);
      const loaded: Record<string, string> = {};
      const tagSupplements = d.tagSupplements || {};
      for (const [tag, items] of Object.entries(tagSupplements)) {
        loaded[tag] = formatSupplementText(items as string[]);
      }
      setSupplementTexts(loaded);
      setLoading(false);
    }).catch(() => setLoading(false));
  }, [id]);

  const handleTagsChange = (nextTags: string[]) => {
    setTags(nextTags);
    setSupplementTexts(prev => {
      const next = { ...prev };
      for (const tag of SUPPLEMENT_TAGS) {
        if (!nextTags.includes(tag)) delete next[tag];
      }
      return next;
    });
  };

  const handleSupplementChange = (tag: string, value: string) => {
    setSupplementTexts(prev => ({ ...prev, [tag]: value }));
  };

  const onSave = async () => {
    const missingSupplements = tags.filter(
      tag => SUPPLEMENT_TAGS.includes(tag) && !(supplementTexts[tag] || '').trim()
    );
    if (missingSupplements.length > 0) {
      toast.error(`请填写补充项：${missingSupplements.join('、')}`);
      return;
    }
    try {
      await recipientApi.update(id, {
        name, relation, tags,
        tagSupplements: buildTagSupplements(tags, supplementTexts),
      });
      setEditing(false);
      toast.success('已更新');
    } catch (err: any) { toast.error(err.message); }
  };

  if (loading) return <Loading />;
  if (!detail) return (
    <div className="text-center py-20 text-gray-500 dark:text-gray-400">
      画像不存在或已删除<br/>
      <Link href="/recipients" className="text-primary-500">返回画像列表</Link>
    </div>
  );

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
          <label className="text-sm text-gray-500 dark:text-gray-400 mb-2 block">性格/兴趣标签</label>
          {editing ? (
            <TagPicker
              selectedTags={tags}
              supplementTexts={supplementTexts}
              onTagsChange={handleTagsChange}
              onSupplementChange={handleSupplementChange}
            />
          ) : (
            <div className="space-y-2">
              <div className="flex flex-wrap gap-1">
                {detail.tags?.map((t: string) => <span key={t} className="tag">{t}</span>)}
                {(!detail.tags || detail.tags.length === 0) && <span className="text-gray-400 dark:text-gray-500">暂无标签</span>}
              </div>
              {Object.keys(detail.tagSupplements || {}).length > 0 && (
                <div className="text-sm text-gray-600 dark:text-gray-300 space-y-1">
                  {Object.entries(detail.tagSupplements).map(([tag, items]: any) => (
                    <p key={tag}>{tag}：{formatSupplementText(items)}</p>
                  ))}
                </div>
              )}
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
