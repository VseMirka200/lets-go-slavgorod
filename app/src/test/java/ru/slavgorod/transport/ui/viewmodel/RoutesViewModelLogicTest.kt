package ru.slavgorod.transport.ui.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.slavgorod.transport.R
import ru.slavgorod.transport.core.AppText
import ru.slavgorod.transport.data.model.BusRoute
import ru.slavgorod.transport.data.repository.RoutesLoadState
import ru.slavgorod.transport.data.repository.ScheduleDataSource
import ru.slavgorod.transport.data.repository.ScheduleDataSourceStatus

class RoutesViewModelLogicTest {

    @Test
    fun `arrangeRoutesForDisplay keeps pinned routes first and sorts by route number`() {
        val routes = listOf(
            route(id = "105", routeNumber = "105"),
            route(id = "3", routeNumber = "3"),
            route(id = "102", routeNumber = "102"),
            route(id = "7A", routeNumber = "7A")
        )

        val arranged = arrangeRoutesForDisplay(routes, setOf("102"))

        assertEquals(listOf("102", "3", "7A", "105"), arranged.map { it.routeNumber })
    }

    @Test
    fun `filterRoutesByQuery matches route number and name`() {
        val routes = listOf(
            route(id = "105", routeNumber = "105", name = "Bus 105"),
            route(id = "102", routeNumber = "102", name = "Bus 102")
        )

        val filtered = filterRoutesByQuery(routes, "102")

        assertEquals(listOf("102"), filtered.map { it.id })
    }

    @Test
    fun `filterRoutesByQuery matches route description`() {
        val routes = listOf(
            route(id = "105", description = "Transfer route"),
            route(id = "102", description = "Direct trip")
        )

        val filtered = filterRoutesByQuery(routes, "Transfer")

        assertEquals(listOf("105"), filtered.map { it.id })
    }

    @Test
    fun `normalizeRouteOrder drops unknown ids and appends new ones`() {
        val normalized = normalizeRouteOrder(
            routeOrder = listOf("105", "missing", "3"),
            allRouteIds = listOf("3", "102", "105", "7A")
        )

        assertEquals(listOf("105", "3", "102", "7A"), normalized)
    }

    @Test
    fun `buildRefreshUiMessage includes update time for successful refresh`() {
        val message = buildRefreshUiMessage(RoutesLoadState.SUCCESS, 1_700_000_000_000L)

        assertEquals(R.string.schedule_refresh_success_with_date, message?.messageResId)
        assertTrue(message?.args?.isNotEmpty() == true)
    }

    @Test
    fun `resolveScheduleStatusLabel maps remote cache to remote label`() {
        val status = ScheduleDataSourceStatus(
            source = ScheduleDataSource.REMOTE_CACHE,
            loadState = RoutesLoadState.SUCCESS
        )

        assertEquals(R.string.schedule_status_remote, resolveScheduleStatusLabel(status))
    }

    @Test
    fun `resolveRoutesErrorMessage returns null for loaded routes`() {
        val message = resolveRoutesErrorMessage(
            routes = listOf(route("1")),
            waitingForInitialLoad = false,
            currentError = "error",
            loadState = RoutesLoadState.NETWORK_ERROR,
            hasLoadedRoutes = true
        )

        assertNull(message)
    }

    @Test
    fun `resolveRoutesErrorMessage maps offline without data to user message`() {
        val message = resolveRoutesErrorMessage(
            routes = emptyList(),
            waitingForInitialLoad = false,
            currentError = null,
            loadState = RoutesLoadState.OFFLINE_NO_DATA,
            hasLoadedRoutes = false
        )

        assertEquals(AppText.get(R.string.status_offline_no_data), message)
    }

    @Test
    fun `resolveRoutesErrorMessage maps invalid json to user message`() {
        val message = resolveRoutesErrorMessage(
            routes = emptyList(),
            waitingForInitialLoad = false,
            currentError = null,
            loadState = RoutesLoadState.EMPTY_OR_INVALID_JSON,
            hasLoadedRoutes = false
        )

        assertEquals(AppText.get(R.string.schedule_status_invalid_json), message)
    }

    @Test
    fun `resolveRoutesErrorMessage clears previous error when routes are loaded`() {
        val message = resolveRoutesErrorMessage(
            routes = listOf(route("102")),
            waitingForInitialLoad = false,
            currentError = "old error",
            loadState = RoutesLoadState.NETWORK_ERROR,
            hasLoadedRoutes = true
        )

        assertNull(message)
    }

    @Test
    fun `resolveInitialLoadTimeoutState stops loading and reports timeout when routes are still empty`() {
        val state = resolveInitialLoadTimeoutState(
            currentState = RoutesUiState(isLoading = true),
            timeoutErrorMessage = "timeout"
        )

        assertEquals(false, state.isLoading)
        assertEquals("timeout", state.error)
    }

    @Test
    fun `resolveInitialLoadTimeoutState keeps loaded state unchanged`() {
        val currentState = RoutesUiState(
            routes = listOf(route("102")),
            isLoading = true
        )

        val state = resolveInitialLoadTimeoutState(
            currentState = currentState,
            timeoutErrorMessage = "timeout"
        )

        assertEquals(currentState, state)
    }

    private fun route(
        id: String,
        routeNumber: String = id,
        name: String = "Route $id",
        description: String = "Description"
    ): BusRoute {
        return BusRoute(
            id = id,
            routeNumber = routeNumber,
            name = name,
            description = description,
            color = "#FFFFFF",
            travelTime = null,
            pricePrimary = null,
            priceSecondary = null,
            paymentMethods = null
        )
    }
}
