package ru.slavgorod.transport.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ru.slavgorod.transport.data.local.ScheduleCacheStore
import ru.slavgorod.transport.data.repository.RemoteScheduleFetcher
import ru.slavgorod.transport.data.repository.RoutesLoadState
import ru.slavgorod.transport.data.repository.RoutesTableDataSource
import ru.slavgorod.transport.data.repository.ScheduleDataSource
import ru.slavgorod.transport.data.repository.ScheduleJsonParser
import ru.slavgorod.transport.data.repository.ScheduleFetcher
import ru.slavgorod.transport.domain.ResetAppDataUseCase
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RoutesTableDataSourceTest {

    private lateinit var context: Context
    private lateinit var server: MockWebServer
    private lateinit var cacheStore: ScheduleCacheStore

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        server = MockWebServer()
        server.start()
        cacheStore = ScheduleCacheStore(context)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `refreshRoutesFromLocal loads routes and schedules from strict contract`() = runTest {
        cacheStore.clear()
        enqueueRoutesResponse()
        val repository = createRepository()

        val loadState = repository.refreshRoutesFromLocal()

        assertEquals(RoutesLoadState.SUCCESS, loadState)
        assertEquals(RoutesLoadState.SUCCESS, repository.dataSourceStatus.value.loadState)
        assertEquals(ScheduleDataSource.REMOTE_CACHE, repository.dataSourceStatus.value.source)
        assertTrue(repository.getAllRoutes().isNotEmpty())

        val route = repository.getRouteById("102")
        assertNotNull(route)
        assertEquals("102", route.id)

        val schedules = repository.getSchedulesForRoute("102")
        assertEquals(2, schedules.size)
        assertEquals("102_1", schedules.first().id)
        assertEquals("06:25", schedules.first().departureTime)
    }

    @Test
    fun `getSchedulesForRoute reuses schedule index after refresh`() = runTest {
        cacheStore.clear()
        enqueueRoutesResponse()
        val parser = CountingScheduleJsonParser()
        val repository = createRepository(scheduleJsonParser = parser)

        repository.refreshRoutesFromLocal()
        val firstSchedules = repository.getSchedulesForRoute("102")
        val secondSchedules = repository.getSchedulesForRoute("102")

        assertEquals(2, firstSchedules.size)
        assertEquals(firstSchedules, secondSchedules)
        assertEquals(1, parser.scheduleIndexBuildCount)
    }

    @Test
    fun `refreshRoutesFromLocal restores cached routes after restart without network`() = runTest {
        cacheStore.clear()
        enqueueRoutesResponse()

        val firstRepository = createRepository()
        assertEquals(RoutesLoadState.SUCCESS, firstRepository.refreshRoutesFromLocal())

        val offlineRepository = createRepository(onlineChecker = { false })
        val loadState = offlineRepository.refreshRoutesFromLocal()

        assertEquals(RoutesLoadState.OFFLINE_USING_SAVED_COPY, loadState)
        assertEquals(
            RoutesLoadState.OFFLINE_USING_SAVED_COPY,
            offlineRepository.dataSourceStatus.value.loadState
        )
        assertEquals(ScheduleDataSource.SAVED_COPY, offlineRepository.dataSourceStatus.value.source)
        assertTrue(offlineRepository.getAllRoutes().isNotEmpty())
        assertEquals(2, offlineRepository.getSchedulesForRoute("102").size)
    }

    @Test
    fun `refreshRoutesFromLocal reports offline state when there is no saved copy`() = runTest {
        cacheStore.clear()
        val repository = createRepository(onlineChecker = { false })

        val loadState = repository.refreshRoutesFromLocal()

        assertEquals(RoutesLoadState.OFFLINE_NO_DATA, loadState)
        assertEquals(RoutesLoadState.OFFLINE_NO_DATA, repository.dataSourceStatus.value.loadState)
        assertEquals(ScheduleDataSource.NONE, repository.dataSourceStatus.value.source)
        assertTrue(repository.getAllRoutes().isEmpty())
    }

    @Test
    fun `refreshRoutesFromLocal reports network error when remote request fails without saved copy`() =
        runTest {
            cacheStore.clear()
            enqueueServerErrors()
            val repository = createRepository()

            val loadState = repository.refreshRoutesFromLocal()

            assertEquals(RoutesLoadState.NETWORK_ERROR, loadState)
            assertEquals(RoutesLoadState.NETWORK_ERROR, repository.dataSourceStatus.value.loadState)
            assertEquals(ScheduleDataSource.NONE, repository.dataSourceStatus.value.source)
            assertTrue(repository.getAllRoutes().isEmpty())
        }

    @Test
    fun `refreshRoutesFromLocal reports network error with saved copy when remote request fails`() =
        runTest {
            cacheStore.clear()
            cacheStore.writeSnapshot(buildRoutesJson())
            enqueueServerErrors()
            val repository = createRepository()

            val loadState = repository.refreshRoutesFromLocal()

            assertEquals(RoutesLoadState.NETWORK_ERROR_WITH_SAVED_COPY, loadState)
            assertEquals(
                RoutesLoadState.NETWORK_ERROR_WITH_SAVED_COPY,
                repository.dataSourceStatus.value.loadState
            )
            assertEquals(ScheduleDataSource.SAVED_COPY, repository.dataSourceStatus.value.source)
            assertTrue(repository.getAllRoutes().isNotEmpty())
        }

    @Test
    fun `searchRoutes filters loaded routes by number and name`() = runTest {
        cacheStore.clear()
        enqueueRoutesResponse()
        val repository = createRepository()
        repository.refreshRoutesFromLocal()

        val byNumber = repository.searchRoutes("102")
        assertTrue(byNumber.isNotEmpty())
        assertTrue(byNumber.all { it.routeNumber.contains("102") })
    }

    @Test
    fun `refreshRoutesFromLocal reports invalid json when routes array is empty`() = runTest {
        cacheStore.clear()
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"routes":[]}"""))
        val repository = createRepository()

        val loadState = repository.refreshRoutesFromLocal()

        assertEquals(RoutesLoadState.EMPTY_OR_INVALID_JSON, loadState)
        assertTrue(repository.getAllRoutes().isEmpty())
    }

    @Test
    fun `refreshRoutesFromLocal keeps cached copy when remote json is invalid`() = runTest {
        cacheStore.clear()
        enqueueRoutesResponse()
        val firstRepository = createRepository()
        assertEquals(RoutesLoadState.SUCCESS, firstRepository.refreshRoutesFromLocal())

        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"routes":[]}"""))
        val secondRepository = createRepository()
        val loadState = secondRepository.refreshRoutesFromLocal()

        assertEquals(RoutesLoadState.EMPTY_OR_INVALID_JSON, loadState)
        assertTrue(secondRepository.getAllRoutes().isNotEmpty())
        assertNotNull(secondRepository.getRouteById("102"))
    }

    @Test
    fun `google script error response preserves saved timetable`() = runTest {
        cacheStore.clear()
        val originalJson = buildRoutesJson()
        cacheStore.writeSnapshot(originalJson)
        server.enqueue(MockResponse().setResponseCode(200).setBody(
            """{"error":"Таблица расписания содержит ошибки","details":"Рейсы, строка 2"}"""
        ))
        val repository = createRepository()

        assertEquals(RoutesLoadState.EMPTY_OR_INVALID_JSON, repository.refreshRoutesFromLocal())
        assertNotNull(repository.getRouteById("102"))
        assertEquals(originalJson.trim(), cacheStore.readSnapshot()?.json)
    }

    @Test
    fun `backup is used only when primary fails and no cached timetable exists`() = runTest {
        cacheStore.clear()
        var backupCalls = 0
        val backupJson = buildRoutesJson().replaceFirst("{", "{\"backupCreatedAt\":\"2026-11-01T12:00:00Z\",")
        val fetcher = object : ScheduleFetcher {
            override fun isOnline() = true
            override suspend fun fetchWithRetry(): String? = null
            override suspend fun fetchBackupWithRetry(): String {
                backupCalls++
                return backupJson
            }
        }
        val repository = RoutesTableDataSource(context = context, remoteScheduleFetcher = fetcher, autoLoadOnInit = false)
        assertEquals(RoutesLoadState.BACKUP_LOADED, repository.refreshRoutesFromLocal())
        assertEquals(ScheduleDataSource.GITHUB_BACKUP, repository.dataSourceStatus.value.source)
        assertEquals(java.time.Instant.parse("2026-11-01T12:00:00Z").toEpochMilli(), repository.dataSourceStatus.value.lastUpdatedAtMillis)
        assertNotNull(repository.getRouteById("102"))
        assertEquals(1, backupCalls)

        assertEquals(RoutesLoadState.NETWORK_ERROR_WITH_SAVED_COPY, repository.refreshRoutesFromLocal())
        assertEquals(1, backupCalls)
        assertEquals(ScheduleDataSource.GITHUB_BACKUP, repository.dataSourceStatus.value.source)

        val reopened = RoutesTableDataSource(context = context, remoteScheduleFetcher = fetcher, autoLoadOnInit = false)
        assertEquals(RoutesLoadState.NETWORK_ERROR_WITH_SAVED_COPY, reopened.refreshRoutesFromLocal())
        assertEquals(ScheduleDataSource.GITHUB_BACKUP, reopened.dataSourceStatus.value.source)
        assertEquals(java.time.Instant.parse("2026-11-01T12:00:00Z").toEpochMilli(), reopened.dataSourceStatus.value.lastUpdatedAtMillis)
        assertEquals(1, backupCalls)
    }

    @Test
    fun `primary always wins and cached primary is never replaced by backup`() = runTest {
        cacheStore.clear()
        var primary: String? = buildRoutesJson()
        var backupCalls = 0
        val fetcher = object : ScheduleFetcher {
            override fun isOnline() = true
            override suspend fun fetchWithRetry() = primary
            override suspend fun fetchBackupWithRetry(): String? { backupCalls++; return null }
        }
        val repository = RoutesTableDataSource(context = context, remoteScheduleFetcher = fetcher, autoLoadOnInit = false)
        assertEquals(RoutesLoadState.SUCCESS, repository.refreshRoutesFromLocal())
        primary = null
        assertEquals(RoutesLoadState.NETWORK_ERROR_WITH_SAVED_COPY, repository.refreshRoutesFromLocal())
        assertEquals(0, backupCalls)
        assertEquals(buildRoutesJson(), cacheStore.readSnapshot()?.json)
    }

    @Test
    fun `invalid primary tries backup without overwriting existing data`() = runTest {
        cacheStore.clear()
        var calls = 0
        val backup = buildRoutesJson().replaceFirst("{", "{\"backupCreatedAt\":\"2026-11-01T12:00:00Z\",")
        val fetcher = object : ScheduleFetcher {
            override fun isOnline() = true
            override suspend fun fetchWithRetry() = """{"error":"bad table"}"""
            override suspend fun fetchBackupWithRetry(): String { calls++; return backup }
        }
        val repository = RoutesTableDataSource(context = context, remoteScheduleFetcher = fetcher, autoLoadOnInit = false)
        assertEquals(RoutesLoadState.BACKUP_LOADED, repository.refreshRoutesFromLocal())
        assertEquals(1, calls)
    }

    @Test
    fun `primary replaces previously loaded backup when it recovers`() = runTest {
        cacheStore.clear()
        val backup = buildRoutesJson().replaceFirst("{", "{\"backupCreatedAt\":\"2026-11-01T12:00:00Z\",")
        var primary: String? = null
        val fetcher = object : ScheduleFetcher {
            override fun isOnline() = true
            override suspend fun fetchWithRetry() = primary
            override suspend fun fetchBackupWithRetry() = backup
        }
        val repository = RoutesTableDataSource(context = context, remoteScheduleFetcher = fetcher, autoLoadOnInit = false)
        assertEquals(RoutesLoadState.BACKUP_LOADED, repository.refreshRoutesFromLocal())
        primary = buildRoutesJson()
        assertEquals(RoutesLoadState.SUCCESS, repository.refreshRoutesFromLocal())
        assertEquals(ScheduleDataSource.REMOTE_CACHE, repository.dataSourceStatus.value.source)
        assertEquals(primary, cacheStore.readSnapshot()?.json)
    }

    @Test
    fun `checkRoutesForUpdates reports no changes when remote json is identical`() = runTest {
        cacheStore.clear()
        enqueueRoutesResponse()
        val repository = createRepository()
        assertEquals(RoutesLoadState.SUCCESS, repository.refreshRoutesFromLocal())

        enqueueRoutesResponse()
        val loadState = repository.checkRoutesForUpdates()

        assertEquals(RoutesLoadState.NO_CHANGES, loadState)
        assertTrue(repository.getAllRoutes().isNotEmpty())
    }

    @Test
    fun `refreshRoutesFromLocal uses the latest remote url without recreating repository`() =
        runTest {
            cacheStore.clear()
            val currentRemoteUrl = AtomicReference(server.url("/routes-a.json").toString())
            server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    return when (request.path) {
                        "/routes-a.json",
                        "/routes-b.json" -> MockResponse()
                            .setResponseCode(200)
                            .setBody(buildRoutesJson())

                        else -> MockResponse().setResponseCode(404)
                    }
                }
            }

            val repository = RoutesTableDataSource(
                context = context,
                onlineChecker = { true },
                remoteScheduleFetcher = RemoteScheduleFetcher(
                    remoteJsonUrlProvider = { currentRemoteUrl.get() },
                    onlineChecker = { true },
                    delayProvider = {}
                ),
                autoLoadOnInit = false
            )

            assertEquals(RoutesLoadState.SUCCESS, repository.refreshRoutesFromLocal())
            assertEquals("/routes-a.json", server.takeRequest().path)

            currentRemoteUrl.set(server.url("/routes-b.json").toString())
            assertEquals(RoutesLoadState.NO_CHANGES, repository.refreshRoutesFromLocal())
            assertEquals("/routes-b.json", server.takeRequest().path)
        }

    @Test
    fun `resetApplicationData clears cached schedule copy`() = runTest {
        cacheStore.clear()
        cacheStore.writeSnapshot(buildRoutesJson())

        val resetUseCase = ResetAppDataUseCase(context)
        resetUseCase.resetApplicationData()

        assertTrue(!cacheStore.hasSnapshot())
    }

    private fun createRepository(
        onlineChecker: () -> Boolean = { true },
        scheduleJsonParser: ScheduleJsonParser = ScheduleJsonParser()
    ): RoutesTableDataSource {
        val remoteJsonUrl = server.url("/routes.json").toString()
        return RoutesTableDataSource(
            context = context,
            onlineChecker = onlineChecker,
            remoteScheduleFetcher = RemoteScheduleFetcher(
                remoteJsonUrlProvider = { remoteJsonUrl },
                onlineChecker = onlineChecker,
                delayProvider = {}
            ),
            scheduleJsonParser = scheduleJsonParser,
            autoLoadOnInit = false
        )
    }

    private fun enqueueRoutesResponse() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(buildRoutesJson())
        )
    }

    private fun enqueueServerErrors() {
        repeat(3) {
            server.enqueue(MockResponse().setResponseCode(500))
        }
    }

    private fun buildRoutesJson(): String {
        return """
            {
              "routes": [
                {
                  "id": "102",
                  "routeNumber": "102",
                  "name": "Slavgorod - Yarovoye",
                  "description": "Market (Slavgorod) - MCS-128 (Yarovoye)",
                  "color": "#1976D2",
                  "travelTime": "~40 minutes",
                  "pricePrimary": "40 city",
                  "priceSecondary": "60 intercity",
                  "paymentMethods": "Cash / Card",
                  "schedules": [
                    {
                      "id": "102_1",
                      "departurePoint": "Market (Slavgorod)",
                      "departureTime": "06:25",
                      "notes": "First trip"
                    },
                    {
                      "id": "102_2",
                      "departurePoint": "Market (Slavgorod)",
                      "departureTime": "08:25",
                      "notes": ""
                    }
                  ]
                },
                {
                  "id": "105",
                  "routeNumber": "105",
                  "name": "Slavgorod",
                  "description": "Rail depot",
                  "color": "#1976D2",
                  "travelTime": "~15 minutes",
                  "pricePrimary": "20 city",
                  "priceSecondary": "30 intercity",
                  "paymentMethods": "Cash"
                }
              ]
            }
        """.trimIndent()
    }

    private class CountingScheduleJsonParser : ScheduleJsonParser() {
        var scheduleIndexBuildCount = 0
            private set

        override fun buildSchedulesIndex(jsonString: String): Map<String, List<ru.slavgorod.transport.data.model.BusSchedule>> {
            scheduleIndexBuildCount += 1
            return super.buildSchedulesIndex(jsonString)
        }
    }
}
