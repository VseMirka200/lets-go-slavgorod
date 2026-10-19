package ru.slavgorod.transport.data.repository

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import ru.slavgorod.transport.R
import ru.slavgorod.transport.core.Constants
import ru.slavgorod.transport.core.search
import ru.slavgorod.transport.data.local.JsonDataSource
import ru.slavgorod.transport.data.local.ScheduleCacheStore
import ru.slavgorod.transport.data.model.BusRoute
import ru.slavgorod.transport.data.model.BusSchedule
import ru.slavgorod.transport.data.model.ScheduleChange
import ru.slavgorod.transport.data.network.NetworkMonitor
import ru.slavgorod.transport.logging.UserActionLogger
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Единая точка загрузки расписания из сети, сохранённой копии и памяти.
 *
 * Подсказка разработчику: сначала публикуйте согласованные маршруты и индекс рейсов,
 * а отмену coroutine всегда пробрасывайте дальше — она не является сетевой ошибкой.
 */
class RoutesTableDataSource(
    context: Context,
    jsonDataSource: JsonDataSource = JsonDataSource(),
    onlineChecker: () -> Boolean = { NetworkMonitor.isConnected(context.applicationContext) },
    scheduleCacheStore: ScheduleCacheStore = ScheduleCacheStore(context.applicationContext),
    httpClient: OkHttpClient = defaultHttpClient(),
    private val remoteScheduleFetcher: ScheduleFetcher = RemoteScheduleFetcher(
        onlineChecker = onlineChecker,
        httpClient = httpClient
    ),
    private val scheduleSnapshotRepository: ScheduleSnapshotStore = ScheduleSnapshotRepository(
        scheduleCacheStore
    ),
    private val scheduleJsonParser: ScheduleJsonParser = ScheduleJsonParser(jsonDataSource),
    externalScope: CoroutineScope? = null,
    autoLoadOnInit: Boolean = true
) {

    private val _routes = MutableStateFlow<List<BusRoute>>(emptyList())
    val routes: StateFlow<List<BusRoute>> = _routes.asStateFlow()

    private val _dataSourceStatus = MutableStateFlow(ScheduleDataSourceStatus())
    val dataSourceStatus: StateFlow<ScheduleDataSourceStatus> = _dataSourceStatus.asStateFlow()

    private val _scheduleUpdateNotices = MutableSharedFlow<ScheduleUpdateNotice>(extraBufferCapacity = 1)
    val scheduleUpdateNotices: SharedFlow<ScheduleUpdateNotice> = _scheduleUpdateNotices.asSharedFlow()

    private val memoryCache = RoutesMemoryCache()
    private val repositoryScope = externalScope ?: CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val loadMutex = Mutex()

    init {
        if (autoLoadOnInit) {
            repositoryScope.launch {
                loadRoutesFromPersistentCache()
                refreshRoutesFromLocal(notifyUser = false)
            }
        }
    }

    fun isOnline(): Boolean = remoteScheduleFetcher.isOnline()

    fun getRouteById(routeId: String?): BusRoute? =
        routeId?.takeIf(String::isNotBlank)?.let(memoryCache::routeById)

    fun searchRoutes(query: String): List<BusRoute> =
        if (query.isBlank()) getAllRoutes() else _routes.value.search(query)

    fun getAllRoutes(): List<BusRoute> = _routes.value

    suspend fun getSchedulesForRoute(routeId: String): List<BusSchedule> {
        require(routeId.isNotBlank()) { "Идентификатор маршрута не может быть пустым" }

        ensureRoutesLoadedFromPersistentCache()
        memoryCache.schedulesForRoute(routeId)?.let { return it }

        val jsonSnapshot = memoryCache.latestJson ?: remoteScheduleFetcher.fetchWithRetry()
        if (jsonSnapshot.isNullOrBlank()) {
            Timber.e("Источник JSON недоступен для маршрута %s", routeId)
            return emptyList()
        }

        cacheSchedulesFromJson(jsonSnapshot)
        if (!memoryCache.hasRoutes) {
            scheduleJsonParser.parseRoutes(jsonSnapshot)
                .takeIf(List<BusRoute>::isNotEmpty)
                ?.let(::updateRoutesState)
        }

        return memoryCache.schedulesForRoute(routeId).orEmpty()
    }

    suspend fun getScheduleChanges(): List<ScheduleChange> {
        ensureRoutesLoadedFromPersistentCache()
        return memoryCache.latestJson?.let(scheduleJsonParser::parseScheduleChanges).orEmpty()
    }

    suspend fun refreshRoutesFromLocal(notifyUser: Boolean = true): RoutesLoadState =
        loadRoutesFromRemote(reportStatus = true, notifyUser = notifyUser)

    suspend fun checkRoutesForUpdates(notifyUser: Boolean = true): RoutesLoadState =
        loadRoutesFromRemote(reportStatus = false, notifyUser = notifyUser)

    private suspend fun loadRoutesFromRemote(
        reportStatus: Boolean,
        notifyUser: Boolean
    ): RoutesLoadState = withContext(Dispatchers.IO) {
        loadMutex.withLock {
            ensureRoutesLoadedFromPersistentCache()

            if (!isOnline()) {
                return@withLock reportUnavailableData(
                    hasCachedData = memoryCache.hasCachedData,
                    reportStatus = reportStatus,
                    isNetworkError = false
                )
            }

            val remoteJson = remoteScheduleFetcher.fetchWithRetry()
            if (remoteJson.isNullOrBlank()) {
                return@withLock reportUnavailableData(
                    hasCachedData = memoryCache.hasCachedData,
                    reportStatus = reportStatus,
                    isNetworkError = true
                )
            }

            val parsedRoutes = scheduleJsonParser.parseRoutes(remoteJson)
            if (parsedRoutes.isEmpty()) {
                Timber.w("JSON удалённого расписания пуст или некорректен; обновление пропущено")
                return@withLock reportInvalidJson(memoryCache.hasCachedData, reportStatus)
            }

            if (remoteJson == memoryCache.latestJson) {
                return@withLock reportNoChanges(reportStatus)
            }

            persistRemoteRoutes(remoteJson, parsedRoutes, notifyUser)
        }
    }

    private fun reportUnavailableData(
        hasCachedData: Boolean,
        reportStatus: Boolean,
        isNetworkError: Boolean
    ): RoutesLoadState {
        val loadState = when {
            isNetworkError && hasCachedData -> RoutesLoadState.NETWORK_ERROR_WITH_SAVED_COPY
            isNetworkError -> RoutesLoadState.NETWORK_ERROR
            hasCachedData -> RoutesLoadState.OFFLINE_USING_SAVED_COPY
            else -> RoutesLoadState.OFFLINE_NO_DATA
        }
        reportLoadState(
            loadState = loadState,
            source = if (hasCachedData) ScheduleDataSource.SAVED_COPY else ScheduleDataSource.NONE,
            reportStatus = reportStatus
        )
        return loadState
    }

    private fun reportInvalidJson(
        hasCachedData: Boolean,
        reportStatus: Boolean
    ): RoutesLoadState {
        reportLoadState(
            loadState = RoutesLoadState.EMPTY_OR_INVALID_JSON,
            source = if (hasCachedData) ScheduleDataSource.SAVED_COPY else ScheduleDataSource.NONE,
            reportStatus = reportStatus
        )
        return RoutesLoadState.EMPTY_OR_INVALID_JSON
    }

    private fun reportNoChanges(reportStatus: Boolean): RoutesLoadState {
        reportLoadState(
            loadState = RoutesLoadState.NO_CHANGES,
            source = ScheduleDataSource.REMOTE_CACHE,
            reportStatus = reportStatus,
            lastUpdatedAtMillis = System.currentTimeMillis()
        )
        return RoutesLoadState.NO_CHANGES
    }

    private suspend fun persistRemoteRoutes(
        remoteJson: String,
        parsedRoutes: List<BusRoute>,
        notifyUser: Boolean
    ): RoutesLoadState {
        val updatedAtMillis = System.currentTimeMillis()
        memoryCache.updateLatestJson(remoteJson)
        updateRoutesState(parsedRoutes)
        cacheSchedulesFromJson(remoteJson)

        try {
            scheduleSnapshotRepository.writeSnapshot(remoteJson)
            UserActionLogger.scheduleCacheSaved(parsedRoutes.size)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Timber.w(exception, "Не удалось сохранить кэш расписания")
        }

        publishDataSourceStatus(
            source = ScheduleDataSource.REMOTE_CACHE,
            lastUpdatedAtMillis = updatedAtMillis,
            loadState = RoutesLoadState.SUCCESS,
            markScheduleUpdated = true
        )
        if (notifyUser) {
            UserActionLogger.scheduleUpdateResult(R.string.log_state_success)
            _scheduleUpdateNotices.tryEmit(buildScheduleUpdateNotice(updatedAtMillis))
        }
        return RoutesLoadState.SUCCESS
    }

    private fun reportLoadState(
        loadState: RoutesLoadState,
        source: ScheduleDataSource,
        reportStatus: Boolean,
        lastUpdatedAtMillis: Long? = null
    ) {
        UserActionLogger.scheduleUpdateResult(loadState.logMessageResId())
        if (reportStatus) {
            publishDataSourceStatus(source, lastUpdatedAtMillis, loadState)
        }
    }

    private fun updateRoutesState(routes: List<BusRoute>) {
        memoryCache.updateRoutes(routes)
        _routes.value = routes
    }

    private fun cacheSchedulesFromJson(json: String) {
        memoryCache.updateSchedules(scheduleJsonParser.buildSchedulesIndex(json))
    }

    private fun publishDataSourceStatus(
        source: ScheduleDataSource = ScheduleDataSource.NONE,
        lastUpdatedAtMillis: Long? = null,
        loadState: RoutesLoadState = RoutesLoadState.IDLE,
        markScheduleUpdated: Boolean = false
    ) {
        val previous = _dataSourceStatus.value
        _dataSourceStatus.value = ScheduleDataSourceStatus(
            source = source,
            lastUpdatedAtMillis = lastUpdatedAtMillis ?: previous.lastUpdatedAtMillis,
            isOnline = isOnline(),
            loadState = loadState,
            updateEventId = if (markScheduleUpdated) previous.updateEventId + 1 else previous.updateEventId
        )
    }

    private suspend fun ensureRoutesLoadedFromPersistentCache(): Boolean {
        if (memoryCache.hasCachedData) return true
        return loadRoutesFromPersistentCache()
    }

    private suspend fun loadRoutesFromPersistentCache(): Boolean {
        val snapshot = scheduleSnapshotRepository.readSnapshot() ?: return false
        val routes = scheduleJsonParser.parseRoutes(snapshot.json)
        if (routes.isEmpty()) {
            Timber.w("Сохранённый кэш расписания пуст или некорректен")
            return false
        }

        memoryCache.updateLatestJson(snapshot.json)
        memoryCache.updateSchedules(scheduleJsonParser.buildSchedulesIndex(snapshot.json))
        updateRoutesState(routes)
        UserActionLogger.scheduleCacheLoaded(
            scheduleSnapshotRepository.formatSavedAt(snapshot.savedAtMillis)
        )
        publishDataSourceStatus(
            source = ScheduleDataSource.SAVED_COPY,
            lastUpdatedAtMillis = snapshot.savedAtMillis,
            loadState = RoutesLoadState.SAVED_COPY_LOADED
        )
        return true
    }

    private companion object {
        fun defaultHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(Constants.REMOTE_CONNECTION_TIMEOUT.toLong(), TimeUnit.MILLISECONDS)
            .readTimeout(Constants.REMOTE_READ_TIMEOUT.toLong(), TimeUnit.MILLISECONDS)
            .build()
    }
}

