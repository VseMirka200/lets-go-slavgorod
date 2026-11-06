package ru.slavgorod.transport.data.repository

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import ru.slavgorod.transport.data.local.JsonDataSource
import ru.slavgorod.transport.data.local.RoutesJsonContract
import ru.slavgorod.transport.data.local.asJsonObjectOrNull
import ru.slavgorod.transport.data.local.optionalText
import ru.slavgorod.transport.data.local.optionalTextList
import ru.slavgorod.transport.data.local.requiredText
import ru.slavgorod.transport.data.model.BusRoute
import ru.slavgorod.transport.data.model.BusSchedule
import ru.slavgorod.transport.data.model.ServiceCalendar
import ru.slavgorod.transport.data.model.ScheduleChange
import ru.slavgorod.transport.domain.util.ValidationUtils
import timber.log.Timber
import java.time.DayOfWeek
import java.time.LocalDate

/** Проверенные маршруты и построенный по их идентификаторам индекс отправлений. */
data class ParsedScheduleSnapshot(
    val routes: List<BusRoute>,
    val schedules: Map<String, List<BusSchedule>>
)

/**
 * Разбирает производные данные расписания из исходного JSON.
 *
 * Подсказка разработчику: поддержка старых полей нужна для уже опубликованных файлов;
 * не удаляйте её без миграции данных на стороне источника.
 */
open class ScheduleJsonParser(
    private val jsonDataSource: JsonDataSource = JsonDataSource()
) {

    /**
     * Разбирает JSON один раз и возвращает согласованный снимок либо `null` при ошибке.
     * Ни маршруты, ни рейсы из неполного снимка не должны заменять сохранённую копию.
     * Вызывающему коду следует выполнять разбор вне главного потока.
     *
     * @param json Полный ответ источника или ранее сохранённая копия.
     * @return Проверенный снимок либо `null`, если структура или обязательные данные неверны.
     */
    open fun parseSnapshot(json: String): ParsedScheduleSnapshot? = try {
        val root = JsonParser.parseString(json).asJsonObject
        val routes = jsonDataSource.parseRoutesFromObject(root)
        val schedules = buildSchedulesIndex(root)
        if (isCompleteSnapshot(root, routes, schedules)) ParsedScheduleSnapshot(routes, schedules) else null
    } catch (exception: Exception) {
        Timber.w(exception, "Не удалось разобрать снимок расписания")
        null
    }

    /**
     * Проверяет соответствие разобранных данных исходному документу.
     * Требуются непустой список маршрутов, уникальные идентификаторы и корректное
     * время каждого рейса. Маршрут с пустым списком рейсов допустим.
     *
     * @param json Исходный документ, из которого получены [routes] и [schedules].
     * @return `true`, если ни один маршрут или рейс не потерян при разборе.
     */
    fun isCompleteSnapshot(
        json: String,
        routes: List<BusRoute>,
        schedules: Map<String, List<BusSchedule>>
    ): Boolean = try {
        isCompleteSnapshot(JsonParser.parseString(json).asJsonObject, routes, schedules)
    } catch (_: Exception) {
        false
    }

    private fun isCompleteSnapshot(
        root: JsonObject,
        routes: List<BusRoute>,
        schedules: Map<String, List<BusSchedule>>
    ): Boolean = try {
        val rawRoutes = root.getAsJsonArray(RoutesJsonContract.ROOT_ROUTES)
        val ids = routes.map { it.id }
        rawRoutes != null && routes.isNotEmpty() && rawRoutes.size() == routes.size &&
            ids.toSet().size == ids.size && schedules.keys == ids.toSet() &&
            rawRoutes.all { element ->
                val raw = element.asJsonObject
                val id = raw.requiredText(RoutesJsonContract.ROUTE_ID) ?: return@all false
                val entries = raw.get(RoutesJsonContract.ROUTE_SCHEDULES)
                if (entries == null || entries.isJsonNull) return@all schedules[id].isNullOrEmpty()
                if (!entries.isJsonArray) return@all false
                val parsed = schedules[id] ?: return@all false
                entries.asJsonArray.size() == parsed.size &&
                    parsed.map { it.id }.toSet().size == parsed.size &&
                    parsed.all { ValidationUtils.isValidTime(it.departureTime) }
            }
    } catch (_: Exception) {
        false
    }

    open suspend fun parseRoutes(jsonString: String): List<BusRoute> {
        return jsonDataSource.parseRoutesFromJson(jsonString)
    }

    /** Строит индекс по идентификаторам маршрутов; при ошибке возвращает пустую карту. */
    open fun buildSchedulesIndex(jsonString: String): Map<String, List<BusSchedule>> {
        return try {
            buildSchedulesIndex(JsonParser.parseString(jsonString).asJsonObject)
        } catch (exception: Exception) {
            Timber.w(exception, "Не удалось построить индекс рейсов")
            emptyMap()
        }
    }

    private fun buildSchedulesIndex(jsonObject: JsonObject): Map<String, List<BusSchedule>> {
        val productionCalendar = jsonObject.parseProductionCalendar()
        return jsonObject.getAsJsonArray(RoutesJsonContract.ROOT_ROUTES)
            ?.mapNotNull { parseRouteSchedules(it, productionCalendar) }
            ?.toMap().orEmpty()
    }

    /** Пропускает объявления без обязательных полей или корректной начальной даты. */
    open fun parseScheduleChanges(jsonString: String): List<ScheduleChange> = runCatching {
        val root = JsonParser.parseString(jsonString).asJsonObject
        root.getAsJsonArray(RoutesJsonContract.ROOT_SCHEDULE_CHANGES)
            ?.mapNotNull { element ->
                val item = element.asJsonObjectOrNull() ?: return@mapNotNull null
                val id = item.requiredText(RoutesJsonContract.CHANGE_ID) ?: return@mapNotNull null
                val title = item.requiredText(RoutesJsonContract.CHANGE_TITLE) ?: return@mapNotNull null
                val text = item.requiredText(RoutesJsonContract.CHANGE_TEXT) ?: return@mapNotNull null
                val validFrom = item.optionalText(RoutesJsonContract.CHANGE_VALID_FROM)
                    ?.toLocalDateOrNull() ?: return@mapNotNull null
                ScheduleChange(
                    id = id,
                    routeIds = item.optionalTextList(RoutesJsonContract.CHANGE_ROUTE_IDS).orEmpty().toSet(),
                    title = title,
                    text = text,
                    validFrom = validFrom,
                    validUntil = item.optionalText(RoutesJsonContract.CHANGE_VALID_UNTIL)?.toLocalDateOrNull()
                )
            }.orEmpty()
    }.getOrElse { emptyList() }
}

