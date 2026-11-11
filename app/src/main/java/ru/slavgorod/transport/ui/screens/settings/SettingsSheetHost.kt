package ru.slavgorod.transport.ui.screens.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
    when (activeSheet) {
        SettingsModalSheet.THEME -> {
            val title = stringResource(R.string.settings_theme_title)
            SettingsBottomSheet(onDismiss = { onCloseSheet(title) }) {
                SettingsTitledContent(title) {
                    DisplaySettingsScreen(
                        themeViewModel = themeViewModel,
                        onBackClick = { onCloseSheet(title) },
                        showThemeSection = true,
                        showDisplayModeSection = false,
                        showTopBar = false,
                        scrollEnabled = true
                    )
                }
            }
        }
        SettingsModalSheet.ABOUT -> {
            val title = stringResource(R.string.settings_about_title)
            SettingsBottomSheet(onDismiss = { onCloseSheet(title) }) {
                SettingsTitledContent(title) {
                    AboutScreen(onBackClick = { onCloseSheet(title) }, showTopBar = false, scrollEnabled = true)
                }
            }
        }
        null -> Unit
    }
}

@Composable
internal fun SettingsTitledContent(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = title,
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 8.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        content()
    }
}

internal fun closeSettingsSheet(sectionTitle: String, onClosed: () -> Unit) {
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
        Box(modifier = Modifier.padding(bottom = 8.dp)) { content() }
    }
}
