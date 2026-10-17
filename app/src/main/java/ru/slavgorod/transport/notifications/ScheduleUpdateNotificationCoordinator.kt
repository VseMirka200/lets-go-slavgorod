package ru.slavgorod.transport.notifications

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.slavgorod.transport.data.local.ScheduleNotificationSettings
import ru.slavgorod.transport.data.repository.RoutesTableDataSource
import ru.slavgorod.transport.data.repository.ScheduleUpdateNotice

class ScheduleUpdateNotificationCoordinator(
    context: Context,
    private val routeRepository: RoutesTableDataSource,
    private val appForegroundTracker: AppForegroundTracker,
    private val notificationSettings: ScheduleNotificationSettings,
    private val scope: CoroutineScope
) {

    private val appContext = context.applicationContext
    private val sender = ScheduleUpdateNotificationSender(appContext)

    fun start() {
        scope.launch {
            routeRepository.scheduleUpdateNotices.collectLatest { notice ->
                handleNotice(notice)
            }
        }
    }

    private suspend fun handleNotice(notice: ScheduleUpdateNotice) {
        if (appForegroundTracker.isAppForeground.value) return
        if (!notificationSettings.isEnabledNow()) return

        if (!appContext.hasPostNotificationsPermission()) {
            notificationSettings.setEnabled(false)
            return
        }

        sender.show(notice)
    }
}
