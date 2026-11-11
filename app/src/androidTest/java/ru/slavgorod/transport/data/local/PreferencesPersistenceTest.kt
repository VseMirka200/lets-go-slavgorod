package ru.slavgorod.transport.data.local

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import ru.slavgorod.transport.core.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

@RunWith(AndroidJUnit4::class)
class PreferencesPersistenceTest {
    @get:Rule val folder = TemporaryFolder()

    @Test
    fun settingsSurviveReopeningAndReturnToDefaultsAfterClearing() = runBlocking {
        val file = folder.root.resolve("settings.preferences_pb")
        val firstJob = SupervisorJob()
        val first = PreferenceDataStoreFactory.create(scope = CoroutineScope(firstJob + Dispatchers.IO)) { file }
        val source = ScheduleSourceSettings(first)
        val notifications = ScheduleNotificationSettings(first, CoroutineScope(firstJob + Dispatchers.IO))
        assertEquals(Constants.REMOTE_JSON_URL, source.currentUrl())
        assertFalse(notifications.isEnabledNow())
        source.saveUrl("https://example.com/routes.json")
        first.edit {
            it[PreferencesKeys.scheduleNotificationsEnabled] = true
            it[PreferencesKeys.appTheme] = "DARK"
            it[PreferencesKeys.gridColumns] = 3
            it[PreferencesKeys.scheduleDaySelectionMode] = "DAY_FIRST"
        }
        firstJob.cancelAndJoin()

        val secondJob = SupervisorJob()
        try {
            val second = PreferenceDataStoreFactory.create(scope = CoroutineScope(secondJob + Dispatchers.IO)) { file }
            assertEquals("https://example.com/routes.json", ScheduleSourceSettings(second).currentUrl())
            assertTrue(ScheduleNotificationSettings(second, CoroutineScope(secondJob)).isEnabledNow())
            val restored = second.data.first()
            assertEquals("DARK", restored[PreferencesKeys.appTheme])
            assertEquals(3, restored[PreferencesKeys.gridColumns])
            assertEquals("DAY_FIRST", restored[PreferencesKeys.scheduleDaySelectionMode])
            second.edit { it.clear() }
            assertEquals(Constants.REMOTE_JSON_URL, ScheduleSourceSettings(second).currentUrl())
            assertFalse(ScheduleNotificationSettings(second, CoroutineScope(secondJob)).isEnabledNow())
            assertTrue(second.data.first().asMap().isEmpty())
        } finally {
            secondJob.cancelAndJoin()
        }
    }
}
