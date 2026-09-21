'use client';
import { useState } from 'react';
import { storyApi } from '@/lib/api';
import { toast } from 'react-hot-toast';

export default function ReportStory({ storyId }: { storyId: number }) {
  const [open, setOpen] = useState(false);
  const [reason, setReason] = useState('spam');
  const [detail, setDetail] = useState('');
  const [busy, setBusy] = useState(false);
  const [sent, setSent] = useState(false);
  return <div className="mt-3">
    <button className="text-xs text-gray-500" type="button" disabled={sent} onClick={() => setOpen(!open)} aria-expanded={open}>{sent ? '已提交举报' : open ? '取消举报' : '举报内容'}</button>
    {open && <form className="mt-2 rounded-lg border border-gray-200 dark:border-gray-700 p-3 space-y-2" onSubmit={async e => {
      e.preventDefault(); if (busy || !detail.trim()) return;
      setBusy(true);
      try { await storyApi.report(storyId, reason, detail.trim()); setSent(true); setOpen(false); toast.success('举报已记录，请等待审核'); }
      catch (error: any) { toast.error(error.message || '举报失败，请先登录后重试'); }
      finally { setBusy(false); }
    }}>
      <label className="block text-sm">举报原因<select className="input-field" value={reason} onChange={e => setReason(e.target.value)}><option value="spam">广告或垃圾信息</option><option value="privacy">泄露隐私</option><option value="abuse">辱骂或不当内容</option><option value="other">其他问题</option></select></label>
      <label className="block text-sm">具体说明<textarea className="input-field" required maxLength={500} value={detail} onChange={e => setDetail(e.target.value)} placeholder="请说明问题，不要填写联系方式等个人信息" /></label>
      <p className="text-xs text-gray-500">举报不会自动删除帖子，审核人员会核查处理。</p>
      <button type="submit" disabled={busy || !detail.trim()} className="btn-outline text-sm">{busy ? '提交中…' : '提交举报'}</button>
    </form>}
  </div>;
}
