package ru.slavgorod.transport.data.repository

import ru.slavgorod.transport.data.model.BusRoute

internal interface RoutesRemoteStateWriter {
    fun updateRoutesState(routes: List<BusRoute>)
    fun cacheSchedulesFromJson(json: String)
    fun setLatestJson(json: String)
    fun publishDataSourceStatus(
        source: ScheduleDataSource,
        lastUpdatedAtMillis: Long?,
        loadState: RoutesLoadState,
        markScheduleUpdated: Boolean
    )

    fun emitScheduleUpdateNotice(notice: ScheduleUpdateNotice)
}
