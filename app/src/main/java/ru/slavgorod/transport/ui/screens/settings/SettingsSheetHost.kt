package ru.slavgorod.transport.ui.screens.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.slavgorod.transport.R
import ru.slavgorod.transport.logging.UserActionLogger
import ru.slavgorod.transport.ui.components.app.AppBackButton
import ru.slavgorod.transport.ui.components.settings.SettingsMenuRow
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
    val scheduleUpdatesSectionTitle = stringResource(R.string.settings_schedule_management_title)
    val scheduleNotificationsSectionTitle =
        stringResource(R.string.settings_schedule_notifications_title)
    val sourceSectionTitle = stringResource(R.string.settings_source_title)

    var schedulePage by rememberSaveable(activeSheet) { mutableStateOf<SettingsModalSheet?>(null) }

    when (activeSheet) {
        SettingsModalSheet.SCHEDULE -> {
            SettingsBottomSheet(onDismiss = { onCloseSheet("Расписание") }) {
              AnimatedContent(
                targetState = schedulePage,
                transitionSpec = {
                    val opening = targetState != null
                    (slideInVertically(tween(300)) { height -> if (opening) height else -height / 4 } +
                        fadeIn(tween(300))) togetherWith
                        (slideOutVertically(tween(250)) { height -> if (opening) -height / 4 else height } +
                            fadeOut(tween(200)))
                },
                label = "schedule settings navigation"
              ) { page ->
               Column {
                if (page != null) {
                    SettingsBackButton(onClick = { schedulePage = null })
                }
                SettingsSectionTitle(when (page) {
                    SettingsModalSheet.SCHEDULE_UPDATES, SettingsModalSheet.SCHEDULE_SOURCE -> scheduleUpdatesSectionTitle
                    SettingsModalSheet.SCHEDULE_NOTIFICATIONS -> scheduleNotificationsSectionTitle
                    else -> stringResource(R.string.settings_schedule_category)
                })
                when (page) {
                    SettingsModalSheet.SCHEDULE_UPDATES -> ScheduleSourceSettingsSection()
                    SettingsModalSheet.SCHEDULE_SOURCE -> ScheduleSourceSettingsSection()
                    SettingsModalSheet.SCHEDULE_NOTIFICATIONS -> ScheduleNotificationsSection(topPadding = 0.dp)
                    else -> Column(Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 0.dp)) {
                        SettingsMenuRow(title = scheduleUpdatesSectionTitle, subtitle = stringResource(R.string.settings_schedule_updates_subtitle), icon = Icons.Default.SystemUpdateAlt, onClick = { schedulePage = SettingsModalSheet.SCHEDULE_UPDATES })
                        SettingsMenuRow(title = scheduleNotificationsSectionTitle, subtitle = stringResource(R.string.settings_schedule_notifications_subtitle), icon = Icons.Default.Notifications, onClick = { schedulePage = SettingsModalSheet.SCHEDULE_NOTIFICATIONS })
                    }
                }
               }
              }
              if (schedulePage != null) {
                  androidx.activity.compose.BackHandler { schedulePage = null }
              }
            }
        }
        SettingsModalSheet.THEME -> {
            SettingsBottomSheet(onDismiss = { onCloseSheet(themeSectionTitle) }) {
              SettingsTitledContent(themeSectionTitle) {
                DisplaySettingsScreen(
                    themeViewModel = themeViewModel,
                    onBackClick = { onCloseSheet(themeSectionTitle) },
                    showThemeSection = true,
                    showDisplayModeSection = false,
                    showTopBar = false,
                    scrollEnabled = true
                )
              }
            }
        }

        SettingsModalSheet.COLUMNS -> {
            SettingsBottomSheet(onDismiss = { onCloseSheet(columnsSectionTitle) }) {
              SettingsTitledContent(columnsSectionTitle) {
                DisplaySettingsScreen(
                    themeViewModel = themeViewModel,
                    onBackClick = { onCloseSheet(columnsSectionTitle) },
                    showThemeSection = false,
                    showDisplayModeSection = true,
                    showTopBar = false,
                    scrollEnabled = true
                )
              }
            }
        }

        SettingsModalSheet.ABOUT -> {
            SettingsBottomSheet(onDismiss = { onCloseSheet(aboutSectionTitle) }) {
              SettingsTitledContent(aboutSectionTitle) {
                AboutScreen(
                    onBackClick = { onCloseSheet(aboutSectionTitle) },
                    showTopBar = false,
                    scrollEnabled = true
                )
              }
            }
        }

        SettingsModalSheet.SCHEDULE_UPDATES -> {
            SettingsBottomSheet(onDismiss = { onCloseSheet(scheduleUpdatesSectionTitle) }) {
                SettingsTitledContent(scheduleUpdatesSectionTitle) { ScheduleSourceSettingsSection() }
            }
        }

        SettingsModalSheet.SCHEDULE_NOTIFICATIONS -> {
            SettingsBottomSheet(onDismiss = { onCloseSheet(scheduleNotificationsSectionTitle) }) {
                SettingsTitledContent(scheduleNotificationsSectionTitle) { ScheduleNotificationsSection(topPadding = 0.dp) }
            }
        }

        SettingsModalSheet.SCHEDULE_SOURCE -> {
            SettingsBottomSheet(onDismiss = { onCloseSheet(sourceSectionTitle) }) {
                SettingsTitledContent(scheduleUpdatesSectionTitle) { ScheduleSourceSettingsSection() }
            }
        }

        null -> Unit
    }
}

@Composable
internal fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 8.dp),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun SettingsTitledContent(title: String, content: @Composable () -> Unit) {
    Column {
        SettingsSectionTitle(title)
        content()
    }
}

@Composable
internal fun SettingsBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    AppBackButton(onClick = onClick, modifier = modifier.padding(start = 12.dp))
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
