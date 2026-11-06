import { test } from 'node:test';
import { strict as assert } from 'node:assert';
import { readFileSync } from 'node:fs';
import { runInNewContext } from 'node:vm';

function page() {
    const entries = new Map();
    const storage = {
        setItem: (key, value) => entries.set(key, value),
        getItem: key => entries.get(key) ?? null,
        removeItem: key => entries.delete(key)
    };
    const context = {
        document: { body: { dataset: { scheduleSource: 'https://example.org/schedule' } }, getElementById: () => null, querySelector: () => null },
        window: { localStorage: storage, location: { href: 'https://example.org/schedule.html' } },
        URL, URLSearchParams, Date, Intl, console
    };
    runInNewContext(readFileSync('docs/assets/scripts/pages/schedule-page.js', 'utf8'), context);
    return { context, storage };
}

test('detail page escapes external route names', () => {
    const { context } = page();
    const html = context.buildRouteDetailCard({ name: '<img src=x onerror=alert(1)>', routeNumber: '1', schedules: [] });
    assert.ok(!html.includes('<img src=x'));
    assert.ok(html.includes('&lt;img src=x'));
});

test('ignores malformed cached JSON and accepts an offline copy', () => {
    const { context, storage } = page();
    const source = 'https://example.org/schedule';
    storage.setItem(context.getScheduleCacheKey(source), '{');
    assert.equal(context.readScheduleCache(source), null);
    context.writeScheduleCache(source, [{ id: '1' }], '2026-01-01', { years: { 2026: { nonWorkingDates: ['2026-01-01'] } } });
    assert.equal(context.readScheduleCache(source).routes[0].id, '1');
    assert.equal(context.readScheduleCache(source).productionCalendar.years[2026].nonWorkingDates[0], '2026-01-01');
});

test('filters calendar days and ignores malformed payload', () => {
    const { context } = page();
    assert.equal(context.scheduleRunsOnDay({ serviceCalendar: { daysOfWeek: ['MONDAY'] } }, 'TUESDAY'), false);
    assert.equal(context.scheduleRunsOnDay({ serviceCalendar: { daysOfWeek: ['MONDAY'] } }, 'MONDAY'), true);
    assert.equal(context.parseRoutesPayload({ routes: 'invalid' }).length, 0);
    assert.equal(context.scheduleRunsOnDay({ dayType: 'weekday' }, 'SUNDAY'), false);
    assert.equal(context.scheduleRunsOnDay({ serviceCalendar: { daysOfWeek: [] } }, 'MONDAY'), false);
    assert.equal(context.scheduleRunsOnDay({ serviceCalendar: { daysOfWeek: ['MON'] } }, 'MONDAY'), true);
});

test('applies calendar exceptions and production holidays to the next selected date', () => {
    const { context } = page();
    const monday = new Date();
    monday.setDate(monday.getDate() + (8 - monday.getDay()) % 7);
    const date = `${monday.getFullYear()}-${String(monday.getMonth() + 1).padStart(2, '0')}-${String(monday.getDate()).padStart(2, '0')}`;
    context.setProductionCalendar({ years: { current: { nonWorkingDates: [date] } } });
    assert.equal(context.scheduleRunsOnDay({ dayType: 'weekday' }, 'MONDAY'), false);
    assert.equal(context.scheduleRunsOnDay({ dayType: 'weekend' }, 'MONDAY'), true);
    assert.equal(context.scheduleRunsOnDay({ serviceCalendar: { daysOfWeek: [], includedDates: [date], excludedDates: [date] } }, 'MONDAY'), false);
});
