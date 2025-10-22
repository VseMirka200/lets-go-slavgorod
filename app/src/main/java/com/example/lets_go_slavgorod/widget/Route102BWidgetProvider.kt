package com.example.lets_go_slavgorod.widget

import com.example.lets_go_slavgorod.R

/**
 * Виджет для маршрута 102Б
 * Цвет: определяется в routes_data.json
 */
class Route102BWidgetProvider : BaseRouteWidgetProvider() {
    override val routeId = "102B"
    override val layoutId = R.layout.widget_route_102b
    override val workName = "route102b_widget_update"
    override val leftDirection = "Славгород"
    override val rightDirection = "Ст. Зори"

    override fun isLeftDirection(direction: String): Boolean {
        return direction.contains("Славгород") || direction.contains("Рынок")
    }

    override fun isRightDirection(direction: String): Boolean {
        return direction.contains("Ст. Зори") || direction.contains("Зори")
    }
}
