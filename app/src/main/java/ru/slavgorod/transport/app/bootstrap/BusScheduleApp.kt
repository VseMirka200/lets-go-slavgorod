package ru.slavgorod.transport.app.bootstrap

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import org.koin.androidx.compose.koinViewModel
import ru.slavgorod.transport.data.network.NetworkMonitor
import ru.slavgorod.transport.ui.components.app.AppScreenScaffold
import ru.slavgorod.transport.ui.model.AppTheme
import ru.slavgorod.transport.ui.theme.LetsGoSlavgorodTheme
import ru.slavgorod.transport.ui.viewmodel.RoutesViewModel
import ru.slavgorod.transport.ui.viewmodel.ThemeViewModel

data class BusScheduleAppLaunchConfig(
    val initialPendingNavigationRouteId: String?,
    val initialShowDisclaimerDialog: Boolean,
    val onNavigationHandled: () -> Unit,
    val onDisclaimerAccept: () -> Unit,
    val onDisclaimerDontShowAgain: () -> Unit,
    val onDisclaimerDismiss: () -> Unit
)

@Composable
fun BusScheduleApp(
    themeViewModel: ThemeViewModel,
    launchConfig: BusScheduleAppLaunchConfig
) {
    val navController = rememberNavController()
    val sharedRoutesViewModel: RoutesViewModel = koinViewModel()
    val routesUiState by sharedRoutesViewModel.uiState.collectAsStateWithLifecycle()
    val routesUiMessage by sharedRoutesViewModel.uiMessage.collectAsStateWithLifecycle()
    val currentAppTheme by themeViewModel.currentTheme.collectAsStateWithLifecycle()
    val appContext = LocalContext.current.applicationContext
    val isConnected by NetworkMonitor.observeConnectionState(appContext)
        .collectAsStateWithLifecycle(initialValue = NetworkMonitor.isConnected(appContext))
    val useDarkTheme = when (currentAppTheme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }
    val routesStatusMessage = rememberRoutesStatusMessage(
        routesUiState = routesUiState,
        routesUiMessage = routesUiMessage,
        isConnected = isConnected,
        onMessageTimeout = sharedRoutesViewModel::clearUiMessage
    )
    val shouldShowStartupSplash = rememberShouldShowStartupSplash(routesUiState)

    LetsGoSlavgorodTheme(darkTheme = useDarkTheme) {
        val view = LocalView.current

        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            WindowInsetsControllerCompat(window, view).isAppearanceLightStatusBars = false
        }

        AppScreenScaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                AppNavHost(
                    navController = navController,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    routesStatusMessage = routesStatusMessage,
                    themeViewModel = themeViewModel,
                    sharedRoutesViewModel = sharedRoutesViewModel
                )

                AppOverlays(
                    config = AppOverlaysConfig(
                        showDisclaimerDialog = launchConfig.initialShowDisclaimerDialog,
                        onDisclaimerDismiss = launchConfig.onDisclaimerDismiss,
                        onDisclaimerAccept = launchConfig.onDisclaimerAccept,
                        onDisclaimerDontShowAgain = launchConfig.onDisclaimerDontShowAgain,
                        navController = navController,
                        pendingRouteId = launchConfig.initialPendingNavigationRouteId,
                        onHandled = launchConfig.onNavigationHandled
                    )
                )

                if (shouldShowStartupSplash) {
                    StartupSplashOverlay()
                }
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
