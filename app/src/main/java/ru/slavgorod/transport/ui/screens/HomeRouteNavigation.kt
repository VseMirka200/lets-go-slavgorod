package ru.slavgorod.transport.ui.screens

import androidx.navigation.NavController
import ru.slavgorod.transport.data.model.BusRoute
import ru.slavgorod.transport.logging.UserActionLogger
import ru.slavgorod.transport.ui.navigation.Screen
import timber.log.Timber

/**
 * Открывает экран расписания выбранного маршрута.
 *
 * Подсказка разработчику: launchSingleTop предотвращает дублирование одного маршрута
 * в стеке при повторном нажатии на карточку.
 */
internal fun navigateToSchedule(navController: NavController, route: BusRoute) {
    try {
        UserActionLogger.routeOpened(route.id, route.routeNumber)
        navController.navigate(Screen.Schedule.createRoute(route.id)) {
            launchSingleTop = true
            restoreState = true
            popUpTo(Screen.Home.route) { saveState = true }
        }
    } catch (exception: Exception) {
        Timber.tag("Navigation").e(exception, "Ошибка навигации для маршрута: %s", route.id)
    }
}
