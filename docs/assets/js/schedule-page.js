const DEFAULT_SCHEDULE_SOURCE = "https://script.google.com/macros/s/AKfycbwKaCxx-FdDvlptqFCaWbg81ZWLvenzZ0e-sjgmgp8n2LYzzhCLokozPi9rTcbeXf2BNA/exec";
const SCHEDULE_CACHE_PREFIX = "lets-go-slavgorod:schedule-cache:";

let currentScheduleSourceUrl = DEFAULT_SCHEDULE_SOURCE;
let currentRouteQuery = "";

function escapeHtml(input) {
    return String(input)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function normalizeText(value) {
    return String(value ?? "")
        .trim()
        .replace(/\s+/g, " ")
        .toLowerCase();
}

function formatRouteNumber(route) {
    return route.routeNumber || route.id || "—";
}

function getRouteQueryValue(route) {
    return route.routeNumber || route.id || route.name || "";
}

function buildRouteHref(route, sourceUrl) {
    const url = new URL(window.location.href);
    url.searchParams.set("route", getRouteQueryValue(route));

    if (sourceUrl && sourceUrl !== DEFAULT_SCHEDULE_SOURCE) {
        url.searchParams.set("source", sourceUrl);
    } else {
        url.searchParams.delete("source");
    }

    return `${url.pathname}${url.search}${url.hash}`;
}

function routeMatchesQuery(route, routeQuery) {
    const normalizedQuery = normalizeText(routeQuery);
    if (!normalizedQuery) {
        return true;
    }

    const candidates = [
        route.id,
        route.routeNumber,
        route.name,
        `автобус №${route.routeNumber || ""}`,
        `маршрут ${route.routeNumber || ""}`,
        `автобус ${route.routeNumber || ""}`
    ]
        .map((value) => normalizeText(value))
        .filter(Boolean);

    return candidates.some((candidate) => candidate === normalizedQuery || candidate.includes(normalizedQuery));
}

function buildSearchText(route) {
    const scheduleText = (route.schedules || []).map((schedule) => [
        schedule.departurePoint,
        schedule.departureTime,
        schedule.dayType,
        schedule.variant,
        schedule.platform,
        schedule.notes,
        schedule.remark
    ].join(" ")).join(" ");

    return normalizeText([
        route.id,
        route.routeNumber,
        route.name,
        getRouteDescription(route),
        route.travelTime,
        route.pricePrimary,
        route.priceSecondary,
        route.paymentMethods,
        scheduleText
    ].join(" "));
}

function sortRoutes(routes) {
    return [...routes].sort((left, right) => {
        const leftNumber = Number.parseFloat(formatRouteNumber(left).replace(",", "."));
        const rightNumber = Number.parseFloat(formatRouteNumber(right).replace(",", "."));

        const leftValid = Number.isFinite(leftNumber);
        const rightValid = Number.isFinite(rightNumber);
        if (leftValid && rightValid && leftNumber !== rightNumber) {
            return leftNumber - rightNumber;
        }
        if (leftValid !== rightValid) {
            return leftValid ? -1 : 1;
        }
        return formatRouteNumber(left).localeCompare(formatRouteNumber(right), "ru");
    });
}

function parseRoutesPayload(payload) {
    if (Array.isArray(payload)) {
        return payload;
    }

    if (payload && Array.isArray(payload.routes)) {
        return payload.routes;
    }

    return [];
}

function canUseStorage() {
    try {
        const testKey = `${SCHEDULE_CACHE_PREFIX}test`;
        window.localStorage.setItem(testKey, "1");
        window.localStorage.removeItem(testKey);
        return true;
    } catch {
        return false;
    }
}

function getScheduleCacheKey(sourceUrl) {
    return `${SCHEDULE_CACHE_PREFIX}${sourceUrl}`;
}

function readScheduleCache(sourceUrl) {
    if (!canUseStorage()) {
        return null;
    }

    try {
        const raw = window.localStorage.getItem(getScheduleCacheKey(sourceUrl));
        if (!raw) {
            return null;
        }

        const parsed = JSON.parse(raw);
        if (!parsed || !Array.isArray(parsed.routes)) {
            return null;
        }

        return {
            routes: parsed.routes,
            updatedAt: parsed.updatedAt || null
        };
    } catch {
        return null;
    }
}

function writeScheduleCache(sourceUrl, routes, updatedAt) {
    if (!canUseStorage()) {
        return;
    }

    try {
        window.localStorage.setItem(getScheduleCacheKey(sourceUrl), JSON.stringify({
            routes,
            updatedAt: updatedAt || new Date().toISOString()
        }));
    } catch {
        // Ignore storage quota and privacy mode failures.
    }
}

function groupSchedulesByDeparturePoint(schedules) {
    const groups = new Map();

    schedules.forEach((schedule) => {
        const key = normalizeText(schedule.departurePoint || "Пункт отправления");
        if (!groups.has(key)) {
            groups.set(key, {
                label: schedule.departurePoint || "Пункт отправления",
                schedules: []
            });
        }
        groups.get(key).schedules.push(schedule);
    });

    return [...groups.values()].sort((left, right) => {
        return normalizeText(left.label).localeCompare(normalizeText(right.label), "ru");
    });
}

function getRouteDescription(route) {
    const candidates = [
        route.description,
        route.routeDescription,
        route.details,
        route.detail,
        route.info,
        route.summary,
        route.text,
        route.note
    ];

    return candidates
        .map((value) => (typeof value === "string" ? value.trim() : ""))
        .find(Boolean) || "";
}

function parseDepartureMinutes(value) {
    const match = String(value ?? "").match(/(\d{1,2}):(\d{2})/);
    if (!match) {
        return null;
    }

    const hours = Number.parseInt(match[1], 10);
    const minutes = Number.parseInt(match[2], 10);
    if (!Number.isFinite(hours) || !Number.isFinite(minutes)) {
        return null;
    }

    return hours * 60 + minutes;
}

function findNearestSchedule(schedules) {
    const now = new Date();
    const currentMinutes = now.getHours() * 60 + now.getMinutes();

    const parsedSchedules = schedules
        .map((schedule, index) => ({
            schedule,
            index,
            minutes: parseDepartureMinutes(schedule.departureTime)
        }))
        .filter((item) => item.minutes !== null);

    if (!parsedSchedules.length) {
        return null;
    }

    const upcoming = parsedSchedules
        .filter((item) => item.minutes >= currentMinutes)
        .sort((left, right) => left.minutes - right.minutes || left.index - right.index);

    if (upcoming.length) {
        return upcoming[0].schedule;
    }

    return parsedSchedules
        .sort((left, right) => left.minutes - right.minutes || left.index - right.index)[0].schedule;
}

function buildRouteSummaryCard(route) {
    const routeNumber = escapeHtml(formatRouteNumber(route));
    const routeHref = escapeHtml(buildRouteHref(route, currentScheduleSourceUrl));

    return `
        <a class="schedule-route-card schedule-route-card--link schedule-route-card--summary" href="${routeHref}" aria-label="Открыть маршрут ${routeNumber}" data-search-text="${escapeHtml(buildSearchText(route))}">
            <h2 class="schedule-route-summary-title">Автобус №${routeNumber}</h2>
        </a>
    `;
}

function buildRouteDetailCard(route) {
    const routeNumber = escapeHtml(formatRouteNumber(route));
    const title = escapeHtml(route.name || "Без названия");
    const description = escapeHtml(getRouteDescription(route) || "Описание не указано");
    const travelTime = escapeHtml(route.travelTime || "Время не указано");
    const pricePrimary = escapeHtml(route.pricePrimary || "Цена не указана");
    const priceSecondary = escapeHtml(route.priceSecondary || "Цена не указана");
    const paymentMethods = escapeHtml(route.paymentMethods || "Способ оплаты не указан");
    const schedules = Array.isArray(route.schedules) ? route.schedules : [];
    const departureGroups = groupSchedulesByDeparturePoint(schedules);
    const schedulesMarkup = departureGroups.length
        ? departureGroups.map((group) => {
            const groupTitle = escapeHtml(group.label);
            const nearestSchedule = findNearestSchedule(group.schedules);
            const entriesMarkup = group.schedules.map((schedule) => {
                const departureTime = escapeHtml(schedule.departureTime || "—");
                const dayType = escapeHtml(schedule.dayType || schedule.remark || "");
                const variant = escapeHtml(schedule.variant || "");
                const platform = escapeHtml(schedule.platform || "");
                const notes = escapeHtml(schedule.notes || schedule.remark || "");
                const isNearest = nearestSchedule === schedule;
                const chips = [dayType, variant && `Вариант ${variant}`, platform && `Платформа ${platform}`]
                    .filter(Boolean)
                    .map((item) => `<span class="schedule-tag">${item}</span>`)
                    .join("");

                return `
                    <li class="schedule-entry${isNearest ? " schedule-entry--next" : ""}">
                        <div class="schedule-entry-time">${departureTime}</div>
                        <div class="schedule-entry-body">
                            ${notes ? `<p>${notes}</p>` : ""}
                            ${chips ? `<div class="schedule-tags">${chips}</div>` : ""}
                        </div>
                    </li>
                `;
            }).join("");

            return `
                <section class="schedule-departure-group">
                    <header class="schedule-departure-header">
                        <strong>${groupTitle}</strong>
                    </header>
                    <ul class="schedule-entries">
                        ${entriesMarkup}
                    </ul>
                </section>
            `;
        }).join("")
        : `<div class="schedule-empty-state schedule-empty-row">Для этого маршрута расписание пока не добавлено.</div>`;

    return `
        <article class="schedule-route-card schedule-route-card--detail">
            <header class="schedule-route-header">
                <div>
                    <span class="schedule-route-badge">Автобус №${routeNumber}</span>
                </div>
                <div class="schedule-route-meta">
                    <span>Время в пути: ${travelTime}</span>
                    <span>Стоимость: ${pricePrimary}${route.priceSecondary ? ` / ${priceSecondary}` : ""}</span>
                    <span>Способ оплаты: ${paymentMethods}</span>
                    <span>Маршрут: ${route.name || `Автобус №${routeNumber}`}</span>
                </div>
            </header>
            <div class="schedule-route-body">
                <div class="schedule-departures">
                    ${schedulesMarkup}
                </div>
            </div>
        </article>
    `;
}

function renderRoutes(routes, container, detailMode = false) {
    if (!routes.length) {
        container.innerHTML = `
            <div class="schedule-empty-state">
                <strong>Расписание не найдено</strong>
                <p>Проверьте источник данных или попробуйте обновить страницу позже.</p>
            </div>
        `;
        return;
    }

    container.innerHTML = detailMode ? buildRouteDetailCard(routes[0]) : routes.map(buildRouteSummaryCard).join("");
}

async function loadScheduleData(sourceUrl) {
    const response = await fetch(sourceUrl, { cache: "no-store" });
    if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
    }

    return response.json();
}

