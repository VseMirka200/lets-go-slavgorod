package ru.slavgorod.transport.ui.screens

import ru.slavgorod.transport.data.model.BusRoute
import ru.slavgorod.transport.data.model.BusSchedule
import ru.slavgorod.transport.data.repository.RoutesTableDataSource
import ru.slavgorod.transport.domain.util.UpcomingScheduleResolver
import java.util.Calendar

internal data class SchedulePointData(
    val name: String,
    val schedules: List<BusSchedule>,
    val nextUpcomingId: String?
)

internal data class NextUpcomingScheduleData(
    val schedule: BusSchedule,
    val departurePoint: String
)

internal data class ScheduleScreenData(
    val points: List<SchedulePointData>,
    val nextUpcomingSchedule: NextUpcomingScheduleData? = null
) {
    val totalSchedules: Int
        get() = points.sumOf { it.schedules.size }
}

internal suspend fun loadRouteSchedules(
    route: BusRoute,
    routeRepository: RoutesTableDataSource
): List<BusSchedule> = routeRepository.getSchedulesForRoute(route.id)

internal fun buildScheduleScreenData(
    allSchedules: List<BusSchedule>,
    currentTime: Calendar = Calendar.getInstance()
): ScheduleScreenData {
    val orderedDeparturePoints = allSchedules.mapNotNull { schedule ->
        schedule.departurePoint
            .trim()
            .takeIf(String::isNotBlank)
    }.distinct()

    val points = orderedDeparturePoints.map { departurePoint ->
        buildSchedulePointData(
            departurePoint = departurePoint,
            allSchedules = allSchedules,
            currentTime = currentTime
        )
    }

    return ScheduleScreenData(
        points = points,
        nextUpcomingSchedule = resolveNextUpcomingSchedule(allSchedules, currentTime)
    )
}

internal fun ScheduleScreenData.withCurrentTime(currentTimeMillis: Long): ScheduleScreenData {
    val currentTime = Calendar.getInstance().apply {
        timeInMillis = currentTimeMillis
    }
    val livePoints = points.map { point ->
        point.copy(
            nextUpcomingId = UpcomingScheduleResolver.findNextScheduleId(
                point.schedules,
                currentTime
            )
        )
    }

    return copy(
        points = livePoints,
        nextUpcomingSchedule = resolveNextUpcomingSchedule(
            allSchedules = livePoints.flatMap { point -> point.schedules },
            currentTime = currentTime
        )
    )
}

internal fun buildScheduleExtraLabel(
    schedule: BusSchedule,
    platformPrefixLabel: String,
    weekdayLabel: String,
    weekendLabel: String,
    dailyLabel: String
): String? {
    return buildList {
        addDistinctLabel(
            schedule.platform
                ?.trim()
                ?.takeIf(String::isNotBlank)
                ?.let { formatPlatformLabel(platformPrefixLabel, it) }
        )
        addDistinctLabel(schedule.variant?.trim()?.takeIf(String::isNotBlank))
        addDistinctLabel(
            formatDayTypeLabel(
                schedule.dayType,
                weekdayLabel,
                weekendLabel,
                dailyLabel
            )
        )
        addDistinctLabel(schedule.notes?.trim()?.takeIf(String::isNotBlank))
    }
        .joinToString(" \u2022 ")
        .takeIf(String::isNotBlank)
}

private fun buildSchedulePointData(
    departurePoint: String,
    allSchedules: List<BusSchedule>,
    currentTime: Calendar
): SchedulePointData {
    val schedules = allSchedules
        .filter { it.departurePoint.trim() == departurePoint }
        .sortedBy(BusSchedule::departureTime)

    return SchedulePointData(
        name = departurePoint,
        schedules = schedules,
        nextUpcomingId = UpcomingScheduleResolver.findNextScheduleId(schedules, currentTime)
    )
}

private fun resolveNextUpcomingSchedule(
    allSchedules: List<BusSchedule>,
    currentTime: Calendar
): NextUpcomingScheduleData? {
    val nextScheduleId =
        UpcomingScheduleResolver.findNextScheduleId(allSchedules, currentTime) ?: return null
    val nextSchedule = allSchedules.firstOrNull { it.id == nextScheduleId } ?: return null

    return NextUpcomingScheduleData(
        schedule = nextSchedule,
        departurePoint = nextSchedule.departurePoint
    )
}

private fun MutableList<String>.addDistinctLabel(value: String?) {
    val candidate = value.normalizedScheduleLabel() ?: return
    if (none { existing -> existing.normalizedScheduleLabel() == candidate.lowercase() }) {
        add(candidate)
    }
}

private fun formatPlatformLabel(platformPrefix: String, platform: String): String {
    return "$platformPrefix $platform"
}

private fun formatDayTypeLabel(
    dayType: String?,
    weekdayLabel: String,
    weekendLabel: String,
    dailyLabel: String
): String? {
    return when (dayType?.trim()?.lowercase()) {
        "weekday" -> weekdayLabel
        "weekend" -> weekendLabel
        "daily" -> dailyLabel
        else -> dayType?.trim()?.takeIf(String::isNotBlank)
    }
}

private fun String?.normalizedScheduleLabel(): String? {
    return this
        ?.trim()
        ?.replace(Regex("\\s+"), " ")
        ?.takeIf(String::isNotBlank)
        ?.lowercase()
}
