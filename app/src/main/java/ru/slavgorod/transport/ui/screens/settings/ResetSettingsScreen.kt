package ru.slavgorod.transport.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import ru.slavgorod.transport.R
import ru.slavgorod.transport.core.Constants
import ru.slavgorod.transport.domain.ResetAppDataUseCase
import ru.slavgorod.transport.logging.UserActionLogger
import ru.slavgorod.transport.ui.components.app.AppDestructiveButton
import ru.slavgorod.transport.ui.components.settings.SettingsScreenScaffold
import ru.slavgorod.transport.ui.components.settings.SettingsSurfaceCard
import ru.slavgorod.transport.ui.theme.scaleDpForFontScale

@Composable
fun ResetSettingsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val resetAppDataUseCase: ResetAppDataUseCase = koinInject()
    val screenTitle = stringResource(R.string.settings_reset_dialog_title)
    LaunchedEffect(Unit) {
        UserActionLogger.screenOpened(screenTitle)
    }
    val fontScale = LocalDensity.current.fontScale
    val coroutineScope = rememberCoroutineScope()
    var showResetDialog by remember { mutableStateOf(false) }

    SettingsScreenScaffold(
        title = screenTitle,
        onBackClick = onBackClick
    ) { contentModifier ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .then(contentModifier)
                .padding(
                    start = scaleDpForFontScale(Constants.SETTINGS_SCREEN_EDGE_PADDING.dp, fontScale),
                    top = scaleDpForFontScale(Constants.SETTINGS_SCREEN_EDGE_PADDING.dp, fontScale),
                    end = scaleDpForFontScale(Constants.SETTINGS_SCREEN_EDGE_PADDING.dp, fontScale),
                    bottom = scaleDpForFontScale(24.dp, fontScale)
                )
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(
                scaleDpForFontScale(Constants.SETTINGS_ITEM_SPACING.dp, fontScale)
            )
        ) {
            ResetWarningCard(onResetClick = { showResetDialog = true })
            ResetSummaryCard(
                title = stringResource(R.string.settings_reset_affected_title),
                lines = listOf(
                    stringResource(R.string.settings_reset_affected_theme_display),
                    stringResource(R.string.settings_reset_affected_schedule_cache),
                    stringResource(R.string.settings_reset_affected_preferences)
                )
            )
            ResetSummaryCard(
                title = stringResource(R.string.settings_reset_preserved_title),
                lines = listOf(stringResource(R.string.settings_reset_preserved_logs))
            )
        }
    }

    if (showResetDialog) {
        ResetConfirmationDialog(
            title = stringResource(R.string.settings_reset_confirmation_title),
            bodyText = stringResource(R.string.settings_reset_confirmation_text),
            confirmText = stringResource(R.string.settings_reset_confirm),
            cancelText = stringResource(R.string.settings_reset_cancel),
            onConfirm = {
                coroutineScope.launch {
                    resetAppDataUseCase.resetApplicationData()
                    delay(Constants.DATA_OPERATION_COMPLETION_DELAY_MS)
                    resetAppDataUseCase.restartApplication()
                }
            },
            onCancel = { }
        )
    }
}

@Composable
private fun ResetWarningCard(onResetClick: () -> Unit) {
    val fontScale = LocalDensity.current.fontScale
    val warningTitle = stringResource(R.string.settings_reset_dialog_title)
    val warningText = stringResource(R.string.settings_reset_warning_text)
    val restartText = stringResource(R.string.settings_reset_restart_text)
    val actionLabel = stringResource(R.string.settings_reset_action_label)

    SettingsSurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.error,
        shadowElevation = 6.dp,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            scaleDpForFontScale(Constants.SETTINGS_SCREEN_EDGE_PADDING.dp, fontScale)
        )
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(scaleDpForFontScale(8.dp, fontScale))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(scaleDpForFontScale(8.dp, fontScale))
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(scaleDpForFontScale(24.dp, fontScale))
                )
                Text(
                    text = warningTitle,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onError
                )
            }

            Text(
                text = warningText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = restartText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onError,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(scaleDpForFontScale(8.dp, fontScale)))

            AppDestructiveButton(
                onClick = onResetClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(scaleDpForFontScale(20.dp, fontScale))
                )
                Spacer(modifier = Modifier.width(scaleDpForFontScale(8.dp, fontScale)))
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun ResetSummaryCard(
    title: String,
    lines: List<String>
) {
    val fontScale = LocalDensity.current.fontScale
    SettingsSurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        shadowElevation = 1.dp,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            scaleDpForFontScale(Constants.SETTINGS_SCREEN_EDGE_PADDING.dp, fontScale)
        )
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(scaleDpForFontScale(8.dp, fontScale))
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
            )

            lines.forEach { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
