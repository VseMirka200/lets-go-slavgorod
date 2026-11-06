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
import ru.slavgorod.transport.core.search
import ru.slavgorod.transport.data.local.JsonDataSource
import ru.slavgorod.transport.data.local.ScheduleCacheStore
import ru.slavgorod.transport.data.model.BusRoute
import ru.slavgorod.transport.data.model.BusSchedule
import ru.slavgorod.transport.data.model.ScheduleChange
import ru.slavgorod.transport.data.network.NetworkMonitor
import ru.slavgorod.transport.logging.UserActionLogger
import timber.log.Timber

/**
 * Единая точка загрузки расписания из сети, сохранённой копии и памяти.
 *
 * Обновления из сети выполняются под блокировкой; отмена корутины пробрасывается
 * дальше и не преобразуется в сетевую ошибку.
 */
class RoutesTableDataSource(
    context: Context,
    jsonDataSource: JsonDataSource = JsonDataSource(),
    onlineChecker: () -> Boolean = { NetworkMonitor.isConnected(context.applicationContext) },
    scheduleCacheStore: ScheduleCacheStore = ScheduleCacheStore(context.applicationContext),
    httpClient: OkHttpClient = defaultScheduleHttpClient(),
    private val remoteScheduleFetcher: ScheduleFetcher = RemoteScheduleFetcher(
        onlineChecker = onlineChecker,
        httpClient = httpClient
    ),
    private val scheduleSnapshotRepository: ScheduleSnapshotStore = ScheduleSnapshotRepository(
        scheduleCacheStore
    ),
    private val scheduleJsonParser: ScheduleJsonParser = ScheduleJsonParser(jsonDataSource),
    externalScope: CoroutineScope? = null,
    autoLoadOnInit: Boolean = true,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis
) {

    private val _routes = MutableStateFlow<List<BusRoute>>(emptyList())
    val routes: StateFlow<List<BusRoute>> = _routes.asStateFlow()

    private val _dataSourceStatus = MutableStateFlow(ScheduleDataSourceStatus())
    val dataSourceStatus: StateFlow<ScheduleDataSourceStatus> = _dataSourceStatus.asStateFlow()

    private val _scheduleUpdateNotices = MutableSharedFlow<ScheduleUpdateNotice>(extraBufferCapacity = 1)
    val scheduleUpdateNotices: SharedFlow<ScheduleUpdateNotice> = _scheduleUpdateNotices.asSharedFlow()

    private val memoryCache = RoutesMemoryCache()
    private val repositoryScope = externalScope ?: CoroutineScope(SupervisorJob() + Dispatchers.IO)
    // Обновления не должны одновременно менять снимок.
    private val loadMutex = Mutex()
    private var snapshotWritePending = false

    init {
        if (autoLoadOnInit) {
            repositoryScope.launch {
                loadMutex.withLock { ensureRoutesLoadedFromPersistentCache() }
                refreshRoutesFromLocal(notifyUser = false)
            }
        }
    }

    fun isOnline(): Boolean = remoteScheduleFetcher.isOnline()

    /** Ищет маршрут в текущем снимке; пустой или неизвестный идентификатор даёт `null`. */
    fun getRouteById(routeId: String?): BusRoute? =
        routeId?.takeIf(String::isNotBlank)?.let(memoryCache::routeById)

    /** Ищет среди загруженных маршрутов; пустой запрос возвращает полный список. */
    fun searchRoutes(query: String): List<BusRoute> =
        if (query.isBlank()) getAllRoutes() else _routes.value.search(query)

    /** Возвращает текущий список без ожидания первоначальной сетевой загрузки. */
    fun getAllRoutes(): List<BusRoute> = _routes.value

    /**
     * Возвращает рейсы маршрута из проверенного снимка, восстанавливая кэш при необходимости.
     * Если подходящие данные недоступны или некорректны, результат будет пустым.
     * Загрузка и разбор выполняются вне главного потока под общей блокировкой обновлений.
     *
     * @param routeId Непустой устойчивый идентификатор маршрута.
     * @throws IllegalArgumentException если идентификатор пустой.
     */
    suspend fun getSchedulesForRoute(routeId: String): List<BusSchedule> = withContext(Dispatchers.IO) {
        loadMutex.withLock {
            require(routeId.isNotBlank()) { "Идентификатор маршрута не может быть пустым" }
            ensureRoutesLoadedFromPersistentCache()
            memoryCache.schedulesForRoute(routeId)?.let { return@withLock it }

            val jsonSnapshot = memoryCache.latestJson ?: remoteScheduleFetcher.fetchWithRetry()
            if (jsonSnapshot.isNullOrBlank()) {
                Timber.e("Источник JSON недоступен для маршрута %s", routeId)
                return@withLock emptyList()
            }

            val snapshot = scheduleJsonParser.parseSnapshot(jsonSnapshot) ?: return@withLock emptyList()
            memoryCache.updateLatestJson(jsonSnapshot)
            snapshotWritePending = true
            memoryCache.updateSchedules(snapshot.schedules)
            updateRoutesState(snapshot.routes)
            memoryCache.schedulesForRoute(routeId).orEmpty()
        }
    }

    /** Возвращает корректные объявления из текущей сохранённой копии расписания. */
    suspend fun getScheduleChanges(): List<ScheduleChange> = withContext(Dispatchers.IO) {
        loadMutex.withLock {
            ensureRoutesLoadedFromPersistentCache()
            memoryCache.latestJson?.let(scheduleJsonParser::parseScheduleChanges).orEmpty()
        }
    }

    /** Проверяет удалённый источник и публикует итоговый статус для интерфейса. */
    suspend fun refreshRoutesFromLocal(notifyUser: Boolean = true): RoutesLoadState =
        loadRoutesFromRemote(reportStatus = true, notifyUser = notifyUser)

    /** Проверяет обновления без замены основного статуса загрузки интерфейса. */
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

            if (remoteJson == memoryCache.latestJson) {
                if (snapshotWritePending) saveSnapshot(remoteJson, _routes.value.size)
                return@withLock reportNoChanges(reportStatus)
            }

            val snapshot = scheduleJsonParser.parseSnapshot(remoteJson)
            if (snapshot == null) {
                Timber.w("JSON удалённого расписания пуст или некорректен; обновление пропущено")
                return@withLock reportInvalidJson(memoryCache.hasCachedData, reportStatus)
            }

            persistRemoteRoutes(remoteJson, snapshot.routes, snapshot.schedules, notifyUser)
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
            source = hasCachedData.cachedDataSource(),
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
            source = hasCachedData.cachedDataSource(),
            reportStatus = reportStatus
        )
        return RoutesLoadState.EMPTY_OR_INVALID_JSON
    }

    private fun reportNoChanges(reportStatus: Boolean): RoutesLoadState {
        val checkedAt = currentTimeMillis()
        reportLoadState(
            loadState = RoutesLoadState.NO_CHANGES,
            source = ScheduleDataSource.REMOTE_CACHE,
            reportStatus = reportStatus,
            lastCheckedAtMillis = checkedAt
        )
        if (!reportStatus) _dataSourceStatus.value = _dataSourceStatus.value.copy(lastCheckedAtMillis = checkedAt)
        return RoutesLoadState.NO_CHANGES
    }

    private suspend fun persistRemoteRoutes(
        remoteJson: String,
        parsedRoutes: List<BusRoute>,
        schedules: Map<String, List<BusSchedule>>,
        notifyUser: Boolean
    ): RoutesLoadState {
        val updatedAtMillis = currentTimeMillis()
        snapshotWritePending = true
        memoryCache.updateLatestJson(remoteJson)
        updateRoutesState(parsedRoutes)
        memoryCache.updateSchedules(schedules)

        saveSnapshot(remoteJson, parsedRoutes.size)

        publishDataSourceStatus(
            source = ScheduleDataSource.REMOTE_CACHE,
            lastUpdatedAtMillis = updatedAtMillis,
            lastCheckedAtMillis = updatedAtMillis,
            loadState = RoutesLoadState.SUCCESS,
            markScheduleUpdated = true
        )
        if (notifyUser) {
            UserActionLogger.scheduleUpdateResult(R.string.log_state_success)
            _scheduleUpdateNotices.tryEmit(buildScheduleUpdateNotice(updatedAtMillis))
        }
        return RoutesLoadState.SUCCESS
    }

    private suspend fun saveSnapshot(json: String, routeCount: Int) {
        try {
            scheduleSnapshotRepository.writeSnapshot(json)
            snapshotWritePending = false
            UserActionLogger.scheduleCacheSaved(routeCount)
        } catch (exception: CancellationException) {
            snapshotWritePending = true
            throw exception
        } catch (exception: Exception) {
            snapshotWritePending = true
            Timber.w(exception, "Не удалось сохранить кэш расписания")
        }
    }

    private fun reportLoadState(
        loadState: RoutesLoadState,
        source: ScheduleDataSource,
        reportStatus: Boolean,
        lastUpdatedAtMillis: Long? = null,
        lastCheckedAtMillis: Long? = null
    ) {
        UserActionLogger.scheduleUpdateResult(loadState.logMessageResId())
        if (reportStatus) {
            publishDataSourceStatus(source, lastUpdatedAtMillis, loadState, lastCheckedAtMillis = lastCheckedAtMillis)
        }
    }

    private fun updateRoutesState(routes: List<BusRoute>) {
        memoryCache.updateRoutes(routes)
        _routes.value = routes
    }

    private fun publishDataSourceStatus(
        source: ScheduleDataSource = ScheduleDataSource.NONE,
        lastUpdatedAtMillis: Long? = null,
        loadState: RoutesLoadState = RoutesLoadState.IDLE,
        markScheduleUpdated: Boolean = false,
        lastCheckedAtMillis: Long? = null
    ) {
        val previous = _dataSourceStatus.value
        _dataSourceStatus.value = ScheduleDataSourceStatus(
            source = source,
            lastUpdatedAtMillis = lastUpdatedAtMillis ?: previous.lastUpdatedAtMillis,
            lastCheckedAtMillis = lastCheckedAtMillis ?: previous.lastCheckedAtMillis,
            isOnline = isOnline(),
            loadState = loadState,
            // Изменение счётчика заставляет экран заново получить рейсы для маршрута.
            updateEventId = if (markScheduleUpdated) previous.updateEventId + 1 else previous.updateEventId
        )
    }

    private suspend fun ensureRoutesLoadedFromPersistentCache(): Boolean {
        if (memoryCache.hasCachedData) return true
        return loadRoutesFromPersistentCache()
    }

    private suspend fun loadRoutesFromPersistentCache(): Boolean {
        val snapshot = scheduleSnapshotRepository.readSnapshot() ?: return false
        val parsed = scheduleJsonParser.parseSnapshot(snapshot.json)
        if (parsed == null) {
            Timber.w("Сохранённый кэш расписания пуст или некорректен")
            return false
        }

        memoryCache.updateLatestJson(snapshot.json)
        memoryCache.updateSchedules(parsed.schedules)
        updateRoutesState(parsed.routes)
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

}

private fun Boolean.cachedDataSource(): ScheduleDataSource =
    if (this) ScheduleDataSource.SAVED_COPY else ScheduleDataSource.NONE

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

/**
 * Состояние источника для интерфейса.
 *
 * [lastUpdatedAtMillis] — время получения текущей копии; [lastCheckedAtMillis] —
 * время последней успешной проверки источника. Обе отметки используют миллисекунды
 * Unix и не подтверждают дату изменения данных перевозчиком.
 */
data class ScheduleDataSourceStatus(
    val source: ScheduleDataSource = ScheduleDataSource.NONE,
    val lastUpdatedAtMillis: Long? = null,
    val lastCheckedAtMillis: Long? = null,
    val isOnline: Boolean = true,
    val loadState: RoutesLoadState = RoutesLoadState.IDLE,
    val updateEventId: Long = 0L
) {
    val isUsingSavedRemoteCopy: Boolean
        get() = source == ScheduleDataSource.SAVED_COPY
}
