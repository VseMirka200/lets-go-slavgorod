package ru.slavgorod.transport.data.repository

import ru.slavgorod.transport.data.model.BusRoute

internal sealed interface RoutesRemoteLoadAction {
    val loadState: RoutesLoadState
    val source: ScheduleDataSource
    val reportStatus: Boolean
}

internal data class RoutesRemoteLoadDecisionAction(
    val decision: RoutesLoadDecision,
    override val reportStatus: Boolean
) : RoutesRemoteLoadAction {

    override val loadState: RoutesLoadState = decision.loadState
    override val source: ScheduleDataSource = decision.source
}

internal data class RoutesRemoteLoadNoChangesAction(
    override val reportStatus: Boolean
) : RoutesRemoteLoadAction {

    override val loadState: RoutesLoadState = RoutesLoadState.NO_CHANGES
    override val source: ScheduleDataSource = ScheduleDataSource.REMOTE_CACHE
}

internal data class RoutesRemoteLoadSuccessAction(
    val remoteJson: String,
    val parsedRoutes: List<BusRoute>,
    val notifyUser: Boolean,
    override val reportStatus: Boolean
) : RoutesRemoteLoadAction {

    override val loadState: RoutesLoadState = RoutesLoadState.SUCCESS
    override val source: ScheduleDataSource = ScheduleDataSource.REMOTE_CACHE
}

internal fun shapeRemoteLoadResult(
    result: RemoteLoadResult,
    reportStatus: Boolean,
    notifyUser: Boolean
): RoutesRemoteLoadAction {
    return when (result) {
        is RemoteLoadResult.Decision -> RoutesRemoteLoadDecisionAction(
            decision = result.decision,
            reportStatus = reportStatus
        )

        RemoteLoadResult.NoChanges -> RoutesRemoteLoadNoChangesAction(
            reportStatus = reportStatus
        )

        is RemoteLoadResult.Success -> RoutesRemoteLoadSuccessAction(
            remoteJson = result.remoteJson,
            parsedRoutes = result.parsedRoutes,
            notifyUser = notifyUser,
            reportStatus = reportStatus
        )
    }
}