function setupSchedulePage() {
    const root = document.querySelector("[data-schedule-source]");
    const results = document.getElementById("schedule-results");
    const statusMessage = document.getElementById("schedule-status-message");
    const searchInput = document.getElementById("schedule-search");
    const reloadButton = document.getElementById("schedule-reload");
    const loadStateLabel = document.getElementById("schedule-load-state");
    const updatedAtLabel = document.getElementById("schedule-updated-at");

    if (!root || !results || !statusMessage || !searchInput || !reloadButton || !loadStateLabel || !updatedAtLabel) {
        return;
    }

    const params = new URLSearchParams(window.location.search);
    currentScheduleSourceUrl = params.get("source") || root.dataset.scheduleSource || DEFAULT_SCHEDULE_SOURCE;
    currentRouteQuery = params.get("route") || "";
    let allRoutes = [];
    let activeLoadToken = 0;

    const applyFilter = () => {
        const query = normalizeText(searchInput.value);
        const routeScopedRoutes = currentRouteQuery
            ? allRoutes.filter((route) => routeMatchesQuery(route, currentRouteQuery))
            : allRoutes;
        const filteredRoutes = query
            ? routeScopedRoutes.filter((route) => buildSearchText(route).includes(query))
            : routeScopedRoutes;
        const detailMode = Boolean(currentRouteQuery);

        renderRoutes(filteredRoutes, results, detailMode);

        if (currentRouteQuery && filteredRoutes.length) {
            const route = filteredRoutes[0];
            statusMessage.textContent = "";
            document.title = `${route.name || `Маршрут ${formatRouteNumber(route)}`} - Поехали! Славгород`;
            return;
        }

        if (currentRouteQuery && !filteredRoutes.length) {
            statusMessage.textContent = "";
            document.title = `Маршрут не найден - Поехали! Славгород`;
            return;
        }

        statusMessage.textContent = "";
        document.title = "Расписание маршрутов г. Славгород - Поехали! Славгород";
    };

    const updateTimestamp = () => {
        updatedAtLabel.textContent = new Intl.DateTimeFormat("ru-RU", {
            dateStyle: "medium",
            timeStyle: "short"
        }).format(new Date());
    };

    const load = async () => {
        const loadToken = ++activeLoadToken;
        const cached = readScheduleCache(currentScheduleSourceUrl);

        if (cached) {
            allRoutes = sortRoutes(cached.routes.map((route) => ({
                ...route,
                schedules: Array.isArray(route.schedules) ? route.schedules : []
            })));
            if (cached.updatedAt) {
                updatedAtLabel.textContent = new Intl.DateTimeFormat("ru-RU", {
                    dateStyle: "medium",
                    timeStyle: "short"
                }).format(new Date(cached.updatedAt));
            }
            loadStateLabel.textContent = `Загружено: ${allRoutes.length}`;
            statusMessage.textContent = "";
            applyFilter();
        } else {
            loadStateLabel.textContent = "Загрузка...";
            statusMessage.textContent = "Подключаемся к источнику таблицы...";
            results.innerHTML = "";
        }

        try {
            const payload = await loadScheduleData(currentScheduleSourceUrl);
            if (loadToken !== activeLoadToken) {
                return;
            }
            const routes = sortRoutes(parseRoutesPayload(payload).filter(Boolean));

            allRoutes = routes.map((route) => ({
                ...route,
                schedules: Array.isArray(route.schedules) ? route.schedules : []
            }));

            const nowIso = new Date().toISOString();
            writeScheduleCache(currentScheduleSourceUrl, allRoutes, nowIso);
            updateTimestamp();
            loadStateLabel.textContent = `Загружено: ${allRoutes.length}`;
            applyFilter();
        } catch (error) {
            if (cached) {
                loadStateLabel.textContent = `Загружено: ${allRoutes.length}`;
                return;
            }
            loadStateLabel.textContent = "Ошибка загрузки";
            statusMessage.textContent = "Не удалось получить расписание из таблицы.";
            results.innerHTML = `
                <div class="schedule-empty-state schedule-empty-state-error">
                    <strong>Источник недоступен</strong>
                    <p>Проверьте ссылку на таблицу или попробуйте обновить страницу.</p>
                    <p class="schedule-error-detail">${escapeHtml(error.message || "unknown error")}</p>
                </div>
            `;
        }
    };

    searchInput.addEventListener("input", applyFilter);
    reloadButton.addEventListener("click", load);
    load();
}

setupSchedulePage();
