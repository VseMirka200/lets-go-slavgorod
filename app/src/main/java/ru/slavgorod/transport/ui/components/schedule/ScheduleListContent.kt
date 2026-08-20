package ru.slavgorod.transport.ui.components.schedule

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.slavgorod.transport.R
import ru.slavgorod.transport.data.model.BusRoute
import ru.slavgorod.transport.data.model.BusSchedule
import ru.slavgorod.transport.ui.components.app.ScheduleHeaderDetails
import ru.slavgorod.transport.ui.components.app.ScheduleTitleBar

internal data class ScheduleListFilterUiState(
    val selectedDescriptionLabel: String?,
    val selectedDeparturePointId: String?,
    val isFiltersSheetOpen: Boolean
)

internal data class ScheduleListFilterCallbacks(
    val onDescriptionSelect: (String) -> Unit,
    val onDescriptionClear: () -> Unit,
    val onDeparturePointSelect: (String) -> Unit,
    val onDeparturePointClear: () -> Unit,
    val onFiltersDismiss: () -> Unit,
    val onFiltersToggle: () -> Unit
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun ScheduleListScaffold(
    route: BusRoute,
    routeRemarkText: String?,
    displayState: ScheduleListDisplayState,
    filterState: ScheduleListFilterUiState,
    filterCallbacks: ScheduleListFilterCallbacks,
    sectionCollapseStates: MutableMap<String, Boolean>,
    layout: ScheduleResponsiveLayoutSpec,
    swapScheduleColumns: Boolean,
    onBackClick: (() -> Unit)?,
    contentPadding: PaddingValues,
    currentTimeMillis: Long,
    scheduleExtraLabelProvider: ((BusSchedule) -> String?)?
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val headerCollapseProgress by rememberHeaderCollapseProgress(
        listState = listState,
        density = density
    )
    val listBottomReserve = resolveScheduleListBottomReserve(displayState.visibleSchedulesCount)
    val filtersBottomReserve = resolveFiltersBottomReserve(
        hasFilterControls = displayState.hasFilterControls,
        isFiltersSheetOpen = filterState.isFiltersSheetOpen,
        layout = layout
    )
    val layoutDirection = LocalLayoutDirection.current

    Column(modifier = Modifier.fillMaxSize()) {
        ScheduleTitleBar(route = route, onBackClick = onBackClick)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = scheduleListContentPadding(
                    contentPadding = contentPadding,
                    layoutDirection = layoutDirection,
                    bottomReserve = listBottomReserve + filtersBottomReserve
                ),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                userScrollEnabled = true
            ) {
                scheduleStickyHeader(
                    route = route,
                    routeRemarkText = routeRemarkText,
                    displayState = displayState,
                    layout = layout,
                    headerCollapseProgress = headerCollapseProgress,
                    currentTimeMillis = currentTimeMillis
                )

                scheduleSections(
                    route = route,
                    displayState = displayState,
                    filterState = filterState,
                    layout = layout,
                    sectionCollapseStates = sectionCollapseStates,
                    swapScheduleColumns = swapScheduleColumns,
                    scheduleExtraLabelProvider = scheduleExtraLabelProvider
                )
            }

            ScheduleFiltersOverlay(
                displayState = displayState,
                filterState = filterState,
                filterCallbacks = filterCallbacks,
                layout = layout,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun rememberHeaderCollapseProgress(
    listState: androidx.compose.foundation.lazy.LazyListState,
    density: androidx.compose.ui.unit.Density
) = remember(listState, density) {
    derivedStateOf {
        when {
            listState.firstVisibleItemIndex > 0 -> 1f
            listState.firstVisibleItemScrollOffset <= 0 -> 0f
            else -> {
                val collapseDistancePx = with(density) { HEADER_COLLAPSE_DISTANCE.dp.toPx() }
                (listState.firstVisibleItemScrollOffset / collapseDistancePx)
                    .coerceIn(0f, 1f)
            }
        }
    }
}

private fun scheduleListContentPadding(
    contentPadding: PaddingValues,
    layoutDirection: androidx.compose.ui.unit.LayoutDirection,
    bottomReserve: Dp
) = PaddingValues(
    start = contentPadding.calculateStartPadding(layoutDirection),
    top = contentPadding.calculateTopPadding(),
    end = contentPadding.calculateEndPadding(layoutDirection),
    bottom = contentPadding.calculateBottomPadding() + bottomReserve
)

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.scheduleStickyHeader(
    route: BusRoute,
    routeRemarkText: String?,
    displayState: ScheduleListDisplayState,
    layout: ScheduleResponsiveLayoutSpec,
    headerCollapseProgress: Float,
    currentTimeMillis: Long
) {
    item(key = "route_header_and_upcoming") {
        ScheduleRouteStickyHeader(
            route = route,
            routeRemarkText = routeRemarkText,
            layout = layout,
            headerCollapseProgress = headerCollapseProgress
        )
    }

    if (displayState.upcomingEntries.isNotEmpty()) {
        stickyHeader(key = "upcoming_departures_header") {
            UpcomingSchedulesCard(
                entries = displayState.upcomingEntries,
                currentTimeMillis = currentTimeMillis,
                layout = layout,
                compactMode = false,
                squareCorners = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = layout.section.sectionVerticalSpacing)
            )
        }
    }
}

@Composable
private fun ScheduleRouteStickyHeader(
    route: BusRoute,
    routeRemarkText: String?,
    layout: ScheduleResponsiveLayoutSpec,
    headerCollapseProgress: Float
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        ScheduleHeaderDetails(
            route = route,
            layout = layout,
            modifier = Modifier
                .padding(bottom = layout.section.sectionVerticalSpacing)
                .graphicsLayer {
                    val collapseDistancePx = 28.dp.toPx()
                    translationY = -collapseDistancePx * headerCollapseProgress
                    alpha = 1f - headerCollapseProgress
                },
            horizontalPadding = layout.header.headerHorizontalPadding,
            noteText = routeRemarkText
        )
    }
}

private fun LazyListScope.scheduleSections(
    route: BusRoute,
    displayState: ScheduleListDisplayState,
    filterState: ScheduleListFilterUiState,
    layout: ScheduleResponsiveLayoutSpec,
    sectionCollapseStates: MutableMap<String, Boolean>,
    swapScheduleColumns: Boolean,
    scheduleExtraLabelProvider: ((BusSchedule) -> String?)?
) {
    val sectionKeyPrefix = "standard_${route.id}"

    when {
        displayState.visibleSections.isEmpty() && filterState.selectedDescriptionLabel != null -> {
            item(key = "standard_${route.id}_filtered_empty") {
                ScheduleFilteredEmptyState(
                    layout = layout,
                    modifier = Modifier.padding(
                        horizontal = layout.emptyState.emptyStateHorizontalPadding,
                        vertical = layout.emptyState.emptyStateVerticalSpacing
                    )
                )
            }
        }

        displayState.visibleSections.size >= 2 -> {
            item(key = "${sectionKeyPrefix}_two_column_grid") {
                TwoColumnScheduleSectionGrid(
                    sections = displayState.visibleSections,
                    showOnlyUpcoming = false,
                    collapsedStates = sectionCollapseStates,
                    swapScheduleColumns = swapScheduleColumns,
                    compactSpacing = false,
                    scheduleExtraLabelProvider = scheduleExtraLabelProvider,
                    layout = layout,
                    containerHorizontalPadding = layout.section.sectionHorizontalPadding,
                    horizontalPadding = 0.dp,
                    columnSpacing = layout.section.sectionColumnSpacing,
                    sectionSpacing = layout.section.sectionVerticalSpacing
                )
            }
        }

        else -> {
            addSectionSequenceLayout(
                sections = displayState.visibleSections,
                keyPrefix = sectionKeyPrefix,
                showOnlyUpcoming = false,
                swapScheduleColumns = swapScheduleColumns,
                collapsedStates = sectionCollapseStates,
                compactSpacing = false,
                scheduleExtraLabelProvider = scheduleExtraLabelProvider,
                layout = layout,
                horizontalPadding = layout.section.sectionHorizontalPadding,
                bottomPadding = layout.section.sectionVerticalSpacing
            )
        }
    }

    if (displayState.allSchedules.isEmpty()) {
        item {
            NoScheduleMessage(
                layout = layout,
                modifier = Modifier.padding(layout.emptyState.emptyStateHorizontalPadding)
            )
        }
    }
}

@Composable
private fun ScheduleFiltersOverlay(
    displayState: ScheduleListDisplayState,
    filterState: ScheduleListFilterUiState,
    filterCallbacks: ScheduleListFilterCallbacks,
    layout: ScheduleResponsiveLayoutSpec,
    modifier: Modifier = Modifier
) {
    if (!displayState.hasFilterControls) return

    if (filterState.isFiltersSheetOpen) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    filterCallbacks.onFiltersDismiss()
                }
        )
    }

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(FILTERS_BAR_COLOR)
        ) {
            AnimatedVisibility(
                visible = filterState.isFiltersSheetOpen,
                enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut()
            ) {
                ScheduleFiltersSheetContent(
                    layout = layout,
                    selectedDescriptionLabel = filterState.selectedDescriptionLabel,
                    descriptionLabels = displayState.descriptionFilterLabels,
                    onDescriptionSelect = filterCallbacks.onDescriptionSelect,
                    onDescriptionClear = filterCallbacks.onDescriptionClear,
                    selectedDeparturePointFilterId = filterState.selectedDeparturePointId,
                    departurePointLabels = displayState.departurePointFilterLabels,
                    onDeparturePointSelect = filterCallbacks.onDeparturePointSelect,
                    onDeparturePointClear = filterCallbacks.onDeparturePointClear
                )
            }

            ScheduleFiltersMenuButton(
                layout = layout,
                title = stringResource(R.string.schedule_filters_title),
                expanded = filterState.isFiltersSheetOpen,
                onClick = filterCallbacks.onFiltersToggle,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(filtersBottomBarOffset(layout))
                    .background(FILTERS_BAR_COLOR)
            )
        }
    }
}

