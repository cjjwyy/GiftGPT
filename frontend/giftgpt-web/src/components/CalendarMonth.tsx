'use client';
import { useState } from 'react';
import type { CalendarEvent } from '@/types';
import { createCalendarIcs, eventDateInMonth } from '@/lib/calendarExport';

export default function CalendarMonth({ events, onSelect }: { events: CalendarEvent[]; onSelect: (event: CalendarEvent) => void }) {
  const [month, setMonth] = useState(() => new Intl.DateTimeFormat('sv-SE', { timeZone: 'Asia/Shanghai', year: 'numeric', month: '2-digit' }).format(new Date()));
  const [year, number] = month.split('-').map(Number);
  const offset = (new Date(Date.UTC(year, number - 1, 1)).getUTCDay() + 6) % 7;
  const days = new Date(Date.UTC(year, number, 0)).getUTCDate();
  const shift = (delta: number) => { const d = new Date(Date.UTC(year, number - 1 + delta, 1)); setMonth(d.toISOString().slice(0, 7)); };
  const download = () => {
    const url = URL.createObjectURL(new Blob([createCalendarIcs(events, year, number)], { type: 'text/calendar;charset=utf-8' }));
    const a = document.createElement('a'); a.href = url; a.download = `GiftGPT-${month}.ics`; a.click(); setTimeout(() => URL.revokeObjectURL(url), 1000);
  };
  return <section className="card mb-6">
    <div className="flex flex-wrap gap-3 items-center justify-between mb-4">
      <div className="flex gap-3 items-center"><button type="button" className="btn-outline" aria-label="上个月" onClick={() => shift(-1)}>‹</button><h2 className="font-semibold">{month} 月历</h2><button type="button" className="btn-outline" aria-label="下个月" onClick={() => shift(1)}>›</button></div>
      <button type="button" className="btn-outline text-sm" onClick={download}>导出本月日历</button>
    </div>
    <p className="text-xs text-gray-500 mb-3">点击事件可编辑。导出为本月单次全天事件，不包含收礼人资料；2 月 29 日在非闰年按 28 日显示。</p>
    <div className="overflow-x-auto"><div className="grid grid-cols-7 min-w-[560px] gap-1">
      {['一', '二', '三', '四', '五', '六', '日'].map(day => <div key={day} className="text-center text-xs text-gray-500 p-2">周{day}</div>)}
      {Array.from({ length: offset }, (_, i) => <div key={`blank-${i}`} />)}
      {Array.from({ length: days }, (_, i) => {
        const date = `${month}-${String(i + 1).padStart(2, '0')}`;
        const matches = events.filter(e => eventDateInMonth(e, year, number) === date);
        return <div key={date} className="min-h-24 border border-gray-100 dark:border-gray-700 rounded p-1">
          <div className="text-xs text-gray-500 mb-1">{i + 1}</div>
          {matches.map(e => <button type="button" key={e.id} onClick={() => onSelect(e)} title={e.title} className="block text-left text-xs bg-primary-50 dark:bg-primary-900/30 text-primary-700 dark:text-primary-200 rounded p-1 w-full mb-1 break-words">{e.title}</button>)}
        </div>;
      })}
    </div></div>
  </section>;
}
