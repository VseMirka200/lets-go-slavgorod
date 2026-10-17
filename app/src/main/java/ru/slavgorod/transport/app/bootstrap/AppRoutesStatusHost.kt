package ru.slavgorod.transport.app.bootstrap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.delay
import ru.slavgorod.transport.R
import ru.slavgorod.transport.domain.util.DateTimeFormatterUtils
import ru.slavgorod.transport.ui.model.UiMessage
import ru.slavgorod.transport.ui.viewmodel.RoutesUiState

private const val HEADER_STATUS_MESSAGE_DURATION_MS = 5_000L

@Composable
internal fun rememberRoutesStatusMessage(
    routesUiState: RoutesUiState,
    routesUiMessage: UiMessage?,
    isConnected: Boolean,
    onMessageTimeout: () -> Unit
): String? {
    val offlineNoDataMessage = stringResource(R.string.status_offline_no_data)
    val offlineCopyBaseMessage = stringResource(R.string.status_offline_copy_base)
    val offlineCopyDateKnown =
        routesUiState.dataSourceStatus.lastUpdatedAtMillis?.let { timestamp ->
            stringResource(
                R.string.status_offline_copy_date_known,
                offlineCopyBaseMessage,
                DateTimeFormatterUtils.formatFullDateTime(timestamp)
            )
        }
    val offlineCopyDateUnknown = stringResource(
        R.string.status_offline_copy_date_unknown,
        offlineCopyBaseMessage
    )
    val routesUiMessageText = routesUiMessage?.let { message ->
        message.text ?: message.textResId?.let { textResId ->
            stringResourceForArgs(textResId, message.textArgs)
        }
    }

    LaunchedEffect(routesUiMessage?.id) {
        if (routesUiMessage != null) {
            delay(HEADER_STATUS_MESSAGE_DURATION_MS)
            onMessageTimeout()
        }
    }

    return remember(
        routesUiMessage?.id,
        routesUiMessageText,
        isConnected,
        routesUiState.dataSourceStatus.lastUpdatedAtMillis,
        routesUiState.dataSourceStatus.loadState,
        routesUiState.error
    ) {
        buildRoutesStatusMessage(
            routesUiMessageText = routesUiMessageText,
            isConnected = isConnected,
            routesUiState = routesUiState,
            offlineNoDataMessage = offlineNoDataMessage,
            offlineCopyDateKnown = offlineCopyDateKnown,
            offlineCopyDateUnknown = offlineCopyDateUnknown
        )
    }
}

@Composable
private fun stringResourceForArgs(textResId: Int, args: List<String>): String {
    return when (args.size) {
        0 -> stringResource(textResId)
        1 -> stringResource(textResId, args[0])
        2 -> stringResource(textResId, args[0], args[1])
        else -> stringResource(textResId, args.joinToString())
    }
}
