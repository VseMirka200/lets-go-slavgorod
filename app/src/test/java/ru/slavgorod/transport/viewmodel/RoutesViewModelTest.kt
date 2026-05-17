package ru.slavgorod.transport.viewmodel

import android.app.Application
import android.os.Looper
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import ru.slavgorod.transport.R
import ru.slavgorod.transport.core.AppText
import ru.slavgorod.transport.core.Constants
import ru.slavgorod.transport.data.repository.RoutesTableDataSource
import ru.slavgorod.transport.data.repository.ScheduleFetcher
import ru.slavgorod.transport.notifications.AppForegroundTracker
import ru.slavgorod.transport.ui.viewmodel.RoutesViewModel
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RoutesViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var application: Application
    private lateinit var remoteResponses: ArrayDeque<String?>
    private lateinit var repository: RoutesTableDataSource
    private lateinit var displayDataStore: DataStore<Preferences>
    private lateinit var displayDataStoreFile: File
    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var appForegroundTracker: AppForegroundTracker
    private lateinit var viewModel: RoutesViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = ApplicationProvider.getApplicationContext()
        AppText.init(application)
        remoteResponses = ArrayDeque()
        displayDataStoreFile = File(
            application.filesDir,
            "routes_viewmodel_test_${System.nanoTime()}.preferences_pb"
        )
        dataStoreScope = CoroutineScope(SupervisorJob() + testDispatcher)
        displayDataStore = createPreferencesDataStore(displayDataStoreFile)
        appForegroundTracker = AppForegroundTracker().apply {
            setForegroundForTesting(true)
        }
    }

    @After
    fun tearDown() {
        testDispatcher.scheduler.advanceUntilIdle()
        shadowOf(Looper.getMainLooper()).idle()
        Dispatchers.resetMain()
        dataStoreScope.cancel()
        displayDataStoreFile.delete()
    }

    @Test
    fun `initial state stays loading while first routes are not resolved`() =
        runTest(testDispatcher) {
            repository = createRepository()

            viewModel = RoutesViewModel(
                routeRepository = repository,
                appForegroundTracker = appForegroundTracker,
                displaySettingsDataStore = displayDataStore,
                loadRoutesOnInit = false,
                initialLoadTimeoutMillis = 1_000L
            )
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isLoading)
            assertEquals(null, viewModel.uiState.value.error)
        }

    @Test
    fun `initial load timeout publishes failure state`() = runTest(testDispatcher) {
        repository = createRepository()

        viewModel = RoutesViewModel(
            routeRepository = repository,
            appForegroundTracker = appForegroundTracker,
            displaySettingsDataStore = displayDataStore,
            loadRoutesOnInit = false,
            initialLoadTimeoutMillis = 1_000L
        )
        advanceViewModelState()
        advanceTimeBy(1_000L)
        advanceViewModelState()

        assertTrue(!viewModel.uiState.value.isLoading)
        assertEquals(
            AppText.get(R.string.routes_initial_load_failed),
            viewModel.uiState.value.error
        )
    }

    @Test
    fun `refresh publishes a human readable message`() = runTest(testDispatcher) {
        enqueueRoutesResponse()
        enqueueUpdatedRoutesResponse()
        repository = createRepository()
        repository.refreshRoutesFromLocal()

        viewModel = RoutesViewModel(repository, appForegroundTracker, displayDataStore)
        advanceViewModelState()

        viewModel.refresh()
        advanceRefresh()
        waitForUiMessage()

        assertNotNull(viewModel.uiMessage.value)
        assertEquals(
            R.string.schedule_update_notice_with_date,
            viewModel.uiMessage.value?.textResId
        )
    }

    @Test
    fun `refresh keeps schedule notice out of ui when app is in background`() =
        runTest(testDispatcher) {
            enqueueRoutesResponse()
            enqueueUpdatedRoutesResponse()
            repository = createRepository()
            repository.refreshRoutesFromLocal()

            appForegroundTracker.setForegroundForTesting(false)
            viewModel = RoutesViewModel(repository, appForegroundTracker, displayDataStore)
            advanceViewModelState()

            viewModel.refresh()
            advanceRefresh()

            assertEquals(null, viewModel.uiMessage.value)
        }

    @Test
    fun `search query filters routes by number`() = runTest(testDispatcher) {
        enqueueRoutesResponse()
        enqueueRoutesResponse()
        repository = createRepository()
        repository.refreshRoutesFromLocal()

        viewModel = RoutesViewModel(repository, appForegroundTracker, displayDataStore)
        advanceViewModelState()

        viewModel.onSearchQueryChange("102")
        advanceViewModelState()

        assertTrue(viewModel.uiState.value.routes.all { route ->
            route.routeNumber.contains("102") || route.name.contains("102", ignoreCase = true)
        })
    }

    @Test
    fun `home transient state stays in the view model and back handling closes menu before pin mode`() =
        runTest(testDispatcher) {
            repository = createRepository()
            viewModel = RoutesViewModel(
                routeRepository = repository,
                appForegroundTracker = appForegroundTracker,
                displaySettingsDataStore = displayDataStore,
                loadRoutesOnInit = false,
                initialLoadTimeoutMillis = 1_000L
            )

            viewModel.showPinAction("102")
            viewModel.openHeaderMenu()

            viewModel.handleBackPressedFromHome()

            assertTrue(!viewModel.isHeaderMenuOpen)
            assertEquals("102", viewModel.pinActionRouteId)

            viewModel.handleBackPressedFromHome()

            assertNull(viewModel.pinActionRouteId)
            assertTrue(!viewModel.isHeaderMenuOpen)
        }

    @Test
    fun `search logging baseline is preserved for repeated queries`() = runTest(testDispatcher) {
        repository = createRepository()
        viewModel = RoutesViewModel(
            routeRepository = repository,
            appForegroundTracker = appForegroundTracker,
            displaySettingsDataStore = displayDataStore,
            loadRoutesOnInit = false,
            initialLoadTimeoutMillis = 1_000L
        )

        assertTrue(viewModel.shouldLogSearchQuery("102"))

        viewModel.markSearchQueryLogged("102")

        assertTrue(!viewModel.shouldLogSearchQuery("102"))
        assertTrue(viewModel.shouldLogSearchQuery("105"))
    }

    private fun createRepository(): RoutesTableDataSource {
        return RoutesTableDataSource(
            context = application,
            onlineChecker = { true },
            remoteScheduleFetcher = FakeScheduleFetcher(remoteResponses),
            autoLoadOnInit = false
        )
    }

    private fun enqueueRoutesResponse() {
        remoteResponses +=
            """
                        {
                          "routes": [
                            {
                              "id": "105",
                              "routeNumber": "105",
                              "name": "Bus 105",
                              "notes": "Slavgorod",
                              "color": "#F2B705",
                              "travelTime": "15 min",
                              "pricePrimary": "20",
                              "priceSecondary": "30",
                              "paymentMethods": "Cash"
                            },
                            {
                              "id": "3",
                              "routeNumber": "3",
                              "name": "Bus 3",
                              "notes": "Loop",
                              "color": "#E91E63",
                              "travelTime": "10 min",
                              "pricePrimary": "10",
                              "priceSecondary": "15",
                              "paymentMethods": "Cash"
                            },
                            {
                              "id": "102",
                              "routeNumber": "102",
                              "name": "Bus 102",
                              "notes": "Slavgorod - Yarovoye",
                              "color": "#1976D2",
                              "travelTime": "40 min",
                              "pricePrimary": "40",
                              "priceSecondary": "60",
                              "paymentMethods": "Cash / Card"
                            }
                          ]
                        }
            """.trimIndent()
    }

    private fun enqueueUpdatedRoutesResponse() {
        remoteResponses +=
            """
                        {
                          "routes": [
                            {
                              "id": "105",
                              "routeNumber": "105",
                              "name": "Bus 105",
                              "notes": "Slavgorod",
                              "color": "#F2B705",
                              "travelTime": "15 min",
                              "pricePrimary": "20",
                              "priceSecondary": "30",
                              "paymentMethods": "Cash"
                            },
                            {
                              "id": "3",
                              "routeNumber": "3",
                              "name": "Bus 3",
                              "notes": "Loop",
                              "color": "#E91E63",
                              "travelTime": "10 min",
                              "pricePrimary": "10",
                              "priceSecondary": "15",
                              "paymentMethods": "Cash"
                            },
                            {
                              "id": "102",
                              "routeNumber": "102",
                              "name": "Bus 102",
                              "notes": "Slavgorod - Yarovoye",
                              "color": "#1976D2",
                              "travelTime": "45 min",
                              "pricePrimary": "40",
                              "priceSecondary": "60",
                              "paymentMethods": "Cash / Card"
                            }
                          ]
                        }
            """.trimIndent()
    }

    private fun createPreferencesDataStore(file: File): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { file }
        )
    }

    private fun waitForUiMessage() {
        repeat(50) {
            if (viewModel.uiMessage.value != null) return
            Thread.sleep(20)
            testDispatcher.scheduler.advanceUntilIdle()
            shadowOf(Looper.getMainLooper()).idle()
        }
    }

    private suspend fun TestScope.advanceViewModelState() {
        advanceTimeBy(Constants.SEARCH_DEBOUNCE_MS + 1)
        advanceUntilIdle()
        shadowOf(Looper.getMainLooper()).idle()
    }

    private suspend fun TestScope.advanceRefresh() {
        withContext(Dispatchers.IO) {
            // Wait for the refresh coroutine that is launched on Dispatchers.IO.
        }
        advanceTimeBy(Constants.PULL_TO_REFRESH_MIN_DELAY_MS + Constants.SEARCH_DEBOUNCE_MS + 1)
        advanceUntilIdle()
        shadowOf(Looper.getMainLooper()).idle()
        withContext(Dispatchers.IO) {
            // Wait for DataStore and repository callbacks triggered by refresh.
        }
        advanceUntilIdle()
        shadowOf(Looper.getMainLooper()).idle()
    }

    private class FakeScheduleFetcher(
        private val responses: ArrayDeque<String?>
    ) : ScheduleFetcher {

        override fun isOnline(): Boolean = true

        override suspend fun fetchWithRetry(): String? = responses.removeFirstOrNull()
    }
}
