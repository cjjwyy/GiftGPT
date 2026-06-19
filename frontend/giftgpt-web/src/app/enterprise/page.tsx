'use client';

import { useState } from 'react';
import { enterpriseApi } from '@/lib/api';
import { Building2 } from 'lucide-react';
import { toast } from 'react-hot-toast';

export default function EnterprisePage() {
  const [name, setName] = useState('');
  const [contactName, setContactName] = useState('');
  const [contactPhone, setContactPhone] = useState('');
  const [loading, setLoading] = useState(false);

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      await enterpriseApi.register({ companyName: name, contactName, contactPhone });
      toast.success('企业注册已提交，等待审核');
    } catch (err: any) {
      toast.error(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-2xl mx-auto px-4 py-10">
      <div className="text-center mb-8">
        <Building2 className="w-12 h-12 text-primary-500 mx-auto mb-3" />
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">企业服务</h1>
        <p className="text-gray-500 dark:text-gray-400 mt-1">注册企业账号，享受批量团购、员工关怀日历等 SaaS 服务</p>
      </div>
      <form onSubmit={onSubmit} className="card space-y-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">企业名称 *</label>
          <input className="input-field" value={name} onChange={e => setName(e.target.value)} placeholder="请输入企业全称" required />
        </div>
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">联系人</label>
            <input className="input-field" value={contactName} onChange={e => setContactName(e.target.value)} placeholder="联系人姓名" />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">联系电话</label>
            <input className="input-field" value={contactPhone} onChange={e => setContactPhone(e.target.value)} placeholder="手机号码" />
          </div>
        </div>
        <button type="submit" disabled={loading} className="btn-primary w-full">
          {loading ? '提交中...' : '提交企业注册'}
        </button>
      </form>
    </div>
  );
}
