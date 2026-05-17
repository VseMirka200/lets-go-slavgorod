package ru.slavgorod.transport.app.bootstrap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay
import ru.slavgorod.transport.R
import ru.slavgorod.transport.core.AppText
import ru.slavgorod.transport.ui.components.DisclaimerDialog
import ru.slavgorod.transport.ui.navigation.Screen
import timber.log.Timber

internal data class AppOverlaysConfig(
    val showDisclaimerDialog: Boolean,
    val onDisclaimerDismiss: () -> Unit,
    val onDisclaimerAccept: () -> Unit,
    val onDisclaimerDontShowAgain: () -> Unit,
    val navController: NavHostController,
    val pendingRouteId: String?,
    val onHandled: () -> Unit
)

@Composable
internal fun AppOverlays(config: AppOverlaysConfig) {
    if (config.showDisclaimerDialog) {
        DisclaimerDialog(
            onDismiss = config.onDisclaimerDismiss,
            onAccept = config.onDisclaimerAccept
        )
    }
    HandlePendingNavigation(
        navController = config.navController,
        pendingRouteId = config.pendingRouteId,
        onHandled = config.onHandled
    )
}

@Composable
@Suppress("TooGenericExceptionCaught")
private fun HandlePendingNavigation(
    navController: NavHostController,
    pendingRouteId: String?,
    onHandled: () -> Unit
) {
    LaunchedEffect(pendingRouteId) {
        pendingRouteId?.let { routeId ->
            try {
                val destination = Screen.Schedule.createRoute(routeId)
                delay(500)
                val currentRoute = navController.currentDestination?.route
                if (currentRoute != destination) {
                    navController.navigate(destination)
                }
                onHandled()
            } catch (e: Exception) {
                Timber.e(e, AppText.get(R.string.navigation_error_route, routeId))
            }
        }
    }
}
