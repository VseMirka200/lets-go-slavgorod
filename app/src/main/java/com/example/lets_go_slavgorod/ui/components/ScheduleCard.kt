package com.example.lets_go_slavgorod.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lets_go_slavgorod.data.model.BusSchedule

/**
 * Карточка отдельного рейса в расписании
 * 
 * Версия: 2.0
 * Последнее обновление: Октябрь 2025
 * 
 * Отображает полную информацию о конкретном времени отправления автобуса.
 * Используется для расширенного отображения рейсов в списках расписания.
 * 
 * Функциональность:
 * - Время отправления в формате HH:mm (крупный шрифт)
 * - Обратный отсчет до отправления (для ближайшего рейса через CountdownTimer)
 * - Кнопка добавления в избранное (звёздочка)
 * - Примечания/комментарии (если есть, например "1 выход", "через центр")
 * - Подсветка ближайшего рейса цветом и стилем
 * 
 * Визуальные состояния:
 * - Обычный рейс: серый фон, обычный текст
 * - Ближайший рейс: цветной фон, жирный текст, таймер обратного отсчета
 * 
 * Избранное:
 * - Пустая звёздочка (☆): время не в избранном
 * - Заполненная звёздочка (★): время в избранном, настроены уведомления
 * 
 * Изменения v2.0:
 * - Иконка избранного изменена с сердца на звезду
 * - Улучшена типографика с использованием Material Design
 * - Оптимизирована компоновка для различных размеров экрана
 * 
 * @param schedule расписание для отображения
 * @param isFavorite добавлено ли время в избранное
 * @param onFavoriteClick callback при клике на звёздочку
 * @param routeNumber номер маршрута (опционально, для отображения в карточке)
 * @param routeName название маршрута (опционально, для отображения)
 * @param isNextUpcoming является ли это ближайшим рейсом
 * @param allSchedules все расписания для расчета обратного отсчета
 * @param hideRouteInfo скрыть информацию о маршруте
 * @param modifier модификатор для настройки внешнего вида
 * 
 * @author VseMirka200
 * @version 3.0
 * @since 1.0
 */
@Composable
fun ScheduleCard(
    schedule: BusSchedule,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    routeNumber: String? = null,
    routeName: String? = null,
    isNextUpcoming: Boolean = false,
    allSchedules: List<BusSchedule> = emptyList(),
    hideRouteInfo: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isNextUpcoming) 4.dp else 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isNextUpcoming) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isNextUpcoming) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (!hideRouteInfo && (!routeNumber.isNullOrBlank() || !routeName.isNullOrBlank())) {
                    val routeText = listOfNotNull(routeNumber?.takeIf { it.isNotBlank() }, routeName?.takeIf { it.isNotBlank() })
                        .joinToString(" ")
                        .trim()
                    if (routeText.isNotEmpty()) {
                        Text(
                            text = routeText,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (isNextUpcoming) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                        )
                    }
                }

                // Унифицированная структура для всех рейсов
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Отправление в ${schedule.departureTime}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (isNextUpcoming) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (isNextUpcoming) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    
                    CountdownTimer(
                        schedule = schedule,
                        allSchedules = allSchedules,
                        showLabel = isNextUpcoming,
                        modifier = Modifier
                    )
                }

                schedule.notes?.let { notes ->
                    if (notes.isNotBlank()) {
                        Text(
                            text = "Примечание: $notes",
                            style = MaterialTheme.typography.bodySmall,
                            color = (if (isNextUpcoming) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.8f)
                        )
                    }
                }
            }

            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (isFavorite) "Убрать из избранного" else "Добавить в избранное",
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}