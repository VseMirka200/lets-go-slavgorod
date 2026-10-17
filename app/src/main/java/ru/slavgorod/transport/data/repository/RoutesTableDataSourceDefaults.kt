package ru.slavgorod.transport.data.repository

import android.content.Context
import okhttp3.OkHttpClient
import ru.slavgorod.transport.core.Constants
import ru.slavgorod.transport.data.local.JsonDataSource
import ru.slavgorod.transport.data.local.ScheduleCacheStore
import ru.slavgorod.transport.data.network.NetworkMonitor
import java.util.concurrent.TimeUnit

internal object RoutesTableDataSourceDefaults {
    fun onlineChecker(context: Context): () -> Boolean = {
        NetworkMonitor.isConnected(context.applicationContext)
    }

    fun scheduleCacheStore(context: Context): ScheduleCacheStore {
        return ScheduleCacheStore(context.applicationContext)
    }

    fun httpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(Constants.REMOTE_CONNECTION_TIMEOUT.toLong(), TimeUnit.MILLISECONDS)
            .readTimeout(Constants.REMOTE_READ_TIMEOUT.toLong(), TimeUnit.MILLISECONDS)
            .build()
    }

    fun remoteScheduleFetcher(
        onlineChecker: () -> Boolean,
        httpClient: OkHttpClient
    ): ScheduleFetcher {
        return RemoteScheduleFetcher(
            onlineChecker = onlineChecker,
            httpClient = httpClient
        )
    }

    fun scheduleSnapshotRepository(scheduleCacheStore: ScheduleCacheStore): ScheduleSnapshotStore {
        return ScheduleSnapshotRepository(scheduleCacheStore)
    }

    fun scheduleJsonParser(jsonDataSource: JsonDataSource): ScheduleJsonParser {
        return ScheduleJsonParser(jsonDataSource)
    }
}
