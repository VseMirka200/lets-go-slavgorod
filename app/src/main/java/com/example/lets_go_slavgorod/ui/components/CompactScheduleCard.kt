package com.example.lets_go_slavgorod.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lets_go_slavgorod.data.model.BusSchedule

/**
 * Компактная карточка рейса для двухколоночной сетки расписания
 * 
 * Версия: 2.0
 * Последнее обновление: Октябрь 2025
 * 
 * Минималистичная карточка для отображения времени отправления в сетке.
 * Используется в FilterableScheduleGrid и TwoColumnScheduleGrid.
 * 
 * Структура:
 * - Время отправления крупным шрифтом (headlineMedium - 28sp) по центру
 * - Метка "Следующий" для ближайшего рейса
 * - Звёздочка избранного справа по центру вертикали
 * 
 * Визуальные состояния:
 * - Обычный рейс: серый фон, средний размер
 * - Ближайший рейс: цветной фон, жирный шрифт, метка "Следующий"
 * 
 * Избранное:
 * - Пустая звёздочка (☆): не в избранном
 * - Заполненная звёздочка (★): в избранном
 * 
 * Изменения v2.0:
 * - Иконка избранного изменена с сердца на звезду
 * - Увеличен размер времени отправления (bodyLarge -> headlineMedium)
 * - Звездочка перемещена в центр по вертикали справа
 * 
 * @param schedule расписание для отображения
 * @param isFavorite добавлено ли время в избранное
 * @param onFavoriteClick callback при клике на звёздочку
 * @param isNextUpcoming является ли это ближайшим рейсом
 * @param orderNumber порядковый номер рейса (опционально, для отображения)
 * @param allSchedules не используется (оставлен для совместимости API)
 * @param modifier модификатор для настройки внешнего вида
 */
@Composable
fun CompactScheduleCard(
    schedule: BusSchedule,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    isNextUpcoming: Boolean = false,
    orderNumber: Int? = null,
    allSchedules: List<BusSchedule> = emptyList(),
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isNextUpcoming) 3.dp else 1.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isNextUpcoming) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Порядковый номер слева по центру вертикали
            if (orderNumber != null) {
                Text(
                    text = "$orderNumber.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 4.dp)
                )
            }
            
            // Основное содержимое по центру
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Время отправления
                Text(
                    text = schedule.departureTime,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = if (isNextUpcoming) FontWeight.Bold else FontWeight.SemiBold
                    ),
                    color = if (isNextUpcoming) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurface
                )
                
                // Простое отображение для ближайшего рейса
                if (isNextUpcoming) {
                    Text(
                        text = "Следующий",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                }
            }
            
            // Кнопка избранного справа по центру
            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(32.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (isFavorite) "Убрать из избранного" else "Добавить в избранное",
                    tint = if (isFavorite) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

