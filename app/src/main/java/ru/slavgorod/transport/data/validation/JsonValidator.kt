package ru.slavgorod.transport.data.validation

import ru.slavgorod.transport.data.model.BusRoute

/**
 * Отбрасывает невалидные маршруты до публикации данных в интерфейсе.
 *
 * Подсказка разработчику: это последняя граница входного JSON; не заменяйте проверку
 * молчаливым исправлением полей, иначе источник ошибок будет сложнее обнаружить.
 */
object JsonValidator {

    fun validateRoutes(routes: List<BusRoute>): List<BusRoute> {
        val validRoutes = routes.filter(::isValidRoute)
        require(validRoutes.isNotEmpty()) { "В JSON-данных не найдено корректных маршрутов" }
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
