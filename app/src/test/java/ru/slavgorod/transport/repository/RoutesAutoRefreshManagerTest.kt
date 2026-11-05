package ru.slavgorod.transport.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ru.slavgorod.transport.core.Constants
import ru.slavgorod.transport.data.repository.RemoteScheduleFetcher
import ru.slavgorod.transport.data.repository.RoutesAutoRefreshManager
import ru.slavgorod.transport.data.repository.RoutesLoadState
import ru.slavgorod.transport.data.repository.RoutesTableDataSource
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RoutesAutoRefreshManagerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `refresh runs when connection is restored`() = runTest {
        val connectionState = MutableStateFlow(false)
        val dispatcher = StandardTestDispatcher(testScheduler)
        val refreshCalls = AtomicInteger(0)
        val repository = RoutesTableDataSource(
            context = context,
            onlineChecker = { false },
            remoteScheduleFetcher = RemoteScheduleFetcher(
                remoteJsonUrlProvider = { "https://example.com/routes.json" },
                onlineChecker = { false },
                delayProvider = {}
            ),
            autoLoadOnInit = false
        )

        val manager = RoutesAutoRefreshManager(
            routeRepository = repository,
            connectionStateFlow = connectionState,
            initialConnectionState = false,
            dispatcher = dispatcher,
            refreshAction = {
                refreshCalls.incrementAndGet()
                RoutesLoadState.NO_CHANGES
            }
        )
        manager.start()

        runCurrent()
        assertEquals(0, refreshCalls.get())

        connectionState.value = true
        runCurrent()

        assertEquals(1, refreshCalls.get())
        manager.stop()
    }

    @Test
    fun `start is idempotent for periodic refresh`() = runTest {
        val connectionState = MutableStateFlow(true)
        val dispatcher = StandardTestDispatcher(testScheduler)
        val refreshCalls = AtomicInteger(0)
        val repository = createRepository()
        val manager = RoutesAutoRefreshManager(
            routeRepository = repository,
            connectionStateFlow = connectionState,
            initialConnectionState = true,
            dispatcher = dispatcher,
            refreshAction = {
                refreshCalls.incrementAndGet()
                RoutesLoadState.NO_CHANGES
            }
        )

        manager.start()
        manager.start()
        advanceTimeBy(Constants.SCHEDULE_AUTO_REFRESH_INTERVAL_MS + 1)
        runCurrent()

        assertEquals(1, refreshCalls.get())
        manager.stop()
    }

    @Test
    fun `stop cancels periodic refresh`() = runTest {
        val connectionState = MutableStateFlow(true)
        val dispatcher = StandardTestDispatcher(testScheduler)
        val refreshCalls = AtomicInteger(0)
        val repository = createRepository()
        val manager = RoutesAutoRefreshManager(
            routeRepository = repository,
            connectionStateFlow = connectionState,
            initialConnectionState = true,
            dispatcher = dispatcher,
            refreshAction = {
                refreshCalls.incrementAndGet()
                RoutesLoadState.NO_CHANGES
            }
        )

        manager.start()
        manager.stop()
        advanceTimeBy(Constants.SCHEDULE_AUTO_REFRESH_INTERVAL_MS + 1)
        runCurrent()

        assertEquals(0, refreshCalls.get())
    }

    @Test
    fun `cancellation stops observing network restoration`() = runTest {
        val connectionState = MutableStateFlow(false)
        var refreshCalls = 0
        val manager = RoutesAutoRefreshManager(
            routeRepository = createRepository(),
            connectionStateFlow = connectionState,
            initialConnectionState = false,
            dispatcher = StandardTestDispatcher(testScheduler),
            refreshAction = {
                refreshCalls++
                throw CancellationException("Отмена обновления")
            }
        )

        manager.start()
        runCurrent()
        connectionState.value = true
        runCurrent()
        connectionState.value = false
        runCurrent()
        connectionState.value = true
        runCurrent()

        assertEquals(1, refreshCalls)
        manager.stop()
    }

    private fun createRepository(): RoutesTableDataSource {
        return RoutesTableDataSource(
            context = context,
            onlineChecker = { false },
            remoteScheduleFetcher = RemoteScheduleFetcher(
                remoteJsonUrlProvider = { "https://example.com/routes.json" },
                onlineChecker = { false },
                delayProvider = {}
            ),
            autoLoadOnInit = false
        )
    }
}
