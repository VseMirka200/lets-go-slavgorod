package ru.slavgorod.transport.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import ru.slavgorod.transport.R
import ru.slavgorod.transport.core.Constants
import ru.slavgorod.transport.data.local.ScheduleSourceSettings
import ru.slavgorod.transport.data.local.isValidScheduleSourceUrl
import ru.slavgorod.transport.data.repository.RoutesLoadState
import ru.slavgorod.transport.data.repository.RoutesTableDataSource

@Composable
internal fun ScheduleSourceSettingsSection() {
    val settings: ScheduleSourceSettings = koinInject()
    val repository: RoutesTableDataSource = koinInject()
    val savedUrl by settings.sourceUrl.collectAsState(initial = Constants.REMOTE_JSON_URL)
    var url by rememberSaveable { mutableStateOf(savedUrl) }
    var isLoading by remember { mutableStateOf(false) }
    var validationError by rememberSaveable { mutableStateOf(false) }
    var resultMessage by rememberSaveable { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(savedUrl) { url = savedUrl }

    fun updateSource(reset: Boolean) {
        if (!reset && !isValidScheduleSourceUrl(url)) {
            validationError = true
            return
        }
        isLoading = true
        validationError = false
        resultMessage = null
        scope.launch {
            try {
                if (reset) settings.reset() else settings.save(url)
                resultMessage = when (repository.refreshRoutesFromLocal(notifyUser = false)) {
                    RoutesLoadState.SUCCESS -> R.string.schedule_source_success
                    RoutesLoadState.NO_CHANGES -> R.string.schedule_source_no_changes
                    RoutesLoadState.BACKUP_LOADED -> R.string.schedule_source_backup
                    else -> R.string.schedule_source_unavailable
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                resultMessage = R.string.schedule_source_save_failed
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.settings_source_title), style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(R.string.schedule_source_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = url,
            onValueChange = { url = it; validationError = false; resultMessage = null },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.schedule_source_url_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            singleLine = true,
            isError = validationError,
            enabled = !isLoading,
            supportingText = if (validationError) {
                { Text(stringResource(R.string.schedule_source_url_invalid)) }
            } else {
                null
            }
        )
        Button(onClick = { updateSource(reset = false) }, enabled = !isLoading, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.schedule_source_save))
        }
        TextButton(onClick = { updateSource(reset = true) }, enabled = !isLoading, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.schedule_source_reset))
        }
        if (isLoading) {
            CircularProgressIndicator()
            Text(stringResource(R.string.schedule_source_checking), style = MaterialTheme.typography.bodySmall)
        }
        resultMessage?.let { message ->
            Text(
                stringResource(message),
                style = MaterialTheme.typography.bodyMedium,
                color = if (message == R.string.schedule_source_success || message == R.string.schedule_source_no_changes) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
        }
    }
}
