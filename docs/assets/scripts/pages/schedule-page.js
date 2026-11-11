/**
 * Адрес источника задаётся в data-schedule-source страницы расписания.
 */
const DEFAULT_SCHEDULE_SOURCE = document.body.dataset.scheduleSource;

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
let productionNonWorkingDates = new Set();

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
            updatedAt: parsed.updatedAt || null,
            productionCalendar: parsed.productionCalendar || null
        };
    } catch {
        return null;
    }
}

/**
 * Записывает нормализованный список маршрутов в localStorage для офлайн-доступа.
 */
function writeScheduleCache(sourceUrl, routes, updatedAt, productionCalendar = null) {
    if (!canUseStorage()) {
        return;
    }

    try {
        window.localStorage.setItem(getScheduleCacheKey(sourceUrl), JSON.stringify({
            routes,
            updatedAt: updatedAt || new Date().toISOString(),
            productionCalendar
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

function formatTimerDuration(durationMilliseconds) {
    const totalSeconds = Math.max(0, Math.floor(durationMilliseconds / 1000));
    const hours = Math.floor(totalSeconds / 3600);
    const minutes = Math.floor((totalSeconds % 3600) / 60);
    const seconds = totalSeconds % 60;
    return `${hours ? `${hours}ч ` : ""}${minutes}м ${String(seconds).padStart(2, "0")}с`;
}

function getDepartureStatus(schedules) {
    const now = new Date();
    const parsed = schedules
        .map((schedule, index) => ({ schedule, index, minutes: parseDepartureMinutes(schedule.departureTime) }))
        .filter((item) => item.minutes !== null)
        .sort((left, right) => left.minutes - right.minutes || left.index - right.index);
    const departureDate = (item, dayOffset) => {
        const departure = new Date(now);
        departure.setHours(Math.floor(item.minutes / 60), item.minutes % 60, 0, 0);
        departure.setDate(departure.getDate() + dayOffset);
        return departure;
    };
    const next = parsed
        .map((item) => ({ item, time: departureDate(item, departureDate(item, 0) > now ? 0 : 1) }))
        .sort((left, right) => left.time - right.time)[0];
    const inTransit = parsed
        .map((item) => ({ item, time: departureDate(item, departureDate(item, 0) <= now ? 0 : -1) }))
        .sort((left, right) => right.time - left.time)[0];
    return {
        inTransit: inTransit ? { schedule: inTransit.item.schedule, elapsedMilliseconds: now - inTransit.time } : null,
        next: next ? { schedule: next.item.schedule, countdownMilliseconds: next.time - now } : null
    };
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

/**
 * Определяет, выполняется ли рейс в ближайшую дату выбранного дня недели.
 * Учитывает период действия, включённые и исключённые даты, старое поле dayType
 * и праздничные дни. Исключение имеет приоритет над включением.
 * @param {object} schedule Рейс из источника расписания.
 * @param {string} day Английский идентификатор дня недели, например MONDAY.
 * @returns {boolean} Выполняется ли рейс в ближайшую выбранную дату.
 */
function scheduleRunsOnDay(schedule, day) {
    const today = new Date();
    const dayIndex = SCHEDULE_DAYS.findIndex((item) => item.key === day);
    if (dayIndex < 0) return false;
    const currentIndex = (today.getDay() + 6) % 7;
    today.setDate(today.getDate() + (dayIndex - currentIndex + 7) % 7);
    const date = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, "0")}-${String(today.getDate()).padStart(2, "0")}`;
    const calendar = schedule.serviceCalendar;
    const legacy = { weekday: ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"], weekend: ["SATURDAY", "SUNDAY"], saturday: ["SATURDAY"], sunday: ["SUNDAY"] };
    const aliases = { MON: "MONDAY", TUE: "TUESDAY", WED: "WEDNESDAY", THU: "THURSDAY", FRI: "FRIDAY", SAT: "SATURDAY", SUN: "SUNDAY" };
    const days = calendar ? (Array.isArray(calendar.daysOfWeek) ? [...new Set(calendar.daysOfWeek.map((value) => aliases[String(value).trim().toUpperCase()] || String(value).trim().toUpperCase()).filter((value) => SCHEDULE_DAYS.some((item) => item.key === value)))] : null)
        : (legacy[String(schedule.dayType || "").trim().toLowerCase()] || null);
    const weekday = days?.length === 5 && ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"].every((value) => days.includes(value));
    const weekend = days?.length === 2 && ["SATURDAY", "SUNDAY"].every((value) => days.includes(value));
    const excluded = Array.isArray(calendar?.excludedDates) && calendar.excludedDates.includes(date);
    const included = Array.isArray(calendar?.includedDates) && calendar.includedDates.includes(date);
    const validFrom = parseCalendarDate(calendar?.validFrom);
    const validUntil = parseCalendarDate(calendar?.validUntil);
    if (excluded || (weekday && productionNonWorkingDates.has(date))) return false;
    if ((validFrom && date < validFrom) || (validUntil && date > validUntil)) return false;
    if (included || (weekend && productionNonWorkingDates.has(date))) return true;
    return days === null || days.includes(day);
}

/**
 * Проверяет дату календаря и сохраняет её в формате ГГГГ-ММ-ДД.
 * @param {unknown} value Значение из внешнего источника.
 * @returns {string|null} Проверенная дата или null при неверном формате и дне.
 */
function parseCalendarDate(value) {
    if (typeof value !== "string" || !/^\d{4}-\d{2}-\d{2}$/.test(value)) return null;
    const date = new Date(`${value}T00:00:00Z`);
    return Number.isNaN(date.getTime()) || date.toISOString().slice(0, 10) !== value ? null : value;
}

/**
 * Заменяет набор праздничных дат, используемый при фильтрации рейсов.
 * Отсутствующий календарь означает пустой набор, а не сохранение старых дат.
 * @param {object|null|undefined} calendar Календарь из ответа источника или кэша.
 */
function setProductionCalendar(calendar) {
    productionNonWorkingDates = new Set(Object.values(calendar?.years || {}).flatMap((year) => Array.isArray(year?.nonWorkingDates) ? year.nonWorkingDates : []));
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

/**
 * Формирует карточку ближайших отправлений по сегодняшним рейсам.
 * Данные, вставляемые в разметку, экранируются до передачи в innerHTML.
 * @param {object[]} schedules Все рейсы выбранного маршрута.
 * @returns {string} Разметка карточки или пустая строка, если рейсов нет.
 */
function buildUpcomingCard(schedules) {
    const todaySchedules = schedules.filter((schedule) => scheduleRunsOnDay(schedule, getTodayScheduleDay()));
    const groups = groupSchedulesByDeparturePoint(todaySchedules);
    let savedOrder = [];
    try { savedOrder = JSON.parse(window.localStorage.getItem("schedule-upcoming-order") || "[]"); } catch { /* storage unavailable */ }
    if (!Array.isArray(savedOrder)) savedOrder = [];
    groups.sort((a, b) => {
        const rank = (group) => { const index = savedOrder.indexOf(group.label); return index < 0 ? savedOrder.length : index; };
        return rank(a) - rank(b);
    });
    const entries = groups.map((group) => {
        const { inTransit, next } = getDepartureStatus(group.schedules);
        if (!inTransit && !next) return "";
        const transitColumn = inTransit
            ? `<div class="schedule-upcoming-time"><small>В пути</small><strong>${escapeHtml(inTransit.schedule.departureTime || "—")}</strong><span>${formatTimerDuration(inTransit.elapsedMilliseconds)}</span></div>`
            : `<div class="schedule-upcoming-time schedule-upcoming-time--empty"><span>Нет автобуса в пути</span></div>`;
        const nextColumn = next
            ? `<div class="schedule-upcoming-time"><small>Отправление</small><strong>${escapeHtml(next.schedule.departureTime || "—")}</strong><span>${formatTimerDuration(next.countdownMilliseconds)}</span></div>`
            : `<div class="schedule-upcoming-time schedule-upcoming-time--empty"><span>Сегодня отправлений нет</span></div>`;
        return `<li data-upcoming-point="${escapeHtml(group.label)}"><span class="schedule-upcoming-point">${escapeHtml(group.label)}</span><div class="schedule-upcoming-times">${transitColumn}${nextColumn}</div></li>`;
    }).filter(Boolean).join("");

    if (!entries) return "";
    return `
        <section class="schedule-upcoming-card" aria-label="Ближайшее отправление">
            <div class="schedule-upcoming-heading"><span>Ближайшие отправления</span><small>${getDayLabel(getTodayScheduleDay())}</small></div>
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
        const sidebar = isMobile ? document.querySelector(".portal-sidebar") : null;
        const sidebarPosition = sidebar ? window.getComputedStyle(sidebar).position : "static";
        const stickyTop = sidebar && (sidebarPosition === "sticky" || sidebarPosition === "fixed")
            ? Math.max(0, sidebar.getBoundingClientRect().bottom)
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
                    <span>Маршрут: ${route.name ? title : `Автобус №${routeNumber}`}</span>
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
 * Загружает JSON или публичный Google Apps Script. JSONP используется только
 * для явно настроенного доверенного адреса script.google.com, не из URL страницы.
 */
async function loadScheduleData() {
    const url = new URL(DEFAULT_SCHEDULE_SOURCE);
    if (url.protocol !== "https:") {
        throw new Error("Источник расписания должен использовать HTTPS");
    }
    if (url.hostname === "script.google.com" && /^\/macros\/s\/[^/]+\/exec$/.test(url.pathname)) {
        return loadGoogleSchedule(url);
    }
    const response = await fetch(DEFAULT_SCHEDULE_SOURCE, { cache: "no-store" });
    if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
    }

    return response.json();
}

function loadGoogleSchedule(url) {
    return new Promise((resolve, reject) => {
        const callbackName = `scheduleJsonp_${Date.now()}_${Math.random().toString(36).slice(2)}`;
        const script = document.createElement("script");
        let timeout;
        const cleanup = () => {
            window.clearTimeout(timeout);
            delete window[callbackName];
            script.remove();
        };
        window[callbackName] = (payload) => {
            cleanup();
            resolve(payload);
        };
        script.onerror = () => {
            cleanup();
            reject(new Error("Не удалось загрузить источник расписания"));
        };
        url.searchParams.set("callback", callbackName);
        script.src = url.toString();
        timeout = window.setTimeout(() => {
            cleanup();
            reject(new Error("Время ожидания источника расписания истекло"));
        }, 15_000);
        document.head.appendChild(script);
    });
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
    const sourceLink = document.querySelector("[data-schedule-source-link]");

    if (!results || !statusMessage || !searchInput || !loadStateLabel || !updatedAtLabel) {
        return;
    }
    if (sourceLink && DEFAULT_SCHEDULE_SOURCE) sourceLink.href = DEFAULT_SCHEDULE_SOURCE;

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
            setProductionCalendar(cached.productionCalendar);
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
            statusMessage.textContent = "Загружаем расписание...";
            results.innerHTML = "";
        }

        try {
            const payload = await loadScheduleData();
            if (loadToken !== activeLoadToken) {
                return;
            }
            const routes = sortRoutes(parseRoutesPayload(payload).filter(Boolean));
            if (!routes.length) {
                throw new Error("Источник не вернул корректное расписание");
            }

            setProductionCalendar(payload?.productionCalendar);
            allRoutes = routes.map((route) => ({
                ...route,
                schedules: Array.isArray(route.schedules) ? route.schedules : []
            }));

            const nowIso = new Date().toISOString();
            writeScheduleCache(DEFAULT_SCHEDULE_SOURCE, allRoutes, nowIso, payload?.productionCalendar || null);
            updateTimestamp();
            loadStateLabel.textContent = `Загружено: ${allRoutes.length}`;
            applyFilter();
        } catch (error) {
            if (loadToken !== activeLoadToken) {
                return;
            }
            if (cached) {
                loadStateLabel.textContent = `Загружено: ${allRoutes.length}`;
                return;
            }
            loadStateLabel.textContent = "Ошибка загрузки";
            statusMessage.textContent = "Не удалось получить расписание.";
            results.innerHTML = `
                <div class="schedule-empty-state schedule-empty-state-error">
                    <strong>Источник недоступен</strong>
                    <p>Попробуйте обновить страницу позже.</p>
                    <p class="schedule-error-detail">${escapeHtml(error.message || "неизвестная ошибка")}</p>
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
    let draggedCard = null;
    let dragTimer = null;
    let dragPointer = null;
    results.addEventListener("pointerdown", (event) => {
        const card = event.target.closest(".schedule-upcoming-card li[data-upcoming-point]");
        if (!card || event.button !== 0) return;
        dragPointer = event.pointerId;
        dragTimer = window.setTimeout(() => {
            draggedCard = card;
            card.classList.add("is-dragging");
            card.setPointerCapture(event.pointerId);
        }, 350);
    });
    results.addEventListener("pointermove", (event) => {
        if (!draggedCard || event.pointerId !== dragPointer) return;
        event.preventDefault();
        const list = draggedCard.parentElement;
        const target = document.elementFromPoint(event.clientX, event.clientY)?.closest("li[data-upcoming-point]");
        if (!target || target === draggedCard || target.parentElement !== list) return;
        const cards = [...list.children];
        const before = new Map(cards.map((item) => [item, item.getBoundingClientRect()]));
        for (const item of cards) item.getAnimations().forEach((animation) => animation.cancel());
        const movingForward = cards.indexOf(draggedCard) < cards.indexOf(target);
        list.insertBefore(draggedCard, movingForward ? target.nextSibling : target);
        for (const item of cards) {
            const old = before.get(item), next = item.getBoundingClientRect();
            item.animate([{ transform: `translate(${old.left - next.left}px, ${old.top - next.top}px)` }, { transform: "translate(0, 0)" }], { duration: 220, easing: "ease-out" });
        }
    });
    const finishUpcomingDrag = (event) => {
        if (event.pointerId !== dragPointer) return;
        window.clearTimeout(dragTimer);
        dragTimer = null;
        dragPointer = null;
        if (!draggedCard) return;
        const order = [...draggedCard.parentElement.children].map((item) => item.dataset.upcomingPoint);
        try {
            const saved = JSON.parse(window.localStorage.getItem("schedule-upcoming-order") || "[]");
            window.localStorage.setItem("schedule-upcoming-order", JSON.stringify([...order, ...(Array.isArray(saved) ? saved.filter((name) => !order.includes(name)) : [])]));
        } catch { /* storage unavailable */ }
        draggedCard.classList.remove("is-dragging");
        draggedCard = null;
    };
    results.addEventListener("pointerup", finishUpcomingDrag);
    results.addEventListener("pointercancel", finishUpcomingDrag);
    let lastRefreshMinute = Math.floor(Date.now() / 60_000);
    window.setInterval(() => {
        if (draggedCard || dragTimer) return;
        const minute = Math.floor(Date.now() / 60_000);
        if (!currentRouteQuery || !allRoutes.length) return;
        if (minute !== lastRefreshMinute) {
            lastRefreshMinute = minute;
            applyFilter();
            return;
        }
        const route = allRoutes.find((item) => routeMatchesQuery(item, currentRouteQuery));
        const list = results.querySelector(".schedule-upcoming-card ul");
        if (route && list) {
            const template = document.createElement("template");
            template.innerHTML = buildUpcomingCard(route.schedules || []);
            const updatedList = template.content.querySelector("ul");
            if (updatedList) list.replaceChildren(...updatedList.childNodes);
        }
    }, 1_000);
    load();
}

setupSchedulePage();
