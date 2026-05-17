package ru.slavgorod.transport.data.repository

import ru.slavgorod.transport.data.model.BusRoute
import timber.log.Timber

internal sealed interface RemoteLoadResult {
    data class Decision(val decision: RoutesLoadDecision) : RemoteLoadResult
    data object NoChanges : RemoteLoadResult
    data class Success(
        val remoteJson: String,
        val parsedRoutes: List<BusRoute>
    ) : RemoteLoadResult
}

internal class RoutesRemoteLoadCoordinator(
    private val remoteScheduleFetcher: ScheduleFetcher,
    private val scheduleJsonParser: ScheduleJsonParser,
    private val isOnline: () -> Boolean
) {

    suspend fun decide(
        latestJson: String?,
        hasCachedData: Boolean
    ): RemoteLoadResult {
        if (!isOnline()) {
            return RemoteLoadResult.Decision(RoutesLoadDecisions.offline(hasCachedData))
        }

        val remoteJson = remoteScheduleFetcher.fetchWithRetry()
        if (remoteJson.isNullOrBlank()) {
            return RemoteLoadResult.Decision(RoutesLoadDecisions.networkError(hasCachedData))
        }

        val parsedRoutes = scheduleJsonParser.parseRoutes(remoteJson)
        if (parsedRoutes.isEmpty()) {
            Timber.w("Remote table JSON is empty or invalid, update skipped")
            return RemoteLoadResult.Decision(RoutesLoadDecisions.invalidJson(hasCachedData))
        }

        if (remoteJson == latestJson) {
            return RemoteLoadResult.NoChanges
        }

        return RemoteLoadResult.Success(
            remoteJson = remoteJson,
            parsedRoutes = parsedRoutes
        )
    }
}
