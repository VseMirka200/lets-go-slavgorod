import test from "node:test";
import assert from "node:assert/strict";
import { validateSchedule } from "./validate-schedule-data.mjs";

const valid = () => ({
  routes: [{ id: "1", routeNumber: "1", name: "Маршрут", description: "Описание",
    schedules: [{ id: "1_1", departurePoint: "Вокзал", departureTime: "07:00" }] }],
  scheduleChanges: []
});
test("accepts valid schedule and leap date", () => {
  const data = valid();
  data.routes[0].schedules[0].serviceCalendar = { includedDates: ["2028-02-29"] };
  assert.deepEqual(validateSchedule(data), []);
});
test("rejects impossible dates", () => {
  for (const value of ["2026-02-29", "2026-04-31", "2026-13-01"]) {
    const data = valid();
    data.routes[0].schedules[0].serviceCalendar = { includedDates: [value] };
    assert.ok(validateSchedule(data).length > 0, value);
  }
});
test("reports malformed types without throwing", () => {
  for (const data of [null, [], { routes: {} }, { routes: [null] }, {
    routes: [{ ...valid().routes[0], schedules: "bad" }]
  }, { routes: [{ ...valid().routes[0], schedules: [null] }] }]) {
    assert.ok(validateSchedule(data).length > 0);
  }
});
test("rejects invalid calendar type and string date list", () => {
  for (const calendar of [null, [], { includedDates: "2026-01-01" }, { validFrom: ["2026-01-01"] }]) {
    const data = valid();
    data.routes[0].schedules[0].serviceCalendar = calendar;
    assert.ok(validateSchedule(data).length > 0);
  }
});
test("rejects duplicate route, trip and change IDs", () => {
  const route = valid();
  route.routes.push(structuredClone(route.routes[0]));
  assert.ok(validateSchedule(route).some((x) => x.includes("повторяющийся id")));
  const trip = valid();
  trip.routes[0].schedules.push({ ...trip.routes[0].schedules[0], departureTime: "08:00" });
  assert.ok(validateSchedule(trip).some((x) => x.includes("повторяющийся id")));
  const change = valid();
  change.scheduleChanges = Array(2).fill({ id: "a", title: "A", text: "A", validFrom: "2026-01-01" });
  assert.ok(validateSchedule(change).some((x) => x.includes("повторяющийся id")));
});
test("rejects unknown route references and conflicting dates", () => {
  const data = valid();
  data.scheduleChanges = [{ id: "a", title: "A", text: "A", validFrom: "2026-01-01", routeIds: ["missing"] }];
  data.routes[0].schedules[0].serviceCalendar = {
    includedDates: ["2026-01-01"], excludedDates: ["2026-01-01"]
  };
  assert.ok(validateSchedule(data).length >= 2);
});
test("checks production-calendar dates and year", () => {
  const data = valid();
  data.productionCalendar = { years: { "2026": { nonWorkingDates: ["2027-01-01"] } } };
  assert.ok(validateSchedule(data).length > 0);
});

test("checks actual daysOfWeek and preHolidayWorkingDates fields", () => {
  const data = valid();
  data.routes[0].schedules[0].serviceCalendar = { daysOfWeek: ["NOT_A_DAY"] };
  data.productionCalendar = { years: { "2026": { preHolidayWorkingDates: ["2026-02-30"] } } };
  assert.equal(validateSchedule(data).length, 2);
});
