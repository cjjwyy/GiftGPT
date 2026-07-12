'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { calendarApi } from '@/lib/api';
import { CalendarEvent, PageData } from '@/types';
import { Loading } from '@/components/Loading';
import { CalendarDays, Plus, X, Bell, Trash2, Pencil } from 'lucide-react';
import { toast } from 'react-hot-toast';

const OCCASIONS = ['生日', '纪念日', '情人节', '母亲节', '父亲节', '教师节', '圣诞节', '春节', '自定义'];

export default function CalendarPage() {
  const router = useRouter();
  const [events, setEvents] = useState<CalendarEvent[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState<CalendarEvent>({
    title: '',
    occasion: '',
    eventDate: '',
    remindBeforeDays: 3,
    recipientId: undefined,
  });

  const fetchEvents = () => {
    setLoading(true);
    calendarApi
      .list(1, 50)
      .then((d: PageData<CalendarEvent>) => {
        setEvents(d.records || []);
        setLoading(false);
      })
      .catch((err: any) => {
        toast.error(err.message || '加载日历失败');
        setLoading(false);
      });
  };

  useEffect(() => { fetchEvents(); }, []);

  const startEdit = (e: CalendarEvent) => {
    setEditingId(e.id!);
    setForm({ title: e.title, occasion: e.occasion, eventDate: e.eventDate, remindBeforeDays: e.remindBeforeDays, recipientId: e.recipientId });
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
      setForm({ title: '', occasion: '', eventDate: '', remindBeforeDays: 3, recipientId: undefined });
      fetchEvents();
    } catch (err: any) {
      toast.error(err.message || '创建失败');
    }
  };

  const upcomingEvents = events
    .filter(e => e.eventDate >= new Date().toISOString().slice(0, 10))
    .sort((a, b) => a.eventDate.localeCompare(b.eventDate));

  const pastEvents = events
    .filter(e => e.eventDate < new Date().toISOString().slice(0, 10))
    .sort((a, b) => b.eventDate.localeCompare(a.eventDate));

  if (loading) return <Loading />;

  return (
    <div className="max-w-4xl mx-auto px-4 py-10">
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">日历提醒</h1>
          <p className="text-gray-500 dark:text-gray-400 mt-1">重要日子不错过</p>
        </div>
        <button onClick={() => setShowForm(true)} className="btn-primary flex items-center gap-2">
          <Plus className="w-4 h-4" /> 添加提醒
        </button>
      </div>

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
                  <button key={occ} type="button"
                    className={form.occasion === occ ? 'tag-selected' : 'tag cursor-pointer'}
                    onClick={() => setForm({ ...form, occasion: form.occasion === occ ? '' : occ })}>
                    {occ}
                  </button>
                ))}
              </div>
            </div>
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
                      {e.eventDate.slice(8).replace(/^0/, '')}
                    </div>
                    <div>
                      <p className="font-semibold text-gray-800 dark:text-gray-200">{e.title}</p>
                      <p className="text-sm text-gray-500 dark:text-gray-400">
                        {e.eventDate} {e.occasion ? `· ${e.occasion}` : ''}
                        {e.remindBeforeDays ? ` · 提前${e.remindBeforeDays}天提醒` : ''}
                      </p>
                    </div>
                  </div>
                  <div className="flex gap-2">
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
              {pastEvents.slice(0, 10).map(e => (
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
