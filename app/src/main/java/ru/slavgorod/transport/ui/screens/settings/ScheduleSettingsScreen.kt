package ru.slavgorod.transport.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.slavgorod.transport.R
import ru.slavgorod.transport.core.Constants
import ru.slavgorod.transport.ui.components.settings.SettingsMenuRow
import ru.slavgorod.transport.ui.components.settings.SettingsScreenScaffold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ScheduleSettingsScreen(
    onBackClick: () -> Unit
) {
    var activeDetail by rememberSaveable { mutableStateOf<ScheduleDetail?>(null) }
    SettingsScreenScaffold(
        title = stringResource(R.string.settings_schedule_category),
        onBackClick = onBackClick
    ) { contentModifier ->
        Column(
            modifier = Modifier.fillMaxSize().then(contentModifier)
                .verticalScroll(rememberScrollState())
                .padding(Constants.SETTINGS_SCREEN_EDGE_PADDING.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            SettingsMenuRow(
                title = stringResource(R.string.settings_schedule_management_title),
                subtitle = stringResource(R.string.settings_schedule_updates_subtitle),
                icon = Icons.Default.SystemUpdateAlt,
                onClick = { activeDetail = ScheduleDetail.UPDATES }
            )
            SettingsMenuRow(
                title = stringResource(R.string.settings_schedule_notifications_title),
                subtitle = stringResource(R.string.settings_schedule_notifications_subtitle),
                icon = Icons.Default.Notifications,
                onClick = { activeDetail = ScheduleDetail.NOTIFICATIONS }
            )
            SettingsMenuRow(
                title = stringResource(R.string.settings_source_title),
                subtitle = stringResource(R.string.schedule_source_subtitle),
                icon = Icons.Default.Link,
                onClick = { activeDetail = ScheduleDetail.SOURCE }
            )
        }
    }
    activeDetail?.let { detail ->
        val title = stringResource(when (detail) {
            ScheduleDetail.UPDATES -> R.string.settings_schedule_management_title
            ScheduleDetail.NOTIFICATIONS -> R.string.settings_schedule_notifications_title
            ScheduleDetail.SOURCE -> R.string.settings_source_title
        })
        SettingsBottomSheet(onDismiss = { activeDetail = null }) {
            SettingsTitledContent(title) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    when (detail) {
                        ScheduleDetail.UPDATES -> ScheduleSourceSettingsSection()
                        ScheduleDetail.NOTIFICATIONS -> ScheduleNotificationsSection(topPadding = 0.dp)
                        ScheduleDetail.SOURCE -> ScheduleSourceEditor()
                    }
                }
            }
        }
    }
}

private enum class ScheduleDetail { UPDATES, NOTIFICATIONS, SOURCE }

@Composable
internal fun ScheduleSettingsDetailScreen(showNotifications: Boolean, onBackClick: () -> Unit) {
    val title = stringResource(
        if (showNotifications) R.string.settings_schedule_notifications_title
        else R.string.settings_schedule_management_title
    )
    SettingsScreenScaffold(title = title, onBackClick = onBackClick) { contentModifier ->
        Column(modifier = Modifier.fillMaxSize().then(contentModifier).verticalScroll(rememberScrollState())) {
            if (showNotifications) ScheduleNotificationsSection()
            else ScheduleSourceSettingsSection()
        }
    }
}
