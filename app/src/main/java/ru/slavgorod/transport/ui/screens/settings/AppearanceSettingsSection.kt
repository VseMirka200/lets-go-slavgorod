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

        GRID_COLUMN_OPTIONS.forEach { columns ->
            SettingsRadioRow(
                selected = currentGridColumns == columns,
                title = columns.toGridColumnsLabel(),
                subtitle = columns.toGridColumnsSubtitle(),
                onClick = { displaySettingsViewModel.setGridColumns(columns) }
            )
        }
    }
}

private val GRID_COLUMN_OPTIONS = listOf(1, 2, 3)

@Composable
private fun AppTheme.toDisplayLabel(): String {
    return when (this) {
        AppTheme.SYSTEM -> stringResource(R.string.theme_system)
        AppTheme.LIGHT -> stringResource(R.string.theme_light)
        AppTheme.DARK -> stringResource(R.string.theme_dark)
    }
}

@Composable
private fun Int.toGridColumnsLabel(): String {
    return when (this) {
        1 -> stringResource(R.string.grid_columns_1)
        2 -> stringResource(R.string.grid_columns_2)
        3 -> stringResource(R.string.grid_columns_3)
        else -> stringResource(R.string.appearance_columns_count_format, this)
    }
}

@Composable
private fun Int.toGridColumnsSubtitle(): String? {
    return when (this) {
        1 -> stringResource(R.string.display_mode_list)
        else -> null
    }
}
