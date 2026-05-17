package ru.slavgorod.transport.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import ru.slavgorod.transport.R
import ru.slavgorod.transport.core.Constants
import ru.slavgorod.transport.core.ScheduleConfig
import ru.slavgorod.transport.data.local.ScheduleSourceSettings
import ru.slavgorod.transport.data.repository.RoutesTableDataSource
import ru.slavgorod.transport.domain.util.ValidationUtils
import ru.slavgorod.transport.logging.UserActionLogger
import ru.slavgorod.transport.ui.components.app.AppButton
import ru.slavgorod.transport.ui.components.app.AppSecondaryButton
import ru.slavgorod.transport.ui.components.settings.SettingsSurfaceCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ScheduleSourceEditorSection(
    modifier: Modifier = Modifier
) {
    val repository: RoutesTableDataSource = koinInject()
    val scheduleSourceSettings: ScheduleSourceSettings = koinInject()

    val editorTitle = stringResource(R.string.schedule_source_editor_title)
    val editorSubtitle = stringResource(R.string.schedule_source_editor_subtitle)
    val sourcePreferenceTitle = stringResource(R.string.schedule_source_title)
    val urlLabel = stringResource(R.string.schedule_source_url_label)
    val urlPlaceholder = stringResource(R.string.schedule_source_url_placeholder)
    val saveButtonText = stringResource(R.string.schedule_source_save_button)
    val resetButtonText = stringResource(R.string.schedule_source_reset_button)
    stringResource(R.string.schedule_source_reset_message)
    val invalidMessage = stringResource(R.string.schedule_source_invalid_message)

    LaunchedEffect(Unit) {
        UserActionLogger.screenOpened(editorTitle)
    }

    val currentSourceUrl by scheduleSourceSettings.remoteJsonUrl.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    var sourceMessage by remember { mutableStateOf<String?>(null) }
    var sourceUrlInput by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(currentSourceUrl) {
        sourceUrlInput = currentSourceUrl
    }

    ScheduleSourceEditorCard(
        title = editorTitle,
        subtitle = editorSubtitle,
        urlLabel = urlLabel,
        urlPlaceholder = urlPlaceholder,
        urlValue = sourceUrlInput,
        onUrlValueChange = { _ ->
        },
        message = sourceMessage,
        invalidMessage = invalidMessage,
        saveButtonText = saveButtonText,
        resetButtonText = resetButtonText,
        onSave = {
            val normalizedUrl = sourceUrlInput.trim()
            if (ValidationUtils.isValidUrl(normalizedUrl)) {
                coroutineScope.launch {
                    scheduleSourceSettings.setRemoteJsonUrl(normalizedUrl)
                    UserActionLogger.preferenceChanged(sourcePreferenceTitle, normalizedUrl)
                    repository.refreshRoutesFromLocal()
                }
            }
        },
        onReset = {
            coroutineScope.launch {
                scheduleSourceSettings.resetRemoteJsonUrl()
                UserActionLogger.preferenceChanged(
                    sourcePreferenceTitle,
                    ScheduleConfig.remoteJsonUrl
                )
                repository.refreshRoutesFromLocal()
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = Constants.SETTINGS_SCREEN_EDGE_PADDING.dp,
                top = Constants.SETTINGS_SCREEN_EDGE_PADDING.dp,
                end = Constants.SETTINGS_SCREEN_EDGE_PADDING.dp,
                bottom = 24.dp
            )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleSourceEditorCard(
    title: String,
    subtitle: String,
    urlLabel: String,
    urlPlaceholder: String,
    urlValue: String,
    onUrlValueChange: (String) -> Unit,
    message: String?,
    invalidMessage: String,
    saveButtonText: String,
    resetButtonText: String,
    onSave: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsSurfaceCard(
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            Constants.SETTINGS_SCREEN_EDGE_PADDING.dp
        )
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedTextField(
                value = urlValue,
                onValueChange = onUrlValueChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(urlLabel) },
                placeholder = { Text(urlPlaceholder) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null
                    )
                },
                singleLine = true,
                isError = message == invalidMessage,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { onSave() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = MaterialTheme.colorScheme.primary
                ),
                shape = MaterialTheme.shapes.medium
            )

            message?.takeIf(String::isNotBlank)?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (it == invalidMessage) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppButton(
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(saveButtonText)
                }

                AppSecondaryButton(
                    onClick = onReset,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(resetButtonText)
                }
            }
        }
    }
}
