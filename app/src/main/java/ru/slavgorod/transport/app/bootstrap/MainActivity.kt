package ru.slavgorod.transport.app.bootstrap

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.slavgorod.transport.data.local.DisclaimerManager
import ru.slavgorod.transport.data.local.ScheduleNotificationSettings
import ru.slavgorod.transport.data.repository.RoutesAutoRefreshManager
import ru.slavgorod.transport.data.repository.RoutesTableDataSource
import ru.slavgorod.transport.ui.viewmodel.ThemeViewModel

class MainActivity : ComponentActivity() {

    private val themeViewModel: ThemeViewModel by viewModel()
    private val routeRepository: RoutesTableDataSource by inject()
    private val routesAutoRefreshManager: RoutesAutoRefreshManager by inject()
    private val scheduleNotificationSettings: ScheduleNotificationSettings by inject()
    private var pendingNavigationRouteId: String? by mutableStateOf(null)
    private val navigationMutex = Mutex()
    private var shouldShowDisclaimerDialog: Boolean by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BusScheduleApp(
                themeViewModel = themeViewModel,
                launchConfig = BusScheduleAppLaunchConfig(
                    initialPendingNavigationRouteId = pendingNavigationRouteId,
                    initialShowDisclaimerDialog = shouldShowDisclaimerDialog,
                    onNavigationHandled = { pendingNavigationRouteId = null },
                    onDisclaimerAccept = { acceptDisclaimer() },
                    onDisclaimerDontShowAgain = { acceptDisclaimer() },
                    onDisclaimerDismiss = { shouldShowDisclaimerDialog = false }
                )
            )
        }

        lifecycleScope.launch {
            if (DisclaimerManager.shouldShowDisclaimer(this@MainActivity)) {
                shouldShowDisclaimerDialog = true
            }
        }

        handleLaunchIntent(intent)
    }

    override fun onStart() {
        super.onStart()

        routesAutoRefreshManager.start()
        lifecycleScope.launch {
            scheduleNotificationSettings.syncPermissionState(this@MainActivity)
        }

        lifecycleScope.launch {
            if (routeRepository.isOnline()) {
                routeRepository.checkRoutesForUpdates(notifyUser = false)
            }
        }
    }

    override fun onStop() {
        routesAutoRefreshManager.stop()
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLaunchIntent(intent)
    }

    private fun handleLaunchIntent(intent: Intent?) {
        intent ?: return

        lifecycleScope.launch {
            navigationMutex.withLock {
                pendingNavigationRouteId = intent.getStringExtra(EXTRA_NAVIGATE_TO_ROUTE)
            }
        }
    }

    private fun acceptDisclaimer() {
        lifecycleScope.launch {
            DisclaimerManager.markDisclaimerAccepted(this@MainActivity)
            shouldShowDisclaimerDialog = false
        }
    }

    private companion object {
        private const val EXTRA_NAVIGATE_TO_ROUTE = "navigate_to_route"
    }
}
