import { readFile } from 'node:fs/promises';
import { pathToFileURL } from 'node:url';

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
        text = await response.text();
    } else {
        text = await readFile(source, 'utf8');
    }
    const errors = validateSchedule(JSON.parse(text));
    if (errors.length) throw new Error(errors.join('\n'));
    console.log('Schedule is valid');
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
    main().catch(error => { console.error(error.message); process.exitCode = 1; });
}