private fun parseRouteSchedules(
    routeElement: JsonElement,
    productionCalendar: ProductionCalendar
): Pair<String, List<BusSchedule>>? {
    val routeJson = routeElement.asJsonObjectOrNull() ?: return null
    val routeId = routeJson.requiredText(RoutesJsonContract.ROUTE_ID) ?: return null
    val schedulesArray = routeJson.getAsJsonArray(RoutesJsonContract.ROUTE_SCHEDULES)
        ?: return routeId to emptyList()

    return routeId to schedulesArray.mapNotNull { scheduleElement ->
        buildBusSchedule(routeId, scheduleElement, productionCalendar)
    }
}

private fun buildBusSchedule(
    routeId: String,
    scheduleElement: JsonElement,
    productionCalendar: ProductionCalendar
): BusSchedule? {
    val scheduleJson = scheduleElement.asJsonObjectOrNull() ?: return null
    val scheduleId = scheduleJson.requiredText(RoutesJsonContract.SCHEDULE_ID) ?: return null
    val departurePoint = scheduleJson.requiredText(RoutesJsonContract.SCHEDULE_DEPARTURE_POINT)
        ?: return null
    val departureTime = scheduleJson.requiredText(RoutesJsonContract.SCHEDULE_DEPARTURE_TIME)
        ?: return null
    val dayType = scheduleJson.optionalText(RoutesJsonContract.SCHEDULE_DAY_TYPE)

    return BusSchedule(
        id = scheduleId,
        routeId = routeId,
        stopName = departurePoint,
        departureTime = departureTime,
        dayOfWeek = 0,
        isWeekend = false,
        dayType = dayType,
        serviceCalendar = scheduleJson.parseServiceCalendar(dayType, productionCalendar),
        variant = scheduleJson.optionalText(RoutesJsonContract.SCHEDULE_VARIANT),
        platform = scheduleJson.optionalText(RoutesJsonContract.SCHEDULE_PLATFORM),
        notes = scheduleJson.optionalText(RoutesJsonContract.SCHEDULE_NOTES),
        remark = scheduleJson.optionalText(
            RoutesJsonContract.SCHEDULE_REMARK,
            RoutesJsonContract.SCHEDULE_LEGACY_NOTE,
            RoutesJsonContract.SCHEDULE_LEGACY_REMARKS,
            "comment",
            RoutesJsonContract.SCHEDULE_RUSSIAN_NOTE,
            RoutesJsonContract.SCHEDULE_RUSSIAN_NOTES
        ),
        departurePoint = departurePoint,
    )
}

