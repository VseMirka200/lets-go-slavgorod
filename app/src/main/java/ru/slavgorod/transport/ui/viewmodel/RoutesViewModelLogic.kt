package ru.slavgorod.transport.ui.viewmodel

import androidx.annotation.StringRes
import ru.slavgorod.transport.R
import ru.slavgorod.transport.core.AppText
import ru.slavgorod.transport.core.search
import ru.slavgorod.transport.data.model.BusRoute
import ru.slavgorod.transport.data.repository.RoutesLoadState
import ru.slavgorod.transport.data.repository.ScheduleDataSource
import ru.slavgorod.transport.data.repository.ScheduleDataSourceStatus
import ru.slavgorod.transport.domain.util.DateTimeFormatterUtils

internal fun arrangeRoutesForDisplay(
    routes: List<BusRoute>,
    pinnedRouteIds: Set<String>
): List<BusRoute> {
    if (routes.isEmpty()) return routes

    val routeNumberComparator = compareBy<BusRoute> { route ->
        route.routeNumber.routeNumberSortKey().numberPart
    }.thenBy { route ->
        route.routeNumber.routeNumberSortKey().suffixPart
    }.thenBy { route ->
        route.routeNumber
    }

    val sortedRoutes = routes.sortedWith(routeNumberComparator)
    val (pinnedRoutes, regularRoutes) = sortedRoutes.partition { route -> route.id in pinnedRouteIds }

    return pinnedRoutes + regularRoutes
}

internal fun filterRoutesByQuery(routes: List<BusRoute>, query: String): List<BusRoute> {
    return routes.search(query)
}

internal fun resolveScheduleStatusLabel(dataSourceStatus: ScheduleDataSourceStatus): Int {
    return when {
        dataSourceStatus.loadState == RoutesLoadState.NO_CHANGES -> R.string.schedule_status_live
        dataSourceStatus.loadState == RoutesLoadState.SAVED_COPY_LOADED -> R.string.schedule_status_saved
        dataSourceStatus.loadState == RoutesLoadState.OFFLINE_USING_SAVED_COPY -> R.string.schedule_status_offline_copy
        dataSourceStatus.loadState == RoutesLoadState.OFFLINE_NO_DATA -> R.string.schedule_status_offline_no_data
        dataSourceStatus.loadState == RoutesLoadState.NETWORK_ERROR_WITH_SAVED_COPY -> R.string.schedule_status_failed_with_copy
        dataSourceStatus.isUsingSavedRemoteCopy -> R.string.schedule_status_saved
        dataSourceStatus.loadState == RoutesLoadState.EMPTY_OR_INVALID_JSON -> R.string.schedule_status_invalid_json
        dataSourceStatus.source == ScheduleDataSource.BUNDLED -> R.string.schedule_status_bundled
        dataSourceStatus.source == ScheduleDataSource.REMOTE_CACHE -> R.string.schedule_status_remote
        else -> R.string.schedule_status_live
    }
}

internal fun resolveRoutesErrorMessage(
    routes: List<BusRoute>,
    waitingForInitialLoad: Boolean,
    currentError: String?,
    loadState: RoutesLoadState,
    hasLoadedRoutes: Boolean
): String? {
    return when {
        routes.isNotEmpty() -> null
        waitingForInitialLoad -> currentError
        loadState == RoutesLoadState.NO_CHANGES -> null
        loadState == RoutesLoadState.SAVED_COPY_LOADED -> null
        loadState == RoutesLoadState.OFFLINE_USING_SAVED_COPY -> null
        loadState == RoutesLoadState.OFFLINE_NO_DATA -> AppText.get(R.string.status_offline_no_data)
        loadState == RoutesLoadState.EMPTY_OR_INVALID_JSON -> AppText.get(R.string.schedule_status_invalid_json)
        loadState == RoutesLoadState.NETWORK_ERROR -> AppText.get(R.string.schedule_refresh_failed)
        loadState == RoutesLoadState.NETWORK_ERROR_WITH_SAVED_COPY -> AppText.get(R.string.schedule_status_failed_with_copy)
        hasLoadedRoutes -> AppText.get(R.string.schedule_refresh_failed)
        else -> currentError
    }
}

internal fun buildRefreshLogMessage(loadState: RoutesLoadState): String {
    return when (loadState) {
        RoutesLoadState.SUCCESS -> AppText.get(R.string.log_state_success)
        RoutesLoadState.NO_CHANGES -> AppText.get(R.string.log_state_no_changes)
        RoutesLoadState.SAVED_COPY_LOADED -> AppText.get(R.string.log_state_saved_copy_loaded)
        RoutesLoadState.OFFLINE_USING_SAVED_COPY -> AppText.get(R.string.log_state_offline_saved_copy)
        RoutesLoadState.OFFLINE_NO_DATA -> AppText.get(R.string.log_state_offline_no_data)
        RoutesLoadState.NETWORK_ERROR -> AppText.get(R.string.log_state_network_error)
        RoutesLoadState.NETWORK_ERROR_WITH_SAVED_COPY -> AppText.get(R.string.log_state_network_error_saved_copy)
        RoutesLoadState.EMPTY_OR_INVALID_JSON -> AppText.get(R.string.log_state_invalid_json)
        RoutesLoadState.IDLE -> AppText.get(R.string.log_state_idle)
    }
}

