import { readFile, stat } from 'node:fs/promises';
import { pathToFileURL } from 'node:url';

export const MAX_SCHEDULE_BYTES = 2 * 1024 * 1024;
const isObject = (value) => value !== null && typeof value === 'object' && !Array.isArray(value);
const isRealDate = (value) => {
    if (typeof value !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(value)) return false;
    const parsed = new Date(value + 'T00:00:00Z');
    return Number.isFinite(parsed.getTime()) && parsed.toISOString().slice(0, 10) === value;
};
const checkDateList = (values, errors, label) => {
    if (values === undefined) return [];
    if (!Array.isArray(values)) { errors.push(label + ' must be an array'); return []; }
    const seen = new Set();
    for (const value of values) {
        if (!isRealDate(value)) errors.push(label + ' has invalid calendar date');
        if (seen.has(value)) errors.push(label + ' has duplicated date');
        seen.add(value);
    }
    return values;
};
const checkPeriod = (entry, errors, label) => {
    for (const key of ['validFrom', 'validUntil']) {
        if (entry[key] !== undefined && !isRealDate(entry[key])) errors.push(label + '.' + key + ' is invalid');
    }
    if (isRealDate(entry.validFrom) && isRealDate(entry.validUntil) && entry.validFrom > entry.validUntil) {
        errors.push(label + '.validFrom is after validUntil');
    }
};
const checkServiceCalendar = (calendar, errors, label) => {
    if (calendar === undefined) return;
    if (!isObject(calendar)) { errors.push(label + '.serviceCalendar must be an object'); return; }
    checkPeriod(calendar, errors, label + '.serviceCalendar');
    if (calendar.daysOfWeek !== undefined) {
        const allowed = new Set(['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']);
        const days = calendar.daysOfWeek;
        if (!Array.isArray(days) || days.some(day => !allowed.has(day)) || new Set(days).size !== days.length) {
            errors.push(label + '.serviceCalendar.daysOfWeek is invalid');
        }
    }
    const included = checkDateList(calendar.includedDates, errors, label + '.includedDates');
    const excluded = checkDateList(calendar.excludedDates, errors, label + '.excludedDates');
    for (const day of excluded) if (included.includes(day)) errors.push(label + '.date is both included and excluded');
};


/**
 * Проверяет обязательные поля маршрутов и рейсов в опубликованном ответе.
 * Проверка структуры не подтверждает достоверность расписания у перевозчика.
 * @param {unknown} data Разобранный ответ источника.
 * @returns {string[]} Список ошибок; пустой список означает успешную проверку.
 */
