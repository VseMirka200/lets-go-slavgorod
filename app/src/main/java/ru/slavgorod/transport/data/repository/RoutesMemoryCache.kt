package ru.slavgorod.transport.data.repository

import ru.slavgorod.transport.data.model.BusRoute
import ru.slavgorod.transport.data.model.BusSchedule

/**
 * Согласованный снимок маршрутов, рейсов и исходного JSON в оперативной памяти.
 *
 * Подсказка разработчику: не возвращайте частичный индекс маршрутов — он должен
 * соответствовать полному StateFlow маршрутов, опубликованному репозиторием.
 */
internal class RoutesMemoryCache {

    @Volatile
    private var routesById: Map<String, BusRoute> = emptyMap()

    @Volatile
    var latestJson: String? = null
        private set

    @Volatile
    private var schedulesByRoute: Map<String, List<BusSchedule>> = emptyMap()

    val hasCachedData: Boolean
        get() = latestJson != null || routesById.isNotEmpty()

    val hasRoutes: Boolean
        get() = routesById.isNotEmpty()

    fun routeById(routeId: String): BusRoute? = routesById[routeId]

    fun schedulesForRoute(routeId: String): List<BusSchedule>? = schedulesByRoute[routeId]

    fun updateRoutes(routes: List<BusRoute>) {
        routesById = routes.associateBy(BusRoute::id)
    }

    fun updateLatestJson(json: String) {
        latestJson = json
    }

    fun updateSchedules(schedulesByRoute: Map<String, List<BusSchedule>>) {
        this.schedulesByRoute = schedulesByRoute
    }
}
