package ru.slavgorod.transport.ui.screens.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import ru.slavgorod.transport.R

internal enum class SettingsModalSheet {
    THEME,
    COLUMNS,
    ABOUT,
    SUPPORT,
    SCHEDULE,
    SCHEDULE_UPDATES,
    SCHEDULE_NOTIFICATIONS,
    SCHEDULE_SOURCE,
    FEEDBACK
}

internal data class SettingsEntry(
    val icon: ImageVector,
    val title: String,
    val subtitle: String? = null,
    val modalSheet: SettingsModalSheet? = null
)

@Composable
internal fun buildSettingsEntries(): List<SettingsEntry> {
    return listOf(
        SettingsEntry(
            icon = Icons.Default.Palette,
            title = stringResource(R.string.settings_theme_title),
            subtitle = stringResource(R.string.settings_theme_subtitle),
            modalSheet = SettingsModalSheet.THEME
        ),
        SettingsEntry(
            icon = Icons.Default.ViewModule,
            title = stringResource(R.string.settings_columns_title),
            subtitle = stringResource(R.string.settings_columns_subtitle),
            modalSheet = SettingsModalSheet.COLUMNS
        ),
        SettingsEntry(
            icon = Icons.Default.SystemUpdateAlt,
            title = stringResource(R.string.settings_schedule_category),
            subtitle = stringResource(R.string.settings_schedule_category_description),
            modalSheet = SettingsModalSheet.SCHEDULE
        ),
        SettingsEntry(
            icon = Icons.Default.Feedback,
            title = stringResource(R.string.settings_feedback_title),
            subtitle = stringResource(R.string.settings_feedback_subtitle),
            modalSheet = SettingsModalSheet.FEEDBACK
        ),
        SettingsEntry(
            icon = Icons.Default.Info,
            title = stringResource(R.string.settings_about_title),
            subtitle = stringResource(R.string.settings_about_subtitle),
            modalSheet = SettingsModalSheet.ABOUT
        ),
        SettingsEntry(
            icon = Icons.Default.Payments,
            title = stringResource(R.string.settings_support_title),
            subtitle = stringResource(R.string.settings_support_subtitle),
            modalSheet = SettingsModalSheet.SUPPORT
        )
    )
}
