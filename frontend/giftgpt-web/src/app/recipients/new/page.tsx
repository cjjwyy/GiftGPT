'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { recipientApi } from '@/lib/api';
import { toast } from 'react-hot-toast';
import TagPicker from '@/components/TagPicker';
import { SUPPLEMENT_TAGS, buildTagSupplements } from '@/lib/tagOptions';

const MBTI_OPTIONS = [
  'INTJ', 'INTP', 'ENTJ', 'ENTP', 'INFJ', 'INFP', 'ENFJ', 'ENFP',
  'ISTJ', 'ISFJ', 'ESTJ', 'ESFJ', 'ISTP', 'ISFP', 'ESTP', 'ESFP',
];

export default function NewRecipientPage() {
  const router = useRouter();
  const [name, setName] = useState('');
  const [relation, setRelation] = useState('');
  const [gender, setGender] = useState(0);
  const [mbti, setMbti] = useState('');
  const [personality, setPersonality] = useState('');
  const [selectedTags, setSelectedTags] = useState<string[]>([]);
  const [supplementTexts, setSupplementTexts] = useState<Record<string, string>>({});
  const [recentPurchases, setRecentPurchases] = useState('');
  const [note, setNote] = useState('');
  const [loading, setLoading] = useState(false);

  const handleTagsChange = (tags: string[]) => {
    setSelectedTags(tags);
    setSupplementTexts(prev => {
      const next = { ...prev };
      for (const tag of SUPPLEMENT_TAGS) {
        if (!tags.includes(tag)) delete next[tag];
      }
      return next;
    });
  };

  const handleSupplementChange = (tag: string, value: string) => {
    setSupplementTexts(prev => ({ ...prev, [tag]: value }));
  };

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) { toast.error('请输入收礼人姓名'); return; }
    const missingSupplements = selectedTags.filter(
      tag => SUPPLEMENT_TAGS.includes(tag) && !(supplementTexts[tag] || '').trim()
    );
    if (missingSupplements.length > 0) {
      toast.error(`请填写补充项：${missingSupplements.join('、')}`);
      return;
    }
    setLoading(true);
    try {
      await recipientApi.create({
        name, relation, gender, mbti, personality, tags: selectedTags,
        tagSupplements: buildTagSupplements(selectedTags, supplementTexts),
        recentPurchases, note,
      });
      toast.success('画像创建成功');
      setTimeout(() => router.push('/recipients'), 500);
    } catch (err: any) {
      toast.error(err.message || '创建失败');
      setLoading(false);
    }
  };

  return (
    <div className="max-w-2xl mx-auto px-4 py-10">
      <h1 className="text-2xl font-bold text-gray-900 dark:text-white mb-6">创建收礼人画像</h1>
      <form onSubmit={onSubmit} className="card space-y-5">
        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">姓名 *</label>
          <input className="input-field" value={name} onChange={e => setName(e.target.value)} placeholder="收礼人的名字" />
        </div>
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">关系</label>
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
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">性别</label>
            <select className="input-field" value={gender} onChange={e => setGender(Number(e.target.value))}>
              <option value={0}>未知</option>
              <option value={1}>男</option>
              <option value={2}>女</option>
            </select>
          </div>
        </div>
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">MBTI人格</label>
            <select className="input-field" value={mbti} onChange={e => setMbti(e.target.value)}>
              <option value="">请选择（可选）</option>
              {MBTI_OPTIONS.map(m => <option key={m} value={m}>{m}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">性格描述</label>
            <input className="input-field" value={personality} onChange={e => setPersonality(e.target.value)} placeholder="如：细心、开朗、沉稳..." />
          </div>
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">性格/兴趣标签 (可多选)</label>
          <TagPicker
            selectedTags={selectedTags}
            supplementTexts={supplementTexts}
            onTagsChange={handleTagsChange}
            onSupplementChange={handleSupplementChange}
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">最近购买/关注</label>
          <textarea className="input-field" rows={2} value={recentPurchases} onChange={e => setRecentPurchases(e.target.value)} placeholder="TA最近买过或关注的东西，帮助AI避免重复推荐..." />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">备注</label>
          <textarea className="input-field" rows={3} value={note} onChange={e => setNote(e.target.value)} placeholder="任何额外信息..." />
        </div>
        <button type="submit" disabled={loading} className="btn-primary w-full">
          {loading ? '创建中...' : '创建画像'}
        </button>
      </form>
    </div>
  );
}
