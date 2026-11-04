package ru.slavgorod.transport.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ru.slavgorod.transport.core.Constants
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class ScheduleSourceSettingsTest {
    @Test
    fun `saved address is available to a new settings instance and reset restores default`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val settings = ScheduleSourceSettings(context.dataStore)
        try {
            settings.reset()
            assertEquals(Constants.REMOTE_JSON_URL, settings.currentUrl())

            settings.save("  https://script.google.com/macros/s/example/exec  ")
            assertEquals(
                "https://script.google.com/macros/s/example/exec",
                ScheduleSourceSettings(context.dataStore).currentUrl()
            )

            settings.reset()
            assertEquals(Constants.REMOTE_JSON_URL, settings.currentUrl())
        } finally {
            settings.reset()
        }
    }

    @Test
    fun `address requires https without credentials or fragment`() {
        assertTrue(isValidScheduleSourceUrl("https://script.google.com/macros/s/example/exec"))
        assertFalse(isValidScheduleSourceUrl("http://example.com/schedule.json"))
        assertFalse(isValidScheduleSourceUrl("https://user:password@example.com/schedule.json"))
        assertFalse(isValidScheduleSourceUrl("https://example.com/schedule.json#fragment"))
        assertFalse(isValidScheduleSourceUrl("не ссылка"))
    }
}
