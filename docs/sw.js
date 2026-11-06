// Версия меняется при обновлении оболочки, чтобы браузер загрузил новые ресурсы.
const CACHE_PREFIX = "lets-go-slavgorod-web-";
const CACHE_NAME = `${CACHE_PREFIX}v41`;
const APP_SHELL = [
    "./",
    "./index.html",
    "./schedule.html",
    "./feedback.html",
    "./manifest.webmanifest",
    "./assets/styles/core/portal.css",
    "./assets/styles/pages/schedule-portal.css",
    "./assets/styles/pages/feedback.css",
    "./assets/styles/core/branding.css",
    "./assets/styles/features/pwa-install.css",
    "./assets/styles/features/store-icons.css",
    "./assets/styles/features/mobile.css",
    "./assets/scripts/pages/schedule-page.js",
    "./assets/scripts/pages/feedback-page.js",
    "./assets/scripts/features/pwa-install.js",
    "./assets/scripts/features/site-search.js",
    "./assets/media/branding/app-logo.png",
    "./assets/media/icons/github-logo.svg",
    "./assets/media/icons/rustore-logo.svg",
    "./assets/media/icons/play-market-logo.svg"
];

self.addEventListener("install", (event) => {
    event.waitUntil(caches.open(CACHE_NAME).then((cache) => cache.addAll(APP_SHELL)));
    self.skipWaiting();
});

self.addEventListener("activate", (event) => {
    // Удаляем только старые кэши этого приложения: у соседних сайтов на том же
    // источнике могут быть собственные записи Cache Storage.
    event.waitUntil(
        caches.keys().then((keys) => Promise.all(
            keys.filter((key) => key.startsWith(CACHE_PREFIX) && key !== CACHE_NAME).map((key) => caches.delete(key))
        ))
    );
    self.clients.claim();
});

self.addEventListener("fetch", (event) => {
    if (event.request.method !== "GET") return;

    // Для переходов параметр route не меняет сам HTML: одна сохранённая страница
    // обслуживает все маршруты, в том числе при отсутствии сети.
    event.respondWith(
        caches.match(event.request, { ignoreSearch: event.request.mode === "navigate" }).then((cached) => cached || fetch(event.request).then((response) => {
            if (response.ok && new URL(event.request.url).origin === self.location.origin) {
                const copy = response.clone();
                caches.open(CACHE_NAME).then((cache) => cache.put(event.request, copy));
            }
            return response;
        }))
    );
});
