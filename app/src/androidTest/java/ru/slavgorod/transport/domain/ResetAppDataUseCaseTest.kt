package ru.slavgorod.transport.domain

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import ru.slavgorod.transport.data.local.DisclaimerManager
import ru.slavgorod.transport.data.local.PreferencesKeys
import ru.slavgorod.transport.data.local.ScheduleCacheStore
import ru.slavgorod.transport.data.local.dataStore
import ru.slavgorod.transport.data.local.displayDataStore
import ru.slavgorod.transport.data.local.themeDataStore
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue

@RunWith(AndroidJUnit4::class)
class ResetAppDataUseCaseTest {
    @Test
    fun resetClearsPreferencesDisclaimerAndBothScheduleCopies() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.dataStore.edit { it[PreferencesKeys.scheduleSourceUrl] = "https://example.com" }
        context.themeDataStore.edit { it[PreferencesKeys.appTheme] = "DARK" }
        context.displayDataStore.edit { it[PreferencesKeys.gridColumns] = 4 }
        DisclaimerManager.markDisclaimerAccepted(context)
        val cache = ScheduleCacheStore(context)
        cache.writeSnapshot("{}")
        val manual = File(context.filesDir, "manual_schedule.json")
        manual.writeText("{}")

        ResetAppDataUseCase(context).resetApplicationData()

        assertTrue(context.dataStore.data.first().asMap().isEmpty())
        assertTrue(context.themeDataStore.data.first().asMap().isEmpty())
        assertTrue(context.displayDataStore.data.first().asMap().isEmpty())
        assertTrue(DisclaimerManager.shouldShowDisclaimer(context))
        assertNull(cache.readSnapshot())
        assertFalse(manual.exists())
    }
}
