package ru.slavgorod.transport.ui.components.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.slavgorod.transport.R

@Composable
internal fun ScheduleFilteredEmptyState(
    layout: ScheduleResponsiveLayoutSpec,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(layout.emptyState.emptyStateVerticalSpacing)
        ) {
            Icon(
                imageVector = Icons.Outlined.Schedule,
                contentDescription = stringResource(R.string.schedule_no_upcoming_routes),
                modifier = Modifier.size(if (layout.mode == ScheduleResponsiveLayoutMode.TIGHT) 40.dp else 48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.schedule_no_upcoming_routes),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
