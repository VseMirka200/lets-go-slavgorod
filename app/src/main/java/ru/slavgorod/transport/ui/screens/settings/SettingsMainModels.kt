package ru.slavgorod.transport.ui.screens.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Info
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
    SCHEDULE,
    SCHEDULE_UPDATES,
    SCHEDULE_NOTIFICATIONS,
    SCHEDULE_SOURCE
}

/** Действие пункта настроек: внутренний раздел приложения или страница сайта. */
internal sealed interface SettingsDestination {
    data class Sheet(val sheet: SettingsModalSheet) : SettingsDestination
    data class Website(val url: String) : SettingsDestination
}

internal data class SettingsEntry(
    val icon: ImageVector,
    val title: String,
    val subtitle: String? = null,
    val destination: SettingsDestination
)

/** Формирует пункты настроек; обратная связь и поддержка открываются на сайте проекта. */
@Composable
internal fun buildSettingsEntries(): List<SettingsEntry> {
    return listOf(
        SettingsEntry(
            icon = Icons.Default.Palette,
            title = stringResource(R.string.settings_theme_title),
            subtitle = stringResource(R.string.settings_theme_subtitle),
            destination = SettingsDestination.Sheet(SettingsModalSheet.THEME)
        ),
        SettingsEntry(
            icon = Icons.Default.ViewModule,
            title = stringResource(R.string.settings_columns_title),
            subtitle = stringResource(R.string.settings_columns_subtitle),
            destination = SettingsDestination.Sheet(SettingsModalSheet.COLUMNS)
        ),
        SettingsEntry(
            icon = Icons.Default.SystemUpdateAlt,
            title = stringResource(R.string.settings_schedule_category),
            subtitle = stringResource(R.string.settings_schedule_category_description),
            destination = SettingsDestination.Sheet(SettingsModalSheet.SCHEDULE)
        ),
        SettingsEntry(
            icon = Icons.Default.Feedback,
            title = stringResource(R.string.settings_feedback_title),
            subtitle = stringResource(R.string.settings_feedback_subtitle),
            destination = SettingsDestination.Website("https://vsemirka200.github.io/lets-go-slavgorod/feedback.html")
        ),
        SettingsEntry(
            icon = Icons.Default.Info,
            title = stringResource(R.string.settings_about_title),
            subtitle = stringResource(R.string.settings_about_subtitle),
            destination = SettingsDestination.Sheet(SettingsModalSheet.ABOUT)
        ),
        SettingsEntry(
            icon = Icons.Default.Payments,
            title = stringResource(R.string.settings_support_title),
            subtitle = stringResource(R.string.settings_support_subtitle),
            destination = SettingsDestination.Website("https://vsemirka200.github.io/lets-go-slavgorod/#support")
        )
    )
}
