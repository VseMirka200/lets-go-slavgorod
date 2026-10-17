package ru.slavgorod.transport.data.validation

import ru.slavgorod.transport.data.model.BusRoute

object JsonValidator {

    fun validateRoutes(routes: List<BusRoute>): List<BusRoute> {
        val validRoutes = routes.filter(::isValidRoute)
        require(validRoutes.isNotEmpty()) { "No valid routes found in JSON data" }
        return validRoutes
    }

    private fun isValidRoute(route: BusRoute): Boolean {
        return route.id.hasText() &&
                route.name.hasText() &&
                route.routeNumber.hasText() &&
                route.description.hasText() &&
                route.color.hasText()
    }

    private fun String?.hasText(): Boolean = !this.isNullOrBlank()
}
