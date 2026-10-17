package ru.slavgorod.transport.ui.navigation

import androidx.navigation.NavController

fun NavController.navigateToSettingsTopLevel() {
    val currentRoute = currentDestination?.route
    if (currentRoute == Screen.Settings.route) return

    val returnedToSettings = Screen.isSettingsDestination(currentRoute) &&
            popBackStack(Screen.Settings.route, false)
    if (!returnedToSettings) {
        navigate(Screen.Settings.route) {
            launchSingleTop = true
            restoreState = true
            popUpTo(Screen.Home.route) { saveState = true }
        }
    }
}
