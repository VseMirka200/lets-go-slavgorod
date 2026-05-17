package ru.slavgorod.transport.ui.viewmodel

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.slavgorod.transport.R
import ru.slavgorod.transport.core.AppText
import ru.slavgorod.transport.core.Constants
import ru.slavgorod.transport.data.local.PreferencesKeys
import ru.slavgorod.transport.data.local.stateInPreferences
import ru.slavgorod.transport.data.model.BusRoute
import ru.slavgorod.transport.data.repository.RoutesLoadState
import ru.slavgorod.transport.data.repository.RoutesTableDataSource
import ru.slavgorod.transport.data.repository.ScheduleDataSourceStatus
import ru.slavgorod.transport.logging.UserActionLogger
import ru.slavgorod.transport.notifications.AppForegroundTracker
import ru.slavgorod.transport.ui.model.UiMessage
import ru.slavgorod.transport.ui.viewmodel.support.UiMessageState
import timber.log.Timber

@Stable
data class RoutesUiState(
    val routes: List<BusRoute> = emptyList(),
    val pinnedRouteIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val dataSourceStatus: ScheduleDataSourceStatus = ScheduleDataSourceStatus()
)

@OptIn(FlowPreview::class)
class RoutesViewModel(
    private val routeRepository: RoutesTableDataSource,
    private val appForegroundTracker: AppForegroundTracker,
    private val displaySettingsDataStore: DataStore<Preferences>? = null,
    loadRoutesOnInit: Boolean = true,
    private val initialLoadTimeoutMillis: Long = LOAD_TIMEOUT_MS
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutesUiState(isLoading = true))
    val uiState: StateFlow<RoutesUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val uiMessageState = UiMessageState()
    val uiMessage: StateFlow<UiMessage?> = uiMessageState.uiMessage

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    var pinActionRouteId: String? by mutableStateOf(null)
        private set

    var isHeaderMenuOpen: Boolean by mutableStateOf(false)
        private set

    private var lastLoggedSearchQuery: String by mutableStateOf("")

    private val routeOrder: StateFlow<List<String>> = displaySettingsDataStore?.stateInPreferences(
        scope = viewModelScope,
        initialValue = emptyList()
    ) { preferences ->
        preferences[PreferencesKeys.routeOrder].toRouteIdList()
    } ?: MutableStateFlow(emptyList())

    private val pinnedRouteIds: StateFlow<Set<String>> =
        displaySettingsDataStore?.stateInPreferences(
            scope = viewModelScope,
            initialValue = emptySet()
        ) { preferences ->
            preferences[PreferencesKeys.pinnedRouteIds].toRouteIdList().toSet()
        } ?: MutableStateFlow(emptySet())
    private var hasLoadedRoutes = false

    init {
        observeScheduleUpdateNotices()
        if (loadRoutesOnInit) {
            ensureRoutesLoaded()
        }
        observeUiState()
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun shouldLogSearchQuery(query: String): Boolean {
        return query != lastLoggedSearchQuery
    }

    fun markSearchQueryLogged(query: String) {
        lastLoggedSearchQuery = query
    }

    fun openHeaderMenu() {
        isHeaderMenuOpen = true
    }

    fun dismissHeaderMenu() {
        isHeaderMenuOpen = false
    }

    fun showPinAction(routeId: String) {
        pinActionRouteId = routeId
    }

    fun clearPinActionRoute() {
        pinActionRouteId = null
    }

    fun clearTransientHomeUiState() {
        clearPinActionRoute()
        dismissHeaderMenu()
    }

    fun handleBackPressedFromHome() {
        when {
            isHeaderMenuOpen -> dismissHeaderMenu()
            pinActionRouteId != null -> clearPinActionRoute()
        }
    }

    fun clearUiMessage() {
        uiMessageState.clear()
    }

    fun toggleRoutePinned(routeId: String) {
        val dataStore = displaySettingsDataStore ?: return
        val update = buildRoutePinUpdate(
            routeId = routeId,
            currentPinnedRouteIds = pinnedRouteIds.value,
            currentRouteOrder = routeOrder.value,
            allRouteIds = routeRepository.routes.value.map(BusRoute::id)
        )
        UserActionLogger.routePinned(routeId, update.isPinningRoute)

        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences.applyRoutePinUpdate(update)
            }
        }
    }

    fun refresh(source: String = AppText.get(R.string.route_refresh_manual)) {
        UserActionLogger.routeRefreshRequested(source)
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            clearUiMessage()

            try {
                handleRefreshResult(routeRepository.refreshRoutesFromLocal())
                delay(Constants.PULL_TO_REFRESH_MIN_DELAY_MS)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun ensureRoutesLoaded() {
        viewModelScope.launch(Dispatchers.IO) {
            if (routeRepository.getAllRoutes().isNotEmpty()) return@launch

            try {
                routeRepository.refreshRoutesFromLocal(notifyUser = false)
            } catch (exception: Exception) {
                Timber.e(exception, "Failed to load routes")
            }
        }
    }

    private fun observeScheduleUpdateNotices() {
        viewModelScope.launch {
            routeRepository.scheduleUpdateNotices.collect { notice ->
                if (appForegroundTracker.isAppForeground.value) {
                    uiMessageState.publish(notice.textResId, notice.textArgs)
                }
            }
        }
    }

    private fun observeUiState() {
        viewModelScope.launch {
            var loadTimeoutJob: Job? = null
            combine(
                routeRepository.routes,
                _searchQuery
                    .debounce(Constants.SEARCH_DEBOUNCE_MS)
                    .distinctUntilChanged(),
                routeRepository.dataSourceStatus,
                pinnedRouteIds
            ) { routes, query, dataSourceStatus, pinnedRouteIds ->
                RoutesStateInput(
                    routes = routes,
                    query = query,
                    dataSourceStatus = dataSourceStatus,
                    pinnedRouteIds = pinnedRouteIds
                )
            }.collect { input ->
                val computedState = withContext(Dispatchers.Default) {
                    buildRoutesUiState(
                        input = input,
                        hasLoadedRoutes = hasLoadedRoutes,
                        currentError = _uiState.value.error
                    )
                }

                hasLoadedRoutes = computedState.hasLoadedRoutes

                if (computedState.uiState.isLoading) {
                    if (loadTimeoutJob?.isActive != true) {
                        loadTimeoutJob = launchLoadTimeout()
                    }
                } else {
                    loadTimeoutJob?.cancel()
                }

                _uiState.value = computedState.uiState
            }
        }
    }

    private fun launchLoadTimeout(): Job {
        return viewModelScope.launch {
            delay(initialLoadTimeoutMillis)
            _uiState.update { currentState ->
                resolveInitialLoadTimeoutState(
                    currentState = currentState,
                    timeoutErrorMessage = AppText.get(R.string.routes_initial_load_failed)
                )
            }
        }
    }

    private fun handleRefreshResult(loadState: RoutesLoadState) {
        val lastUpdatedAtMillis = routeRepository.dataSourceStatus.value.lastUpdatedAtMillis
        when (val effect = buildRoutesRefreshResultEffect(loadState, lastUpdatedAtMillis)) {
            is RoutesRefreshResultEffect.SuccessNotice -> {
                uiMessageState.publish(effect.textResId, effect.args)
            }

            is RoutesRefreshResultEffect.LoggedMessage -> {
                UserActionLogger.scheduleUpdateResult(effect.logMessage)
                effect.message?.let { message ->
                    uiMessageState.publish(message.messageResId, message.args)
                }
            }
        }
    }

    private companion object {
        private const val LOAD_TIMEOUT_MS = 35_000L
    }
}
