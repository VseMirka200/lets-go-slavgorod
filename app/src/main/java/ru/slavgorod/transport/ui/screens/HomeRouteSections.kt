package ru.slavgorod.transport.ui.screens

import ru.slavgorod.transport.R
import ru.slavgorod.transport.data.model.BusRoute

internal data class RouteSection(
    val titleResId: Int,
    val keyPrefix: String,
    val routes: List<BusRoute>,
    val pinned: Boolean
)

internal fun buildRouteSections(
    routes: List<BusRoute>,
    pinnedRouteIds: Set<String>
): List<RouteSection> {
    val pinnedRoutes = routes.filter { route -> route.id in pinnedRouteIds }
    return buildList {
        if (pinnedRoutes.isNotEmpty()) {
            add(
                RouteSection(
                    titleResId = R.string.home_pinned_routes,
                    keyPrefix = "pinned",
                    routes = pinnedRoutes,
                    pinned = true
                )
            )
        }
        if (routes.isNotEmpty()) {
            add(
                RouteSection(
                    titleResId = R.string.home_all_routes,
                    keyPrefix = "all",
                    routes = routes,
                    pinned = false
                )
            )
        }
    }
}
