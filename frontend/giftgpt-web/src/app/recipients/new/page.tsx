'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { recipientApi } from '@/lib/api';
import { toast } from 'react-hot-toast';

const TAG_OPTIONS = [
  '开朗', '文艺', '极客', '养生派', '摄影', '户外', '音乐', '运动',
  '美食', '咖啡', '旅行', '阅读', '动漫', '游戏', '宠物', '科技',
  '时尚', '简约', '复古', '浪漫', '艺术', '理性',
];

export default function NewRecipientPage() {
  const router = useRouter();
  const [name, setName] = useState('');
  const [relation, setRelation] = useState('');
  const [gender, setGender] = useState(0);
  const [selectedTags, setSelectedTags] = useState<string[]>([]);
  const [note, setNote] = useState('');
  const [loading, setLoading] = useState(false);

  const toggleTag = (tag: string) => {
    setSelectedTags(prev => prev.includes(tag) ? prev.filter(t => t !== tag) : [...prev, tag]);
  };

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) { toast.error('请输入收礼人姓名'); return; }
    setLoading(true);
    try {
      await recipientApi.create({ name, relation, gender, tags: selectedTags, note });
      toast.success('画像创建成功');
      router.push('/recipients');
    } catch (err: any) {
      toast.error(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-2xl mx-auto px-4 py-10">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">创建收礼人画像</h1>
      <form onSubmit={onSubmit} className="card space-y-5">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">姓名 *</label>
          <input className="input-field" value={name} onChange={e => setName(e.target.value)} placeholder="收礼人的名字" />
        </div>
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">关系</label>
            <select className="input-field" value={relation} onChange={e => setRelation(e.target.value)}>
              <option value="">请选择</option>
              <option value="恋人">恋人</option>
              <option value="朋友">朋友</option>
              <option value="家人">家人</option>
              <option value="同事">同事</option>
              <option value="同学">同学</option>
              <option value="老师">老师</option>
              <option value="其他">其他</option>
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">性别</label>
            <select className="input-field" value={gender} onChange={e => setGender(Number(e.target.value))}>
              <option value={0}>未知</option>
              <option value={1}>男</option>
              <option value={2}>女</option>
            </select>
          </div>
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">性格标签 (可多选)</label>
          <div className="flex flex-wrap gap-2">
            {TAG_OPTIONS.map(tag => (
              <button key={tag} type="button"
                className={selectedTags.includes(tag) ? 'tag-selected' : 'tag cursor-pointer hover:bg-primary-100'}
                onClick={() => toggleTag(tag)}>
                {tag}
              </button>
            ))}
          </div>
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">备注</label>
          <textarea className="input-field" rows={3} value={note} onChange={e => setNote(e.target.value)} placeholder="任何额外信息..." />
        </div>
        <button type="submit" disabled={loading} className="btn-primary w-full">
          {loading ? '创建中...' : '创建画像'}
        </button>
      </form>
    </div>
  );
}
