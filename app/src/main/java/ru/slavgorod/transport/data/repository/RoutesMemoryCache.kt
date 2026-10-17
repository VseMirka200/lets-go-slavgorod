package ru.slavgorod.transport.data.repository

import ru.slavgorod.transport.core.Constants
import ru.slavgorod.transport.data.model.BusRoute
import ru.slavgorod.transport.data.model.BusSchedule

internal class RoutesMemoryCache {

    private val routesCache = object : LinkedHashMap<String, BusRoute>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, BusRoute>?): Boolean {
            return size > Constants.ROUTES_MAX_CACHE_SIZE
        }
    }

    var latestJson: String? = null
        private set

    @Volatile
    private var routeSchedulesCache: Map<String, List<BusSchedule>> = emptyMap()

    val hasCachedData: Boolean
        get() = latestJson != null || routesCache.isNotEmpty()

    val hasSchedules: Boolean
        get() = routeSchedulesCache.isNotEmpty()

    val hasRoutes: Boolean
        get() = routesCache.isNotEmpty()

    fun routeById(routeId: String): BusRoute? = routesCache[routeId]

    fun schedulesForRoute(routeId: String): List<BusSchedule>? = routeSchedulesCache[routeId]

    fun updateRoutes(routes: List<BusRoute>) {
        routesCache.clear()
        routes.forEach { route ->
            routesCache[route.id] = route
        }
    }

    fun updateLatestJson(json: String) {
        latestJson = json
    }

    fun updateSchedules(schedulesByRoute: Map<String, List<BusSchedule>>) {
        routeSchedulesCache = schedulesByRoute
    }
}
