package com.example.lets_go_slavgorod.ui.navigation

/**
 * Экраны навигации приложения
 * 
 * Версия: 2.0
 * Последнее обновление: Октябрь 2025
 * 
 * Определяет основные маршруты навигации в приложении.
 * Используется sealed class для type-safe навигации.
 * 
 * Основные экраны:
 * - **Home**: главный экран со списком всех маршрутов
 * - **Settings**: настройки приложения (тема, отображение, уведомления, данные, информация о приложении)
 * 
 * Дополнительные маршруты (не в sealed class):
 * - **schedule/{routeId}**: расписание конкретного маршрута с фильтрами
 * - **route_notifications/{routeId}**: настройки уведомлений для конкретного маршрута
 * 
 * v3.0 Changes (Октябрь 2025):
 * - Удалены неиспользуемые экраны (ThemeSettings, DownloadStats)
 * - Оптимизированы импорты и зависимости
 * - Улучшена производительность навигации
 * 
 * @property route строковый идентификатор маршрута для NavController
 * 
 * @author VseMirka200
 * @version 2.0
 * @since 1.0
 */
sealed class Screen(val route: String) {
    /** Главный экран со списком маршрутов и поиском */
    object Home : Screen("home")
    
    /** Главный экран настроек (список разделов) */
    object Settings : Screen("settings")
    
    /** Настройки отображения */
    object DisplaySettings : Screen("display_settings")
    
    /** Настройки уведомлений */
    object NotificationSettings : Screen("notification_settings")
    
    /** Управление данными */
    object DataManagement : Screen("data_management")
    
    /** О приложении */
    object About : Screen("about")
    
    /** Экран логов */
    object Logs : Screen("logs")
    
}