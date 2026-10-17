package ru.slavgorod.transport.ui.viewmodel

import ru.slavgorod.transport.data.repository.RoutesLoadState
import ru.slavgorod.transport.data.repository.buildScheduleUpdateNotice

internal sealed interface RoutesRefreshResultEffect {
    data class SuccessNotice(
        val textResId: Int,
        val args: List<String>
    ) : RoutesRefreshResultEffect

    data class LoggedMessage(
        val logMessage: String,
        val message: UiMessageSpec?
    ) : RoutesRefreshResultEffect
}

internal fun buildRoutesRefreshResultEffect(
    loadState: RoutesLoadState,
    lastUpdatedAtMillis: Long?
): RoutesRefreshResultEffect {
    if (loadState == RoutesLoadState.SUCCESS) {
        val notice = lastUpdatedAtMillis?.let(::buildScheduleUpdateNotice)
            ?: return RoutesRefreshResultEffect.LoggedMessage(
                logMessage = buildRefreshLogMessage(loadState),
                message = buildRefreshUiMessage(loadState, lastUpdatedAtMillis)
            )
        return RoutesRefreshResultEffect.SuccessNotice(
            textResId = notice.textResId,
            args = notice.textArgs
        )
    }

    return RoutesRefreshResultEffect.LoggedMessage(
        logMessage = buildRefreshLogMessage(loadState),
        message = buildRefreshUiMessage(loadState, lastUpdatedAtMillis)
    )
}