export function validateSchedule(data) {
    const errors = [];
    if (!data || !Array.isArray(data.routes) || data.routes.length === 0) return ['routes must be a non-empty array'];
    const routeIds = new Set();
    for (const [index, route] of data.routes.entries()) {
        const label = `routes[${index}]`;
        if (!route || typeof route !== 'object') { errors.push(`${label} must be an object`); continue; }
        for (const field of ['id', 'routeNumber', 'name', 'color']) {
            if (typeof route[field] !== 'string' || !route[field].trim()) errors.push(`${label}.${field} is required`);
        }
        if (!String(route.notes || route.description || '').trim()) errors.push(`${label}.description is required`);
        if (routeIds.has(route.id)) errors.push(`${label}.id is duplicated`);
        routeIds.add(route.id);
        if (route.schedules == null) continue;
        if (!Array.isArray(route.schedules)) { errors.push(`${label}.schedules must be an array`); continue; }
        const scheduleIds = new Set();
        for (const [entryIndex, entry] of route.schedules.entries()) {
            const entryLabel = `${label}.schedules[${entryIndex}]`;
            if (!entry || typeof entry !== 'object') { errors.push(`${entryLabel} must be an object`); continue; }
            for (const field of ['id', 'departurePoint', 'departureTime']) {
                if (typeof entry[field] !== 'string' || !entry[field].trim()) errors.push(`${entryLabel}.${field} is required`);
            }
            if (scheduleIds.has(entry.id)) errors.push(`${entryLabel}.id is duplicated`);
            scheduleIds.add(entry.id);
            if (!/^\s*(?:[01]?\d|2[0-3]):[0-5]?\d\s*$/.test(entry.departureTime || '')) errors.push(`${entryLabel}.departureTime is invalid`);
            checkServiceCalendar(entry.serviceCalendar, errors, entryLabel);
        }
    }
    if (data.scheduleChanges !== undefined) {
        if (!Array.isArray(data.scheduleChanges)) errors.push('scheduleChanges must be an array');
        else {
            const changeIds = new Set();
            for (const [i, change] of data.scheduleChanges.entries()) {
                const label = 'scheduleChanges[' + i + ']';
                if (!isObject(change)) { errors.push(label + ' must be an object'); continue; }
                if (typeof change.id !== 'string' || !change.id.trim()) errors.push(label + '.id is required');
                else if (changeIds.has(change.id)) errors.push(label + '.id is duplicated');
                changeIds.add(change.id);
                for (const key of ['title', 'text']) {
                    if (typeof change[key] !== 'string' || !change[key].trim()) errors.push(label + '.' + key + ' is required');
                }
                if (!isRealDate(change.validFrom)) errors.push(label + '.validFrom is invalid');
                checkPeriod(change, errors, label);
                if (change.routeIds !== undefined) {
                    if (!Array.isArray(change.routeIds)) errors.push(label + '.routeIds must be an array');
                    else for (const id of change.routeIds) if (!routeIds.has(id)) errors.push(label + '.routeIds contains unknown route');
                }
            }
        }
    }
    if (data.productionCalendar !== undefined) {
        const calendar = data.productionCalendar;
        if (!isObject(calendar) || !isObject(calendar.years)) errors.push('productionCalendar.years must be an object');
        else {
            for (const [year, info] of Object.entries(calendar.years)) {
                if (!/^\d{4}$/.test(year) || !isObject(info)) {
                    errors.push('productionCalendar has invalid year');
                    continue;
                }
                const nonWorking = checkDateList(info.nonWorkingDates, errors, year + '.nonWorkingDates');
                const working = checkDateList(info.preHolidayWorkingDates, errors, year + '.preHolidayWorkingDates');
                for (const value of [...nonWorking, ...working]) {
                    if (isRealDate(value) && !value.startsWith(year + '-')) errors.push('productionCalendar date mismatches year');
                }
                for (const value of working) if (nonWorking.includes(value)) errors.push('productionCalendar date is both working and non-working');
            }
        }
    }
    return errors;
}

async function main() {
    const source = process.argv[2];
    if (!source) throw new Error('Provide an HTTPS URL or a local JSON file');
    let text;
    if (source.startsWith('https://')) {
        const response = await fetch(source, { signal: AbortSignal.timeout(30000) });
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        if (Number(response.headers.get('content-length')) > MAX_SCHEDULE_BYTES) throw new Error('Schedule exceeds 2 MiB');
        if (!response.body) throw new Error('Schedule response is empty');
        const chunks = [];
        let size = 0;
        for await (const chunk of response.body) {
            size += chunk.byteLength;
            if (size > MAX_SCHEDULE_BYTES) throw new Error('Schedule exceeds 2 MiB');
            chunks.push(Buffer.from(chunk));
        }
        text = Buffer.concat(chunks, size).toString('utf8');
    } else {
        if ((await stat(source)).size > MAX_SCHEDULE_BYTES) throw new Error('Schedule exceeds 2 MiB');
        text = await readFile(source, 'utf8');
    }
    if (Buffer.byteLength(text, 'utf8') > MAX_SCHEDULE_BYTES) throw new Error('Schedule exceeds 2 MiB');
    const errors = validateSchedule(JSON.parse(text));
    if (errors.length) throw new Error(errors.join('\n'));
    console.log('Schedule is valid');
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
    main().catch(error => { console.error(error.message); process.exitCode = 1; });
}
