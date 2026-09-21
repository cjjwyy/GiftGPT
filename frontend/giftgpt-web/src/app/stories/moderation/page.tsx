'use client';
import { useEffect, useState } from 'react';
import Link from 'next/link';
import { storyApi } from '@/lib/api';
import { toast } from 'react-hot-toast';

interface Report { id: number; storyId: number; reason: string; detail: string; status: string; decisionNote?: string }
const labels: Record<string,string> = { spam: '广告或垃圾信息', privacy: '泄露隐私', abuse: '辱骂或不当内容', other: '其他', pending: '待审核', hide: '已隐藏', dismiss: '已驳回' };

export default function ModerationPage() {
  const [allowed, setAllowed] = useState<boolean | null>(null);
  const [reports, setReports] = useState<Report[]>([]);
  const [status, setStatus] = useState('pending');
  const [page, setPage] = useState(1);
  const [pages, setPages] = useState(0);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<Report | null>(null);
  const [story, setStory] = useState<{ title: string; content: string } | null>(null);
  const [note, setNote] = useState('');
  const [busy, setBusy] = useState(false);
  const [revision, setRevision] = useState(0);
  useEffect(() => { storyApi.moderationCapabilities().then(r => setAllowed(r.canModerate)).catch(() => setAllowed(false)); }, []);
  useEffect(() => {
    if (!allowed) return;
    let active = true; setLoading(true); setSelected(null); setStory(null);
    storyApi.reports(page, status).then(r => { if (active) { setReports(r.records); setPages(r.pages); } }).catch(e => { if (active) toast.error(e.message); }).finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [allowed, page, status, revision]);
  const review = async (action: 'hide' | 'dismiss') => {
    if (!selected || !story || !note.trim() || busy) return;
    if (action === 'hide' && !confirm('确认隐藏此帖子？帖子及评论将从公开社区隐藏，原数据保留。')) return;
    setBusy(true);
    try { await storyApi.review(selected.id, action, note.trim()); toast.success('审核已记录'); setRevision(v => v + 1); }
    catch (e: any) { toast.error(e.message || '审核失败'); }
    finally { setBusy(false); }
  };
  return <main className="max-w-4xl mx-auto px-4 py-10 space-y-4">
    <Link className="text-primary-500" href="/stories">返回社区</Link><h1 className="text-2xl font-bold">社区举报审核</h1>
    {allowed === null ? <p>检查权限中…</p> : !allowed ? <p>此页面仅供已配置的审核人员使用，请先登录授权账号。</p> : <>
      <label className="block">处理状态<select className="input-field" value={status} disabled={busy} onChange={e => { setStatus(e.target.value); setPage(1); }}>{['pending','hide','dismiss'].map(s => <option key={s} value={s}>{labels[s]}</option>)}</select></label>
      {loading ? <p>加载中…</p> : reports.length === 0 ? <p>暂无该状态的举报。</p> : reports.map(r => <article className="card" key={r.id}>
        <p className="font-medium">举报 #{r.id} · 帖子 #{r.storyId} · {labels[r.reason]}</p><p className="whitespace-pre-wrap text-sm my-2">{r.detail}</p>
        {r.decisionNote && <p className="text-sm">处理说明：{r.decisionNote}</p>}
        <button type="button" className="btn-outline text-sm" disabled={busy} onClick={async () => {
          setBusy(true); setStory(null); setSelected(r); setNote('');
          try { setStory(await storyApi.reportStory(r.id)); } catch (e: any) { toast.error(e.message || '原文加载失败'); } finally { setBusy(false); }
        }}>查看原文{r.status === 'pending' ? '并处理' : ''}</button>
      </article>)}
      <div className="flex gap-3 items-center"><button className="btn-outline" disabled={page <= 1 || busy || loading} onClick={() => setPage(v => v - 1)}>上一页</button><span>{page} / {Math.max(1,pages)}</span><button className="btn-outline" disabled={page >= pages || busy || loading} onClick={() => setPage(v => v + 1)}>下一页</button></div>
      {selected && story && <section className="card space-y-3"><h2 className="font-semibold">{story.title}</h2><p className="whitespace-pre-wrap">{story.content}</p>
        {selected.status === 'pending' && <><label className="block">审核说明<textarea className="input-field" maxLength={500} value={note} onChange={e => setNote(e.target.value)} /></label><div className="flex gap-3"><button disabled={busy || !note.trim()} className="btn-outline" onClick={() => review('dismiss')}>驳回举报</button><button disabled={busy || !note.trim()} className="btn-primary" onClick={() => review('hide')}>隐藏帖子</button></div></>}
      </section>}
    </>}
  </main>;
}
