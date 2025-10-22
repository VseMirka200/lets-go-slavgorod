package com.example.lets_go_slavgorod.widget

/**
 * Конфигурация виджетов для устранения дублирования кода
 * 
 * Содержит настройки для каждого типа виджета, включая:
 * - ID маршрута и название WorkManager задачи
 * - Ключевые слова для определения направлений
 * - Названия направлений для отображения
 * 
 * @author VseMirka200
 * @version 2.0
 * @since 1.0
 */
data class WidgetConfig(
    val routeId: String,
    val workName: String,
    val leftKeywords: List<String>,
    val rightKeywords: List<String>,
    val leftDirection: String,
    val rightDirection: String
)

/**
 * Реестр конфигураций всех виджетов
 */
object WidgetConfigRegistry {
    
    /**
     * Конфигурация для маршрута 102 (Славгород — Яровое)
     */
    val ROUTE_102 = WidgetConfig(
        routeId = "102",
        workName = "route102_widget_update",
        leftKeywords = listOf("Рынок", "Славгород"),
        rightKeywords = listOf("МСЧ", "Яровое"),
        leftDirection = "Славгород",
        rightDirection = "Яровое"
    )
    
    /**
     * Конфигурация для маршрута 1 (Вокзал — Совхоз)
     */
    val ROUTE_1 = WidgetConfig(
        routeId = "1",
        workName = "route1_widget_update",
        leftKeywords = listOf("вокзал"),
        rightKeywords = listOf("совхоз"),
        leftDirection = "Вокзал",
        rightDirection = "Совхоз"
    )
    
    /**
     * Конфигурация для маршрута 102Б (Славгород — Ст. Зори)
     */
    val ROUTE_102B = WidgetConfig(
        routeId = "102B",
        workName = "route102b_widget_update",
        leftKeywords = listOf("Славгород", "Рынок"),
        rightKeywords = listOf("Ст. Зори", "Зори"),
        leftDirection = "Славгород",
        rightDirection = "Ст. Зори"
    )
    
    /**
     * Конфигурация для маршрута 3 (Кольцевой)
     */
    val ROUTE_3 = WidgetConfig(
        routeId = "3",
        workName = "route3_widget_update",
        leftKeywords = listOf("Славгород", "Вокзал"),
        rightKeywords = listOf("Яровое", "МСЧ"),
        leftDirection = "Славгород",
        rightDirection = "Яровое"
    )
    
    /**
     * Конфигурация для маршрута 4 (Пригородный)
     */
    val ROUTE_4 = WidgetConfig(
        routeId = "4",
        workName = "route4_widget_update",
        leftKeywords = listOf("Славгород", "Вокзал"),
        rightKeywords = listOf("Яровое", "МСЧ"),
        leftDirection = "Славгород",
        rightDirection = "Яровое"
    )
    
    /**
     * Получает конфигурацию по ID маршрута
     * 
     * @param routeId ID маршрута
     * @return конфигурация виджета или null если не найдена
     */
    fun getConfig(routeId: String): WidgetConfig? {
        return when (routeId) {
            "102" -> ROUTE_102
            "1" -> ROUTE_1
            "102B" -> ROUTE_102B
            "3" -> ROUTE_3
            "4" -> ROUTE_4
            else -> null
        }
    }
    
    /**
     * Получает все доступные конфигурации
     */
    fun getAllConfigs(): List<WidgetConfig> {
        return listOf(ROUTE_102, ROUTE_1, ROUTE_102B, ROUTE_3, ROUTE_4)
    }
}