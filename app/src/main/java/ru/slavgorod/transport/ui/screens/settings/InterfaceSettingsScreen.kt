package ru.slavgorod.transport.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ViewModule
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
import ru.slavgorod.transport.ui.viewmodel.ThemeViewModel

private enum class InterfaceDetail { THEME, COLUMNS, SCHEDULE_DAY }

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun InterfaceSettingsScreen(themeViewModel: ThemeViewModel, onBackClick: () -> Unit) {
    var activeDetail by rememberSaveable { mutableStateOf<InterfaceDetail?>(null) }
    SettingsScreenScaffold(title = stringResource(R.string.settings_appearance_title), onBackClick = onBackClick) { contentModifier ->
        Column(
            modifier = Modifier.fillMaxSize().then(contentModifier)
                .verticalScroll(rememberScrollState())
                .padding(Constants.SETTINGS_SCREEN_EDGE_PADDING.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            SettingsMenuRow(
                title = stringResource(R.string.settings_theme_title),
                subtitle = stringResource(R.string.settings_theme_subtitle),
                icon = Icons.Default.Palette,
                onClick = { activeDetail = InterfaceDetail.THEME }
            )
            SettingsMenuRow(
                title = stringResource(R.string.settings_columns_title),
                subtitle = stringResource(R.string.settings_columns_subtitle),
                icon = Icons.Default.ViewModule,
                onClick = { activeDetail = InterfaceDetail.COLUMNS }
            )
            SettingsMenuRow(
                title = stringResource(R.string.settings_schedule_day_mode_title),
                subtitle = stringResource(R.string.settings_schedule_day_mode_subtitle),
                icon = Icons.Default.CalendarToday,
                onClick = { activeDetail = InterfaceDetail.SCHEDULE_DAY }
            )
        }
    }
    activeDetail?.let { detail ->
        val title = stringResource(when (detail) {
            InterfaceDetail.THEME -> R.string.settings_theme_title
            InterfaceDetail.COLUMNS -> R.string.settings_columns_title
            InterfaceDetail.SCHEDULE_DAY -> R.string.settings_schedule_day_mode_title
        })
        SettingsBottomSheet(onDismiss = { activeDetail = null }) {
            SettingsTitledContent(title) {
                when (detail) {
                    InterfaceDetail.THEME, InterfaceDetail.COLUMNS, InterfaceDetail.SCHEDULE_DAY -> DisplaySettingsScreen(
                        themeViewModel = themeViewModel,
                        onBackClick = { activeDetail = null },
                        showThemeSection = detail == InterfaceDetail.THEME,
                        showDisplayModeSection = detail == InterfaceDetail.COLUMNS,
                        showScheduleDaySection = detail == InterfaceDetail.SCHEDULE_DAY,
                        showTopBar = false,
                        scrollEnabled = true
                    )
                }
            }
        }
    }
}
