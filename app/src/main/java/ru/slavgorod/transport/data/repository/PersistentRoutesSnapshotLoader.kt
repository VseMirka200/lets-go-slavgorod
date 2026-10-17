package ru.slavgorod.transport.data.repository

import ru.slavgorod.transport.data.model.BusRoute
import ru.slavgorod.transport.data.model.BusSchedule
import ru.slavgorod.transport.logging.UserActionLogger
import timber.log.Timber

internal data class PersistentRoutesSnapshot(
    val json: String,
    val savedAtMillis: Long,
    val routes: List<BusRoute>,
    val schedulesByRoute: Map<String, List<BusSchedule>>
)

internal class PersistentRoutesSnapshotLoader(
    private val scheduleSnapshotRepository: ScheduleSnapshotStore,
    private val scheduleJsonParser: ScheduleJsonParser
) {

    suspend fun load(): PersistentRoutesSnapshot? {
        val snapshot = scheduleSnapshotRepository.readSnapshot() ?: return null
        val parsedRoutes = scheduleJsonParser.parseRoutes(snapshot.json)
        if (parsedRoutes.isEmpty()) {
            Timber.w("Saved schedule cache is empty or invalid")
            return null
        }

        val formattedSavedAt = scheduleSnapshotRepository.formatSavedAt(snapshot.savedAtMillis)
        UserActionLogger.scheduleCacheLoaded(formattedSavedAt)
        return PersistentRoutesSnapshot(
            json = snapshot.json,
            savedAtMillis = snapshot.savedAtMillis,
            routes = parsedRoutes,
            schedulesByRoute = scheduleJsonParser.buildSchedulesIndex(snapshot.json)
        )
    }
}
