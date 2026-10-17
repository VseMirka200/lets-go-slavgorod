package ru.slavgorod.transport.data.repository

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import ru.slavgorod.transport.R
import ru.slavgorod.transport.core.AppText
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
import timber.log.Timber
import java.time.DayOfWeek
import java.time.LocalDate

open class ScheduleJsonParser(
    private val jsonDataSource: JsonDataSource = JsonDataSource()
) {

    open suspend fun parseRoutes(jsonString: String): List<BusRoute> {
        return jsonDataSource.parseRoutesFromJson(jsonString)
    }

    open fun buildSchedulesIndex(jsonString: String): Map<String, List<BusSchedule>> {
        return try {
            val jsonObject = JsonParser.parseString(jsonString).asJsonObject
            val productionCalendar = jsonObject.parseProductionCalendar()
            jsonObject
                .getAsJsonArray(RoutesJsonContract.ROOT_ROUTES)
                ?.mapNotNull { routeElement ->
                    parseRouteSchedules(routeElement, productionCalendar)
                }
                ?.toMap()
                .orEmpty()
        } catch (exception: Exception) {
            Timber.w(exception, "Failed to build schedules index")
            emptyMap()
        }
    }

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
            "note",
            "remarks",
            "comment",
            AppText.get(R.string.schedule_json_note_key),
            AppText.get(R.string.schedule_json_notes_key)
        ),
        departurePoint = departurePoint,
    )
}

private fun com.google.gson.JsonObject.parseServiceCalendar(
    dayType: String?,
    productionCalendar: ProductionCalendar
): ServiceCalendar {
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
