package ru.slavgorod.transport.notifications

import android.Manifest
import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import ru.slavgorod.transport.data.local.CachedScheduleSnapshot
import ru.slavgorod.transport.data.local.PreferencesKeys
import ru.slavgorod.transport.data.local.ScheduleNotificationSettings
import ru.slavgorod.transport.data.repository.RoutesTableDataSource
import ru.slavgorod.transport.data.repository.ScheduleFetcher
import ru.slavgorod.transport.data.repository.ScheduleSnapshotStore
import ru.slavgorod.transport.data.repository.ScheduleUpdateNotice
import ru.slavgorod.transport.data.repository.RoutesLoadState
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ScheduleUpdateNotificationCoordinatorTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val sent = mutableListOf<ScheduleUpdateNotice>()

    @Test fun `background update with permission sends notification`() = runTest {
        val settings = exercise(foreground = false, enabled = true, permission = true)
        assertEquals(1, sent.size)
        assertTrue(settings.isEnabledNow())
    }

    @Test fun `foreground update does not send notification`() = runTest {
        exercise(foreground = true, enabled = true, permission = true)
        assertEquals(0, sent.size)
    }

    @Test fun `disabled notifications are not sent`() = runTest {
        exercise(foreground = false, enabled = false, permission = true)
        assertEquals(0, sent.size)
    }

    @Test fun `missing permission disables setting without sending`() = runTest {
        val settings = exercise(foreground = false, enabled = true, permission = false)
        assertEquals(0, sent.size)
        assertFalse(settings.isEnabledNow())
    }

    @Test fun `unchanged schedule does not send another notification`() = runTest {
        exercise(foreground = false, enabled = true, permission = true, repeatRefresh = true)
        assertEquals(1, sent.size)
    }

    private suspend fun TestScope.exercise(
        foreground: Boolean,
        enabled: Boolean,
        permission: Boolean,
        repeatRefresh: Boolean = false
    ): ScheduleNotificationSettings {
        val application = shadowOf(app)
        if (permission) application.grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        else application.denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val store = MemoryStore(enabled)
        val settings = ScheduleNotificationSettings(store, backgroundScope)
        val tracker = AppForegroundTracker().apply { setForegroundForTesting(foreground) }
        val json = """{"routes":[{"id":"1","routeNumber":"1","name":"Route","description":"Route","color":"#123456","schedules":[]}]}"""
        val fetcher = object : ScheduleFetcher {
            override fun isOnline() = true
            override suspend fun fetchWithRetry() = json
        }
        val snapshot = object : ScheduleSnapshotStore {
            override suspend fun readSnapshot(): CachedScheduleSnapshot? = null
            override suspend fun writeSnapshot(json: String) = Unit
            override suspend fun clear() = Unit
            override suspend fun hasSnapshot() = false
            override fun formatSavedAt(timestampMillis: Long) = ""
        }
        val repository = RoutesTableDataSource(app, remoteScheduleFetcher = fetcher,
            scheduleSnapshotRepository = snapshot, autoLoadOnInit = false)
        ScheduleUpdateNotificationCoordinator(app, repository, tracker, settings, backgroundScope) { sent.add(it) }.start()
        runCurrent()
        assertEquals(RoutesLoadState.SUCCESS, repository.refreshRoutesFromLocal(notifyUser = true))
        runCurrent()
        if (repeatRefresh) {
            assertEquals(RoutesLoadState.NO_CHANGES, repository.refreshRoutesFromLocal(notifyUser = true))
            runCurrent()
        }
        advanceUntilIdle()
        return settings
    }

    private class MemoryStore(enabled: Boolean) : DataStore<Preferences> {
        override val data = MutableStateFlow<Preferences>(preferencesOf(PreferencesKeys.scheduleNotificationsEnabled to enabled))
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
            transform(data.value).also { data.value = it }
    }
}
