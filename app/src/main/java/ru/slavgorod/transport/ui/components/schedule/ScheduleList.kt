package ru.slavgorod.transport.ui.components.schedule

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
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
import ru.slavgorod.transport.logging.UserActionLogger
import ru.slavgorod.transport.ui.model.ScheduleUiState

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ScheduleList(
    scheduleState: ScheduleUiState,
    swapScheduleColumns: Boolean = false,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(bottom = 24.dp),
    currentTimeMillis: Long = System.currentTimeMillis(),
    scheduleExtraLabelProvider: ((BusSchedule) -> String?)? = null
) {
    val sectionCollapseStates = remember { mutableStateMapOf<String, Boolean>() }

    val route = scheduleState.route

    val routeRemarkText = remember(route.remark) {
        route.remark?.trim()?.takeIf(String::isNotBlank)
    }

    var selectedDescriptionFilterLabel by remember(route.id) { mutableStateOf<String?>(null) }
    var selectedDeparturePointFilterId by remember(route.id) { mutableStateOf<String?>(null) }
    var isFiltersSheetOpen by remember(route.id) { mutableStateOf(false) }

    val displayState = remember(
        scheduleState.departurePoints,
        selectedDescriptionFilterLabel,
        selectedDeparturePointFilterId,
        scheduleExtraLabelProvider
    ) {
        buildScheduleListDisplayState(
            departurePoints = scheduleState.departurePoints,
            filterState = ScheduleFilterState(
                selectedDescriptionLabel = selectedDescriptionFilterLabel,
                selectedDeparturePointId = selectedDeparturePointFilterId
            ),
            scheduleExtraLabelProvider = scheduleExtraLabelProvider
        )
    }
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val layout = resolveScheduleResponsiveLayout(
            containerWidth = maxWidth,
            fontScale = LocalDensity.current.fontScale
        )
        ScheduleListScaffold(
            route = route,
            routeRemarkText = routeRemarkText,
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
            onBackClick = onBackClick,
            contentPadding = contentPadding,
            currentTimeMillis = currentTimeMillis,
            scheduleExtraLabelProvider = scheduleExtraLabelProvider
        )
    }
}
