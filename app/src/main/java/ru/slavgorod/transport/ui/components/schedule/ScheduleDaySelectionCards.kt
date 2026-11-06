package ru.slavgorod.transport.ui.components.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.platform.LocalDensity
import ru.slavgorod.transport.ui.theme.adaptiveColumnCount
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.slavgorod.transport.R
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.ui.res.stringResource

@Composable
internal fun ScheduleDaySelectionCards(
    currentDay: DayOfWeek,
    onDaySelected: (DayOfWeek) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.schedule_choose_day_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        BoxWithConstraints {
            val columns = adaptiveColumnCount(maxWidth, LocalDensity.current.fontScale, 2, 150.dp, 12.dp)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DayOfWeek.entries.chunked(columns).forEach { rowDays ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowDays.forEach { day ->
                            DaySelectionCard(
                                day = day,
                                isToday = day == currentDay,
                                onClick = { onDaySelected(day) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowDays.size < columns) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DaySelectionCard(
    day: DayOfWeek,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.heightIn(min = 88.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isToday) TODAY_HIGHLIGHT else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isToday) TODAY_HIGHLIGHT_CONTENT else MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth().heightIn(min = 88.dp)
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = day.getDisplayName(TextStyle.FULL_STANDALONE, RUSSIAN_LOCALE)
                    .replaceFirstChar(Char::titlecase),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,

                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            if (isToday) {
                Text(
                    text = stringResource(R.string.schedule_day_today),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private val RUSSIAN_LOCALE = Locale.forLanguageTag("ru")
