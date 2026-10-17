const CACHE_NAME = "lets-go-slavgorod-web-v2";
const APP_SHELL = [
    "./",
    "./index.html",
    "./schedule.html",
    "./manifest.webmanifest",
    "./assets/styles/core/portal.css",
    "./assets/styles/pages/schedule-portal.css",
    "./assets/styles/core/branding.css",
    "./assets/styles/features/pwa-install.css",
    "./assets/styles/features/store-icons.css",
    "./assets/styles/features/mobile.css",
    "./assets/scripts/pages/schedule-page.js",
    "./assets/scripts/features/pwa-install.js",
    "./assets/media/branding/app-logo.png",
    "./assets/media/icons/rustore-logo.svg",
    "./assets/media/icons/play-market-logo.svg"
];

self.addEventListener("install", (event) => {
    event.waitUntil(caches.open(CACHE_NAME).then((cache) => cache.addAll(APP_SHELL)));
    self.skipWaiting();
});

self.addEventListener("activate", (event) => {
    event.waitUntil(
        caches.keys().then((keys) => Promise.all(
            keys.filter((key) => key !== CACHE_NAME).map((key) => caches.delete(key))
        ))
    );
    self.clients.claim();
});

self.addEventListener("fetch", (event) => {
    if (event.request.method !== "GET") return;

    event.respondWith(
        caches.match(event.request).then((cached) => cached || fetch(event.request).then((response) => {
            if (new URL(event.request.url).origin === self.location.origin) {
                const copy = response.clone();
                caches.open(CACHE_NAME).then((cache) => cache.put(event.request, copy));
            }
            return response;
        }))
    );
});
