package com.example.lets_go_slavgorod.widget

import com.example.lets_go_slavgorod.R

/**
 * Виджет для маршрута 1 (Вокзал — Совхоз)
 * Цвет: #FF1976D2 (синий)
 */
class Route1WidgetProvider : BaseRouteWidgetProvider() {

    override val routeId = "1"
    override val layoutId = R.layout.widget_route_1
    override val workName = "route1_widget_update"
    override val leftDirection = "Вокзал"
    override val rightDirection = "Совхоз"

    override fun isLeftDirection(direction: String): Boolean {
        return direction.contains("вокзал")
    }

    override fun isRightDirection(direction: String): Boolean {
        return direction.contains("совхоз")
    }
}