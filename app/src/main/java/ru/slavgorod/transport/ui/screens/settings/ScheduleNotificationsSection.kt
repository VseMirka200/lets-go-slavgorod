package ru.slavgorod.transport.ui.screens.settings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.koinInject
import ru.slavgorod.transport.R
import ru.slavgorod.transport.core.Constants
import ru.slavgorod.transport.data.local.ScheduleNotificationSettings
import ru.slavgorod.transport.logging.UserActionLogger
import ru.slavgorod.transport.notifications.hasPostNotificationsPermission
import ru.slavgorod.transport.ui.components.settings.SettingsCheckboxRow
import ru.slavgorod.transport.ui.components.settings.SettingsSurfaceCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ScheduleNotificationsSection(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val notificationSettings: ScheduleNotificationSettings = koinInject()

    val screenTitle = stringResource(R.string.settings_schedule_notifications_title)
    val checkboxTitle = stringResource(R.string.schedule_notifications_checkbox_title)
    val checkboxSubtitle = stringResource(R.string.schedule_notifications_checkbox_subtitle)

    LaunchedEffect(Unit) {
        UserActionLogger.screenOpened(screenTitle)
    }

    val enabled by notificationSettings.enabled.collectAsStateWithLifecycle()
    val permissionGranted = context.hasPostNotificationsPermission()
    val checked = enabled && permissionGranted

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationSettings.setEnabled(granted)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = Constants.SETTINGS_SCREEN_EDGE_PADDING.dp,
                top = Constants.SETTINGS_SCREEN_EDGE_PADDING.dp,
                end = Constants.SETTINGS_SCREEN_EDGE_PADDING.dp,
                bottom = 24.dp
            )
    ) {
        SettingsSurfaceCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(0.dp)
        ) {
            SettingsCheckboxRow(
                checked = checked,
                title = checkboxTitle,
                subtitle = checkboxSubtitle,
                contentHorizontalPadding = Constants.SETTINGS_SCREEN_EDGE_PADDING.dp,
                contentVerticalPadding = Constants.SETTINGS_SCREEN_EDGE_PADDING.dp,
                onCheckedChange = { nextChecked ->
                    UserActionLogger.preferenceChanged(
                        checkboxTitle,
                        nextChecked.toString()
                    )

                    when {
                        !nextChecked -> notificationSettings.setEnabled(false)
                        permissionGranted -> notificationSettings.setEnabled(true)
                        else -> permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            )
        }
    }
}
