// Сравнивает стоимость формирования полного экрана и обновляемого блока.
// Запуск: node scripts/benchmark-schedule-page.mjs
import { readFileSync } from 'node:fs';
import { runInNewContext } from 'node:vm';
import { performance } from 'node:perf_hooks';

const context = {
    document: { body: { dataset: {} }, getElementById: () => null, querySelector: () => null },
    window: {}, URL, URLSearchParams, Date, Intl, console
};
runInNewContext(readFileSync('docs/assets/scripts/pages/schedule-page.js', 'utf8'), context);
for (const size of [100, 1000]) {
    const schedules = Array.from({ length: size }, (_, index) => ({
        id: String(index), departurePoint: `Остановка ${index % 10}`,
        departureTime: `${String(Math.floor(index % 1440 / 60)).padStart(2, '0')}:${String(index % 60).padStart(2, '0')}`
    }));
    const route = { id: '1', routeNumber: '1', name: 'Маршрут', schedules };
    const iterations = 100;
    function measure(fn) {
        for (let i = 0; i < 10; i++) fn();
        const start = performance.now();
        for (let i = 0; i < iterations; i++) fn();
        return (performance.now() - start) / iterations;
    }
    console.log(`${size} рейсов: весь экран ${measure(() => context.buildRouteDetailCard(route)).toFixed(2)} мс; блок отправлений ${measure(() => context.buildUpcomingCard(schedules)).toFixed(2)} мс`);
}
