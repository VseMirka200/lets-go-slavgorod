package ru.slavgorod.transport.data.repository

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import ru.slavgorod.transport.R
import ru.slavgorod.transport.core.AppText
import ru.slavgorod.transport.data.local.JsonDataSource
import ru.slavgorod.transport.data.local.RoutesJsonContract
import ru.slavgorod.transport.data.local.asJsonObjectOrNull
import ru.slavgorod.transport.data.local.optionalText
import ru.slavgorod.transport.data.local.requiredText
import ru.slavgorod.transport.data.model.BusRoute
import ru.slavgorod.transport.data.model.BusSchedule
import timber.log.Timber

open class ScheduleJsonParser(
    private val jsonDataSource: JsonDataSource = JsonDataSource()
) {

    open suspend fun parseRoutes(jsonString: String): List<BusRoute> {
        return jsonDataSource.parseRoutesFromJson(jsonString)
    }

    open fun buildSchedulesIndex(jsonString: String): Map<String, List<BusSchedule>> {
        return try {
            val jsonObject = JsonParser.parseString(jsonString).asJsonObject
            jsonObject
                .getAsJsonArray(RoutesJsonContract.ROOT_ROUTES)
                ?.mapNotNull { routeElement ->
                    parseRouteSchedules(routeElement)
                }
                ?.toMap()
                .orEmpty()
        } catch (exception: Exception) {
            Timber.w(exception, "Failed to build schedules index")
            emptyMap()
        }
    }
}

private fun parseRouteSchedules(routeElement: JsonElement): Pair<String, List<BusSchedule>>? {
    val routeJson = routeElement.asJsonObjectOrNull() ?: return null
    val routeId = routeJson.requiredText(RoutesJsonContract.ROUTE_ID) ?: return null
    val schedulesArray = routeJson.getAsJsonArray(RoutesJsonContract.ROUTE_SCHEDULES)
        ?: return routeId to emptyList()

    return routeId to schedulesArray.mapNotNull { scheduleElement ->
        buildBusSchedule(routeId, scheduleElement)
    }
}

private fun buildBusSchedule(
    routeId: String,
    scheduleElement: JsonElement
): BusSchedule? {
    val scheduleJson = scheduleElement.asJsonObjectOrNull() ?: return null
    val scheduleId = scheduleJson.requiredText(RoutesJsonContract.SCHEDULE_ID) ?: return null
    val departurePoint = scheduleJson.requiredText(RoutesJsonContract.SCHEDULE_DEPARTURE_POINT)
        ?: return null
    val departureTime = scheduleJson.requiredText(RoutesJsonContract.SCHEDULE_DEPARTURE_TIME)
        ?: return null

    return BusSchedule(
        id = scheduleId,
        routeId = routeId,
        stopName = departurePoint,
        departureTime = departureTime,
        dayOfWeek = 0,
        isWeekend = false,
        dayType = scheduleJson.optionalText(RoutesJsonContract.SCHEDULE_DAY_TYPE),
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
