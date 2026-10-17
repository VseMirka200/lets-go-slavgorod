package ru.slavgorod.transport.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import ru.slavgorod.transport.R
import ru.slavgorod.transport.core.Constants
import ru.slavgorod.transport.logging.UserActionLogger
import ru.slavgorod.transport.ui.components.settings.SettingsRadioRow
import ru.slavgorod.transport.ui.model.AppTheme
import ru.slavgorod.transport.ui.viewmodel.DisplaySettingsViewModel
import ru.slavgorod.transport.ui.viewmodel.ThemeViewModel

@Composable
internal fun AppearanceSettingsSection(
    themeViewModel: ThemeViewModel,
    modifier: Modifier = Modifier
) {
    val displaySettingsViewModel: DisplaySettingsViewModel = koinViewModel()
    val sectionTitle = stringResource(R.string.settings_appearance_title)
    val themeTitle = stringResource(R.string.settings_theme_title)
    val themeSubtitle = stringResource(R.string.settings_theme_subtitle)
    val columnsTitle = stringResource(R.string.settings_columns_title)
    val columnsSubtitle = stringResource(R.string.settings_columns_subtitle)
    val currentTheme by themeViewModel.currentTheme.collectAsStateWithLifecycle(initialValue = AppTheme.SYSTEM)
    val currentGridColumns by displaySettingsViewModel.gridColumns.collectAsStateWithLifecycle(
        initialValue = 2
    )

    LaunchedEffect(Unit) {
        UserActionLogger.screenOpened(sectionTitle)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(all = Constants.SETTINGS_SCREEN_EDGE_PADDING.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = sectionTitle,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = themeTitle,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = themeSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AppTheme.entries.forEach { theme ->
            SettingsRadioRow(
                selected = currentTheme == theme,
                title = theme.toDisplayLabel(),
                onClick = { themeViewModel.setTheme(theme) }
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp),
            color = DividerDefaults.color
        )

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = columnsTitle,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = columnsSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        GridColumnOptions.forEach { columns ->
            SettingsRadioRow(
                selected = currentGridColumns == columns,
                title = columns.toGridColumnsLabel(),
                subtitle = columns.toGridColumnsSubtitle(
                    listModeSubtitle = stringResource(R.string.display_mode_list)
                ),
                onClick = { displaySettingsViewModel.setGridColumns(columns) }
            )
        }
    }
}
