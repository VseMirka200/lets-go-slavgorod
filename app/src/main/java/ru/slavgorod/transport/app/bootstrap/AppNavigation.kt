package ru.slavgorod.transport.app.bootstrap

import androidx.compose.runtime.Composable
import androidx.activity.BackEventCompat
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import org.koin.compose.koinInject
import ru.slavgorod.transport.data.repository.RoutesTableDataSource
import ru.slavgorod.transport.ui.navigation.Screen
import ru.slavgorod.transport.ui.screens.HomeScreen
import ru.slavgorod.transport.ui.screens.ScheduleScreen
import ru.slavgorod.transport.ui.screens.settings.AboutScreen
import ru.slavgorod.transport.ui.screens.settings.DisplaySettingsScreen
import ru.slavgorod.transport.ui.screens.settings.ResetSettingsScreen
import ru.slavgorod.transport.ui.screens.settings.SettingsMainScreen
import ru.slavgorod.transport.ui.screens.settings.ScheduleSettingsScreen
import ru.slavgorod.transport.ui.screens.settings.ScheduleSettingsDetailScreen
import ru.slavgorod.transport.ui.viewmodel.RoutesViewModel
import ru.slavgorod.transport.ui.viewmodel.ThemeViewModel

@Composable
/**
 * Описывает единый граф экранов приложения.
 *
 * Подсказка разработчику: RoutesViewModel передаётся как общий экземпляр, чтобы
 * возвращение со страницы расписания не запускало загрузку маршрутов заново.
 */
fun AppNavHost(
    navController: NavHostController,
    routesStatusMessage: String?,
    themeViewModel: ThemeViewModel,
    sharedRoutesViewModel: RoutesViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier,
        popEnterTransition = { slideInHorizontally(tween(300)) { -it / 4 } },
        popExitTransition = { slideOutHorizontally(tween(300)) { it } },
        predictivePopEnterTransition = { edge ->
            slideInHorizontally(tween(300)) { if (edge == BackEventCompat.EDGE_RIGHT) it / 4 else -it / 4 }
        },
        predictivePopExitTransition = { edge ->
            slideOutHorizontally(tween(300)) { if (edge == BackEventCompat.EDGE_RIGHT) -it else it }
        }
    ) {
        homeGraph(navController, sharedRoutesViewModel, routesStatusMessage)
        scheduleGraph(navController, sharedRoutesViewModel)
        settingsGraph(navController, themeViewModel)
    }
}

private fun NavGraphBuilder.homeGraph(
    navController: NavHostController,
    sharedRoutesViewModel: RoutesViewModel,
    routesStatusMessage: String?
) {
    composable(route = Screen.Home.route) {
        HomeScreen(
            navController = navController,
            routesViewModel = sharedRoutesViewModel,
            statusMessage = routesStatusMessage
        )
    }
}

private fun NavGraphBuilder.scheduleGraph(
    navController: NavHostController,
    sharedRoutesViewModel: RoutesViewModel
) {
    composable(
        route = Screen.Schedule.route,
        arguments = listOf(
            navArgument(Screen.Schedule.ARG_ROUTE_ID) { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val routeId = backStackEntry.arguments?.getString(Screen.Schedule.ARG_ROUTE_ID) ?: ""
        val routeRepository: RoutesTableDataSource = koinInject()
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            ScheduleScreen(
                routeId = routeId,
                routeRepository = routeRepository,
                routesViewModel = sharedRoutesViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

private fun NavGraphBuilder.settingsGraph(
    navController: NavHostController,
    themeViewModel: ThemeViewModel
) {
    composable(route = Screen.Settings.route) {
        SettingsMainScreen(
            themeViewModel = themeViewModel,
            onBackClick = { navController.popBackStack() },
            onOpenPage = { navController.navigate(it) }
        )
    }
    composable(route = Screen.About.route) {
        AboutScreen(onBackClick = { navController.popBackStack() })
    }
    composable(route = Screen.DisplaySettings.route) {
        DisplaySettingsScreen(
            themeViewModel = themeViewModel,
            onBackClick = { navController.popBackStack() }
        )
    }
    composable(route = Screen.ColumnsSettings.route) {
        DisplaySettingsScreen(
            themeViewModel = themeViewModel,
            onBackClick = { navController.popBackStack() },
            showThemeSection = false
        )
    }
    composable(route = Screen.ScheduleSettings.route) {
        ScheduleSettingsScreen(
            onBackClick = { navController.popBackStack() }
        )
    }
    composable(route = Screen.ScheduleUpdates.route) {
        ScheduleSettingsDetailScreen(showNotifications = false, onBackClick = { navController.popBackStack() })
    }
    composable(route = Screen.ScheduleNotifications.route) {
        ScheduleSettingsDetailScreen(showNotifications = true, onBackClick = { navController.popBackStack() })
    }
    composable(route = Screen.ResetSettings.route) {
        ResetSettingsScreen(onBackClick = { navController.popBackStack() })
    }
}
