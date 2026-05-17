package ru.slavgorod.transport.ui.viewmodel

import androidx.datastore.preferences.core.MutablePreferences
import ru.slavgorod.transport.data.local.PreferencesKeys

internal data class RoutePinUpdate(
    val isPinningRoute: Boolean,
    val pinnedRouteIds: Set<String>,
    val routeOrder: List<String>
)

internal fun buildRoutePinUpdate(
    routeId: String,
    currentPinnedRouteIds: Set<String>,
    currentRouteOrder: List<String>,
    allRouteIds: List<String>
): RoutePinUpdate {
    val isPinningRoute = routeId !in currentPinnedRouteIds
    val updatedPinnedRouteIds = currentPinnedRouteIds.toMutableSet().apply {
        if (isPinningRoute) {
            add(routeId)
        } else {
            remove(routeId)
        }
    }
    val updatedRouteOrder = if (isPinningRoute) {
        listOf(routeId) + normalizeRouteOrder(
            routeOrder = currentRouteOrder,
            allRouteIds = allRouteIds
        ).filterNot { id -> id == routeId }
    } else {
        currentRouteOrder
    }

    return RoutePinUpdate(
        isPinningRoute = isPinningRoute,
        pinnedRouteIds = updatedPinnedRouteIds,
        routeOrder = updatedRouteOrder
    )
}

internal fun MutablePreferences.applyRoutePinUpdate(update: RoutePinUpdate) {
    this[PreferencesKeys.pinnedRouteIds] = update.pinnedRouteIds.toRouteIdPreference()
    if (update.isPinningRoute) {
        this[PreferencesKeys.routeOrder] = update.routeOrder.toRouteIdPreference()
    }
}

private const val ROUTE_ID_SEPARATOR = "|"

internal fun String?.toRouteIdList(): List<String> {
    return this
        ?.split(ROUTE_ID_SEPARATOR)
        ?.map(String::trim)
        ?.filter(String::isNotBlank)
        ?.distinct()
        .orEmpty()
}

internal fun Collection<String>.toRouteIdPreference(): String {
    return distinct().joinToString(ROUTE_ID_SEPARATOR)
}
