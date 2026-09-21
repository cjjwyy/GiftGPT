'use client';

import { useEffect, useState } from 'react';
import { calendarApi, notificationApi, recipientApi } from '@/lib/api';
import Link from 'next/link';
import { CalendarEvent, PageData } from '@/types';
import { Loading } from '@/components/Loading';
import { CalendarDays, Plus, X, Bell, Trash2, Pencil } from 'lucide-react';
import { toast } from 'react-hot-toast';
import { OCCASIONS, OCCASION_LABELS } from '@/lib/occasions';

export default function CalendarPage() {
  const [events, setEvents] = useState<CalendarEvent[]>([]);
  const [recipients, setRecipients] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [notifications, setNotifications] = useState<any[]>([]);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState<CalendarEvent>({
    title: '',
    occasion: '',
    eventDate: '',
    remindBeforeDays: 3,
    isRepeat: 1,
    recipientId: undefined,
  });

  const fetchEvents = async () => {
    setLoading(true);
    try {
      const all: CalendarEvent[] = [];
      let page = 1;
      let pages = 1;
      do {
        const data: PageData<CalendarEvent> = await calendarApi.list(page, 100);
        all.push(...data.records); pages = data.pages; page++;
      } while (page <= pages);
      setEvents(all);
    } catch (err: any) { toast.error(err.message || '加载日历失败'); }
    finally { setLoading(false); }
  };

  const loadNotifications = () => {
    notificationApi.list(1, 20).then(data => setNotifications(data.records || []))
      .catch((err: any) => toast.error(err?.message || '加载提醒失败'));
  };

  useEffect(() => {
    fetchEvents();
    recipientApi.list(1, 100).then(data => setRecipients(data.records || [])).catch(() => toast.error('加载收礼人失败'));
    notificationApi.checkNow().catch(() => 0).finally(loadNotifications);
  }, []);

  const checkNow = async () => {
    try {
      const created = await notificationApi.checkNow();
      await notificationApi.list(1, 20).then(data => setNotifications(data.records || []));
      toast.success(created > 0 ? `新增 ${created} 条提醒` : '已检查，没有新的到期提醒');
    } catch (err: any) {
      toast.error(err?.message || '检查提醒失败');
    }
  };

  const markRead = async (id: number) => {
    await notificationApi.markRead(id);
    setNotifications(items => items.map(item => item.id === id ? { ...item, isRead: 1 } : item));
  };

  const startEdit = (e: CalendarEvent) => {
    setEditingId(e.id!);
    setForm({ title: e.title, occasion: e.occasion, eventDate: e.eventDate, remindBeforeDays: e.remindBeforeDays, recipientId: e.recipientId, isRepeat: e.isRepeat ?? 1 });
    setShowForm(true);
  };

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.title.trim()) { toast.error('请输入事件标题'); return; }
    if (!form.eventDate) { toast.error('请选择日期'); return; }
    try {
      if (editingId) {
        await calendarApi.update(editingId, form);
        setEditingId(null);
      } else {
        await calendarApi.create(form);
      }
      toast.success(editingId ? '日历事件已更新' : '日历事件已添加');
      setShowForm(false);
      setForm({ title: '', occasion: '', eventDate: '', remindBeforeDays: 3, recipientId: undefined, isRepeat: 1 });
      fetchEvents();
    } catch (err: any) {
      toast.error(err.message || '创建失败');
    }
  };

  const upcomingEvents = events
    .filter(e => (e.daysUntil ?? -1) >= 0)
    .sort((a, b) => (a.nextOccurrence || a.eventDate).localeCompare(b.nextOccurrence || b.eventDate));

  const pastEvents = events
    .filter(e => (e.daysUntil ?? -1) < 0)
    .sort((a, b) => b.eventDate.localeCompare(a.eventDate));

  if (loading) return <Loading />;

  return (
    <div className="max-w-4xl mx-auto px-4 py-10">
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">日历提醒</h1>
          <p className="text-gray-500 dark:text-gray-400 mt-1">重要日子不错过</p>
        </div>
        <div className="flex gap-2">
          <button onClick={checkNow} className="btn-outline flex items-center gap-2">
            <Bell className="w-4 h-4" /> 立即检查
          </button>
          <button onClick={() => setShowForm(true)} className="btn-primary flex items-center gap-2">
            <Plus className="w-4 h-4" /> 添加提醒
          </button>
        </div>
      </div>

      <section id="notifications" className="card mb-8 scroll-mt-24">
        <div className="flex justify-between items-center mb-3">
          <h2 className="font-semibold text-gray-800 dark:text-gray-200 flex items-center gap-2"><Bell className="w-4 h-4 text-primary-500" /> 提醒消息</h2>
          {notifications.some(item => item.isRead === 0) && (
            <button className="text-xs text-primary-500" onClick={async () => { await notificationApi.markAllRead(); setNotifications(items => items.map(item => ({ ...item, isRead: 1 }))); }}>全部已读</button>
          )}
        </div>
        {notifications.length === 0 ? (
          <p className="text-sm text-gray-400">暂无提醒；到达设定日期时会在这里通知你。</p>
        ) : (
          <div className="space-y-2">
            {notifications.map(item => (
              <button key={item.id} type="button" onClick={() => item.isRead === 0 && markRead(item.id)}
                className={`w-full text-left rounded-lg px-3 py-2 ${item.isRead === 0 ? 'bg-primary-50 dark:bg-primary-900/20' : 'bg-gray-50 dark:bg-gray-800/50'}`}>
                <div className="flex justify-between gap-3">
                  <span className="text-sm font-medium text-gray-800 dark:text-gray-100">{item.title}</span>
                  {item.isRead === 0 && <span className="text-xs text-primary-500">未读</span>}
                </div>
                <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">{item.content}</p>
              </button>
            ))}
          </div>
        )}
      </section>

      {showForm && (
        <div className="card mb-8">
          <div className="flex items-center justify-between mb-4">
            <h2 className="font-semibold text-gray-800 dark:text-gray-200">新建日历事件</h2>
            <button onClick={() => { setShowForm(false); setEditingId(null); }} className="text-gray-400 dark:text-gray-500 hover:text-gray-600 dark:hover:text-gray-200">
              <X className="w-5 h-5" />
            </button>
          </div>
          <form onSubmit={onSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">事件标题 *</label>
              <input className="input-field" value={form.title} onChange={e => setForm({ ...form, title: e.target.value })} placeholder="如：妈妈生日" />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">日期 *</label>
                <input type="date" className="input-field" value={form.eventDate} onChange={e => setForm({ ...form, eventDate: e.target.value })} />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">提前提醒天数</label>
                <select className="input-field" value={form.remindBeforeDays} onChange={e => setForm({ ...form, remindBeforeDays: Number(e.target.value) })}>
                  <option value={0}>当天</option>
                  <option value={1}>1天前</option>
                  <option value={3}>3天前</option>
                  <option value={7}>7天前</option>
                  <option value={14}>14天前</option>
                </select>
              </div>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">场合</label>
              <div className="flex flex-wrap gap-2">
                {OCCASIONS.map(occ => (
                  <button key={occ.value} type="button"
                    className={form.occasion === occ.value ? 'tag-selected' : 'tag cursor-pointer'}
                    onClick={() => setForm({ ...form, occasion: form.occasion === occ.value ? '' : occ.value })}>
                    {occ.label}
                  </button>
                ))}
              </div>
            </div>
            <label className="flex items-center gap-2 text-sm text-gray-600 dark:text-gray-300">
              <input type="checkbox" checked={(form.isRepeat ?? 1) === 1}
                onChange={e => setForm({ ...form, isRepeat: e.target.checked ? 1 : 0 })} />
              每年重复提醒
            </label>
            <select className="input-field" value={form.recipientId || ''} onChange={e => setForm({ ...form, recipientId: e.target.value ? Number(e.target.value) : undefined })}>
              <option value="">关联收礼人（可选）</option>
              {recipients.map(r => <option key={r.id} value={r.id}>{r.name}</option>)}
            </select>
            <button type="submit" className="btn-primary w-full">保存事件</button>
          </form>
        </div>
      )}

      <div className="space-y-6">
        {upcomingEvents.length > 0 && (
          <div>
            <h2 className="font-semibold text-lg text-gray-800 dark:text-gray-200 mb-4 flex items-center gap-2">
              <Bell className="w-5 h-5 text-primary-500" />
              即将到来
            </h2>
            <div className="space-y-3">
              {upcomingEvents.map(e => (
                <div key={e.id} className="card flex items-center justify-between">
                  <div className="flex items-center gap-4">
                    <div className="w-12 h-12 bg-primary-100 dark:bg-primary-900/30 rounded-xl flex items-center justify-center text-primary-600 dark:text-primary-400 font-bold text-lg">
                      {(e.nextOccurrence || e.eventDate).slice(8).replace(/^0/, '')}
                    </div>
                    <div>
                      <p className="font-semibold text-gray-800 dark:text-gray-200">{e.title}</p>
                      <p className="text-sm text-gray-500 dark:text-gray-400">
                        {e.nextOccurrence || e.eventDate} · {e.daysUntil === 0 ? '今天' : `还有${e.daysUntil}天`} {e.occasion ? `· ${OCCASION_LABELS[e.occasion] || e.occasion}` : ''}
                        {e.remindBeforeDays ? ` · 提前${e.remindBeforeDays}天提醒` : ''}
                      </p>
                    </div>
                  </div>
                  <div className="flex gap-2">
                    {e.recipientId && <Link className="text-primary-500 text-sm" href={`/recommend?recipientId=${e.recipientId}&occasion=${encodeURIComponent(e.occasion || 'other')}`}>去选礼物</Link>}
                    <button onClick={() => startEdit(e)} className="text-gray-400 hover:text-primary-500 transition-colors">
                      <Pencil className="w-4 h-4" />
                    </button>
                    <button onClick={() => { if (confirm('确认删除此提醒？')) { calendarApi.delete(e.id!).then(fetchEvents).catch((err: any) => toast.error(err.message)); } }}
                      className="text-gray-400 hover:text-rose-500 transition-colors">
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {pastEvents.length > 0 && (
          <div>
            <h2 className="font-semibold text-lg text-gray-800 dark:text-gray-200 mb-4 flex items-center gap-2">
              <CalendarDays className="w-5 h-5 text-gray-400 dark:text-gray-500" />
              已过去
            </h2>
            <div className="space-y-3 opacity-70">
              {pastEvents.map(e => (
                <div key={e.id} className="card flex items-center justify-between">
                  <div className="flex items-center gap-4">
                    <div className="w-12 h-12 bg-gray-100 dark:bg-gray-800 rounded-xl flex items-center justify-center text-gray-500 dark:text-gray-400 font-bold text-lg">
                      {e.eventDate.slice(8).replace(/^0/, '')}
                    </div>
                    <div>
                      <p className="font-semibold text-gray-600 dark:text-gray-400">{e.title}</p>
                      <p className="text-sm text-gray-400 dark:text-gray-500">{e.eventDate}{e.occasion ? ` · ${e.occasion}` : ''}</p>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {events.length === 0 && (
          <div className="card text-center py-20 text-gray-400 dark:text-gray-500">
            <CalendarDays className="w-12 h-12 mx-auto mb-3 opacity-50" />
            <p>还没有日历提醒</p>
            <p className="text-sm mt-1">点击上方按钮添加重要日期</p>
          </div>
        )}
      </div>
    </div>
  );
}
