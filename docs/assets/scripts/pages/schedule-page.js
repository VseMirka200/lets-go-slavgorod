/**
 * Стандартная точка входа Google Apps Script, которая отдаёт JSON расписания.
 * Пользователь может переопределить её через query-параметр `source`.
 */
const DEFAULT_SCHEDULE_SOURCE = "https://raw.githubusercontent.com/VseMirka200/lets-go-slavgorod/main/schedule-data/schedule.json";

/**
 * Префикс для всех записей localStorage, связанных с кэшем расписания.
 */
const SCHEDULE_CACHE_PREFIX = "lets-go-slavgorod:schedule-cache:";

/**
 * Общие настройки форматирования даты для всех временных меток на странице расписания.
 */
const SCHEDULE_DATE_FORMAT_OPTIONS = {
    dateStyle: "medium",
    timeStyle: "short"
};

let currentRouteQuery = "";
let clearUpcomingStickyState = null;

/**
 * Форматирует дату или timestamp в единый вид, принятый на странице расписания.
 */
function formatScheduleDate(value) {
    return new Intl.DateTimeFormat("ru-RU", SCHEDULE_DATE_FORMAT_OPTIONS).format(new Date(value));
}

/**
 * Экранирует пользовательские значения перед вставкой в HTML.
 */
function escapeHtml(input) {
    return String(input)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

/**
 * Схлопывает пробелы, обрезает строку и переводит её в нижний регистр.
 * Используется для поиска, чтобы маршрут находился независимо от оформления текста.
 */
function normalizeText(value) {
    return String(value ?? "")
        .trim()
        .replace(/\s+/g, " ")
        .toLowerCase();
}

/**
 * Возвращает человекочитаемую подпись маршрута для интерфейса.
 * Если номер отсутствует, используется id маршрута.
 */
function formatRouteNumber(route) {
    return route.routeNumber || route.id || "—";
}

/**
 * Выбирает наиболее подходящее значение для query-параметра `route`.
 */
function getRouteQueryValue(route) {
    return route.routeNumber || route.id || route.name || "";
}

/**
 * Строит ссылку на ту же страницу с сохранением параметра route.
 */
function buildRouteHref(route) {
    const url = new URL(window.location.href);
    url.searchParams.set("route", getRouteQueryValue(route));
    url.searchParams.delete("source");

    return `${url.pathname}${url.search}${url.hash}`;
}

/**
 * Проверяет, соответствует ли маршрут текущему запросу маршрута.
 */
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

/**
 * Склеивает все поисковые данные маршрута в одну нормализованную строку.
 */
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

/**
 * Сортирует маршруты в естественном порядке, чтобы числовые номера шли по возрастанию.
 */
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

/**
 * Извлекает пригодный массив маршрутов из любой формы ответа бэкенда.
 */
function parseRoutesPayload(payload) {
    if (Array.isArray(payload)) {
        return payload;
    }

    if (payload && Array.isArray(payload.routes)) {
        return payload.routes;
    }

    return [];
}

/**
 * Проверяет, доступна ли запись в localStorage в текущем браузерном контексте.
 */
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

/**
 * Возвращает ключ кэша для указанного URL источника расписания.
 */
function getScheduleCacheKey(sourceUrl) {
    return `${SCHEDULE_CACHE_PREFIX}${sourceUrl}`;
}

/**
 * Читает кэшированные маршруты и время последнего обновления из localStorage.
 */
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

/**
 * Записывает нормализованный список маршрутов в localStorage для офлайн-доступа.
 */
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
        // Игнорируем переполнение хранилища и ограничения приватного режима.
    }
}

/**
 * Группирует рейсы по пункту отправления, чтобы детальная страница могла строить секции.
 */
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

/**
 * Достаёт человекочитаемое описание маршрута из набора возможных полей источника.
 */
function getRouteDescription(route) {
    const candidates = [
        route.description,
        route.routeDescription,
        route.details,
        route.detail,
        route.info,
        route.summary,
        route.text,
        route.note,
        route.notes,
        route.remark
    ];

    return candidates
        .map((value) => (typeof value === "string" ? value.trim() : ""))
        .find(Boolean) || "";
}

/**
 * Преобразует время отправления в формате HH:mm в минуты от начала суток.
 * Возвращает null, если значение отсутствует или записано неверно.
 */
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

