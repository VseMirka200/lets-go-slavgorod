package ru.slavgorod.transport.ui

import android.content.Context
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import ru.slavgorod.transport.R
import ru.slavgorod.transport.core.Constants
import ru.slavgorod.transport.data.local.PreferencesKeys
import ru.slavgorod.transport.data.local.dataStore
import ru.slavgorod.transport.ui.screens.settings.ResetSettingsScreen
import ru.slavgorod.transport.ui.screens.settings.ResetConfirmationDialog
import ru.slavgorod.transport.ui.screens.settings.ScheduleSourceEditor
import ru.slavgorod.transport.ui.theme.LetsGoSlavgorodTheme

class SettingsBehaviorTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun resetConfirmationInvokesConfirmOnly() {
        var confirmed = 0
        var cancelled = 0
        compose.setContent {
            LetsGoSlavgorodTheme {
                ResetConfirmationDialog("Title", "Warning", "Confirm", "Cancel",
                    onConfirm = { confirmed++ }, onCancel = { cancelled++ })
            }
        }
        compose.onNodeWithText("Confirm").performClick()
        compose.runOnIdle {
            assertEquals(1, confirmed)
            assertEquals(0, cancelled)
        }
    }

    @Test
    fun cancelResetClosesDialogWithoutClearingSettings() {
        val key = PreferencesKeys.scheduleSourceUrl
        val previous = runBlocking { context.dataStore.data.first()[key] }
        try {
            runBlocking { context.dataStore.edit { it[key] = "https://example.com/keep" } }
            compose.setContent { LetsGoSlavgorodTheme { ResetSettingsScreen(onBackClick = {}) } }
            compose.onNodeWithText(context.getString(R.string.settings_reset_action_label)).performScrollTo().performClick()
            compose.onNodeWithText(context.getString(R.string.settings_reset_confirmation_title)).assertExists()
            compose.onNodeWithText(context.getString(R.string.settings_reset_cancel)).performClick()
            compose.onNodeWithText(context.getString(R.string.settings_reset_confirmation_title)).assertDoesNotExist()
            assertEquals("https://example.com/keep", runBlocking { context.dataStore.data.first()[key] })
        } finally {
            runBlocking { context.dataStore.edit { if (previous == null) it.remove(key) else it[key] = previous } }
        }
    }

    @Test
    fun sourceEditorValidatesSavesAndRestoresDefault() {
        val key = PreferencesKeys.scheduleSourceUrl
        val previous = runBlocking { context.dataStore.data.first()[key] }
        try {
            runBlocking { context.dataStore.edit { it.remove(key) } }
            compose.setContent {
                LetsGoSlavgorodTheme {
                    androidx.compose.foundation.layout.Column(Modifier.verticalScroll(rememberScrollState())) {
                        ScheduleSourceEditor()
                    }
                }
            }
            compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)).performClick()
            val field = compose.onNodeWithText(context.getString(R.string.schedule_source_url))
            field.performScrollTo().performTextClearance()
            field.performTextInput("http://example.com")
            compose.onNodeWithText(context.getString(R.string.schedule_source_invalid)).assertExists()
            compose.onNodeWithText(context.getString(R.string.schedule_source_save)).assertIsNotEnabled()
            field.performTextClearance()
            field.performTextInput("https://example.com/routes.json")
            compose.onNodeWithText(context.getString(R.string.schedule_source_save)).performScrollTo().performClick()
            compose.waitUntil(5_000) { runBlocking { context.dataStore.data.first()[key] } == "https://example.com/routes.json" }
            compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)).performClick()
            compose.onNodeWithText(context.getString(R.string.schedule_source_restore_default)).performScrollTo().performClick()
            compose.onNodeWithText(context.getString(R.string.schedule_source_save)).performScrollTo().performClick()
            compose.waitUntil(5_000) { runBlocking { context.dataStore.data.first()[key] } == Constants.REMOTE_JSON_URL }
        } finally {
            runBlocking { context.dataStore.edit { if (previous == null) it.remove(key) else it[key] = previous } }
        }
    }
}
