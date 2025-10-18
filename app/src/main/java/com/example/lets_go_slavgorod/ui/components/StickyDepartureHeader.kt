package com.example.lets_go_slavgorod.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Sticky заголовок секции с возможностью сворачивания
 * 
 * Версия: 2.0
 * Последнее обновление: Октябрь 2025
 * 
 * Используется для создания прилипающих заголовков секций в LazyColumn.
 * При прокрутке заголовок остается видимым до появления следующего заголовка.
 * 
 * Функциональность:
 * - Sticky эффект при прокрутке (через stickyHeader в LazyColumn)
 * - Кликабельность для сворачивания/разворачивания секции
 * - Анимированная иконка (стрелка вверх/вниз)
 * - Визуальная обратная связь при клике
 * 
 * Используется в:
 * - Секции выходов маршрута №1 ("1 Выход", "2 Выход", "3 Выход")
 * 
 * @param title название секции
 * @param isExpanded текущее состояние секции (развернута/свернута)
 * @param onToggleExpand callback для изменения состояния секции
 * @param modifier модификатор для настройки внешнего вида
 */
@Composable
fun StickyDepartureHeader(
    title: String,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onToggleExpand
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Свернуть" else "Развернуть",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

