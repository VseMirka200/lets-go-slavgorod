package com.example.lets_go_slavgorod.ui.screens.settings

/**
 * Навигационные пункты назначения для экранов настроек
 * 
 * Определяет все доступные экраны настроек и их маршруты для навигации.
 * Используется для type-safe навигации между экранами настроек.
 * 
 * @param route строка маршрута для NavHost
 * @param title отображаемое название экрана
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.1
 */
sealed class SettingsDestination(val route: String, val title: String) {
    /** Главный экран настроек с списком всех секций */
    object Main : SettingsDestination("settings", "Настройки")
    
    /** Настройки внешнего вида (тема приложения) */
    object Appearance : SettingsDestination("settings/appearance", "Внешний вид")
    
    /** Настройки отображения маршрутов (режим, колонки) */
    object Display : SettingsDestination("settings/display", "Отображение")
    
    /** Настройки обновлений приложения и данных */
    object Updates : SettingsDestination("settings/updates", "Обновления")
    
    /** Настройки уведомлений о времени отправления */
    object Notifications : SettingsDestination("settings/notifications", "Уведомления")
    
    /** Настройки тихого режима */
    object QuietMode : SettingsDestination("settings/quiet", "Тихий режим")
    
    /** Настройки вибрации при уведомлениях */
    object Vibration : SettingsDestination("settings/vibration", "Вибрация")
    
    /** Управление данными (очистка, сброс) */
    object Data : SettingsDestination("settings/data", "Управление данными")
    
    /** О программе и разработчике */
    object About : SettingsDestination("settings/about", "О программе")
    
    companion object {
        /**
         * Возвращает все доступные пункты назначения
         */
        fun all(): List<SettingsDestination> = listOf(
            Appearance,
            Display,
            Updates,
            Notifications,
            QuietMode,
            Vibration,
            Data,
            About
        )
    }
}

