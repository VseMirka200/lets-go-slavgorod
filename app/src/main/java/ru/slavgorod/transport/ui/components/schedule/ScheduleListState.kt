package ru.slavgorod.transport.ui.components.schedule

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.slavgorod.transport.data.model.BusSchedule
import ru.slavgorod.transport.ui.model.DeparturePointSchedules

internal data class ScheduleFilterState(
    val selectedDescriptionLabel: String?,
    val selectedDeparturePointId: String?
)

internal data class ScheduleListDisplayState(
    val allSchedules: List<BusSchedule>,
    val pointSections: List<ScheduleSectionDescriptor>,
    val upcomingEntries: List<UpcomingScheduleEntry>,
    val descriptionFilterLabels: List<String>,
    val departurePointFilterLabels: List<Pair<String, String>>,
    val visibleSections: List<ScheduleSectionDescriptor>,
    val visibleSchedulesCount: Int,
    val hasFilterControls: Boolean
)

internal fun buildScheduleListDisplayState(
    departurePoints: List<DeparturePointSchedules>,
    filterState: ScheduleFilterState,
    scheduleExtraLabelProvider: ((BusSchedule) -> String?)?
): ScheduleListDisplayState {
    val allSchedules = departurePoints.flatMap { it.schedules }
    val pointSections = departurePoints.mapIndexed { index, departurePoint ->
        ScheduleSectionDescriptor(
            key = (index + 1).toString(),
            title = departurePoint.name,
            schedules = departurePoint.schedules,
            nextUpcomingId = departurePoint.nextUpcomingId
        )
    }
    val upcomingEntries = departurePoints.mapNotNull { departurePoint ->
        buildUpcomingScheduleEntry(
            title = departurePoint.name,
            schedules = departurePoint.schedules,
            nextUpcomingId = departurePoint.nextUpcomingId
        )
    }
    val descriptionFilterLabels = buildDescriptionFilterLabels(
        allSchedules = allSchedules,
        scheduleExtraLabelProvider = scheduleExtraLabelProvider
    )
    val departurePointFilterLabels = pointSections.mapNotNull { section ->
        section.title.trim().takeIf(String::isNotBlank)?.let { title -> section.key to title }
    }
    val visibleSections = buildVisibleSections(
        pointSections = pointSections,
        filterState = filterState,
        scheduleExtraLabelProvider = scheduleExtraLabelProvider
    )

    return ScheduleListDisplayState(
        allSchedules = allSchedules,
        pointSections = pointSections,
        upcomingEntries = upcomingEntries,
        descriptionFilterLabels = descriptionFilterLabels,
        departurePointFilterLabels = departurePointFilterLabels,
        visibleSections = visibleSections,
        visibleSchedulesCount = visibleSections.sumOf { section -> section.schedules.size },
        hasFilterControls = descriptionFilterLabels.size > 1 || departurePointFilterLabels.size > 1
    )
}

internal fun resolveScheduleListBottomReserve(visibleSchedulesCount: Int): Dp {
    return when {
        visibleSchedulesCount <= 5 -> 860.dp
        visibleSchedulesCount <= 8 -> 520.dp
        visibleSchedulesCount <= 12 -> 180.dp
        else -> 0.dp
    }
}

private fun buildDescriptionFilterLabels(
    allSchedules: List<BusSchedule>,
    scheduleExtraLabelProvider: ((BusSchedule) -> String?)?
): List<String> {
    if (scheduleExtraLabelProvider == null) return emptyList()

    return allSchedules
        .mapNotNull { schedule ->
            scheduleExtraLabelProvider(schedule)?.trim()?.takeIf(String::isNotBlank)
        }
        .distinct()
}

private fun buildVisibleSections(
    pointSections: List<ScheduleSectionDescriptor>,
    filterState: ScheduleFilterState,
    scheduleExtraLabelProvider: ((BusSchedule) -> String?)?
): List<ScheduleSectionDescriptor> {
    return pointSections.map { section ->
        section.copy(
            schedules = section.schedules.filter { schedule ->
                matchesDescriptionFilter(
                    schedule = schedule,
                    descriptionProvider = scheduleExtraLabelProvider,
                    selectedLabel = filterState.selectedDescriptionLabel
                )
            }
        )
    }.filter { section ->
        (
                filterState.selectedDeparturePointId == null ||
                        section.key == filterState.selectedDeparturePointId
                ) &&
                section.schedules.isNotEmpty()
    }
}

private fun matchesDescriptionFilter(
    schedule: BusSchedule,
    descriptionProvider: ((BusSchedule) -> String?)?,
    selectedLabel: String?
): Boolean {
    if (descriptionProvider == null || selectedLabel == null) {
        return true
    }

    return descriptionProvider(schedule)
        ?.trim()
        ?.takeIf(String::isNotBlank) == selectedLabel
}

private fun buildUpcomingScheduleEntry(
    title: String,
    schedules: List<BusSchedule>?,
    nextUpcomingId: String?
): UpcomingScheduleEntry? {
    if (schedules.isNullOrEmpty() || nextUpcomingId == null) return null

    val sortedSchedules = schedules.sortedBy(BusSchedule::departureTime)
    val currentIndex = sortedSchedules.indexOfFirst { it.id == nextUpcomingId }
    if (currentIndex < 0) return null

    val currentSchedule = sortedSchedules[currentIndex]
    val nextDepartureTime = when {
        sortedSchedules.size <= 1 -> null
        currentIndex < sortedSchedules.lastIndex -> sortedSchedules[currentIndex + 1].departureTime
        else -> sortedSchedules.firstOrNull()?.departureTime
    }

    return UpcomingScheduleEntry(
        title = title,
        schedule = currentSchedule,
        nextDepartureTime = nextDepartureTime
    )
}
