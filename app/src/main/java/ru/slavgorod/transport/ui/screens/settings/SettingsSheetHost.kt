package ru.slavgorod.transport.ui.screens.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.slavgorod.transport.R
import ru.slavgorod.transport.logging.UserActionLogger
import ru.slavgorod.transport.ui.viewmodel.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsSheetHost(
    activeSheet: SettingsModalSheet?,
    themeViewModel: ThemeViewModel,
    onCloseSheet: (String) -> Unit
) {
    val aboutSectionTitle = stringResource(R.string.settings_about_title)
    val themeSectionTitle = stringResource(R.string.settings_theme_title)
    val columnsSectionTitle = stringResource(R.string.settings_columns_title)
    val scheduleUpdatesSectionTitle = stringResource(R.string.settings_schedule_updates_title)
    val scheduleNotificationsSectionTitle =
        stringResource(R.string.settings_schedule_notifications_title)
    val scheduleSourceSectionTitle = stringResource(R.string.schedule_source_title)

    when (activeSheet) {
        SettingsModalSheet.THEME -> {
            SettingsBottomSheet(onDismiss = { onCloseSheet(themeSectionTitle) }) {
                DisplaySettingsScreen(
                    themeViewModel = themeViewModel,
                    onBackClick = { onCloseSheet(themeSectionTitle) },
                    showThemeSection = true,
                    showDisplayModeSection = false,
                    showTopBar = false
                )
            }
        }

        SettingsModalSheet.COLUMNS -> {
            SettingsBottomSheet(onDismiss = { onCloseSheet(columnsSectionTitle) }) {
                DisplaySettingsScreen(
                    themeViewModel = themeViewModel,
                    onBackClick = { onCloseSheet(columnsSectionTitle) },
                    showThemeSection = false,
                    showDisplayModeSection = true,
                    showTopBar = false
                )
            }
        }

        SettingsModalSheet.ABOUT -> {
            SettingsBottomSheet(onDismiss = { onCloseSheet(aboutSectionTitle) }) {
                AboutScreen(
                    onBackClick = { onCloseSheet(aboutSectionTitle) },
                    showTopBar = false
                )
            }
        }

        SettingsModalSheet.SCHEDULE_UPDATES -> {
            SettingsBottomSheet(onDismiss = { onCloseSheet(scheduleUpdatesSectionTitle) }) {
                ScheduleUpdateSettingsSection()
            }
        }

        SettingsModalSheet.SCHEDULE_NOTIFICATIONS -> {
            SettingsBottomSheet(onDismiss = { onCloseSheet(scheduleNotificationsSectionTitle) }) {
                ScheduleNotificationsSection()
            }
        }

        SettingsModalSheet.SOURCE_EDITOR -> {
            SettingsBottomSheet(onDismiss = { onCloseSheet(scheduleSourceSectionTitle) }) {
                ScheduleSourceEditorSection()
            }
        }

        SettingsModalSheet.SUPPORT -> Unit
        null -> Unit
    }
}

internal fun closeSettingsSheet(
    sectionTitle: String,
    onClosed: () -> Unit
) {
    UserActionLogger.action(R.string.settings_close_section_format, sectionTitle)
    onClosed()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsBottomSheet(
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Box(
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            content()
        }
    }
}
