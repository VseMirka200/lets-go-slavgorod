package com.example.lets_go_slavgorod.ui.navigation

/**
 * Экраны навигации приложения
 * 
 * Определяет основные маршруты навигации в приложении.
 * Используется sealed class для type-safe навигации.
 * 
 * Основные экраны:
 * - Home: главный экран со списком всех маршрутов
 * - Settings: настройки приложения (тема, отображение, обновления)
 * - About: информация о приложении и разработчике
 * 
 * Дополнительные маршруты (не в sealed class):
 * - schedule/{routeId}: расписание конкретного маршрута
 * - route_notifications/{routeId}: настройки уведомлений маршрута
 * 
 * @property route строковый идентификатор маршрута для NavController
 */
sealed class Screen(val route: String) {
    /** Главный экран со списком маршрутов */
    object Home : Screen("home")
    
    /** Экран настроек приложения */
    object Settings : Screen("settings")
    
    /** Экран информации о приложении */
    object About : Screen("about")
}