/**
 * Находит ближайший предстоящий рейс в группе расписания.
 * Если на сегодня ничего не осталось, возвращается самый ранний валидный рейс.
 */
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

const SCHEDULE_DAYS = [
    { key: "MONDAY", label: "Понедельник" },
    { key: "TUESDAY", label: "Вторник" },
    { key: "WEDNESDAY", label: "Среда" },
    { key: "THURSDAY", label: "Четверг" },
    { key: "FRIDAY", label: "Пятница" },
    { key: "SATURDAY", label: "Суббота" },
    { key: "SUNDAY", label: "Воскресенье" }
];

function getTodayScheduleDay() {
    const dayIndex = new Date().getDay();
    return SCHEDULE_DAYS[(dayIndex + 6) % SCHEDULE_DAYS.length].key;
}

function scheduleRunsOnDay(schedule, day) {
    const days = schedule.serviceCalendar?.daysOfWeek;
    return !Array.isArray(days) || days.length === 0 || days.includes(day);
}

function getDayLabel(day) {
    return SCHEDULE_DAYS.find((item) => item.key === day)?.label || "Сегодня";
}

function buildDayPicker(selectedDay) {
    const today = getTodayScheduleDay();
    return `
        <section class="schedule-day-picker" aria-label="Выбор дня недели">
            <h2>Выберите день недели</h2>
            <div class="schedule-day-grid">
                ${SCHEDULE_DAYS.map((day) => `
                    <button class="schedule-day-card${day.key === selectedDay ? " is-selected" : ""}${day.key === today ? " is-today" : ""}" type="button" data-schedule-day="${day.key}" aria-pressed="${day.key === selectedDay}">
                        <strong>${day.label}</strong>
                        ${day.key === today ? "<span>Сегодня</span>" : ""}
                    </button>
                `).join("")}
            </div>
        </section>
    `;
}

function buildUpcomingCard(schedules) {
    const todaySchedules = schedules.filter((schedule) => scheduleRunsOnDay(schedule, getTodayScheduleDay()));
    const groups = groupSchedulesByDeparturePoint(todaySchedules);
    const entries = groups.map((group) => {
        const next = findNearestSchedule(group.schedules);
        if (!next) return "";
        return `<li><strong>${escapeHtml(next.departureTime || "—")}</strong><span>${escapeHtml(group.label)}</span></li>`;
    }).filter(Boolean).join("");

    if (!entries) return "";
    return `
        <section class="schedule-upcoming-card" aria-label="Ближайшие отправления">
            <div><span>Ближайшие отправления</span><small>${getDayLabel(getTodayScheduleDay())}</small></div>
            <ul>${entries}</ul>
        </section>
    `;
}

/**
 * Меняет вид ближайших отправлений только после их закрепления у верхней границы.
 * В обычном положении карточка остаётся частью карточки маршрута.
 */
function setupUpcomingStickyState() {
    clearUpcomingStickyState?.();
    clearUpcomingStickyState = null;

    const card = document.querySelector(".schedule-upcoming-card");
    if (!card) {
        return;
    }

    const sentinel = document.createElement("div");
    sentinel.className = "schedule-upcoming-sentinel";
    sentinel.setAttribute("aria-hidden", "true");
    card.before(sentinel);
    const routeCard = card.closest(".schedule-route-card");

    let frameId = 0;
    const updateState = () => {
        frameId = 0;
        const isMobile = window.matchMedia("(max-width: 780px)").matches;
        const stickyTop = isMobile
            ? document.querySelector(".portal-sidebar")?.getBoundingClientRect().bottom || 0
            : 0;
        const routeBottom = routeCard?.getBoundingClientRect().bottom || 0;
        const shouldStick = sentinel.getBoundingClientRect().top <= stickyTop
            && routeBottom > stickyTop + card.offsetHeight;

        if (shouldStick) {
            if (!card.classList.contains("is-stuck")) {
                sentinel.style.height = `${card.offsetHeight}px`;
                card.classList.add("is-stuck");
            }
            const bounds = routeCard?.getBoundingClientRect();
            if (bounds) {
                card.style.left = `${Math.round(bounds.left)}px`;
                card.style.width = `${Math.round(bounds.width)}px`;
            }
            card.style.top = `${Math.round(stickyTop)}px`;
        } else if (card.classList.contains("is-stuck")) {
            card.classList.remove("is-stuck");
            sentinel.style.height = "";
            card.style.removeProperty("left");
            card.style.removeProperty("width");
            card.style.removeProperty("top");
        }
    };
    const scheduleUpdate = () => {
        if (!frameId) {
            frameId = window.requestAnimationFrame(updateState);
        }
    };

    window.addEventListener("scroll", scheduleUpdate, { passive: true });
    window.addEventListener("resize", scheduleUpdate);
    updateState();

    clearUpcomingStickyState = () => {
        window.removeEventListener("scroll", scheduleUpdate);
        window.removeEventListener("resize", scheduleUpdate);
        if (frameId) {
            window.cancelAnimationFrame(frameId);
        }
        card.classList.remove("is-stuck");
        card.style.removeProperty("left");
        card.style.removeProperty("width");
        card.style.removeProperty("top");
        sentinel.remove();
    };
}

