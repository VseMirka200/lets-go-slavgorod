package ru.slavgorod.transport.integration

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import ru.slavgorod.transport.data.repository.RoutesTableDataSource
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
        repository = RoutesTableDataSource(context)
        appForegroundTracker = AppForegroundTracker().apply { setForegroundForTesting(true) }
        viewModel = RoutesViewModel(repository, appForegroundTracker)
    }

    @Test
    fun searchByRouteNumber_returnsCorrectRoute() = runTest {
        viewModel.onSearchQueryChange("102")
        kotlinx.coroutines.delay(100)
        val uiState = viewModel.uiState.first()
        assertTrue(uiState.routes.any { it.routeNumber == "102" })
    }

    @Test
    fun searchByRouteName_returnsMatchingRoutes() = runTest {
        val query = repository.getAllRoutes().firstOrNull()?.name?.take(3).orEmpty()
        viewModel.onSearchQueryChange(query)
        kotlinx.coroutines.delay(100)
        val uiState = viewModel.uiState.first()
        assertTrue(uiState.routes.all { it.name.contains(query, ignoreCase = true) })
    }

    @Test
    fun searchWithNonExistentQuery_returnsEmptyList() = runTest {
        viewModel.onSearchQueryChange("MissingRoute123")
        kotlinx.coroutines.delay(100)
        val uiState = viewModel.uiState.first()
        assertTrue(uiState.routes.isEmpty())
    }

    @Test
    fun clearSearch_returnsAllRoutes() = runTest {
        viewModel.onSearchQueryChange("102")
        kotlinx.coroutines.delay(100)
        val searchResults = viewModel.uiState.first().routes.size

        viewModel.onSearchQueryChange("")
        kotlinx.coroutines.delay(100)

        val allRoutes = viewModel.uiState.first().routes.size
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
