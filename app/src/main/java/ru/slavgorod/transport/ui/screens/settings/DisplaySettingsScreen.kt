package ru.slavgorod.transport.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
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
import ru.slavgorod.transport.ui.components.settings.SettingsContentLayout
import ru.slavgorod.transport.ui.components.settings.SettingsRadioRow
import ru.slavgorod.transport.ui.components.settings.SettingsScreenScaffold
import ru.slavgorod.transport.ui.model.AppTheme
import ru.slavgorod.transport.ui.viewmodel.DisplaySettingsViewModel
import ru.slavgorod.transport.ui.viewmodel.ThemeViewModel

@Composable
fun DisplaySettingsScreen(
    themeViewModel: ThemeViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    showThemeSection: Boolean = true,
    showDisplayModeSection: Boolean = true,
    showTopBar: Boolean = true,
    scrollEnabled: Boolean = true
) {
    val displaySettingsViewModel: DisplaySettingsViewModel = koinViewModel()
    LaunchedEffect(Unit) {
        UserActionLogger.screenOpened(R.string.settings_appearance_title)
    }
    val currentGridColumns by displaySettingsViewModel.gridColumns.collectAsStateWithLifecycle(
        initialValue = 2
    )
    val currentTheme by themeViewModel.currentTheme.collectAsStateWithLifecycle(initialValue = AppTheme.SYSTEM)
    val isInlineEmbedded = !showTopBar && !scrollEnabled
    val contentSpacing = if (showTopBar) Constants.SETTINGS_ITEM_SPACING.dp else 8.dp
    val optionVerticalPadding = if (showTopBar) 8.dp else 0.dp
    val contentHorizontalPadding = when {
        isInlineEmbedded -> 0.dp
        showTopBar -> 0.dp
        else -> 8.dp
    }
    val contentOuterPadding = when {
        isInlineEmbedded -> 0.dp
        showTopBar -> Constants.SETTINGS_SCREEN_EDGE_PADDING.dp
        else -> 0.dp
    }
    val contentBottomPadding = when {
        isInlineEmbedded -> 0.dp
        showTopBar -> Constants.SETTINGS_SCREEN_EDGE_PADDING.dp
        else -> 24.dp
    }
    val rowHorizontalPadding = if (showTopBar) 10.dp else 4.dp
    val inlineTitleStyle =
        MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
    val appearanceTitle = stringResource(R.string.settings_appearance_title)
    val themeTitle = stringResource(R.string.settings_theme_title)
    val columnsTitle = stringResource(R.string.settings_columns_title)
    val gridColumnLabels = mapOf(
        1 to stringResource(R.string.grid_columns_1),
        2 to stringResource(R.string.grid_columns_2),
        3 to stringResource(R.string.grid_columns_3)
    )
    val listModeSubtitle = stringResource(R.string.display_mode_list_subtitle)
    val screenTitle = if (showTopBar) {
        resolveDisplayScreenTitle(
            showThemeSection = showThemeSection,
            showDisplayModeSection = showDisplayModeSection,
            appearanceTitle = appearanceTitle,
            themeTitle = themeTitle,
            columnsTitle = columnsTitle
        )
    } else {
        ""
    }

    val content: @Composable (Modifier) -> Unit = { rootModifier ->
        val themeLabels = mapOf(
            AppTheme.SYSTEM to stringResource(R.string.theme_system),
            AppTheme.LIGHT to stringResource(R.string.theme_light),
            AppTheme.DARK to stringResource(R.string.theme_dark)
        )
        SettingsContentLayout(
            title = screenTitle,
            showTopBar = showTopBar,
            scrollEnabled = scrollEnabled,
            isInlineEmbedded = isInlineEmbedded,
            horizontalPadding = contentHorizontalPadding,
            verticalArrangement = Arrangement.spacedBy(contentSpacing),
            inlineTitleStyle = inlineTitleStyle,
            content = {
                if (showThemeSection) {
                    SettingsSectionHeader(
                        title = themeTitle,
                        visible = showTopBar
                    )
                }

                if (showThemeSection) {
                    AppTheme.entries.forEach { theme ->
                        SettingsRadioRow(
                            selected = currentTheme == theme,
                            title = themeLabels.getValue(theme),
                            onClick = { themeViewModel.setTheme(theme) },
                            verticalPadding = optionVerticalPadding,
                            horizontalPadding = rowHorizontalPadding
                        )
                    }
                }

                if (showDisplayModeSection) {
                    SettingsSectionHeader(
                        title = columnsTitle,
                        visible = showTopBar
                    )
                }

                if (showDisplayModeSection) {
                    GRID_COLUMN_OPTIONS.forEach { columns ->
                        SettingsRadioRow(
                            selected = currentGridColumns == columns,
                            title = columns.toGridColumnsLabel(gridColumnLabels),
                            subtitle = columns.toGridColumnsSubtitle(listModeSubtitle),
                            onClick = { displaySettingsViewModel.setGridColumns(columns) },
                            verticalPadding = optionVerticalPadding,
                            horizontalPadding = rowHorizontalPadding
                        )
                    }
                }
            },
            modifier = rootModifier
                .then(modifier)
                .padding(
                    start = contentOuterPadding,
                    top = contentOuterPadding,
                    end = contentOuterPadding,
                    bottom = contentBottomPadding
                )
        )
    }

    SettingsScreenScaffold(
        title = screenTitle,
        onBackClick = onBackClick,
        showTopBar = showTopBar,
        content = content
    )
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    visible: Boolean
) {
    if (!visible) return

    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private fun resolveDisplayScreenTitle(
    showThemeSection: Boolean,
    showDisplayModeSection: Boolean,
    appearanceTitle: String,
    themeTitle: String,
    columnsTitle: String
): String {
    return when {
        showThemeSection && showDisplayModeSection -> appearanceTitle
        showThemeSection -> themeTitle
        showDisplayModeSection -> columnsTitle
        else -> appearanceTitle
    }
}

private val GRID_COLUMN_OPTIONS = listOf(1, 2, 3)

private fun Int.toGridColumnsLabel(labels: Map<Int, String>): String = labels.getValue(this)

private fun Int.toGridColumnsSubtitle(listModeSubtitle: String): String? {
    return when (this) {
        1 -> listModeSubtitle
        else -> null
    }
}
