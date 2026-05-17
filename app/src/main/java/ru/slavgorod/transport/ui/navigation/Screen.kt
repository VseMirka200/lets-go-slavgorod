package ru.slavgorod.transport.ui.navigation

private const val HOME_ROUTE = "home"
private const val SETTINGS_ROUTE = "settings"
private const val DISPLAY_SETTINGS_ROUTE = "display_settings"
private const val ABOUT_ROUTE = "about"
private const val RESET_SETTINGS_ROUTE = "reset_settings"

private const val ROUTE_ID_ARG = "routeId"
private const val SCHEDULE_BASE_ROUTE = "schedule"
private const val SCHEDULE_ROUTE = "$SCHEDULE_BASE_ROUTE/{$ROUTE_ID_ARG}"

sealed class Screen(val route: String) {
    data object Home : Screen(HOME_ROUTE)

    data object Schedule : Screen(SCHEDULE_ROUTE) {
        const val ARG_ROUTE_ID = ROUTE_ID_ARG
        const val BASE_ROUTE = SCHEDULE_BASE_ROUTE

        fun createRoute(routeId: String): String {
            return "$BASE_ROUTE/$routeId"
        }
    }

    data object Settings : Screen(SETTINGS_ROUTE)
    data object DisplaySettings : Screen(DISPLAY_SETTINGS_ROUTE)
    data object About : Screen(ABOUT_ROUTE)
    data object ResetSettings : Screen(RESET_SETTINGS_ROUTE)

    companion object {
        fun isRoutesDestination(route: String?): Boolean {
            return when (route) {
                HOME_ROUTE,
                SCHEDULE_ROUTE -> true

                else -> false
            }
        }

        fun isSettingsDestination(route: String?): Boolean {
            return when (route) {
                SETTINGS_ROUTE,
                DISPLAY_SETTINGS_ROUTE,
                ABOUT_ROUTE,
                RESET_SETTINGS_ROUTE -> true

                else -> false
            }
        }
    }
}
