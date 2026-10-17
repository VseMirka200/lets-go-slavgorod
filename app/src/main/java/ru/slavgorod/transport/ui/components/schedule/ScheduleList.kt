package ru.slavgorod.transport.ui.components.schedule

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import ru.slavgorod.transport.data.model.BusSchedule
import ru.slavgorod.transport.data.model.ScheduleChange
import ru.slavgorod.transport.domain.util.UpcomingScheduleResolver
import ru.slavgorod.transport.logging.UserActionLogger
import ru.slavgorod.transport.ui.model.ScheduleUiState
import ru.slavgorod.transport.ui.model.ScheduleDaySelectionMode
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ScheduleList(
    scheduleState: ScheduleUiState,
    swapScheduleColumns: Boolean = false,
    daySelectionMode: ScheduleDaySelectionMode = ScheduleDaySelectionMode.DAY_FIRST,
    onBackClick: (() -> Unit)? = null,
    onShareClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(bottom = 24.dp),
    currentTimeMillis: Long = System.currentTimeMillis(),
    scheduleChanges: List<ScheduleChange> = emptyList(),
    scheduleExtraLabelProvider: ((BusSchedule) -> String?)? = null
) {
    val sectionCollapseStates = remember { mutableStateMapOf<String, Boolean>() }

    val route = scheduleState.route
    val todayDate = remember(currentTimeMillis) { currentLocalDate(currentTimeMillis) }
    var selectedDay by remember(route.id, daySelectionMode) {
        mutableStateOf(
            if (daySelectionMode == ScheduleDaySelectionMode.CLASSIC) todayDate.dayOfWeek else null
        )
    }
    val selectedDate = remember(selectedDay, todayDate) {
        selectedDay?.let { day -> nextDateForDay(day, todayDate) }
    }
    val isDaySelectionScreen = daySelectionMode == ScheduleDaySelectionMode.DAY_FIRST && selectedDay == null
    val upcomingDeparturePoints = remember(
        scheduleState.departurePoints,
        todayDate,
        currentTimeMillis
    ) {
        val selectedCalendar = java.util.Calendar.getInstance().apply {
            timeInMillis = currentTimeMillis
        }
        scheduleState.departurePoints.map { point ->
            val schedules = point.schedules.filter { it.serviceCalendar.runsOn(todayDate) }
            point.copy(
                schedules = schedules,
                nextUpcomingId = UpcomingScheduleResolver.findNextScheduleId(schedules, selectedCalendar)
            )
        }
    }
    val displayedDeparturePoints = remember(
        scheduleState.departurePoints,
        selectedDate,
        todayDate,
        currentTimeMillis
    ) {
        if (selectedDate == null) return@remember emptyList()
        val selectedCalendar = java.util.Calendar.getInstance().apply {
            timeInMillis = currentTimeMillis
        }
        scheduleState.departurePoints.map { point ->
            val schedules = point.schedules.filter { it.serviceCalendar.runsOn(selectedDate) }
            point.copy(
                schedules = schedules,
                nextUpcomingId = if (selectedDate == todayDate) {
                    UpcomingScheduleResolver.findNextScheduleId(schedules, selectedCalendar)
                } else {
                    null
                }
            )
        }
    }

    BackHandler(enabled = daySelectionMode == ScheduleDaySelectionMode.DAY_FIRST && selectedDay != null) {
        selectedDay = null
    }

    val routeRemarkText = remember(route.remark) {
        route.remark?.trim()?.takeIf(String::isNotBlank)
    }

    var selectedDescriptionFilterLabel by remember(route.id) { mutableStateOf<String?>(null) }
    var selectedDeparturePointFilterId by remember(route.id) { mutableStateOf<String?>(null) }
    var isFiltersSheetOpen by remember(route.id) { mutableStateOf(false) }

    val displayState = remember(
        displayedDeparturePoints,
        upcomingDeparturePoints,
        selectedDescriptionFilterLabel,
        selectedDeparturePointFilterId,
        scheduleExtraLabelProvider
    ) {
        val selectedDayDisplayState = buildScheduleListDisplayState(
            departurePoints = displayedDeparturePoints,
            filterState = ScheduleFilterState(
                selectedDescriptionLabel = selectedDescriptionFilterLabel,
                selectedDeparturePointId = selectedDeparturePointFilterId
            ),
            scheduleExtraLabelProvider = scheduleExtraLabelProvider
        )
        val todayDisplayState = buildScheduleListDisplayState(
            departurePoints = upcomingDeparturePoints,
            filterState = ScheduleFilterState(
                selectedDescriptionLabel = null,
                selectedDeparturePointId = null
            ),
            scheduleExtraLabelProvider = null
        )
        selectedDayDisplayState.copy(upcomingEntries = todayDisplayState.upcomingEntries)
    }
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val layout = resolveScheduleResponsiveLayout(
            containerWidth = maxWidth,
            fontScale = LocalDensity.current.fontScale
        )
        ScheduleListScaffold(
            route = route,
            routeRemarkText = routeRemarkText,
            scheduleChanges = scheduleChanges,
            selectedDay = selectedDay,
            isDaySelected = selectedDay != null,
            isDaySelectionScreen = isDaySelectionScreen,
            showInlineDayPicker = daySelectionMode == ScheduleDaySelectionMode.CLASSIC,
            currentDay = todayDate.dayOfWeek,
            onDaySelected = { selectedDay = it },
            onReturnToDaySelection = { selectedDay = null },
            displayState = displayState,
            filterState = ScheduleListFilterUiState(
                selectedDescriptionLabel = selectedDescriptionFilterLabel,
                selectedDeparturePointId = selectedDeparturePointFilterId,
                isFiltersSheetOpen = isFiltersSheetOpen
            ),
            filterCallbacks = ScheduleListFilterCallbacks(
                onDescriptionSelect = { selectedLabel ->
                    UserActionLogger.filterSelected("Description", selectedLabel)
                    selectedDescriptionFilterLabel = selectedLabel
                },
                onDescriptionClear = {
                    UserActionLogger.filterCleared("Description")
                    selectedDescriptionFilterLabel = null
                },
                onDeparturePointSelect = { selectedId ->
                    val selectedLabel = displayState.departurePointFilterLabels
                        .firstOrNull { (id, _) -> id == selectedId }
                        ?.second
                        ?: selectedId
                    UserActionLogger.filterSelected("Departure points", selectedLabel)
                    selectedDeparturePointFilterId = selectedId
                },
                onDeparturePointClear = {
                    UserActionLogger.filterCleared("Departure points")
                    selectedDeparturePointFilterId = null
                },
                onFiltersDismiss = { isFiltersSheetOpen = false },
                onFiltersToggle = {
                    UserActionLogger.filtersOpened(route.routeNumber)
                    isFiltersSheetOpen = !isFiltersSheetOpen
                }
            ),
            sectionCollapseStates = sectionCollapseStates,
            layout = layout,
            swapScheduleColumns = swapScheduleColumns,
            onBackClick = if (daySelectionMode == ScheduleDaySelectionMode.DAY_FIRST && selectedDay != null) {
                { selectedDay = null }
            } else {
                onBackClick
            },
            onShareClick = onShareClick,
            contentPadding = contentPadding,
            currentTimeMillis = currentTimeMillis,
            scheduleExtraLabelProvider = scheduleExtraLabelProvider
        )
    }
}

private fun currentLocalDate(currentTimeMillis: Long): LocalDate = Instant
    .ofEpochMilli(currentTimeMillis)
    .atZone(ZoneId.systemDefault())
    .toLocalDate()

private fun nextDateForDay(selectedDay: DayOfWeek, fromDate: LocalDate): LocalDate {
    val daysAhead = (selectedDay.value - fromDate.dayOfWeek.value + 7) % 7
    return fromDate.plusDays(daysAhead.toLong())
}
