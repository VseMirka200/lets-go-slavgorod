package com.example.lets_go_slavgorod.widget

import com.example.lets_go_slavgorod.R

/**
 * Виджет для маршрута 4
 * Цвет: определяется в routes_data.json
 */
class Route4WidgetProvider : BaseRouteWidgetProvider() {
    override val routeId = "4"
    override val layoutId = R.layout.widget_route_4
    override val workName = "route4_widget_update"
    override val leftDirection = "Славгород"
    override val rightDirection = "Яровое"

    override fun isLeftDirection(direction: String): Boolean {
        return direction.contains("Славгород") || direction.contains("Вокзал")
    }

    override fun isRightDirection(direction: String): Boolean {
        return direction.contains("Яровое") || direction.contains("МСЧ")
    }
}
