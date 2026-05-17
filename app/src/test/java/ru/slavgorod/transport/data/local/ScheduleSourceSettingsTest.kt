package ru.slavgorod.transport.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import ru.slavgorod.transport.core.ScheduleConfig
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleSourceSettingsTest {

    @Test
    fun `remote url persists and resets to default`() = runTest {
        val dataStoreFile =
            Files.createTempFile("schedule_source_settings_", ".preferences_pb").toFile()
        val dataStore = createPreferencesDataStore(dataStoreFile, backgroundScope)
        val settings = ScheduleSourceSettings(dataStore)
        val customUrl = "https://example.com/routes.json"

        try {
            settings.setRemoteJsonUrl(customUrl)

            assertEquals(customUrl, readRemoteUrl(dataStore))

            settings.resetRemoteJsonUrl()

            assertEquals(ScheduleConfig.remoteJsonUrl, readRemoteUrl(dataStore))
        } finally {
            dataStoreFile.delete()
        }
    }

    private suspend fun readRemoteUrl(dataStore: DataStore<Preferences>): String {
        return dataStore.data.first()[PreferencesKeys.remoteJsonUrl] ?: ScheduleConfig.remoteJsonUrl
    }

    private fun createPreferencesDataStore(
        file: File,
        scope: CoroutineScope
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { file }
        )
    }
}