/**
 * Рендерит компактную карточку результата поиска для списка маршрутов.
 */
function buildRouteSummaryCard(route) {
    const routeNumber = escapeHtml(formatRouteNumber(route));
    const routeHref = escapeHtml(buildRouteHref(route));
    const next = findNearestSchedule((route.schedules || []).filter((schedule) => scheduleRunsOnDay(schedule, getTodayScheduleDay())));

    return `
        <a class="schedule-route-card schedule-route-card--link schedule-route-card--summary" href="${routeHref}" aria-label="Открыть маршрут ${routeNumber}" data-search-text="${escapeHtml(buildSearchText(route))}">
            <span class="schedule-route-summary-number">Автобус №${routeNumber}</span>
            <h2 class="schedule-route-summary-title">${escapeHtml(route.name || "Маршрут не указан")}</h2>
            <span class="schedule-route-summary-next">${next ? `Ближайшее: ${escapeHtml(next.departureTime)}` : "Отправлений на сегодня нет"}</span>
        </a>
    `;
}

/**
 * Рендерит развёрнутую карточку маршрута с секциями по пунктам отправления.
 */
function buildRouteDetailCard(route, selectedDay = getTodayScheduleDay()) {
    const routeNumber = escapeHtml(formatRouteNumber(route));
    const title = escapeHtml(route.name || "Без названия");
    const description = escapeHtml(getRouteDescription(route) || "Описание не указано");
    const travelTime = escapeHtml(route.travelTime || "Время не указано");
    const pricePrimary = escapeHtml(route.pricePrimary || "Цена не указана");
    const priceSecondary = escapeHtml(route.priceSecondary || "Цена не указана");
    const paymentMethods = escapeHtml(route.paymentMethods || "Способ оплаты не указан");
    const allSchedules = Array.isArray(route.schedules) ? route.schedules : [];
    const schedules = allSchedules.filter((schedule) => scheduleRunsOnDay(schedule, selectedDay));
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
                ${description ? `<p class="schedule-route-description">${description}</p>` : ""}
            </header>
            <div class="schedule-route-body">
                ${buildUpcomingCard(allSchedules)}
                ${buildDayPicker(selectedDay)}
                <div class="schedule-departures">
                    ${schedulesMarkup}
                </div>
            </div>
        </article>
    `;
}

function renderRoutes(routes, container, detailMode = false, selectedDay = getTodayScheduleDay()) {
    if (!routes.length) {
        container.innerHTML = `
            <div class="schedule-empty-state">
                <strong>Расписание не найдено</strong>
                <p>Проверьте источник данных или попробуйте обновить страницу позже.</p>
            </div>
        `;
        return;
    }

    container.innerHTML = detailMode
        ? buildRouteDetailCard(routes[0], selectedDay)
        : routes.map(buildRouteSummaryCard).join("");
}

/**
 * Загружает опубликованный JSON-файл расписания из GitHub.
 */
async function loadScheduleData() {
    const response = await fetch(DEFAULT_SCHEDULE_SOURCE, { cache: "no-store" });
    if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
    }

    return response.json();
}

/**
 * Подготавливает страницу расписания и связывает поиск, кэш и процесс загрузки в один поток.
 */
function setupSchedulePage() {
    const results = document.getElementById("schedule-results");
    const statusMessage = document.getElementById("schedule-status-message");
    const searchInput = document.getElementById("schedule-search");
    const loadStateLabel = document.getElementById("schedule-load-state");
    const updatedAtLabel = document.getElementById("schedule-updated-at");

    if (!results || !statusMessage || !searchInput || !loadStateLabel || !updatedAtLabel) {
        return;
    }

    // Параметр route открывает конкретный маршрут из единого файла расписания.
    const params = new URLSearchParams(window.location.search);
    currentRouteQuery = params.get("route") || "";
    let allRoutes = [];
    let activeLoadToken = 0;
    let selectedScheduleDay = getTodayScheduleDay();

    // Фильтрация пересчитывается и при вводе текста, и после загрузки новых данных.
    const applyFilter = () => {
        const query = normalizeText(searchInput.value);
        const routeScopedRoutes = currentRouteQuery
            ? allRoutes.filter((route) => routeMatchesQuery(route, currentRouteQuery))
            : allRoutes;
        const filteredRoutes = query
            ? routeScopedRoutes.filter((route) => buildSearchText(route).includes(query))
            : routeScopedRoutes;
        const detailMode = Boolean(currentRouteQuery);

        renderRoutes(filteredRoutes, results, detailMode, selectedScheduleDay);
        setupUpcomingStickyState();

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

    /**
     * Обновляет метку времени, используя стандартный формат страницы.
     */
    const updateTimestamp = (value = new Date()) => {
        updatedAtLabel.textContent = formatScheduleDate(value);
    };

    // Основной поток загрузки сначала показывает кэш, а затем по возможности обновляет его из сети.
    const load = async () => {
        const loadToken = ++activeLoadToken;
        const cached = readScheduleCache(DEFAULT_SCHEDULE_SOURCE);

        if (cached) {
            allRoutes = sortRoutes(cached.routes.map((route) => ({
                ...route,
                schedules: Array.isArray(route.schedules) ? route.schedules : []
            })));
            if (cached.updatedAt) {
                updatedAtLabel.textContent = formatScheduleDate(cached.updatedAt);
            }
            loadStateLabel.textContent = `Загружено: ${allRoutes.length}`;
            statusMessage.textContent = "";
            applyFilter();
        } else {
            loadStateLabel.textContent = "Загрузка...";
            statusMessage.textContent = "Загружаем расписание из GitHub...";
            results.innerHTML = "";
        }

        try {
            const payload = await loadScheduleData();
            if (loadToken !== activeLoadToken) {
                return;
            }
            const routes = sortRoutes(parseRoutesPayload(payload).filter(Boolean));

            allRoutes = routes.map((route) => ({
                ...route,
                schedules: Array.isArray(route.schedules) ? route.schedules : []
            }));

            const nowIso = new Date().toISOString();
            writeScheduleCache(DEFAULT_SCHEDULE_SOURCE, allRoutes, nowIso);
            updateTimestamp();
            loadStateLabel.textContent = `Загружено: ${allRoutes.length}`;
            applyFilter();
        } catch (error) {
            if (cached) {
                loadStateLabel.textContent = `Загружено: ${allRoutes.length}`;
                return;
            }
            loadStateLabel.textContent = "Ошибка загрузки";
            statusMessage.textContent = "Не удалось получить расписание из GitHub.";
            results.innerHTML = `
                <div class="schedule-empty-state schedule-empty-state-error">
                    <strong>Источник недоступен</strong>
                    <p>Попробуйте обновить страницу позже.</p>
                    <p class="schedule-error-detail">${escapeHtml(error.message || "unknown error")}</p>
                </div>
            `;
        }
    };

    searchInput.addEventListener("input", applyFilter);
    document.querySelector("[data-focus-schedule-search]")?.addEventListener("click", () => {
        searchInput.scrollIntoView({ behavior: "smooth", block: "center" });
        window.setTimeout(() => searchInput.focus({ preventScroll: true }), 250);
    });
    document.addEventListener("keydown", (event) => {
        if (event.key === "/" && !event.ctrlKey && !event.metaKey && document.activeElement !== searchInput) {
            event.preventDefault();
            searchInput.focus();
        }
    });
    results.addEventListener("click", (event) => {
        const dayButton = event.target.closest("[data-schedule-day]");
        if (!dayButton) {
            return;
        }

        selectedScheduleDay = dayButton.dataset.scheduleDay || getTodayScheduleDay();
        applyFilter();
    });
    load();
}

setupSchedulePage();
