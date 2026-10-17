package ru.slavgorod.transport.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import ru.slavgorod.transport.R
import ru.slavgorod.transport.core.AppText
import ru.slavgorod.transport.core.Constants
import ru.slavgorod.transport.data.repository.RoutesLoadState
import ru.slavgorod.transport.data.repository.RoutesTableDataSource
import ru.slavgorod.transport.data.repository.ScheduleDataSourceStatus
import ru.slavgorod.transport.domain.util.DateTimeFormatterUtils
import ru.slavgorod.transport.logging.UserActionLogger
import ru.slavgorod.transport.ui.components.app.AppOutlinedButton
import ru.slavgorod.transport.ui.components.settings.SettingsSurfaceCard
import ru.slavgorod.transport.ui.viewmodel.UiMessageSpec
import ru.slavgorod.transport.ui.viewmodel.buildRefreshUiMessage
import ru.slavgorod.transport.ui.viewmodel.resolveScheduleStatusLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ScheduleUpdateSettingsSection(
    modifier: Modifier = Modifier
) {
    val repository: RoutesTableDataSource = koinInject()

    val screenTitle = stringResource(R.string.settings_schedule_updates_title)
    val updatedAtTitle = stringResource(R.string.schedule_updated_at_title)
    val networkTitle = stringResource(R.string.schedule_network_title)
    val statusTitle = stringResource(R.string.schedule_status_title)
    val refreshButton = stringResource(R.string.schedule_refresh_button)
    val refreshingLabel = stringResource(R.string.schedule_refreshing_label)
    val refreshFailed = stringResource(R.string.schedule_refresh_failed)
    val refreshOffline = stringResource(R.string.schedule_refresh_offline)
    val networkOnline = stringResource(R.string.schedule_network_online)
    val networkOffline = stringResource(R.string.schedule_network_offline)
    val updatedAtUnknown = stringResource(R.string.schedule_updated_unknown)

    LaunchedEffect(Unit) {
        UserActionLogger.screenOpened(screenTitle)
    }

    val dataSourceStatus by repository.dataSourceStatus.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    var isRefreshing by remember { mutableStateOf(false) }

    val rows = buildScheduleSettingsRows(
        dataSourceStatus = dataSourceStatus,
        isOnline = repository.isOnline(),
        updatedAtTitle = updatedAtTitle,
        networkTitle = networkTitle,
        statusTitle = statusTitle,
        updatedAtUnknown = updatedAtUnknown,
        networkOnline = networkOnline,
        networkOffline = networkOffline
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = Constants.SETTINGS_SCREEN_EDGE_PADDING.dp,
                top = Constants.SETTINGS_SCREEN_EDGE_PADDING.dp,
                end = Constants.SETTINGS_SCREEN_EDGE_PADDING.dp,
                bottom = 24.dp
            ),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        SettingsInfoBlock(rows = rows)

        SettingsSurfaceCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = Constants.SETTINGS_SCREEN_EDGE_PADDING.dp,
                top = 8.dp,
                end = Constants.SETTINGS_SCREEN_EDGE_PADDING.dp,
                bottom = Constants.SETTINGS_SCREEN_EDGE_PADDING.dp
            )
        ) {
            AppOutlinedButton(
                onClick = {
                    UserActionLogger.routeRefreshRequested(screenTitle)
                    if (!isRefreshing) {
                        coroutineScope.launch {
                            isRefreshing = true

                            try {
                                val loadState = repository.refreshRoutesFromLocal()
                                val refreshNotice = buildRefreshUiMessage(
                                    loadState = loadState,
                                    lastUpdatedAtMillis = repository.dataSourceStatus.value.lastUpdatedAtMillis
                                )
                                val refreshResultMessage = refreshNotice?.let(::renderUiMessageSpec)
                                    ?: when (loadState) {
                                        RoutesLoadState.SAVED_COPY_LOADED,
                                        RoutesLoadState.OFFLINE_USING_SAVED_COPY -> refreshOffline

                                        else -> refreshFailed
                                    }
                                UserActionLogger.scheduleUpdateResult("$screenTitle: $refreshResultMessage")
                            } finally {
                                isRefreshing = false
                            }
                        }
                    }
                },
                enabled = !isRefreshing,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
            ) {
                if (isRefreshing) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = refreshingLabel,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                } else {
                    Text(
                        text = refreshButton,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsInfoBlock(
    rows: List<Pair<String, String>>
) {
    SettingsSurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            Constants.SETTINGS_SCREEN_EDGE_PADDING.dp
        )
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            rows.forEach { (label, value) ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun buildScheduleSettingsRows(
    dataSourceStatus: ScheduleDataSourceStatus,
    isOnline: Boolean,
    updatedAtTitle: String,
    networkTitle: String,
    statusTitle: String,
    updatedAtUnknown: String,
    networkOnline: String,
    networkOffline: String
): List<Pair<String, String>> {
    return listOf(
        updatedAtTitle to (dataSourceStatus.lastUpdatedAtMillis?.let(::formatUpdateTime)
            ?: updatedAtUnknown),
        networkTitle to if (isOnline) networkOnline else networkOffline,
        statusTitle to stringResource(resolveScheduleStatusLabel(dataSourceStatus))
    )
}

private fun formatUpdateTime(timestampMillis: Long): String {
    return DateTimeFormatterUtils.formatFullDateTime(timestampMillis)
}

private fun renderUiMessageSpec(message: UiMessageSpec): String {
    return when (message.args.size) {
        0 -> AppText.get(message.messageResId)
        1 -> AppText.get(message.messageResId, message.args[0])
        2 -> AppText.get(message.messageResId, message.args[0], message.args[1])
        else -> AppText.get(message.messageResId, message.args.joinToString())
    }
}