private fun RoutesLoadState.logMessageResId(): Int = when (this) {
    RoutesLoadState.OFFLINE_USING_SAVED_COPY -> R.string.log_state_offline_saved_copy
    RoutesLoadState.OFFLINE_NO_DATA -> R.string.log_state_offline_no_data
    RoutesLoadState.NETWORK_ERROR_WITH_SAVED_COPY -> R.string.log_state_network_error_saved_copy
    RoutesLoadState.NETWORK_ERROR -> R.string.log_state_network_error
    RoutesLoadState.EMPTY_OR_INVALID_JSON -> R.string.log_state_invalid_json
    RoutesLoadState.NO_CHANGES -> R.string.log_state_no_changes
    else -> R.string.log_state_idle
}

enum class RoutesLoadState {
    IDLE,
    SUCCESS,
    NO_CHANGES,
    SAVED_COPY_LOADED,
    OFFLINE_USING_SAVED_COPY,
    OFFLINE_NO_DATA,
    NETWORK_ERROR,
    NETWORK_ERROR_WITH_SAVED_COPY,
    EMPTY_OR_INVALID_JSON
}

enum class ScheduleDataSource {
    NONE,
    SAVED_COPY,
    BUNDLED,
    REMOTE_CACHE
}

data class ScheduleDataSourceStatus(
    val source: ScheduleDataSource = ScheduleDataSource.NONE,
    val lastUpdatedAtMillis: Long? = null,
    val isOnline: Boolean = true,
    val loadState: RoutesLoadState = RoutesLoadState.IDLE,
    val updateEventId: Long = 0L
) {
    val isUsingSavedRemoteCopy: Boolean
        get() = source == ScheduleDataSource.SAVED_COPY
}
