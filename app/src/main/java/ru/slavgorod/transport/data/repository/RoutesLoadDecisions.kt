package ru.slavgorod.transport.data.repository

import ru.slavgorod.transport.R

internal data class RoutesLoadDecision(
    val loadState: RoutesLoadState,
    val source: ScheduleDataSource
)

internal object RoutesLoadDecisions {

    fun offline(hasCachedData: Boolean): RoutesLoadDecision {
        return RoutesLoadDecision(
            loadState = offlineLoadState(hasCachedData),
            source = savedCopyOrNone(hasCachedData)
        )
    }

    fun networkError(hasCachedData: Boolean): RoutesLoadDecision {
        return RoutesLoadDecision(
            loadState = networkErrorLoadState(hasCachedData),
            source = savedCopyOrNone(hasCachedData)
        )
    }

    fun invalidJson(hasCachedData: Boolean): RoutesLoadDecision {
        return RoutesLoadDecision(
            loadState = RoutesLoadState.EMPTY_OR_INVALID_JSON,
            source = savedCopyOrNone(hasCachedData)
        )
    }

    fun updateLogMessageResId(loadState: RoutesLoadState): Int {
        return when (loadState) {
            RoutesLoadState.OFFLINE_USING_SAVED_COPY -> R.string.log_state_offline_saved_copy
            RoutesLoadState.OFFLINE_NO_DATA -> R.string.log_state_offline_no_data
            RoutesLoadState.NETWORK_ERROR_WITH_SAVED_COPY -> R.string.log_state_network_error_saved_copy
            RoutesLoadState.NETWORK_ERROR -> R.string.log_state_network_error
            RoutesLoadState.EMPTY_OR_INVALID_JSON -> R.string.log_state_invalid_json
            RoutesLoadState.NO_CHANGES -> R.string.log_state_no_changes
            else -> R.string.log_state_idle
        }
    }

    private fun savedCopyOrNone(hasCachedData: Boolean): ScheduleDataSource {
        return if (hasCachedData) ScheduleDataSource.SAVED_COPY else ScheduleDataSource.NONE
    }

    private fun offlineLoadState(hasCachedData: Boolean): RoutesLoadState {
        return if (hasCachedData) RoutesLoadState.OFFLINE_USING_SAVED_COPY else RoutesLoadState.OFFLINE_NO_DATA
    }

    private fun networkErrorLoadState(hasCachedData: Boolean): RoutesLoadState {
        return if (hasCachedData) RoutesLoadState.NETWORK_ERROR_WITH_SAVED_COPY else RoutesLoadState.NETWORK_ERROR
    }
}
