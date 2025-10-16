package com.example.lets_go_slavgorod.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
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
import androidx.compose.ui.unit.sp
import com.example.lets_go_slavgorod.data.model.BusSchedule

/**
 * Переиспользуемый компонент карточки расписания автобуса
 * 
 * Отображает информацию о времени отправления автобуса с остановки
 * в виде интерактивной карточки с возможностью добавления в избранное.
 * 
 * Компонент включает в себя:
 * - Информацию о маршруте (номер и название)
 * - Время отправления
 * - Название остановки
 * - Обратный отсчет времени до отправления
 * - Кнопку добавления в избранное
 * - Специальное отображение для ближайшего рейса
 * 
 * @param schedule расписание для отображения
 * @param isFavorite флаг, добавлено ли время в избранное
 * @param onFavoriteClick callback-функция при клике на кнопку избранного
 * @param routeNumber номер маршрута для отображения (опционально)
 * @param routeName название маршрута для отображения (опционально)
 * @param isNextUpcoming флаг, является ли это расписание ближайшим рейсом
 * @param allSchedules все расписания для расчета времени до отправления
 * @param hideRouteInfo скрыть информацию о маршруте
 * @param modifier модификатор для настройки внешнего вида
 * 
 * @author VseMirka200
 * @version 1.0
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
                            color = if (isNextUpcoming) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                            fontSize = 15.sp
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
                            fontWeight = if (isNextUpcoming) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 18.sp
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
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (isFavorite) "Убрать из избранного" else "Добавить в избранное",
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}