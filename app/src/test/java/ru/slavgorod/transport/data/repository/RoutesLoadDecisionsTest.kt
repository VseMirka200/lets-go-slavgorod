package ru.slavgorod.transport.data.repository

import org.junit.Test
import ru.slavgorod.transport.R
import kotlin.test.assertEquals

class RoutesLoadDecisionsTest {

    @Test
    fun `offline decision uses saved copy when cache exists`() {
        val decision = RoutesLoadDecisions.offline(hasCachedData = true)

        assertEquals(RoutesLoadState.OFFLINE_USING_SAVED_COPY, decision.loadState)
        assertEquals(ScheduleDataSource.SAVED_COPY, decision.source)
    }

    @Test
    fun `offline decision reports no data without cache`() {
        val decision = RoutesLoadDecisions.offline(hasCachedData = false)

        assertEquals(RoutesLoadState.OFFLINE_NO_DATA, decision.loadState)
        assertEquals(ScheduleDataSource.NONE, decision.source)
    }

    @Test
    fun `network error decision preserves saved copy source`() {
        val decision = RoutesLoadDecisions.networkError(hasCachedData = true)

        assertEquals(RoutesLoadState.NETWORK_ERROR_WITH_SAVED_COPY, decision.loadState)
        assertEquals(ScheduleDataSource.SAVED_COPY, decision.source)
    }

    @Test
    fun `invalid json decision keeps cached source when available`() {
        val decision = RoutesLoadDecisions.invalidJson(hasCachedData = true)

        assertEquals(RoutesLoadState.EMPTY_OR_INVALID_JSON, decision.loadState)
        assertEquals(ScheduleDataSource.SAVED_COPY, decision.source)
    }

    @Test
    fun `update log message res ids map to the expected strings`() {
        assertEquals(
            R.string.log_state_offline_saved_copy,
            RoutesLoadDecisions.updateLogMessageResId(RoutesLoadState.OFFLINE_USING_SAVED_COPY)
        )
        assertEquals(
            R.string.log_state_offline_no_data,
            RoutesLoadDecisions.updateLogMessageResId(RoutesLoadState.OFFLINE_NO_DATA)
        )
        assertEquals(
            R.string.log_state_network_error_saved_copy,
            RoutesLoadDecisions.updateLogMessageResId(RoutesLoadState.NETWORK_ERROR_WITH_SAVED_COPY)
        )
        assertEquals(
            R.string.log_state_network_error,
            RoutesLoadDecisions.updateLogMessageResId(RoutesLoadState.NETWORK_ERROR)
        )
        assertEquals(
            R.string.log_state_invalid_json,
            RoutesLoadDecisions.updateLogMessageResId(RoutesLoadState.EMPTY_OR_INVALID_JSON)
        )
        assertEquals(
            R.string.log_state_no_changes,
            RoutesLoadDecisions.updateLogMessageResId(RoutesLoadState.NO_CHANGES)
        )
        assertEquals(
            R.string.log_state_idle,
            RoutesLoadDecisions.updateLogMessageResId(RoutesLoadState.SUCCESS)
        )
    }
}
