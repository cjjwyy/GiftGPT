import type { CalendarEvent } from '@/types';

export function eventDateInMonth(event: CalendarEvent, year: number, month: number): string | null {
  const [originalYear, originalMonth, day] = event.eventDate.split('-').map(Number);
  if (originalMonth !== month || year < originalYear || (event.isRepeat !== 1 && year !== originalYear)) return null;
  const days = new Date(Date.UTC(year, month, 0)).getUTCDate();
  return `${year}-${String(month).padStart(2, '0')}-${String(Math.min(day, days)).padStart(2, '0')}`;
}

function escapeIcs(text: string): string {
  return text.replace(/\\/g, '\\\\').replace(/\r\n|\r|\n/g, '\\n').replace(/;/g, '\\;').replace(/,/g, '\\,');
}

// RFC5545 lines folded at <=75 UTF-8 octets, never in the middle of a code point.
function fold(line: string): string {
  const encoder = new TextEncoder(); let current = ''; const result: string[] = []; let bytes = 0;
  for (const char of line) {
    const size = encoder.encode(char).length;
    if (bytes + size > 75) { result.push(current); current = ' '; bytes = 1; }
    current += char; bytes += size;
  }
  result.push(current); return result.join('\r\n');
}

export function createCalendarIcs(events: CalendarEvent[], year: number, month: number): string {
  const lines = ['BEGIN:VCALENDAR', 'VERSION:2.0', 'PRODID:-//GiftGPT//Gift Calendar//ZH', 'CALSCALE:GREGORIAN'];
  for (const event of events) {
    const date = eventDateInMonth(event, year, month);
    if (!date) continue;
    const next = new Date(`${date}T00:00:00Z`); next.setUTCDate(next.getUTCDate() + 1);
    lines.push('BEGIN:VEVENT', `UID:giftgpt-${event.id}-${date}@giftgpt.local`,
      `DTSTAMP:${new Date().toISOString().replace(/[-:]/g, '').replace(/\.\d{3}/, '')}`,
      `DTSTART;VALUE=DATE:${date.replace(/-/g, '')}`, `DTEND;VALUE=DATE:${next.toISOString().slice(0, 10).replace(/-/g, '')}`,
      `SUMMARY:${escapeIcs(event.title)}`, 'END:VEVENT');
  }
  lines.push('END:VCALENDAR'); return lines.map(fold).join('\r\n') + '\r\n';
}
