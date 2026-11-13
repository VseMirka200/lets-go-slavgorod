package ru.slavgorod.transport.data.local

import org.junit.Test
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import ru.slavgorod.transport.core.Constants
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScheduleSourceSettingsTest {
    @Test
    fun `source accepts only valid https urls without credentials`() {
        assertTrue(isValidScheduleSourceUrl("https://example.com/schedule.json"))
        assertFalse(isValidScheduleSourceUrl("http://example.com/schedule.json"))
        assertFalse(isValidScheduleSourceUrl("https://user:password@example.com/"))
        assertFalse(isValidScheduleSourceUrl("not a url"))
        assertFalse(isValidScheduleSourceUrl(""))
        assertFalse(isValidScheduleSourceUrl("https://example.com:99999/"))
    }

    @Test
    fun `saved source is trimmed and shared by new settings instance`() = runTest {
        val store = MemoryPreferencesStore()
        val settings = ScheduleSourceSettings(store)
        assertEquals(Constants.REMOTE_JSON_URL, settings.currentUrl())

        settings.saveUrl("  https://example.com/schedule.json  ")
        assertEquals("https://example.com/schedule.json", ScheduleSourceSettings(store).currentUrl())

        store.edit { it.clear() }
        assertEquals(Constants.REMOTE_JSON_URL, settings.currentUrl())
    }

    @Test
    fun `invalid source does not replace saved source`() = runTest {
        val settings = ScheduleSourceSettings(MemoryPreferencesStore())
        settings.saveUrl("https://example.com/schedule.json")
        assertFailsWith<IllegalArgumentException> { settings.saveUrl("http://example.com/") }
        assertEquals("https://example.com/schedule.json", settings.currentUrl())
    }

    private class MemoryPreferencesStore : DataStore<Preferences> {
        override val data = MutableStateFlow<Preferences>(emptyPreferences())
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            return transform(data.value).also { data.value = it }
        }
    }
}
