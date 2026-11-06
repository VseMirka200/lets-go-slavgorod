import { test } from 'node:test';
import { strict as assert } from 'node:assert';
import { readFileSync } from 'node:fs';
import { runInNewContext } from 'node:vm';

function page() {
    // Фиксируем местный понедельник, чтобы результаты не зависели от даты запуска CI.
    class FixedDate extends Date {
        constructor(...args) {
            super(...(args.length ? args : [2026, 10, 2, 12, 0, 0]));
        }
        static now() { return new Date(2026, 10, 2, 12, 0, 0).getTime(); }
    }
    const entries = new Map();
    const storage = {
        setItem: (key, value) => entries.set(key, value),
        getItem: key => entries.get(key) ?? null,
        removeItem: key => entries.delete(key)
    };
    const context = {
        document: { body: { dataset: { scheduleSource: 'https://example.org/schedule' } }, getElementById: () => null, querySelector: () => null },
        window: { localStorage: storage, location: { href: 'https://example.org/schedule.html' } },
        URL, URLSearchParams, Date: FixedDate, Intl, console
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

test('route details escape external schedule and price fields', () => {
    const { context } = page();
    const payload = '<img src=x onerror=alert(1)>';
    const html = context.buildRouteDetailCard({
        name: 'Маршрут', routeNumber: '1', pricePrimary: payload,
        schedules: [{ id: 'a', departurePoint: payload, departureTime: '12:00', notes: payload }]
    });
    assert.ok(!html.includes(payload));
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
    const date = '2026-11-02';
    context.setProductionCalendar({ years: { current: { nonWorkingDates: [date] } } });
    assert.equal(context.scheduleRunsOnDay({ dayType: 'weekday' }, 'MONDAY'), false);
    assert.equal(context.scheduleRunsOnDay({ dayType: 'weekend' }, 'MONDAY'), true);
    assert.equal(context.scheduleRunsOnDay({ serviceCalendar: { daysOfWeek: [], includedDates: [date], excludedDates: [date] } }, 'MONDAY'), false);
});

test('calendar respects date boundaries before explicitly included dates', () => {
    const { context } = page();
    const day = context.getTodayScheduleDay();
    const iso = '2026-11-02';
    assert.equal(context.scheduleRunsOnDay({ serviceCalendar: { daysOfWeek: [], includedDates: [iso] } }, day), true);
    assert.equal(context.scheduleRunsOnDay({ serviceCalendar: { daysOfWeek: [], includedDates: [iso], validFrom: '9999-12-31' } }, day), false);
});
