package ru.slavgorod.transport.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.slavgorod.transport.R
import ru.slavgorod.transport.ui.model.AppTheme

internal val GridColumnOptions = listOf(1, 2, 3)

@Composable
internal fun AppTheme.toDisplayLabel(): String {
    return when (this) {
        AppTheme.SYSTEM -> stringResource(R.string.theme_system)
        AppTheme.LIGHT -> stringResource(R.string.theme_light)
        AppTheme.DARK -> stringResource(R.string.theme_dark)
    }
}

@Composable
internal fun Int.toGridColumnsLabel(): String {
    return when (this) {
        1 -> stringResource(R.string.grid_columns_1)
        2 -> stringResource(R.string.grid_columns_2)
        3 -> stringResource(R.string.grid_columns_3)
        else -> stringResource(R.string.appearance_columns_count_format, this)
    }
}

internal fun Int.toGridColumnsSubtitle(listModeSubtitle: String): String? {
    return when (this) {
        1 -> listModeSubtitle
        else -> null
    }
}
