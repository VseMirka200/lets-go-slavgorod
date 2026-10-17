package ru.slavgorod.transport.app.bootstrap

import org.junit.Test
import ru.slavgorod.transport.data.repository.RoutesLoadState
import ru.slavgorod.transport.data.repository.ScheduleDataSourceStatus
import ru.slavgorod.transport.ui.viewmodel.RoutesUiState
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppStatusMessageTest {

    @Test
    fun `ui message has priority over connection status`() {
        val result = buildRoutesStatusMessage(
            routesUiMessageText = "Updated",
            isConnected = false,
            routesUiState = RoutesUiState(
                dataSourceStatus = ScheduleDataSourceStatus(
                    loadState = RoutesLoadState.OFFLINE_NO_DATA
                )
            ),
            offlineNoDataMessage = "Offline",
            offlineCopyDateKnown = "Saved",
            offlineCopyDateUnknown = "Saved unknown"
        )

        assertEquals("Updated", result)
    }

    @Test
    fun `offline no data uses dedicated message`() {
        val result = buildRoutesStatusMessage(
            routesUiMessageText = null,
            isConnected = false,
            routesUiState = RoutesUiState(
                dataSourceStatus = ScheduleDataSourceStatus(
                    loadState = RoutesLoadState.OFFLINE_NO_DATA
                )
            ),
            offlineNoDataMessage = "No data",
            offlineCopyDateKnown = "Saved",
            offlineCopyDateUnknown = "Saved unknown"
        )

        assertEquals("No data", result)
    }

    @Test
    fun `online state falls back to route error`() {
        val result = buildRoutesStatusMessage(
            routesUiMessageText = null,
            isConnected = true,
            routesUiState = RoutesUiState(error = "Failed"),
            offlineNoDataMessage = "No data",
            offlineCopyDateKnown = "Saved",
            offlineCopyDateUnknown = "Saved unknown"
        )

        assertEquals("Failed", result)
    }

    @Test
    fun `online state without message or error is empty`() {
        val result = buildRoutesStatusMessage(
            routesUiMessageText = null,
            isConnected = true,
            routesUiState = RoutesUiState(),
            offlineNoDataMessage = "No data",
            offlineCopyDateKnown = "Saved",
            offlineCopyDateUnknown = "Saved unknown"
        )

        assertNull(result)
    }
}
