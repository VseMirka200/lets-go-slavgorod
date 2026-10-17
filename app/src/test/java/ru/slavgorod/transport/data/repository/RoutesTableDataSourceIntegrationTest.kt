package ru.slavgorod.transport.data.repository

import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ru.slavgorod.transport.data.local.ScheduleCacheStore
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RoutesTableDataSourceIntegrationTest {

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
            throw IllegalStateException("Snapshot persistence failed")
        }

        override suspend fun hasSnapshot(): Boolean = repository.hasSnapshot()

        override fun formatSavedAt(timestampMillis: Long): String =
            repository.formatSavedAt(timestampMillis)
    }
}
