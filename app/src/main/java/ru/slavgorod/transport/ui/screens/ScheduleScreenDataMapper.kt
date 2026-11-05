package ru.slavgorod.transport.ui.screens

import ru.slavgorod.transport.data.model.BusRoute
import ru.slavgorod.transport.data.model.BusSchedule
import ru.slavgorod.transport.data.repository.RoutesTableDataSource

internal data class SchedulePointData(
    val name: String,
    val schedules: List<BusSchedule>
)

internal data class ScheduleScreenData(
    val points: List<SchedulePointData>
) {
    val totalSchedules: Int
        get() = points.sumOf { it.schedules.size }
}

internal suspend fun loadRouteSchedules(
    route: BusRoute,
    routeRepository: RoutesTableDataSource
): List<BusSchedule> = routeRepository.getSchedulesForRoute(route.id)

internal fun buildScheduleScreenData(allSchedules: List<BusSchedule>): ScheduleScreenData {
    val orderedDeparturePoints = allSchedules.mapNotNull { schedule ->
        schedule.departurePoint
            .trim()
            .takeIf(String::isNotBlank)
    }.distinct()

    val points = orderedDeparturePoints.map { departurePoint ->
        buildSchedulePointData(
            departurePoint = departurePoint,
            allSchedules = allSchedules
        )
    }

    return ScheduleScreenData(points = points)
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
    allSchedules: List<BusSchedule>
): SchedulePointData {
    val schedules = allSchedules
        .filter { it.departurePoint.trim() == departurePoint }
        .sortedBy(BusSchedule::departureTime)

    return SchedulePointData(
        name = departurePoint,
        schedules = schedules
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
