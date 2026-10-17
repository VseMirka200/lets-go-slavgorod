package ru.slavgorod.transport.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RoutesRemoteLoadCoordinatorTest {

    @Test
    fun `returns offline decision without cached data`() = runTest {
        val coordinator = createCoordinator(
            isOnline = { false },
            fetchResult = """{"routes":[]}"""
        )

        val result = coordinator.decide(latestJson = null, hasCachedData = false)

        assertTrue(result is RemoteLoadResult.Decision)
        assertEquals(RoutesLoadState.OFFLINE_NO_DATA, result.decision.loadState)
        assertEquals(ScheduleDataSource.NONE, result.decision.source)
    }

    @Test
    fun `returns network error decision when fetch fails`() = runTest {
        val coordinator = createCoordinator(
            isOnline = { true },
            fetchResult = null
        )

        val result = coordinator.decide(latestJson = null, hasCachedData = true)

        assertTrue(result is RemoteLoadResult.Decision)
        assertEquals(RoutesLoadState.NETWORK_ERROR_WITH_SAVED_COPY, result.decision.loadState)
        assertEquals(ScheduleDataSource.SAVED_COPY, result.decision.source)
    }

    @Test
    fun `returns invalid json decision when parsed routes are empty`() = runTest {
        val coordinator = createCoordinator(
            isOnline = { true },
            fetchResult = """{"routes":[]}"""
        )

        val result = coordinator.decide(latestJson = null, hasCachedData = false)

        assertTrue(result is RemoteLoadResult.Decision)
        assertEquals(RoutesLoadState.EMPTY_OR_INVALID_JSON, result.decision.loadState)
        assertEquals(ScheduleDataSource.NONE, result.decision.source)
    }

    @Test
    fun `returns no changes when remote json matches latest copy`() = runTest {
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
        val coordinator = createCoordinator(
            isOnline = { true },
            fetchResult = remoteJson
        )

        val result = coordinator.decide(latestJson = remoteJson, hasCachedData = true)

        assertTrue(result is RemoteLoadResult.NoChanges)
    }

    @Test
    fun `returns success with parsed routes when remote data changes`() = runTest {
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
        val coordinator = createCoordinator(
            isOnline = { true },
            fetchResult = remoteJson
        )

        val result = coordinator.decide(latestJson = null, hasCachedData = false)

        assertTrue(result is RemoteLoadResult.Success)
        assertEquals(remoteJson, result.remoteJson)
        assertEquals(1, result.parsedRoutes.size)
        assertEquals("102", result.parsedRoutes.first().id)
    }

    private fun createCoordinator(
        isOnline: () -> Boolean,
        fetchResult: String?
    ): RoutesRemoteLoadCoordinator {
        val scheduleFetcher = object : ScheduleFetcher {
            override fun isOnline(): Boolean = isOnline()

            override suspend fun fetchWithRetry(): String? = fetchResult
        }

        return RoutesRemoteLoadCoordinator(
            remoteScheduleFetcher = scheduleFetcher,
            scheduleJsonParser = ScheduleJsonParser(),
            isOnline = isOnline
        )
    }
}
