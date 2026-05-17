package ru.slavgorod.transport.data.repository

import android.content.Context
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
import ru.slavgorod.transport.core.search
import ru.slavgorod.transport.data.local.JsonDataSource
import ru.slavgorod.transport.data.local.ScheduleCacheStore
import ru.slavgorod.transport.data.model.BusRoute
import ru.slavgorod.transport.data.model.BusSchedule
import timber.log.Timber

class RoutesTableDataSource(
    context: Context,
    jsonDataSource: JsonDataSource = JsonDataSource(),
    onlineChecker: () -> Boolean = RoutesTableDataSourceDefaults.onlineChecker(context),
    scheduleCacheStore: ScheduleCacheStore = RoutesTableDataSourceDefaults.scheduleCacheStore(
        context
    ),
    httpClient: OkHttpClient = RoutesTableDataSourceDefaults.httpClient(),
    private val remoteScheduleFetcher: ScheduleFetcher = RoutesTableDataSourceDefaults.remoteScheduleFetcher(
        onlineChecker = onlineChecker,
        httpClient = httpClient
    ),
    private val scheduleSnapshotRepository: ScheduleSnapshotStore = RoutesTableDataSourceDefaults.scheduleSnapshotRepository(
        scheduleCacheStore
    ),
    private val scheduleJsonParser: ScheduleJsonParser = RoutesTableDataSourceDefaults.scheduleJsonParser(
        jsonDataSource
    ),
    externalScope: CoroutineScope? = null,
    autoLoadOnInit: Boolean = true
) {

    private val _routes = MutableStateFlow<List<BusRoute>>(emptyList())
    val routes: StateFlow<List<BusRoute>> = _routes.asStateFlow()

    private val _dataSourceStatus = MutableStateFlow(ScheduleDataSourceStatus())
    val dataSourceStatus: StateFlow<ScheduleDataSourceStatus> = _dataSourceStatus.asStateFlow()

    private val _scheduleUpdateNotices =
        MutableSharedFlow<ScheduleUpdateNotice>(extraBufferCapacity = 1)
    val scheduleUpdateNotices: SharedFlow<ScheduleUpdateNotice> =
        _scheduleUpdateNotices.asSharedFlow()

    private val memoryCache = RoutesMemoryCache()
    private val persistentSnapshotLoader = PersistentRoutesSnapshotLoader(
        scheduleSnapshotRepository = scheduleSnapshotRepository,
        scheduleJsonParser = scheduleJsonParser
    )
    private val remoteStateWriter = RoutesRemoteStateWriterAdapter(
        updateRoutesState = ::updateRoutesState,
        cacheSchedulesFromJson = ::cacheSchedulesFromJson,
        setLatestJson = ::setLatestJson,
        publishDataSourceStatus = ::publishDataSourceStatus,
        emitScheduleUpdateNotice = ::emitScheduleUpdateNotice
    )

    private val remoteLoadCoordinator = RoutesRemoteLoadCoordinator(
        remoteScheduleFetcher = remoteScheduleFetcher,
        scheduleJsonParser = scheduleJsonParser,
        isOnline = ::isOnline
    )
    private val remoteLoadEffectHandler = RoutesRemoteLoadEffectHandler(
        scheduleSnapshotRepository = scheduleSnapshotRepository,
        stateWriter = remoteStateWriter
    )

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

    fun getRouteById(routeId: String?): BusRoute? {
        if (routeId.isNullOrBlank()) return null
        return memoryCache.routeById(routeId)
    }

    fun searchRoutes(query: String): List<BusRoute> {
        if (query.isBlank()) return getAllRoutes()
        return _routes.value.search(query)
    }

    fun getAllRoutes(): List<BusRoute> = _routes.value

    suspend fun getSchedulesForRoute(routeId: String): List<BusSchedule> {
        require(routeId.isNotBlank()) { "Route ID cannot be blank" }

        ensureRoutesLoadedFromPersistentCache()

        memoryCache.schedulesForRoute(routeId)?.let { return it }

        val jsonSnapshot = memoryCache.latestJson ?: remoteScheduleFetcher.fetchWithRetry()
        if (jsonSnapshot.isNullOrBlank()) {
            Timber.e("JSON source is unavailable for route %s", routeId)
            return emptyList()
        }

        if (!memoryCache.hasSchedules) {
            cacheSchedulesFromJson(jsonSnapshot)
        }

        if (!memoryCache.hasRoutes) {
            val parsedRoutes = scheduleJsonParser.parseRoutes(jsonSnapshot)
            if (parsedRoutes.isNotEmpty()) {
                updateRoutesState(parsedRoutes)
            }
        }

        return memoryCache.schedulesForRoute(routeId).orEmpty()
    }

    suspend fun refreshRoutesFromLocal(notifyUser: Boolean = true): RoutesLoadState {
        return loadRoutesFromRemote(reportStatus = true, notifyUser = notifyUser)
    }

    suspend fun checkRoutesForUpdates(notifyUser: Boolean = true): RoutesLoadState {
        return loadRoutesFromRemote(reportStatus = false, notifyUser = notifyUser)
    }

    private suspend fun loadRoutesFromRemote(
        reportStatus: Boolean,
        notifyUser: Boolean
    ): RoutesLoadState {
        return withContext(Dispatchers.IO) {
            loadMutex.withLock {
                ensureRoutesLoadedFromPersistentCache()
                remoteLoadEffectHandler.apply(
                    action = shapeRemoteLoadResult(
                        result = remoteLoadCoordinator.decide(
                            latestJson = memoryCache.latestJson,
                            hasCachedData = hasCachedData()
                        ),
                        reportStatus = reportStatus,
                        notifyUser = notifyUser
                    ),
                    reportStatus = reportStatus,
                    notifyUser = notifyUser
                )
            }
        }
    }

    private fun updateRoutesState(routes: List<BusRoute>) {
        memoryCache.updateRoutes(routes)
        _routes.value = routes
    }

    private fun setLatestJson(json: String) {
        memoryCache.updateLatestJson(json)
    }

    private fun emitScheduleUpdateNotice(notice: ScheduleUpdateNotice) {
        _scheduleUpdateNotices.tryEmit(notice)
    }

    private fun cacheSchedulesFromJson(jsonString: String) {
        memoryCache.updateSchedules(scheduleJsonParser.buildSchedulesIndex(jsonString))
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

    private fun hasCachedData(): Boolean = memoryCache.hasCachedData

    private suspend fun ensureRoutesLoadedFromPersistentCache(): Boolean {
        if (hasCachedData()) return true

        return loadRoutesFromPersistentCache()
    }

    private suspend fun loadRoutesFromPersistentCache(): Boolean {
        val snapshot = persistentSnapshotLoader.load() ?: return false
        memoryCache.updateLatestJson(snapshot.json)
        memoryCache.updateSchedules(snapshot.schedulesByRoute)
        updateRoutesState(snapshot.routes)
        publishDataSourceStatus(
            source = ScheduleDataSource.SAVED_COPY,
            lastUpdatedAtMillis = snapshot.savedAtMillis,
            loadState = RoutesLoadState.SAVED_COPY_LOADED
        )
        return true
    }

    private companion object
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
