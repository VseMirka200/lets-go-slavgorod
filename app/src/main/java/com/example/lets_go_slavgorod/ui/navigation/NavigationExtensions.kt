package com.example.lets_go_slavgorod.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import timber.log.Timber

/**
 * Extension функции для упрощения навигации в приложении
 * 
 * Предоставляет удобные методы для навигации с общими параметрами,
 * устраняя дублирование кода и обеспечивая единообразие поведения.
 * 
 * Архитектура навигации:
 * - Один основной экран (Home) с маршрутами
 * - Настройки доступны из главного экрана
 * - Детали маршрута и уведомления в отдельных экранах
 * 
 * Особенности:
 * - Стандартизированная обработка ошибок с Timber
 * - Логирование всех навигационных событий
 * - Переиспользуемые конфигурации (launchSingleTop, restoreState)
 * - Type-safe навигация через параметры
 * 
 * @author VseMirka200
 * @version 2.0
 * @since 2.0
 */

/**
 * Навигация с оптимизированными параметрами
 * 
 * Использует стандартные оптимизации:
 * - launchSingleTop - избегаем дублирования экранов
 * - restoreState - восстанавливаем состояние при возврате
 * - saveState - сохраняем состояние для восстановления
 * 
 * @param route маршрут для навигации
 * @param popUpToRoute маршрут для popUpTo (по умолчанию "home")
 * @param inclusive удалить popUpToRoute из стека
 * @param builder дополнительная конфигурация навигации
 */
fun NavController.navigateOptimized(
    route: String,
    popUpToRoute: String = "home",
    inclusive: Boolean = false,
    builder: NavOptionsBuilder.() -> Unit = {}
) {
    try {
        Timber.d("Navigating to: $route")
        navigate(route) {
            launchSingleTop = true
            restoreState = true
            popUpTo(popUpToRoute) {
                saveState = true
                this.inclusive = inclusive
            }
            builder()
        }
    } catch (e: Exception) {
        Timber.e(e, "Navigation error to route: $route")
    }
}

/**
 * Навигация к расписанию маршрута
 * 
 * @param routeId ID маршрута
 */
fun NavController.navigateToSchedule(routeId: String) {
    navigateOptimized(
        route = "schedule/$routeId",
        popUpToRoute = "home"
    )
}

/**
 * Навигация к настройкам уведомлений маршрута
 * 
 * @param routeId ID маршрута
 */
fun NavController.navigateToRouteNotificationSettings(routeId: String) {
    navigateOptimized(
        route = "route_notifications/$routeId"
    )
}

/**
 * Навигация к экрану "О программе"
 */
fun NavController.navigateToAbout() {
    navigateOptimized(
        route = Screen.About.route,
        popUpToRoute = "settings"
    )
}

/**
 * Навигация назад с обработкой ошибок
 * 
 * @return true если навигация успешна, false иначе
 */
fun NavController.navigateBackSafe(): Boolean {
    return try {
        Timber.d("Navigating back")
        popBackStack()
    } catch (e: Exception) {
        Timber.e(e, "Error navigating back")
        false
    }
}

/**
 * Навигация к главному экрану с очисткой стека
 */
fun NavController.navigateToHome(clearBackStack: Boolean = false) {
    navigate("home") {
        if (clearBackStack) {
            popUpTo(0) { inclusive = true }
        }
        launchSingleTop = true
    }
}

/**
 * Проверка текущего маршрута
 * 
 * @param route маршрут для проверки
 * @return true если текущий маршрут совпадает
 */
fun NavController.isCurrentRoute(route: String): Boolean {
    return currentDestination?.route == route
}

