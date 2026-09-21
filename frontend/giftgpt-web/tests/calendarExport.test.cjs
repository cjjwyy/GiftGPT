const { test } = require('node:test');
const assert = require('node:assert/strict');
const ts = require('typescript');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const source = fs.readFileSync(path.join(__dirname, '../src/lib/calendarExport.ts'), 'utf8');
const js = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 } }).outputText;
const output = {};
vm.runInNewContext(js, { exports: output, TextEncoder, Date });

test('monthly recurrence matches leap-year and future-start policies', () => {
  const event = { id: 1, title: '生日', eventDate: '2020-02-29', isRepeat: 1 };
  assert.equal(output.eventDateInMonth(event, 2026, 2), '2026-02-28');
  assert.equal(output.eventDateInMonth(event, 2028, 2), '2028-02-29');
  assert.equal(output.eventDateInMonth(event, 2019, 2), null);
  assert.equal(output.eventDateInMonth({ ...event, isRepeat: 0 }, 2026, 2), null);
  assert.equal(output.eventDateInMonth(event, 2026, 3), null);
});
test('ICS escapes content, limits UTF8 line length, and only exports chosen month', () => {
  const text = output.createCalendarIcs([{ id: 9, title: '心意'.repeat(60) + '\r\nEND:VEVENT,;', eventDate: '2025-12-31', isRepeat: 1 }], 2026, 12);
  assert.ok(text.includes('DTEND;VALUE=DATE:20270101'));
  assert.equal(text.split('\r\nEND:VEVENT').length, 2);
  assert.ok(text.replace(/\r\n /g, '').includes('\\nEND:VEVENT\\,\\;'));
  assert.ok(text.split('\r\n').every(line => Buffer.byteLength(line, 'utf8') <= 75));
  assert.ok(!output.createCalendarIcs([{ id: 1, title: 'other', eventDate: '2026-01-01' }], 2026, 12).includes('BEGIN:VEVENT'));
});
