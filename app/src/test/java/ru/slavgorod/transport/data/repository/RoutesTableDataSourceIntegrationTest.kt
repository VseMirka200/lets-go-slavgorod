package ru.slavgorod.transport.data.repository

import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ru.slavgorod.transport.data.local.ScheduleCacheStore
import ru.slavgorod.transport.data.local.CachedScheduleSnapshot
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RoutesTableDataSourceIntegrationTest {

    @Test
    fun `delete clears saved and in-memory data without downloading`() = runTest {
        val json = """{"routes":[{"id":"1","routeNumber":"1","name":"Route","description":"Description","color":"#123456","schedules":[]}]}"""
        cacheStore.writeSnapshot(json)
        var fetches = 0
        val fetcher = object : ScheduleFetcher {
            override fun isOnline() = true
            override suspend fun fetchWithRetry(): String { fetches++; return json }
        }
        val repository = RoutesTableDataSource(context, remoteScheduleFetcher = fetcher,
            scheduleSnapshotRepository = ScheduleSnapshotRepository(cacheStore), autoLoadOnInit = false)

        repository.getSchedulesForRoute("1")
        assertNotNull(repository.getRouteById("1"))
        repository.deleteSchedule()

        assertEquals(0, fetches)
        assertNull(cacheStore.readSnapshot())
        assertNull(repository.getRouteById("1"))
        assertTrue(repository.routes.value.isEmpty())
        assertNull(repository.dataSourceStatus.value.lastUpdatedAtMillis)
    }

    @Test
    fun `delete and reload replaces cached and in-memory schedules even when response is unchanged`() = runTest {
        val json = """{"routes":[{"id":"1","routeNumber":"1","name":"Route","description":"Description","color":"#123456","schedules":[{"id":"a","departurePoint":"Stop","departureTime":"06:25"}]}]}"""
        cacheStore.writeSnapshot(json)
        var fetches = 0
        val fetcher = object : ScheduleFetcher {
            override fun isOnline() = true
            override suspend fun fetchWithRetry(): String {
                fetches++
                assertNull(cacheStore.readSnapshot())
                return json
            }
        }
        val repository = RoutesTableDataSource(context, remoteScheduleFetcher = fetcher,
            scheduleSnapshotRepository = ScheduleSnapshotRepository(cacheStore), autoLoadOnInit = false)

        assertEquals("06:25", repository.getSchedulesForRoute("1").single().departureTime)
        assertEquals(RoutesLoadState.SUCCESS, repository.deleteAndReloadSchedule())
        assertEquals(1, fetches)
        assertEquals(json, cacheStore.readSnapshot()?.json)
        assertEquals("06:25", repository.getSchedulesForRoute("1").single().departureTime)
    }

    @Test
    fun `delete and reload offline removes old data and reports absence of a copy`() = runTest {
        val json = """{"routes":[{"id":"1","routeNumber":"1","name":"Route","description":"Description","color":"#123456","schedules":[]}]}"""
        cacheStore.writeSnapshot(json)
        val fetcher = object : ScheduleFetcher {
            override fun isOnline() = false
            override suspend fun fetchWithRetry(): String? = null
        }
        val repository = RoutesTableDataSource(context, remoteScheduleFetcher = fetcher,
            scheduleSnapshotRepository = ScheduleSnapshotRepository(cacheStore), autoLoadOnInit = false)

        repository.refreshRoutesFromLocal()
        assertNotNull(repository.getRouteById("1"))
        assertEquals(RoutesLoadState.OFFLINE_NO_DATA, repository.deleteAndReloadSchedule())
        assertNull(cacheStore.readSnapshot())
        assertNull(repository.getRouteById("1"))
        assertTrue(repository.routes.value.isEmpty())
        assertNull(repository.dataSourceStatus.value.lastUpdatedAtMillis)
    }

    @Test
    fun `unchanged update records check time without changing snapshot time`() = runTest {
        val json = """{"routes":[{"id":"1","routeNumber":"1","name":"Route","description":"Description","color":"#123456","schedules":[]}]}"""
        val store = object : ScheduleSnapshotStore {
            override suspend fun readSnapshot() = CachedScheduleSnapshot(json, 100L)
            override suspend fun writeSnapshot(json: String) = Unit
            override suspend fun clear() = Unit
            override suspend fun hasSnapshot() = true
            override fun formatSavedAt(timestampMillis: Long) = "saved"
        }
        val fetcher = object : ScheduleFetcher {
            override fun isOnline() = true
            override suspend fun fetchWithRetry() = json
        }
        val repository = RoutesTableDataSource(
            context = context, remoteScheduleFetcher = fetcher, scheduleSnapshotRepository = store,
            autoLoadOnInit = false, currentTimeMillis = { 200L }
        )
        assertEquals(RoutesLoadState.NO_CHANGES, repository.refreshRoutesFromLocal())
        assertEquals(100L, repository.dataSourceStatus.value.lastUpdatedAtMillis)
        assertEquals(200L, repository.dataSourceStatus.value.lastCheckedAtMillis)
    }

    @Test
    fun `initial cache restore completes before concurrent refresh publishes new data`() = runTest {
        val old = """{"routes":[{"id":"1","routeNumber":"1","name":"Old","description":"Description","color":"#123456","schedules":[]}]}"""
        val fresh = old.replace("Old", "New")
        val readStarted = CompletableDeferred<Unit>()
        val allowRead = CompletableDeferred<Unit>()
        val store = object : ScheduleSnapshotStore {
            override suspend fun readSnapshot(): CachedScheduleSnapshot {
                readStarted.complete(Unit)
                allowRead.await()
                return CachedScheduleSnapshot(old, 1)
            }
            override suspend fun writeSnapshot(json: String) = Unit
            override suspend fun clear() = Unit
            override suspend fun hasSnapshot() = true
            override fun formatSavedAt(timestampMillis: Long) = "saved"
        }
        val fetcher = object : ScheduleFetcher {
            override fun isOnline() = true
            override suspend fun fetchWithRetry() = fresh
        }
        val repository = RoutesTableDataSource(
            context = context, remoteScheduleFetcher = fetcher,
            scheduleSnapshotRepository = store, externalScope = backgroundScope
        )
        readStarted.await()
        val refresh = async { repository.refreshRoutesFromLocal() }
        allowRead.complete(Unit)
        refresh.await()
        assertEquals("New", repository.getRouteById("1")?.name)
    }

    @Test
    fun `invalid update preserves last good snapshot`() = runTest {
        val good = """{"routes":[{"id":"1","routeNumber":"1","name":"Route","description":"Description","color":"#123456","schedules":[{"id":"a","departurePoint":"Stop","departureTime":"06:25"}]}]}"""
        val bad = good.replace("06:25", "25:25")
        cacheStore.writeSnapshot(good)
        val fetcher = object : ScheduleFetcher {
            override fun isOnline() = true
            override suspend fun fetchWithRetry() = bad
        }
        val repository = RoutesTableDataSource(
            context = context,
            remoteScheduleFetcher = fetcher,
            scheduleSnapshotRepository = ScheduleSnapshotRepository(cacheStore),
            autoLoadOnInit = false
        )
        assertEquals(RoutesLoadState.EMPTY_OR_INVALID_JSON, repository.refreshRoutesFromLocal())
        assertEquals(good, cacheStore.readSnapshot()?.json)
        assertEquals("06:25", repository.getSchedulesForRoute("1").single().departureTime)
    }

    private lateinit var context: Context
    private lateinit var tempFilesDir: File
    private lateinit var cacheStore: ScheduleCacheStore

    @Before
    fun setUp() {
        tempFilesDir = Files.createTempDirectory("schedule-cache-test").toFile()
        context = TestFilesDirContext(
            baseContext = ApplicationProvider.getApplicationContext(),
            filesDir = tempFilesDir
        )
        cacheStore = ScheduleCacheStore(context)
        runTest {
            cacheStore.clear()
        }
    }

    @After
    fun tearDown() = runTest {
        cacheStore.clear()
        tempFilesDir.deleteRecursively()
    }

    @Test
    fun `refreshRoutesFromLocal updates state and persists snapshot`() = runTest {
        cacheStore.clear()

        val remoteJson = """
            {
              "routes": [
                {
                  "id": "102",
                  "routeNumber": "102",
                  "name": "Slavgorod - Yarovoye",
                  "description": "Market route",
                  "color": "#1976D2",
                  "schedules": [
                    {
                      "id": "102_1",
                      "departurePoint": "Market",
                      "departureTime": "06:25"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val remoteFetcher = object : ScheduleFetcher {
            override fun isOnline(): Boolean = true

            override suspend fun fetchWithRetry(): String = remoteJson
        }

        val dataSource = RoutesTableDataSource(
            context = context,
            remoteScheduleFetcher = remoteFetcher,
            scheduleSnapshotRepository = ScheduleSnapshotRepository(cacheStore),
            autoLoadOnInit = false
        )

        val loadState = dataSource.refreshRoutesFromLocal()
        val snapshot = cacheStore.readSnapshot()
        val schedules = dataSource.getSchedulesForRoute("102")

        assertEquals(RoutesLoadState.SUCCESS, loadState)
        assertEquals(1, dataSource.routes.value.size)
        assertEquals("102", dataSource.routes.value.first().id)
        assertEquals(ScheduleDataSource.REMOTE_CACHE, dataSource.dataSourceStatus.value.source)
        assertEquals(RoutesLoadState.SUCCESS, dataSource.dataSourceStatus.value.loadState)
        assertTrue(dataSource.dataSourceStatus.value.lastUpdatedAtMillis != null)
        assertNotNull(snapshot)
        assertEquals(remoteJson, snapshot.json)
        assertEquals(listOf("102_1"), schedules.map { it.id })
    }

    @Test
    fun `refreshRoutesFromLocal keeps cached data on no changes and updates on success`() =
        runTest {
            val initialJson = """
                {
                  "routes": [
                    {
                      "id": "102",
                      "routeNumber": "102",
                      "name": "Slavgorod - Yarovoye",
                      "description": "Market route",
                      "color": "#1976D2",
                      "schedules": [
                        {
                          "id": "102_1",
                          "departurePoint": "Market",
                          "departureTime": "06:25"
                        }
                      ]
                    }
                  ]
                }
            """.trimIndent()
            val updatedJson = """
                {
                  "routes": [
                    {
                      "id": "102",
                      "routeNumber": "102",
                      "name": "Slavgorod - Yarovoye Express",
                      "description": "Market route",
                      "color": "#1976D2",
                      "schedules": [
                        {
                          "id": "102_1",
                          "departurePoint": "Market",
                          "departureTime": "06:30"
                        }
                      ]
                    }
                  ]
                }
            """.trimIndent()

            cacheStore.writeSnapshot(initialJson)

            var fetchCount = 0
            val remoteFetcher = object : ScheduleFetcher {
                override fun isOnline(): Boolean = true

                override suspend fun fetchWithRetry(): String? {
                    fetchCount += 1
                    return if (fetchCount == 1) initialJson else updatedJson
                }
            }

            val dataSource = RoutesTableDataSource(
                context = context,
                remoteScheduleFetcher = remoteFetcher,
                scheduleSnapshotRepository = ScheduleSnapshotRepository(cacheStore),
                autoLoadOnInit = false
            )

            val notices = mutableListOf<ScheduleUpdateNotice>()
            val noticeCollector = launch {
                dataSource.scheduleUpdateNotices.collect {
                    notices += it
                }
            }

            val noChangesState = dataSource.refreshRoutesFromLocal()
            val snapshotAfterNoChanges = cacheStore.readSnapshot()
            val noChangesUpdateEventId = dataSource.dataSourceStatus.value.updateEventId

            assertEquals(RoutesLoadState.NO_CHANGES, noChangesState)
            assertEquals(RoutesLoadState.NO_CHANGES, dataSource.dataSourceStatus.value.loadState)
            assertEquals(ScheduleDataSource.REMOTE_CACHE, dataSource.dataSourceStatus.value.source)
            assertEquals("Slavgorod - Yarovoye", dataSource.getRouteById("102")?.name)
            assertEquals(initialJson, snapshotAfterNoChanges?.json)
            assertEquals(0L, noChangesUpdateEventId)
            assertTrue(notices.isEmpty())

            val successState = dataSource.refreshRoutesFromLocal()
            val snapshotAfterSuccess = cacheStore.readSnapshot()
            advanceUntilIdle()

            assertEquals(RoutesLoadState.SUCCESS, successState)
            assertEquals(RoutesLoadState.SUCCESS, dataSource.dataSourceStatus.value.loadState)
            assertEquals(ScheduleDataSource.REMOTE_CACHE, dataSource.dataSourceStatus.value.source)
            assertEquals("Slavgorod - Yarovoye Express", dataSource.getRouteById("102")?.name)
            assertEquals(updatedJson, snapshotAfterSuccess?.json)
            assertTrue(dataSource.dataSourceStatus.value.lastUpdatedAtMillis != null)
            assertEquals(1L, dataSource.dataSourceStatus.value.updateEventId)
            assertEquals(1, notices.size)
            noticeCollector.cancel()
        }

    @Test
    fun `refreshRoutesFromLocal keeps in-memory routes when snapshot write fails`() = runTest {
        val remoteJson = """
            {
              "routes": [
                {
                  "id": "102",
                  "routeNumber": "102",
                  "name": "Slavgorod - Yarovoye",
                  "description": "Market route",
                  "color": "#1976D2",
                  "schedules": [
                    {
                      "id": "102_1",
                      "departurePoint": "Market",
                      "departureTime": "06:25"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val remoteFetcher = object : ScheduleFetcher {
            override fun isOnline(): Boolean = true

            override suspend fun fetchWithRetry(): String = remoteJson
        }

        val throwingRepository = ThrowingScheduleSnapshotStore(cacheStore)
        val dataSource = RoutesTableDataSource(
            context = context,
            remoteScheduleFetcher = remoteFetcher,
            scheduleSnapshotRepository = throwingRepository,
            autoLoadOnInit = false
        )

        val loadState = dataSource.refreshRoutesFromLocal()
        val snapshot = cacheStore.readSnapshot()

        assertEquals(RoutesLoadState.SUCCESS, loadState)
        assertEquals(RoutesLoadState.SUCCESS, dataSource.dataSourceStatus.value.loadState)
        assertEquals(ScheduleDataSource.REMOTE_CACHE, dataSource.dataSourceStatus.value.source)
        assertEquals(1, dataSource.routes.value.size)
        assertEquals("102", dataSource.getRouteById("102")?.id)
        assertEquals("Slavgorod - Yarovoye", dataSource.routes.value.first().name)
        assertTrue(dataSource.dataSourceStatus.value.lastUpdatedAtMillis != null)
        assertEquals(1L, dataSource.dataSourceStatus.value.updateEventId)
        assertTrue(snapshot == null)
    }

    @Test
    fun `unchanged remote data is saved after an earlier write failure`() = runTest {
        val json = """{"routes":[{"id":"1","routeNumber":"1","name":"Маршрут","description":"Маршрут","color":"#1976D2","schedules":[{"id":"1","departurePoint":"Центр","departureTime":"06:25"}]}]}"""
        val fetcher = object : ScheduleFetcher {
            override fun isOnline() = true
            override suspend fun fetchWithRetry() = json
        }
        val delegate = ScheduleSnapshotRepository(cacheStore)
        var writes = 0
        val store = object : ScheduleSnapshotStore by delegate {
            override suspend fun writeSnapshot(json: String) {
                if (++writes == 1) throw IllegalStateException("Ошибка записи")
                delegate.writeSnapshot(json)
            }
        }
        val dataSource = RoutesTableDataSource(
            context = context,
            remoteScheduleFetcher = fetcher,
            scheduleSnapshotRepository = store,
            autoLoadOnInit = false
        )

        assertEquals(RoutesLoadState.SUCCESS, dataSource.refreshRoutesFromLocal())
        assertEquals(null, cacheStore.readSnapshot())
        assertEquals(RoutesLoadState.NO_CHANGES, dataSource.refreshRoutesFromLocal())
        assertEquals(json, cacheStore.readSnapshot()?.json)
        assertEquals(2, writes)
    }

    private class TestFilesDirContext(
        baseContext: Context,
        private val filesDir: File
    ) : ContextWrapper(baseContext) {
        override fun getFilesDir(): File = filesDir

        override fun getApplicationContext(): Context = this
    }

    private class ThrowingScheduleSnapshotStore(
        cacheStore: ScheduleCacheStore
    ) : ScheduleSnapshotStore {
        private val repository = ScheduleSnapshotRepository(cacheStore)

        override suspend fun readSnapshot() = repository.readSnapshot()

        override suspend fun writeSnapshot(json: String) {
            throw IllegalStateException("Не удалось сохранить снимок")
        }

        override suspend fun hasSnapshot(): Boolean = repository.hasSnapshot()

        override suspend fun clear() = repository.clear()

        override fun formatSavedAt(timestampMillis: Long): String =
            repository.formatSavedAt(timestampMillis)
    }
}
