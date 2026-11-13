package ru.slavgorod.transport.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import ru.slavgorod.transport.R
import ru.slavgorod.transport.core.Constants
import ru.slavgorod.transport.data.local.ScheduleSourceSettings
import ru.slavgorod.transport.data.local.isValidScheduleSourceUrl
import ru.slavgorod.transport.ui.components.app.AppButton
import ru.slavgorod.transport.ui.components.app.AppOutlinedButton
import timber.log.Timber

@Composable
internal fun ScheduleSourceEditor() {
    val settings: ScheduleSourceSettings = koinInject()
    val savedUrl by settings.url.collectAsStateWithLifecycle(initialValue = Constants.REMOTE_JSON_URL)
    var draft by rememberSaveable { mutableStateOf("") }
    var unlocked by remember { mutableStateOf(false) }
    var saveFailed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(savedUrl, unlocked) { if (!unlocked) draft = savedUrl }
    val valid = isValidScheduleSourceUrl(draft.trim())

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.schedule_source_warning), color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = unlocked, onCheckedChange = { unlocked = it; saveFailed = false })
            Text(stringResource(R.string.schedule_source_unlock), modifier = Modifier.weight(1f))
        }
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it; saveFailed = false },
            modifier = Modifier.fillMaxWidth(),
            enabled = unlocked,
            label = { Text(stringResource(R.string.schedule_source_url)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            isError = unlocked && draft.isNotBlank() && !valid,
            supportingText = {
                if (saveFailed) Text(stringResource(R.string.schedule_source_save_failed))
                else if (unlocked && draft.isNotBlank() && !valid) Text(stringResource(R.string.schedule_source_invalid))
            }
        )
        AppButton(
            onClick = {
                scope.launch {
                    try {
                        settings.saveUrl(draft)
                        unlocked = false
                    } catch (exception: java.io.IOException) {
                        Timber.w(exception, "Failed to save schedule source")
                        saveFailed = true
                    }
                }
            },
            enabled = unlocked && valid && draft.trim() != savedUrl,
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.schedule_source_save)) }
        if (unlocked) {
            AppOutlinedButton(
                onClick = { draft = Constants.REMOTE_JSON_URL; saveFailed = false },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.schedule_source_restore_default)) }
        }
    }
}
