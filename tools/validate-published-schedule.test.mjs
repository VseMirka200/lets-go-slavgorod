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
