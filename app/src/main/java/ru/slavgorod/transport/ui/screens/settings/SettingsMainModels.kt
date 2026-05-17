package ru.slavgorod.transport.ui.screens.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
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
    SCHEDULE_UPDATES,
    SCHEDULE_NOTIFICATIONS,
    SOURCE_EDITOR
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
            title = stringResource(R.string.settings_schedule_updates_title),
            subtitle = stringResource(R.string.settings_schedule_updates_subtitle),
            modalSheet = SettingsModalSheet.SCHEDULE_UPDATES
        ),
        SettingsEntry(
            icon = Icons.Default.Notifications,
            title = stringResource(R.string.settings_schedule_notifications_title),
            subtitle = stringResource(R.string.settings_schedule_notifications_subtitle),
            modalSheet = SettingsModalSheet.SCHEDULE_NOTIFICATIONS
        ),
        SettingsEntry(
            icon = Icons.Default.Edit,
            title = stringResource(R.string.schedule_source_title),
            subtitle = stringResource(R.string.settings_schedule_source_subtitle),
            modalSheet = SettingsModalSheet.SOURCE_EDITOR
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
