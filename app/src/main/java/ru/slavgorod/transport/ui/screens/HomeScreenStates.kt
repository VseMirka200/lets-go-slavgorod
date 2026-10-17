package ru.slavgorod.transport.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.slavgorod.transport.R
import ru.slavgorod.transport.core.Constants
import ru.slavgorod.transport.ui.components.app.AppButton
import ru.slavgorod.transport.ui.theme.scaleDpForFontScale

private val EmptyStateIconSize = 64.dp
private val EmptyStateSpacing = 16.dp

@Composable
internal fun LoadingState() {
    val fontScale = LocalDensity.current.fontScale
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(scaleDpForFontScale(16.dp, fontScale))
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(scaleDpForFontScale(48.dp, fontScale))
            )
            Text(
                text = stringResource(R.string.home_loading_routes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun ErrorState(
    errorMessage: String,
    onRetry: (() -> Unit)? = null
) {
    val fontScale = LocalDensity.current.fontScale
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(scaleDpForFontScale(Constants.SETTINGS_HORIZONTAL_PADDING.dp, fontScale)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(scaleDpForFontScale(16.dp, fontScale))
        ) {
            Icon(
                imageVector = Icons.Default.DirectionsBus,
                contentDescription = stringResource(R.string.error_icon_description),
                modifier = Modifier.size(scaleDpForFontScale(64.dp, fontScale)),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = stringResource(id = R.string.error_loading_routes),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = errorMessage.ifEmpty { stringResource(id = R.string.unknown_error) },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            onRetry?.let {
                AppButton(
                    onClick = it,
                    modifier = Modifier.padding(top = scaleDpForFontScale(8.dp, fontScale))
                ) {
                    Text(stringResource(R.string.home_retry))
                }
            }
        }
    }
}

@Composable
internal fun EmptyState(searchQuery: String) {
    val fontScale = LocalDensity.current.fontScale
    val isSearchResult = searchQuery.isNotEmpty()
    val title = if (isSearchResult) {
        stringResource(R.string.home_no_results_title)
    } else {
        stringResource(R.string.home_no_routes_title)
    }
    val body = if (isSearchResult) {
        stringResource(R.string.home_no_results_body, searchQuery)
    } else {
        stringResource(R.string.home_no_routes_body)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(scaleDpForFontScale(Constants.SETTINGS_HORIZONTAL_PADDING.dp, fontScale)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(scaleDpForFontScale(EmptyStateSpacing, fontScale))
        ) {
            Icon(
                imageVector = if (isSearchResult) Icons.Filled.SearchOff else Icons.Default.DirectionsBus,
                contentDescription = stringResource(R.string.empty_state_icon_description),
                modifier = Modifier.size(scaleDpForFontScale(EmptyStateIconSize, fontScale)),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (!isSearchResult) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(scaleDpForFontScale(8.dp, fontScale)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = stringResource(R.string.home_info_description),
                        modifier = Modifier.size(scaleDpForFontScale(16.dp, fontScale)),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = stringResource(R.string.home_no_routes_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}
