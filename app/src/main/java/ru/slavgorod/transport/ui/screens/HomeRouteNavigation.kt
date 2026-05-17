package ru.slavgorod.transport.ui.screens

import androidx.navigation.NavController
import ru.slavgorod.transport.data.model.BusRoute
import ru.slavgorod.transport.logging.UserActionLogger
import ru.slavgorod.transport.ui.navigation.Screen
import timber.log.Timber

internal fun navigateToSchedule(navController: NavController, route: BusRoute) {
    try {
        UserActionLogger.routeOpened(route.id, route.routeNumber)
        navController.navigate(Screen.Schedule.createRoute(route.id)) {
            launchSingleTop = true
            restoreState = true
            popUpTo(Screen.Home.route) { saveState = true }
        }
    } catch (exception: Exception) {
        Timber.tag("Navigation").e(exception, "Navigation error for route: %s", route.id)
    }
}
