package ru.slavgorod.transport.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.slavgorod.transport.core.Constants
import timber.log.Timber

/**
 * Запускает фоновую проверку обновлений по таймеру и после восстановления сети.
 *
 * Подсказка разработчику: start и stop должны оставаться идемпотентными, поскольку
 * жизненный цикл приложения может вызывать их повторно.
 */
class RoutesAutoRefreshManager(
    routeRepository: RoutesTableDataSource,
    private val connectionStateFlow: Flow<Boolean>,
    initialConnectionState: Boolean,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    externalScope: CoroutineScope? = null,
    private val refreshAction: suspend () -> RoutesLoadState = {
        routeRepository.checkRoutesForUpdates(notifyUser = false)
    }
) {

    private val scope = externalScope ?: CoroutineScope(SupervisorJob() + dispatcher)
    private var refreshJob: Job? = null
    private var connectionObserverJob: Job? = null
    private var wasOnline = initialConnectionState

    fun start() {
        startPeriodicRefresh()
        observeNetworkRestoration()
    }

    fun stop() {
        refreshJob?.cancel()
        refreshJob = null
        connectionObserverJob?.cancel()
        connectionObserverJob = null
    }

    private fun startPeriodicRefresh() {
        if (refreshJob?.isActive == true) return

        refreshJob = scope.launch {
            delay(Constants.SCHEDULE_AUTO_REFRESH_INTERVAL_MS)

            while (isActive) {
                runRefreshSafely("Не удалось выполнить периодическое обновление расписания")

                delay(Constants.SCHEDULE_AUTO_REFRESH_INTERVAL_MS)
            }
        }
    }

    private fun observeNetworkRestoration() {
        if (connectionObserverJob?.isActive == true) return

        connectionObserverJob = scope.launch {
            connectionStateFlow
                .distinctUntilChanged()
                .collect { isOnline ->
                    if (!wasOnline && isOnline) {
                        runRefreshSafely("Не удалось обновить расписание после восстановления сети")
                    }
                    wasOnline = isOnline
                }
        }
    }

    private suspend fun runRefreshSafely(errorMessage: String) {
        runCatching {
            refreshAction()
        }.onFailure { exception ->
            Timber.w(exception, errorMessage)
        }
    }
}
