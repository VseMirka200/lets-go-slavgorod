package ru.slavgorod.transport.data.repository

import ru.slavgorod.transport.data.model.BusRoute
import ru.slavgorod.transport.data.model.BusSchedule

/**
 * Хранит маршруты, индекс рейсов и исходный JSON в оперативной памяти.
 *
 * Отдельные ссылки видимы между потоками, но их обновление не является общей
 * атомарной операцией. Согласованность данных обеспечивает вызывающий репозиторий.
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

    /** Отсутствующий ключ отличается от маршрута с пустым списком рейсов. */
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
