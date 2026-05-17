package ru.slavgorod.transport.data.repository

import ru.slavgorod.transport.R
import ru.slavgorod.transport.logging.UserActionLogger
import timber.log.Timber

internal class RoutesRemoteLoadEffectHandler(
    private val scheduleSnapshotRepository: ScheduleSnapshotStore,
    private val stateWriter: RoutesRemoteStateWriter
) {

    suspend fun apply(
        action: RoutesRemoteLoadAction,
        reportStatus: Boolean,
        notifyUser: Boolean
    ): RoutesLoadState {
        return when (action) {
            is RoutesRemoteLoadDecisionAction -> {
                reportLoadState(
                    loadState = action.loadState,
                    reportStatus = reportStatus,
                    source = action.source,
                    lastUpdatedAtMillis = null,
                    markScheduleUpdated = false
                )
                action.loadState
            }

            is RoutesRemoteLoadNoChangesAction -> {
                val updatedAtMillis = System.currentTimeMillis()
                reportLoadState(
                    loadState = action.loadState,
                    reportStatus = reportStatus,
                    source = action.source,
                    lastUpdatedAtMillis = updatedAtMillis,
                    markScheduleUpdated = false
                )
                action.loadState
            }

            is RoutesRemoteLoadSuccessAction -> persistRemoteRoutes(
                action = action,
                notifyUser = notifyUser
            )
        }
    }

    private suspend fun persistRemoteRoutes(
        action: RoutesRemoteLoadSuccessAction,
        notifyUser: Boolean
    ): RoutesLoadState {
        val updatedAtMillis = System.currentTimeMillis()

        stateWriter.setLatestJson(action.remoteJson)
        stateWriter.updateRoutesState(action.parsedRoutes)
        stateWriter.cacheSchedulesFromJson(action.remoteJson)
        runCatching { scheduleSnapshotRepository.writeSnapshot(action.remoteJson) }
            .onSuccess {
                UserActionLogger.scheduleCacheSaved(action.parsedRoutes.size)
            }
            .onFailure { exception ->
                Timber.w(exception, "Failed to persist schedule cache")
            }

        stateWriter.publishDataSourceStatus(
            ScheduleDataSource.REMOTE_CACHE,
            updatedAtMillis,
            RoutesLoadState.SUCCESS,
            true
        )

        if (notifyUser) {
            UserActionLogger.scheduleUpdateResult(R.string.log_state_success)
            stateWriter.emitScheduleUpdateNotice(buildScheduleUpdateNotice(updatedAtMillis))
        }

        return action.loadState
    }

    private fun reportLoadState(
        loadState: RoutesLoadState,
        reportStatus: Boolean,
        source: ScheduleDataSource,
        lastUpdatedAtMillis: Long?,
        markScheduleUpdated: Boolean
    ) {
        UserActionLogger.scheduleUpdateResult(RoutesLoadDecisions.updateLogMessageResId(loadState))

        if (!reportStatus) return

        stateWriter.publishDataSourceStatus(
            source,
            lastUpdatedAtMillis,
            loadState,
            markScheduleUpdated
        )
    }
}
