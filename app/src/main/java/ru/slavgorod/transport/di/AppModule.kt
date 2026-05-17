package ru.slavgorod.transport.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import ru.slavgorod.transport.core.ApplicationCoroutineScope
import ru.slavgorod.transport.data.local.AppLogStore
import ru.slavgorod.transport.data.local.JsonDataSource
import ru.slavgorod.transport.data.local.ScheduleCacheStore
import ru.slavgorod.transport.data.local.ScheduleNotificationSettings
import ru.slavgorod.transport.data.local.ScheduleSourceSettings
import ru.slavgorod.transport.data.local.dataStore
import ru.slavgorod.transport.data.local.displayDataStore
import ru.slavgorod.transport.data.local.themeDataStore
import ru.slavgorod.transport.data.network.NetworkMonitor
import ru.slavgorod.transport.data.repository.RemoteScheduleFetcher
import ru.slavgorod.transport.data.repository.RoutesAutoRefreshManager
import ru.slavgorod.transport.data.repository.RoutesTableDataSource
import ru.slavgorod.transport.data.repository.ScheduleFetcher
import ru.slavgorod.transport.data.repository.ScheduleJsonParser
import ru.slavgorod.transport.data.repository.ScheduleSnapshotRepository
import ru.slavgorod.transport.data.repository.ScheduleSnapshotStore
import ru.slavgorod.transport.domain.ResetAppDataUseCase
import ru.slavgorod.transport.notifications.AppForegroundTracker
import ru.slavgorod.transport.notifications.ScheduleUpdateNotificationCoordinator
import ru.slavgorod.transport.ui.viewmodel.DisplaySettingsViewModel
import ru.slavgorod.transport.ui.viewmodel.RoutesViewModel
import ru.slavgorod.transport.ui.viewmodel.ThemeViewModel

val appModule = module {
    single { ApplicationCoroutineScope(CoroutineScope(SupervisorJob() + Dispatchers.IO)) }
    single { AppLogStore(androidContext()) }
    single { JsonDataSource() }
    single { ScheduleCacheStore(androidContext()) }
    single {
        ScheduleSourceSettings(
            androidContext().dataStore,
            get<ApplicationCoroutineScope>().scope
        )
    }
    single {
        ScheduleNotificationSettings(
            androidContext().dataStore,
            get<ApplicationCoroutineScope>().scope
        )
    }
    single { ScheduleJsonParser(get()) }
    single<ScheduleSnapshotStore> { ScheduleSnapshotRepository(get()) }
    single<ScheduleFetcher> {
        val context = androidContext()
        val scheduleSourceSettings: ScheduleSourceSettings = get()
        RemoteScheduleFetcher(
            remoteJsonUrlProvider = { scheduleSourceSettings.getRemoteJsonUrl() },
            onlineChecker = { NetworkMonitor.isConnected(context) }
        )
    }
    single {
        RoutesTableDataSource(
            context = androidContext(),
            jsonDataSource = get(),
            scheduleCacheStore = get(),
            remoteScheduleFetcher = get(),
            scheduleSnapshotRepository = get(),
            scheduleJsonParser = get(),
            externalScope = get<ApplicationCoroutineScope>().scope
        )
    }
    single {
        val context = androidContext()
        RoutesAutoRefreshManager(
            routeRepository = get(),
            connectionStateFlow = NetworkMonitor.observeConnectionState(context),
            initialConnectionState = NetworkMonitor.isConnected(context),
            externalScope = get<ApplicationCoroutineScope>().scope
        )
    }
    single { AppForegroundTracker() }
    single {
        ScheduleUpdateNotificationCoordinator(
            context = androidContext(),
            routeRepository = get(),
            appForegroundTracker = get(),
            notificationSettings = get(),
            scope = get<ApplicationCoroutineScope>().scope
        )
    }
    single { ResetAppDataUseCase(androidContext()) }

    viewModel { RoutesViewModel(get(), get(), androidContext().displayDataStore) }
    viewModel { ThemeViewModel(androidContext().themeDataStore) }
    viewModel { DisplaySettingsViewModel(androidContext().displayDataStore) }
}
