package com.example.lets_go_slavgorod.widget

import com.example.lets_go_slavgorod.R

/**
 * Универсальный провайдер виджетов на основе конфигурации
 * 
 * Устраняет дублирование кода в виджетах, используя конфигурацию
 * для определения поведения каждого типа виджета.
 * 
 * @param config конфигурация виджета
 * 
 * @author VseMirka200
 * @version 2.0
 * @since 1.0
 */
open class ConfigurableWidgetProvider(
    private val config: WidgetConfig
) : BaseRouteWidgetProvider() {
    
    override val routeId: String = config.routeId
    override val layoutId: Int = getLayoutId(config.routeId)
    override val workName: String = config.workName
    override val leftDirection: String = config.leftDirection
    override val rightDirection: String = config.rightDirection
    
    /**
     * Определяет, является ли направление левым
     * 
     * @param direction Строка с описанием направления
     * @return true, если направление содержит любое из левых ключевых слов
     */
    override fun isLeftDirection(direction: String): Boolean {
        return config.leftKeywords.any { keyword ->
            direction.contains(keyword, ignoreCase = true)
        }
    }
    
    /**
     * Определяет, является ли направление правым
     * 
     * @param direction Строка с описанием направления
     * @return true, если направление содержит любое из правых ключевых слов
     */
    override fun isRightDirection(direction: String): Boolean {
        return config.rightKeywords.any { keyword ->
            direction.contains(keyword, ignoreCase = true)
        }
    }
    
    /**
     * Получает ID макета для маршрута
     * 
     * @param routeId ID маршрута
     * @return ID макета
     */
    private fun getLayoutId(routeId: String): Int {
        return when (routeId) {
            "102" -> R.layout.widget_route_102
            "1" -> R.layout.widget_route_1
            "102B" -> R.layout.widget_route_102b
            "3" -> R.layout.widget_route_3
            "4" -> R.layout.widget_route_4
            else -> throw IllegalArgumentException("Unknown route ID: $routeId")
        }
    }
}