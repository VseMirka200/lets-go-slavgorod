package ru.slavgorod.transport.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.slavgorod.transport.R
import ru.slavgorod.transport.core.Constants
import ru.slavgorod.transport.data.local.AppLogStore
import ru.slavgorod.transport.domain.ResetAppDataUseCase
import ru.slavgorod.transport.logging.UserActionLogger
import ru.slavgorod.transport.ui.components.settings.SettingsMenuRow
import ru.slavgorod.transport.ui.components.settings.SettingsScreenScaffold
import ru.slavgorod.transport.ui.viewmodel.ThemeViewModel

@Composable
fun SettingsMainScreen(
    themeViewModel: ThemeViewModel,
    onBackClick: () -> Unit,
    onOpenPage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appLogStore: AppLogStore = org.koin.compose.koinInject()
    val resetAppDataUseCase: ResetAppDataUseCase = org.koin.compose.koinInject()
    val coroutineScope = rememberCoroutineScope()
    val settingsEntries = buildSettingsEntries()
    val settingsTitle = stringResource(R.string.settings_title)
    val menuMoreDescription = stringResource(R.string.settings_more)
    val resetMenuItemText = stringResource(R.string.settings_menu_reset)
    val exportLogsMenuItemText = stringResource(R.string.settings_menu_export_logs)
    val resetDialogTitle = stringResource(R.string.settings_reset_dialog_title)
    val resetDialogText = stringResource(R.string.settings_reset_dialog_text)
    val resetConfirmText = stringResource(R.string.settings_reset_confirm)
    val resetCancelText = stringResource(R.string.settings_reset_cancel)

    var activeSheet by rememberSaveable { mutableStateOf<SettingsModalSheet?>(null) }
    var isMenuOpen by rememberSaveable { mutableStateOf(false) }
    var showResetDialog by rememberSaveable { mutableStateOf(false) }
    val closeSheet = { sectionTitle: String ->
        closeSettingsSheet(sectionTitle) { activeSheet = null }
    }

    LaunchedEffect(Unit) {
        UserActionLogger.screenOpened(settingsTitle)
    }

    SettingsScreenScaffold(
        title = settingsTitle,
        onBackClick = onBackClick,
        actions = {
            SettingsActionsMenu(
                expanded = isMenuOpen,
                menuMoreDescription = menuMoreDescription,
                resetMenuItemText = resetMenuItemText,
                exportLogsMenuItemText = exportLogsMenuItemText,
                onOpen = {
                    UserActionLogger.menuOpened(settingsTitle)
                    isMenuOpen = true
                },
                onDismiss = { isMenuOpen = false },
                onResetClick = {
                    isMenuOpen = false
                    UserActionLogger.action(R.string.settings_reset_warning_opened)
                    showResetDialog = true
                },
                onExportLogsClick = {
                    isMenuOpen = false
                    UserActionLogger.action(R.string.settings_export_logs_action)
                    val isExportStarted = context.exportApplicationLogs(appLogStore)
                    UserActionLogger.logExportResult(isExportStarted)
                }
            )
        }
    ) { contentModifier ->
        SettingsContent(
            entries = settingsEntries,
            onEntryClick = { entry ->
                UserActionLogger.settingsEntryOpened(entry.title)
                when (val destination = entry.destination) {
                    is SettingsDestination.Website -> {
                        if (!context.openExternalUrl(
                                destination.url,
                                context.getString(R.string.welcome_link_failed)
                            )
                        ) {
                            Toast.makeText(
                                context,
                                R.string.welcome_link_failed,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    is SettingsDestination.Sheet -> {
                        UserActionLogger.action(R.string.settings_open_section_format, entry.title)
                        activeSheet = destination.sheet
                    }
                    is SettingsDestination.Page -> {
                        UserActionLogger.action(R.string.settings_open_section_format, entry.title)
                        onOpenPage(destination.screen.route)
                    }
                }
            },
            modifier = modifier
                .fillMaxSize()
                .then(contentModifier)
        )
    }

    SettingsSheetHost(
        activeSheet = activeSheet,
        themeViewModel = themeViewModel,
        onCloseSheet = closeSheet
    )

    if (showResetDialog) {
        ResetConfirmationDialog(
            title = resetDialogTitle,
            bodyText = resetDialogText,
            confirmText = resetConfirmText,
            cancelText = resetCancelText,
            onConfirm = {
                UserActionLogger.action(R.string.settings_reset_confirmed)
                coroutineScope.launch {
                    resetAppDataUseCase.resetApplicationData()
                    delay(Constants.DATA_OPERATION_COMPLETION_DELAY_MS)
                    resetAppDataUseCase.restartApplication()
                }
            },
            onCancel = {
                UserActionLogger.action(R.string.settings_reset_cancelled)
                showResetDialog = false
            }
        )
    }
}

@Composable
internal fun SettingsContent(
    entries: List<SettingsEntry>,
    onEntryClick: (SettingsEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(all = Constants.SETTINGS_SCREEN_EDGE_PADDING.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            entries.forEach { entry ->
                SettingsMenuRow(
                    title = entry.title,
                    subtitle = entry.subtitle,
                    icon = entry.icon,
                    onClick = { onEntryClick(entry) }
                )
            }
        }
    }
}
