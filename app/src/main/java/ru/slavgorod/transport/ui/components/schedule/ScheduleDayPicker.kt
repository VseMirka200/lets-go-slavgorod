package ru.slavgorod.transport.ui.components.schedule

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@Composable
internal fun ScheduleDayPicker(
    selectedDay: DayOfWeek?,
    currentDay: DayOfWeek,
    onDaySelected: (DayOfWeek) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        DayOfWeek.entries.forEach { day ->
            OutlinedButton(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp),
                onClick = { onDaySelected(day) },
                shape = DAY_BUTTON_SHAPE,
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (day == currentDay) {
                        TODAY_HIGHLIGHT
                    } else if (day == selectedDay) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    contentColor = if (day == currentDay) {
                        TODAY_HIGHLIGHT_CONTENT
                    } else if (day == selectedDay) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            ) {
                Text(
                    text = day.getDisplayName(TextStyle.SHORT, RUSSIAN_LOCALE)
                        .replaceFirstChar(Char::titlecase),
                    fontWeight = if (day == selectedDay || day == currentDay) {
                        FontWeight.SemiBold
                    } else {
                        FontWeight.Normal
                    }
                )
            }
        }
    }
}

private val RUSSIAN_LOCALE = Locale.forLanguageTag("ru")
private val DAY_BUTTON_SHAPE = RoundedCornerShape(8.dp)
internal val TODAY_HIGHLIGHT = androidx.compose.ui.graphics.Color(0xFFFFD54F)
internal val TODAY_HIGHLIGHT_CONTENT = androidx.compose.ui.graphics.Color(0xFF251A00)
