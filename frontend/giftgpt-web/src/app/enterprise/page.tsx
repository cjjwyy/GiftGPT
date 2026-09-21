'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { enterpriseApi, recipientApi } from '@/lib/api';
import { Building2, Plus, Trash2, Users } from 'lucide-react';
import { toast } from 'react-hot-toast';
import { OCCASIONS } from '@/lib/occasions';
import { Loading } from '@/components/Loading';

interface BatchRow {
  recipientId: string;
  occasion: string;
  budget: string;
}

const emptyRow = (): BatchRow => ({ recipientId: '', occasion: 'birthday', budget: '300' });

export default function EnterprisePage() {
  const [enterprise, setEnterprise] = useState<any>(null);
  const [recipients, setRecipients] = useState<any[]>([]);
  const [checking, setChecking] = useState(true);
  const [name, setName] = useState('');
  const [contactName, setContactName] = useState('');
  const [contactPhone, setContactPhone] = useState('');
  const [rows, setRows] = useState<BatchRow[]>([emptyRow()]);
  const [result, setResult] = useState<any>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    Promise.all([
      enterpriseApi.getMy().catch(() => null),
      recipientApi.list(1, 100).catch(() => ({ records: [] })),
    ]).then(([company, people]) => {
      setEnterprise(company);
      setRecipients(people.records || []);
      setChecking(false);
    });
  }, []);

  const register = async (event: React.FormEvent) => {
    event.preventDefault();
    setLoading(true);
    try {
      const company = await enterpriseApi.register({ companyName: name, contactName, contactPhone });
      setEnterprise(company);
      toast.success('企业资料已保存');
    } catch (error: any) {
      toast.error(error?.message || '保存失败');
    } finally {
      setLoading(false);
    }
  };

  const updateRow = (index: number, rowPatch: Partial<BatchRow>) => {
    setRows(current => current.map((row, rowIndex) => rowIndex === index ? { ...row, ...rowPatch } : row));
  };

  const createBatch = async () => {
    if (rows.some(row => !row.recipientId || Number(row.budget) <= 0)) {
      toast.error('请完整选择收礼人并填写预算');
      return;
    }
    if (new Set(rows.map(row => row.recipientId)).size !== rows.length) {
      toast.error('同一批次不能重复选择收礼人');
      return;
    }
    setLoading(true);
    try {
      const data = await enterpriseApi.batchOrder({
        enterpriseId: enterprise.id,
        employees: rows.map(row => ({
          recipientId: Number(row.recipientId),
          occasion: row.occasion,
          budget: Number(row.budget),
        })),
      });
      setResult(data);
      toast.success('已创建 ' + data.total + ' 个真实订单');
    } catch (error: any) {
      toast.error(error?.message || '批量下单失败');
    } finally {
      setLoading(false);
    }
  };

  if (checking) return <Loading />;

  return (
    <div className="max-w-4xl mx-auto px-4 py-10">
      <div className="text-center mb-8">
        <Building2 className="w-12 h-12 text-primary-500 mx-auto mb-3" />
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">企业员工关怀</h1>
        <p className="text-gray-500 dark:text-gray-400 mt-1">企业资料、员工画像与批量订单形成可追踪的真实链路</p>
      </div>

      {!enterprise ? (
        <form onSubmit={register} className="card max-w-2xl mx-auto space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">企业名称 *</label>
            <input className="input-field" value={name} onChange={event => setName(event.target.value.slice(0, 200))}
              placeholder="请输入企业全称" required />
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">联系人</label>
              <input className="input-field" value={contactName} onChange={event => setContactName(event.target.value.slice(0, 50))} />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">联系电话</label>
              <input className="input-field" value={contactPhone} onChange={event => setContactPhone(event.target.value)}
                placeholder="11位手机号" />
            </div>
          </div>
          <button type="submit" disabled={loading} className="btn-primary w-full">
            {loading ? '保存中...' : '提交企业资料'}
          </button>
        </form>
      ) : (
        <div className="space-y-6">
          <div className="card flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-400">当前企业</p>
              <p className="text-xl font-semibold text-gray-900 dark:text-white">{enterprise.companyName}</p>
            </div>
            <span className={'tag ' + (enterprise.status === 'approved' ? 'text-green-600' : 'text-amber-600')}>
              {enterprise.status === 'approved' ? '已认证' : enterprise.status === 'pending' ? '审核中' : enterprise.status}
            </span>
          </div>

          <div className="card">
            <div className="flex items-center justify-between mb-4">
              <div>
                <h2 className="font-semibold text-lg flex items-center gap-2"><Users className="w-5 h-5" /> 批量创建员工礼物订单</h2>
                <p className="text-xs text-gray-400 mt-1">每位员工需先有收礼人画像；整批事务执行，失败不会留下半批数据。</p>
              </div>
              <button type="button" onClick={() => setRows(current => current.length >= 50 ? current : [...current, emptyRow()])}
                className="btn-outline text-sm flex items-center gap-1"><Plus className="w-4 h-4" /> 添加</button>
            </div>

            {recipients.length === 0 ? (
              <div className="rounded-lg bg-amber-50 dark:bg-amber-900/20 text-amber-700 dark:text-amber-300 p-4 text-sm">
                还没有员工画像，先<Link href="/recipients/new" className="underline ml-1">创建收礼人</Link>。
              </div>
            ) : (
              <div className="space-y-3">
                {rows.map((row, index) => (
                  <div key={index} className="grid grid-cols-1 sm:grid-cols-[1fr_1fr_140px_36px] gap-2 items-center">
                    <select className="input-field" value={row.recipientId}
                      onChange={event => updateRow(index, { recipientId: event.target.value })}>
                      <option value="">选择员工画像</option>
                      {recipients.map(person => <option key={person.id} value={person.id}>{person.name}{person.relation ? ' · ' + person.relation : ''}</option>)}
                    </select>
                    <select className="input-field" value={row.occasion}
                      onChange={event => updateRow(index, { occasion: event.target.value })}>
                      {OCCASIONS.map(item => <option key={item.value} value={item.value}>{item.label}</option>)}
                    </select>
                    <input type="number" min={1} max={100000} className="input-field" value={row.budget}
                      onChange={event => updateRow(index, { budget: event.target.value })} placeholder="预算" />
                    <button type="button" title="删除" disabled={rows.length === 1}
                      onClick={() => setRows(current => current.filter((_, rowIndex) => rowIndex !== index))}
                      className="text-gray-400 hover:text-rose-500 disabled:opacity-30"><Trash2 className="w-4 h-4" /></button>
                  </div>
                ))}
                <button type="button" onClick={createBatch} disabled={loading} className="btn-primary w-full mt-4">
                  {loading ? '创建中...' : '创建 ' + rows.length + ' 份订单'}
                </button>
              </div>
            )}
          </div>

          {result && (
            <div className="card border border-green-100 dark:border-green-900">
              <h2 className="font-semibold text-green-700 dark:text-green-300">批量订单已真实落库</h2>
              <p className="text-sm text-gray-500 mt-1">共 {result.total} 份，订单金额合计 ¥{result.totalAmount}</p>
              <div className="flex flex-wrap gap-2 mt-3">
                {result.giftRecordIds?.map((id: number) => (
                  <Link key={id} href={'/gifts/' + id} className="tag hover:text-primary-600">送礼记录 #{id}</Link>
                ))}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
