package ru.slavgorod.transport.core

import ru.slavgorod.transport.data.model.BusRoute

fun List<BusRoute>.search(query: String): List<BusRoute> {
    if (query.isBlank()) return this

    return filter { route -> route.matchesSearchQuery(query) }
}

private fun BusRoute.matchesSearchQuery(query: String): Boolean {
    return name.contains(query, ignoreCase = true) ||
            routeNumber.contains(query, ignoreCase = true) ||
            description.contains(query, ignoreCase = true)
}