private fun filtersBottomBarOffset(layout: ScheduleResponsiveLayoutSpec) =
    if (layout.mode == ScheduleResponsiveLayoutMode.TIGHT) 8.dp else 10.dp

private const val HEADER_COLLAPSE_DISTANCE = 72
private val FILTERS_BOTTOM_RESERVE_COLLAPSED = 96.dp
private val FILTERS_BOTTOM_RESERVE_EXPANDED = 240.dp
private val FILTERS_PANEL_EXTRA_HEIGHT = 32.dp

private fun resolveFiltersBottomReserve(
    hasFilterControls: Boolean,
    isFiltersSheetOpen: Boolean,
    layout: ScheduleResponsiveLayoutSpec
): Dp {
    if (!hasFilterControls) return 0.dp
    val buttonHeightReserve = filtersBottomBarOffset(layout) +
            layout.filters.filterSheetVerticalSpacing * 2 +
            FILTERS_PANEL_EXTRA_HEIGHT
    return if (isFiltersSheetOpen) {
        FILTERS_BOTTOM_RESERVE_EXPANDED + buttonHeightReserve
    } else {
        FILTERS_BOTTOM_RESERVE_COLLAPSED + buttonHeightReserve
    }
}

private val FILTERS_BAR_COLOR = Color(0xFF414753)
