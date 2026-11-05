import { readFile, mkdir, writeFile } from "node:fs/promises";
import { dirname } from "node:path";

const [sourceUrl, destination] = process.argv.slice(2);
if (!sourceUrl?.startsWith("https://") || !destination) {
  throw new Error("Использование: node tools/sync-schedule-backup.mjs <https-url> <путь к резервной копии>");
}

const response = await fetch(sourceUrl, { signal: AbortSignal.timeout(45_000), redirect: "follow" });
if (!response.ok) throw new Error(`Источник расписания вернул HTTP ${response.status}`);
const schedule = await response.json();
validateSchedule(schedule);
delete schedule.backupCreatedAt;

let existing = null;
try {
  existing = JSON.parse(await readFile(destination, "utf8"));
} catch (error) {
  if (error.code !== "ENOENT") throw error;
}
if (existing && typeof existing.backupCreatedAt === "string" && !Number.isNaN(Date.parse(existing.backupCreatedAt))) {
  delete existing.backupCreatedAt;
  if (JSON.stringify(existing) === JSON.stringify(schedule)) {
    console.log("Расписание не изменилось; резервная копия сохранена без изменений.");
    process.exit(0);
  }
}

schedule.backupCreatedAt = new Date().toISOString();
await mkdir(dirname(destination), { recursive: true });
await writeFile(destination, `${JSON.stringify(schedule, null, 2)}\n`, "utf8");
console.log(`Обновлена резервная копия: ${schedule.routes.length} маршрутов.`);

function validateSchedule(data) {
  if (!data || !Array.isArray(data.routes) || data.routes.length === 0) {
    throw new Error("Источник не вернул список маршрутов; резервная копия не изменена.");
  }
  const ids = new Set();
  for (const route of data.routes) {
    if (!route || ["id", "routeNumber", "name", "description", "color"].some(key =>
      typeof route[key] !== "string" || !route[key].trim())) throw new Error("Маршрут содержит пустые обязательные поля");
    if (ids.has(route.id)) throw new Error(`Повтор ID маршрута: ${route.id}`);
    ids.add(route.id);
    if (!Array.isArray(route.schedules) || route.schedules.length === 0) throw new Error(`Нет рейсов маршрута ${route.id}`);
    const tripIds = new Set();
    for (const trip of route.schedules) {
      if (!trip || typeof trip.id !== "string" || !trip.id.trim() ||
          typeof trip.departurePoint !== "string" || !trip.departurePoint.trim() ||
          !/^(?:[01]\d|2[0-3]):[0-5]\d$/.test(trip.departureTime || "")) {
        throw new Error(`Некорректный рейс маршрута ${route.id}`);
      }
      if (tripIds.has(trip.id)) throw new Error(`Повтор ID рейса: ${trip.id}`);
      tripIds.add(trip.id);
    }
  }
}

