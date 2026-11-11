package ru.slavgorod.transport.integration

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import ru.slavgorod.transport.data.repository.RoutesTableDataSource
import ru.slavgorod.transport.data.repository.ScheduleFetcher
import ru.slavgorod.transport.data.repository.ScheduleSnapshotStore
import ru.slavgorod.transport.data.local.CachedScheduleSnapshot
import ru.slavgorod.transport.notifications.AppForegroundTracker
import ru.slavgorod.transport.ui.viewmodel.RoutesViewModel

@RunWith(AndroidJUnit4::class)
class SearchFlowTest {

    private lateinit var viewModel: RoutesViewModel
    private lateinit var repository: RoutesTableDataSource
    private lateinit var context: Context
    private lateinit var appForegroundTracker: AppForegroundTracker

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val json = """{"routes":[{"id":"102","routeNumber":"102","name":"Market route","description":"Route","color":"#123456","schedules":[]},{"id":"102B","routeNumber":"102B","name":"Other route","description":"Route","color":"#123456","schedules":[]},{"id":"1","routeNumber":"1","name":"City route","description":"Route","color":"#123456","schedules":[]}]}"""
        val fetcher = object : ScheduleFetcher {
            override fun isOnline() = true
            override suspend fun fetchWithRetry() = json
        }
        val store = object : ScheduleSnapshotStore {
            override suspend fun readSnapshot(): CachedScheduleSnapshot? = null
            override suspend fun writeSnapshot(json: String) = Unit
            override suspend fun clear() = Unit
            override suspend fun hasSnapshot() = false
            override fun formatSavedAt(timestampMillis: Long) = ""
        }
        repository = RoutesTableDataSource(context, remoteScheduleFetcher = fetcher,
            scheduleSnapshotRepository = store, autoLoadOnInit = false)
        runBlocking { repository.refreshRoutesFromLocal(false) }
        appForegroundTracker = AppForegroundTracker().apply { setForegroundForTesting(true) }
        viewModel = RoutesViewModel(repository, appForegroundTracker, loadRoutesOnInit = false)
    }

    @Test
    fun searchByRouteNumber_returnsCorrectRoute() = runBlocking {
        viewModel.onSearchQueryChange("102")
        val uiState = withTimeout(5000) {
            viewModel.uiState.first { state ->
                state.routes.any { it.routeNumber == "102" } && state.routes.all { it.routeNumber.contains("102") }
            }
        }
        assertTrue(uiState.routes.any { it.routeNumber == "102" })
    }

    @Test
    fun searchByRouteName_returnsMatchingRoutes() = runBlocking {
        val query = repository.getAllRoutes().firstOrNull()?.name?.take(3).orEmpty()
        viewModel.onSearchQueryChange(query)
        val uiState = withTimeout(5000) { viewModel.uiState.first { it.routes.isNotEmpty() && it.routes.all { route -> route.name.contains(query, ignoreCase = true) } } }
        assertTrue(uiState.routes.all { it.name.contains(query, ignoreCase = true) })
    }

    @Test
    fun searchWithNonExistentQuery_returnsEmptyList() = runBlocking {
        // Сначала дожидаемся исходного списка: начальное пустое состояние не является результатом поиска.
        withTimeout(5000) { viewModel.uiState.first { it.routes.size == 3 } }
        viewModel.onSearchQueryChange("MissingRoute123")
        val uiState = withTimeout(5000) { viewModel.uiState.first { !it.isLoading && it.routes.isEmpty() } }
        assertTrue(uiState.routes.isEmpty())
    }

    @Test
    fun clearSearch_returnsAllRoutes() = runBlocking {
        viewModel.onSearchQueryChange("102")
        val searchResults = withTimeout(5000) { viewModel.uiState.first { it.routes.isNotEmpty() && it.routes.size < 3 } }.routes.size

        viewModel.onSearchQueryChange("")
        val allRoutes = withTimeout(5000) { viewModel.uiState.first { it.routes.size == 3 } }.routes.size
        assertTrue(allRoutes > searchResults)
    }

    @Test
    fun repository_getAllRoutes_returnsNonEmptyList() {
        val routes = repository.getAllRoutes()
        assertTrue(routes.isNotEmpty())
        assertTrue(routes.any { it.id == "102" })
        assertTrue(routes.any { it.id == "102B" })
        assertTrue(routes.any { it.id == "1" })
    }

    @Test
    fun repository_getRouteById_returnsCorrectRoute() {
        val route = repository.getRouteById("102")
        assertNotNull(route)
        assertEquals("102", route?.id)
        assertEquals("102", route?.routeNumber)
    }
}