internal fun buildRefreshUiMessage(
    loadState: RoutesLoadState,
    lastUpdatedAtMillis: Long?
): UiMessageSpec? {
    return when (loadState) {
        RoutesLoadState.SUCCESS -> buildMessageWithUpdatedAt(lastUpdatedAtMillis)
        RoutesLoadState.NO_CHANGES -> UiMessageSpec(R.string.schedule_update_no_changes)
        RoutesLoadState.SAVED_COPY_LOADED -> null
        RoutesLoadState.OFFLINE_USING_SAVED_COPY -> null
        RoutesLoadState.OFFLINE_NO_DATA -> UiMessageSpec(R.string.status_offline_no_data)
        RoutesLoadState.NETWORK_ERROR -> UiMessageSpec(R.string.schedule_refresh_failed)
        RoutesLoadState.NETWORK_ERROR_WITH_SAVED_COPY -> UiMessageSpec(R.string.schedule_status_failed_with_copy)
        RoutesLoadState.EMPTY_OR_INVALID_JSON -> UiMessageSpec(R.string.schedule_status_invalid_json)
        RoutesLoadState.IDLE -> UiMessageSpec(R.string.schedule_refresh_failed)
    }
}

internal fun normalizeRouteOrder(
    routeOrder: List<String>,
    allRouteIds: List<String>
): List<String> {
    val knownRouteIds = allRouteIds.toSet()
    val savedRouteIds = routeOrder.filter { routeId -> routeId in knownRouteIds }
    val newRouteIds = allRouteIds.filterNot { routeId -> routeId in savedRouteIds }
    return savedRouteIds + newRouteIds
}

internal fun resolveInitialLoadTimeoutState(
    currentState: RoutesUiState,
    timeoutErrorMessage: String
): RoutesUiState {
    return if (currentState.routes.isEmpty() && currentState.isLoading) {
        currentState.copy(
            isLoading = false,
            error = timeoutErrorMessage
        )
    } else {
        currentState
    }
}

internal data class RoutesStateInput(
    val routes: List<BusRoute>,
    val query: String,
    val dataSourceStatus: ScheduleDataSourceStatus,
    val pinnedRouteIds: Set<String>
)

internal data class ComputedRoutesUiState(
    val uiState: RoutesUiState,
    val hasLoadedRoutes: Boolean
)

internal fun buildRoutesUiState(
    input: RoutesStateInput,
    hasLoadedRoutes: Boolean,
    currentError: String?
): ComputedRoutesUiState {
    val shouldMarkLoaded = input.routes.isNotEmpty()
    val currentHasLoadedRoutes = hasLoadedRoutes || shouldMarkLoaded
    val arrangedRoutes = arrangeRoutesForDisplay(
        routes = input.routes,
        pinnedRouteIds = input.pinnedRouteIds
    )
    val waitingForInitialLoad = input.routes.isEmpty() &&
            !currentHasLoadedRoutes &&
            input.query.isBlank() &&
            input.dataSourceStatus.loadState == RoutesLoadState.IDLE

    return ComputedRoutesUiState(
        uiState = RoutesUiState(
            routes = filterRoutesByQuery(arrangedRoutes, input.query),
            pinnedRouteIds = input.pinnedRouteIds,
            isLoading = waitingForInitialLoad,
            dataSourceStatus = input.dataSourceStatus,
            error = resolveRoutesErrorMessage(
                routes = input.routes,
                waitingForInitialLoad = waitingForInitialLoad,
                currentError = currentError,
                loadState = input.dataSourceStatus.loadState,
                hasLoadedRoutes = currentHasLoadedRoutes
            )
        ),
        hasLoadedRoutes = currentHasLoadedRoutes
    )
}

private fun String.routeNumberSortKey(): RouteNumberSortKey {
    val trimmed = trim()
    val match = ROUTE_NUMBER_REGEX.find(trimmed) ?: return RouteNumberSortKey(
        numberPart = Int.MAX_VALUE,
        suffixPart = trimmed.uppercase()
    )

    return RouteNumberSortKey(
        numberPart = match.groupValues[1].toIntOrNull() ?: Int.MAX_VALUE,
        suffixPart = match.groupValues[2].trim().uppercase()
    )
}

private fun buildMessageWithUpdatedAt(lastUpdatedAtMillis: Long?): UiMessageSpec {
    val formattedDate = lastUpdatedAtMillis?.let(::formatUpdatedAt)
    return if (formattedDate != null) {
        UiMessageSpec(
            messageResId = R.string.schedule_refresh_success_with_date,
            args = listOf(formattedDate)
        )
    } else {
        UiMessageSpec(R.string.schedule_refresh_success_date_unknown)
    }
}

private fun formatUpdatedAt(timestampMillis: Long): String {
    return DateTimeFormatterUtils.formatShortDateTime(timestampMillis)
}

internal data class UiMessageSpec(
    @param:StringRes val messageResId: Int,
    val args: List<String> = emptyList()
)

private data class RouteNumberSortKey(
    val numberPart: Int,
    val suffixPart: String
)

private val ROUTE_NUMBER_REGEX = Regex("""^(\d+)(.*)$""")
