package ru.slavgorod.transport.app.bootstrap

import ru.slavgorod.transport.data.repository.RoutesLoadState
import ru.slavgorod.transport.ui.viewmodel.RoutesUiState

internal fun buildRoutesStatusMessage(
    routesUiMessageText: String?,
    isConnected: Boolean,
    routesUiState: RoutesUiState,
    offlineNoDataMessage: String,
    offlineCopyDateKnown: String?,
    offlineCopyDateUnknown: String
): String? {
    return when {
        routesUiMessageText != null -> routesUiMessageText
        !isConnected && routesUiState.dataSourceStatus.loadState == RoutesLoadState.OFFLINE_NO_DATA -> {
            offlineNoDataMessage
        }

        !isConnected -> buildOfflineStatusMessage(
            lastUpdatedAtMillis = routesUiState.dataSourceStatus.lastUpdatedAtMillis,
            dateKnown = offlineCopyDateKnown,
            dateUnknown = offlineCopyDateUnknown
        )

        routesUiState.error != null -> routesUiState.error
        else -> null
    }
}

private fun buildOfflineStatusMessage(
    lastUpdatedAtMillis: Long?,
    dateKnown: String?,
    dateUnknown: String
): String {
    return if (lastUpdatedAtMillis != null) dateKnown.orEmpty() else dateUnknown
}
