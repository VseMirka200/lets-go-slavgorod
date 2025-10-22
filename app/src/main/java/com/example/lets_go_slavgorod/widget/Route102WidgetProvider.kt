package com.example.lets_go_slavgorod.widget

import com.example.lets_go_slavgorod.R

/**
 * Виджет для маршрута 102 (Славгород — Яровое)
 * Цвет: #FF6200EE (фиолетовый)
 */
class Route102WidgetProvider : BaseRouteWidgetProvider() {

    override val routeId = "102"
    override val layoutId = R.layout.widget_route_102
    override val workName = "route102_widget_update"
    override val leftDirection = "Славгород"
    override val rightDirection = "Яровое"

    override fun isLeftDirection(direction: String): Boolean {
        return direction.contains("Рынок") || direction.contains("Славгород")
    }

    override fun isRightDirection(direction: String): Boolean {
        return direction.contains("МСЧ") || direction.contains("Яровое")
    }
}