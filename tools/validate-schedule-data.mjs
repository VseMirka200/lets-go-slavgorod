import { readFileSync } from "node:fs";

const file = process.argv[2] ?? "schedule-data/schedule.json";
const fail = (message) => errors.push(message);
const errors = [];
const datePattern = /^\d{4}-\d{2}-\d{2}$/;
const timePattern = /^(?:[01]\d|2[0-3]):[0-5]\d$/;

let data;
try {
  data = JSON.parse(readFileSync(file, "utf8"));
} catch (error) {
  console.error(`Не удалось прочитать ${file}: ${error.message}`);
  process.exit(1);
}

if (!Array.isArray(data.routes) || data.routes.length === 0) fail("routes должен быть непустым массивом");
for (const route of data.routes ?? []) {
  for (const field of ["id", "routeNumber", "name", "description"]) {
    if (!String(route[field] ?? "").trim()) fail(`Маршрут: пустое поле ${field}`);
  }
  const duplicates = new Set();
  for (const trip of route.schedules ?? []) {
    const label = `Маршрут ${route.routeNumber}, рейс ${trip.id ?? "без id"}`;
    if (!String(trip.id ?? "").trim()) fail(`${label}: отсутствует id`);
    if (!String(trip.departurePoint ?? "").trim()) fail(`${label}: пустая остановка`);
    if (!timePattern.test(String(trip.departureTime ?? ""))) fail(`${label}: неверное время`);
    const calendar = trip.serviceCalendar ?? {};
    for (const field of ["validFrom", "validUntil", "includedDates", "excludedDates"]) {
      const values = Array.isArray(calendar[field]) ? calendar[field] : [calendar[field]].filter(Boolean);
      values.forEach((value) => { if (!datePattern.test(String(value))) fail(`${label}: неверная дата ${field}`); });
    }
    const included = new Set(calendar.includedDates ?? []);
    for (const date of calendar.excludedDates ?? []) if (included.has(date)) fail(`${label}: дата ${date} одновременно включена и исключена`);
    if (calendar.validFrom && calendar.validUntil && calendar.validFrom > calendar.validUntil) fail(`${label}: validFrom позже validUntil`);
    const key = [trip.departurePoint, trip.departureTime, JSON.stringify(calendar)].join("|");
    if (duplicates.has(key)) fail(`${label}: дубликат рейса`);
    duplicates.add(key);
  }
}

for (const change of data.scheduleChanges ?? []) {
  const label = `Изменение ${change.id ?? "без id"}`;
  for (const field of ["id", "title", "text", "validFrom"]) if (!String(change[field] ?? "").trim()) fail(`${label}: пустое поле ${field}`);
  if (!datePattern.test(String(change.validFrom ?? ""))) fail(`${label}: неверная validFrom`);
  if (change.validUntil && !datePattern.test(String(change.validUntil))) fail(`${label}: неверная validUntil`);
  if (change.validUntil && change.validFrom > change.validUntil) fail(`${label}: validFrom позже validUntil`);
}

if (errors.length) {
  console.error(`Найдено ошибок: ${errors.length}`);
  errors.forEach((error) => console.error(`- ${error}`));
  process.exit(1);
}
console.log(`Проверка ${file} пройдена.`);
