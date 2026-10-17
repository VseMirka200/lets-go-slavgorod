package ru.slavgorod.transport.data.repository

import ru.slavgorod.transport.data.model.BusRoute

internal class RoutesRemoteStateWriterAdapter(
    private val updateRoutesState: (List<BusRoute>) -> Unit,
    private val cacheSchedulesFromJson: (String) -> Unit,
    private val setLatestJson: (String) -> Unit,
    private val publishDataSourceStatus: (
        source: ScheduleDataSource,
        lastUpdatedAtMillis: Long?,
        loadState: RoutesLoadState,
        markScheduleUpdated: Boolean
    ) -> Unit,
    private val emitScheduleUpdateNotice: (ScheduleUpdateNotice) -> Unit
) : RoutesRemoteStateWriter {

    override fun updateRoutesState(routes: List<BusRoute>) {
        updateRoutesState.invoke(routes)
    }

    override fun cacheSchedulesFromJson(json: String) {
        cacheSchedulesFromJson.invoke(json)
    }

    override fun setLatestJson(json: String) {
        setLatestJson.invoke(json)
    }

    override fun publishDataSourceStatus(
        source: ScheduleDataSource,
        lastUpdatedAtMillis: Long?,
        loadState: RoutesLoadState,
        markScheduleUpdated: Boolean
    ) {
        publishDataSourceStatus.invoke(
            source,
            lastUpdatedAtMillis,
            loadState,
            markScheduleUpdated
        )
    }

    override fun emitScheduleUpdateNotice(notice: ScheduleUpdateNotice) {
        emitScheduleUpdateNotice.invoke(notice)
    }
}
