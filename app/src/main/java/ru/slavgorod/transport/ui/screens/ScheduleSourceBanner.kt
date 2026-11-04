package ru.slavgorod.transport.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.slavgorod.transport.R
import ru.slavgorod.transport.data.repository.ScheduleDataSource
import ru.slavgorod.transport.data.repository.ScheduleDataSourceStatus
import ru.slavgorod.transport.domain.util.DateTimeFormatterUtils

@Composable
internal fun ScheduleSourceBanner(status: ScheduleDataSourceStatus, modifier: Modifier = Modifier) {
    val source = when (status.source) {
        ScheduleDataSource.REMOTE_CACHE -> stringResource(R.string.schedule_source_remote)
        ScheduleDataSource.SAVED_COPY -> stringResource(R.string.schedule_source_saved)
        ScheduleDataSource.BUNDLED -> stringResource(R.string.schedule_source_bundled)
        ScheduleDataSource.NONE -> stringResource(R.string.schedule_source_unknown)
    }
    val updatedAt = status.lastUpdatedAtMillis?.let(DateTimeFormatterUtils::formatFullDateTime)
        ?: stringResource(R.string.schedule_updated_unknown)
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text("Источник: $source", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Последняя загрузка: $updatedAt", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
