import { readFileSync, statSync } from "node:fs";
import { pathToFileURL } from "node:url";

export const MAX_SCHEDULE_BYTES = 2 * 1024 * 1024;
const object = (value) => value !== null && typeof value === "object" && !Array.isArray(value);
const text = (value) => typeof value === "string" && value.trim().length > 0;
const date = (value) => {
  if (typeof value !== "string" || !/^\d{4}-\d{2}-\d{2}$/.test(value)) return false;
  const parsed = new Date(value + "T00:00:00Z");
  return Number.isFinite(parsed.getTime()) && parsed.toISOString().slice(0, 10) === value;
};

export function validateSchedule(data) {
  const errors = [];
  const fail = (message) => errors.push(message);
  const array = (value, label, optional = false) => {
    if (optional && value === undefined) return [];
    if (!Array.isArray(value)) { fail(label + ": требуется массив"); return []; }
    return value;
  };
  const uniqueId = (item, ids, label) => {
    if (!text(item.id)) fail(label + ": id должен быть непустой строкой");
    else if (ids.has(item.id)) fail(label + ": повторяющийся id " + item.id);
    else ids.add(item.id);
  };
  const dates = (value, label) => {
    const values = array(value, label, true);
    const seen = new Set();
    for (const value of values) {
      if (!date(value)) fail(label + ": некорректная календарная дата");
      if (seen.has(value)) fail(label + ": повторяющаяся дата");
      seen.add(value);
    }
    return values;
  };
  const period = (item, label) => {
    for (const key of ["validFrom", "validUntil"]) {
      if (item[key] !== undefined && !date(item[key])) fail(label + ": неверная " + key);
    }
    if (date(item.validFrom) && date(item.validUntil) && item.validFrom > item.validUntil) {
      fail(label + ": validFrom позже validUntil");
    }
  };
  if (!object(data)) return ["Корень JSON должен быть объектом"];
  const routes = array(data.routes, "routes");
  if (routes.length === 0) fail("routes должен быть непустым массивом");
  const routeIds = new Set();
  for (const route of routes) {
    if (!object(route)) { fail("Маршрут должен быть объектом"); continue; }
    uniqueId(route, routeIds, "Маршрут");
    for (const key of ["routeNumber", "name", "description"]) {
      if (!text(route[key])) fail("Маршрут: пустое или нестроковое поле " + key);
    }
    const tripIds = new Set();
    const duplicates = new Set();
    const trips = array(route.schedules, "schedules");
    if (trips.length === 0) fail("schedules должен быть непустым массивом");
    for (const trip of trips) {
      if (!object(trip)) { fail("Рейс должен быть объектом"); continue; }
      const label = "Рейс маршрута " + route.id;
      uniqueId(trip, tripIds, label);
      if (!text(trip.departurePoint)) fail(label + ": пустая остановка");
      if (typeof trip.departureTime !== "string" || !/^(?:[01]\d|2[0-3]):[0-5]\d$/.test(trip.departureTime)) {
        fail(label + ": неверное время");
      }
      const calendar = trip.serviceCalendar === undefined ? {} : trip.serviceCalendar;
      if (!object(calendar)) { fail(label + ": serviceCalendar должен быть объектом"); continue; }
      period(calendar, label);
      const included = dates(calendar.includedDates, label + " includedDates");
      const excluded = dates(calendar.excludedDates, label + " excludedDates");
      for (const value of excluded) if (included.includes(value)) fail(label + ": дата одновременно включена и исключена");
      const key = JSON.stringify([trip.departurePoint, trip.departureTime, calendar]);
      if (duplicates.has(key)) fail(label + ": дубликат рейса");
      duplicates.add(key);
    }
  }
  const changeIds = new Set();
  for (const change of array(data.scheduleChanges, "scheduleChanges", true)) {
    if (!object(change)) { fail("Изменение должно быть объектом"); continue; }
    uniqueId(change, changeIds, "Изменение");
    for (const key of ["title", "text"]) if (!text(change[key])) fail("Изменение: пустое поле " + key);
    if (!date(change.validFrom)) fail("Изменение: неверная validFrom");
    period(change, "Изменение");
    for (const id of array(change.routeIds, "routeIds", true)) {
      if (!text(id) || !routeIds.has(id)) fail("Изменение ссылается на неизвестный маршрут");
    }
  }
  if (data.productionCalendar !== undefined) {
    const calendar = data.productionCalendar;
    if (!object(calendar) || !object(calendar.years)) fail("productionCalendar.years должен быть объектом");
    else for (const [year, entry] of Object.entries(calendar.years)) {
      if (!/^\d{4}$/.test(year) || !object(entry)) { fail("Некорректный год производственного календаря"); continue; }
      const nonWorking = dates(entry.nonWorkingDates, year + " nonWorkingDates");
      const working = dates(entry.workingDates, year + " workingDates");
      for (const value of [...nonWorking, ...working]) if (date(value) && !value.startsWith(year + "-")) fail("Дата календаря не соответствует году");
      for (const value of working) if (nonWorking.includes(value)) fail("Дата одновременно рабочая и нерабочая");
    }
  }
  return errors;
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  const file = process.argv[2] ?? "schedule-data/schedule.json";
  try {
    if (statSync(file).size > MAX_SCHEDULE_BYTES) throw new Error("Размер расписания превышает 2 MiB");
    const errors = validateSchedule(JSON.parse(readFileSync(file, "utf8")));
    if (errors.length) {
      errors.forEach((error) => console.error("- " + error));
      process.exitCode = 1;
    } else console.log("Проверка " + file + " пройдена.");
  } catch (error) {
    console.error("Не удалось проверить расписание: " + error.message);
    process.exitCode = 1;
  }
}
