package ru.slavgorod.transport.ui.components.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/** Одна полоса дней: при недостатке места её можно прокрутить по горизонтали. */
@Composable
internal fun ScheduleDayPicker(
    selectedDay: DayOfWeek?,
    currentDay: DayOfWeek,
    onDaySelected: (DayOfWeek) -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = Locale.forLanguageTag("ru")
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        val minimumDayWidth = 48.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
        val dayWidth = maxOf(maxWidth / DayOfWeek.entries.size, minimumDayWidth)
        val shape = RoundedCornerShape(12.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                .horizontalScroll(rememberScrollState())
                .selectableGroup()
                .testTag("schedule_day_picker")
        ) {
            DayOfWeek.entries.forEach { day ->
                val isToday = day == currentDay
                val isSelected = day == selectedDay
                val background = when {
                    isToday -> TODAY_HIGHLIGHT
                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                    else -> Color.Transparent
                }
                val contentColor = when {
                    isToday -> TODAY_HIGHLIGHT_CONTENT
                    isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Box(
                    modifier = Modifier
                        .width(dayWidth)
                        .heightIn(min = 48.dp)
                        .background(background)
                        .selectable(
                            selected = isSelected,
                            role = Role.Tab,
                            onClick = { onDaySelected(day) })
                        .semantics {
                            contentDescription =
                                day.getDisplayName(TextStyle.FULL_STANDALONE, locale)
                                    .replaceFirstChar(Char::titlecase)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day.getDisplayName(TextStyle.SHORT, locale)
                            .replaceFirstChar(Char::titlecase),
                        color = contentColor,
                        fontWeight = if (isSelected || isToday) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

internal val TODAY_HIGHLIGHT = Color(0xFFFFD54F)
internal val TODAY_HIGHLIGHT_CONTENT = Color(0xFF251A00)
