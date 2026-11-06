import { test } from 'node:test';
import { strict as assert } from 'node:assert';
import { validateSchedule } from './validate-published-schedule.mjs';

const valid = () => ({ routes: [{ id: '1', routeNumber: '1', name: 'Route', description: 'Description', color: '#123456', schedules: [{ id: 'a', departurePoint: 'Stop', departureTime: '06:25' }] }] });

test('accepts a complete schedule', () => assert.deepEqual(validateSchedule(valid()), []));
test('rejects invalid time and duplicate routes', () => {
    const data = valid();
    data.routes[0].schedules[0].departureTime = '25:00';
    data.routes.push({ ...data.routes[0] });
    assert.ok(validateSchedule(data).some(error => error.includes('departureTime')));
    assert.ok(validateSchedule(data).some(error => error.includes('duplicated')));
});

test('rejects malformed schedule entries and duplicate departure ids', () => {
    const data = valid();
    data.routes[0].schedules.push({ ...data.routes[0].schedules[0] }, { id: 'b', departureTime: '12:00' });
    const errors = validateSchedule(data);
    assert.ok(errors.some(error => error.includes('id is duplicated')));
    assert.ok(errors.some(error => error.includes('departurePoint is required')));
});

test('accepts a route without departures and rejects a non-array schedule', () => {
    const data = valid();
    delete data.routes[0].schedules;
    assert.deepEqual(validateSchedule(data), []);
    data.routes[0].schedules = {};
    assert.ok(validateSchedule(data).some(error => error.includes('must be an array')));
});

test('rejects impossible dates and conflicting calendar entries', () => {
    const data = valid();
    data.routes[0].schedules[0].serviceCalendar = {
        includedDates: ['2026-02-29', '2026-01-01'],
        excludedDates: ['2026-01-01']
    };
    const errors = validateSchedule(data);
    assert.ok(errors.some(error => error.includes('invalid calendar date')));
    assert.ok(errors.some(error => error.includes('both included and excluded')));
});

test('rejects invalid production-calendar years and dates', () => {
    const data = valid();
    data.productionCalendar = { years: { '2026': { nonWorkingDates: ['2027-01-01'] } } };
    assert.ok(validateSchedule(data).some(error => error.includes('mismatches year')));
});

test('rejects invalid days of week', () => {
    const data = valid();
    data.routes[0].schedules[0].serviceCalendar = { daysOfWeek: ['MONDAY', 'NOT_A_DAY'] };
    assert.ok(validateSchedule(data).some(error => error.includes('daysOfWeek')));
});