private fun com.google.gson.JsonObject.parseServiceCalendar(
    dayType: String?,
    productionCalendar: ProductionCalendar
): ServiceCalendar {
    // Старые файлы задают дни работы только через тип дня.
    val calendarJson = getAsJsonObject(RoutesJsonContract.SCHEDULE_SERVICE_CALENDAR)
        ?: return legacyServiceCalendar(dayType).withProductionCalendar(productionCalendar)

    return ServiceCalendar(
        operatingDays = calendarJson.optionalTextList(RoutesJsonContract.CALENDAR_DAYS_OF_WEEK)
            ?.mapNotNull(::parseDayOfWeek)
            ?.toSet(),
        validFrom = calendarJson.optionalText(RoutesJsonContract.CALENDAR_VALID_FROM)
            ?.toLocalDateOrNull(),
        validUntil = calendarJson.optionalText(RoutesJsonContract.CALENDAR_VALID_UNTIL)
            ?.toLocalDateOrNull(),
        includedDates = calendarJson.optionalTextList(RoutesJsonContract.CALENDAR_INCLUDED_DATES)
            .orEmpty()
            .mapNotNull(String::toLocalDateOrNull)
            .toSet(),
        excludedDates = calendarJson.optionalTextList(RoutesJsonContract.CALENDAR_EXCLUDED_DATES)
            .orEmpty()
            .mapNotNull(String::toLocalDateOrNull)
            .toSet()
    ).withProductionCalendar(productionCalendar)
}

private fun com.google.gson.JsonObject.parseProductionCalendar(): ProductionCalendar {
    val calendarJson = getAsJsonObject(RoutesJsonContract.ROOT_PRODUCTION_CALENDAR)
        ?: return ProductionCalendar.Empty
    val yearsJson = calendarJson.getAsJsonObject(RoutesJsonContract.PRODUCTION_CALENDAR_YEARS)
        ?: return ProductionCalendar.Empty

    return ProductionCalendar(
        nonWorkingDates = yearsJson.entrySet().flatMap { (_, yearJson) ->
            yearJson.asJsonObjectOrNull()
                ?.optionalTextList(RoutesJsonContract.PRODUCTION_CALENDAR_NON_WORKING_DATES)
                .orEmpty()
                .mapNotNull(String::toLocalDateOrNull)
        }.toSet()
    )
}

private fun ServiceCalendar.withProductionCalendar(
    productionCalendar: ProductionCalendar
): ServiceCalendar {
    if (productionCalendar.nonWorkingDates.isEmpty()) return this

    // Праздники меняют только стандартные группы будних и выходных дней.
    return when (operatingDays) {
        Weekdays -> copy(excludedDates = excludedDates + productionCalendar.nonWorkingDates)
        Weekends -> copy(includedDates = includedDates + productionCalendar.nonWorkingDates)
        else -> this
    }
}

private fun legacyServiceCalendar(dayType: String?): ServiceCalendar {
    val operatingDays = when (dayType?.trim()?.lowercase()) {
        "weekday" -> setOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
        )
        "weekend" -> setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
        "saturday" -> setOf(DayOfWeek.SATURDAY)
        "sunday" -> setOf(DayOfWeek.SUNDAY)
        else -> return ServiceCalendar.Daily
    }
    return ServiceCalendar(operatingDays = operatingDays)
}

private data class ProductionCalendar(
    val nonWorkingDates: Set<LocalDate>
) {
    companion object {
        val Empty = ProductionCalendar(emptySet())
    }
}

private val Weekdays = setOf(
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY
)

private val Weekends = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

private fun parseDayOfWeek(value: String): DayOfWeek? = when (value.trim().uppercase()) {
    "MONDAY", "MON" -> DayOfWeek.MONDAY
    "TUESDAY", "TUE" -> DayOfWeek.TUESDAY
    "WEDNESDAY", "WED" -> DayOfWeek.WEDNESDAY
    "THURSDAY", "THU" -> DayOfWeek.THURSDAY
    "FRIDAY", "FRI" -> DayOfWeek.FRIDAY
    "SATURDAY", "SAT" -> DayOfWeek.SATURDAY
    "SUNDAY", "SUN" -> DayOfWeek.SUNDAY
    else -> null
}

private fun String.toLocalDateOrNull(): LocalDate? = runCatching(LocalDate::parse).getOrNull()
