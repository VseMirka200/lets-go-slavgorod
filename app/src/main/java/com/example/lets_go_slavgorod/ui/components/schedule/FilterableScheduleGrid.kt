package com.example.lets_go_slavgorod.ui.components.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lets_go_slavgorod.data.model.BusSchedule
import com.example.lets_go_slavgorod.ui.components.CompactScheduleCard
import com.example.lets_go_slavgorod.ui.viewmodel.BusViewModel

/**
 * Компонент для отображения расписания в две колонки с фильтрацией
 * 
 * При нажатии на заголовок колонки она подсвечивается, а другая становится полупрозрачной.
 * Это позволяет пользователю фокусироваться на конкретной точке отправления.
 * 
 * @param leftSchedules список расписаний для левой колонки
 * @param rightSchedules список расписаний для правой колонки
 * @param leftTitle заголовок для левой колонки
 * @param rightTitle заголовок для правой колонки
 * @param nextUpcomingLeftId ID ближайшего предстоящего рейса в левой колонке
 * @param nextUpcomingRightId ID ближайшего предстоящего рейса в правой колонке
 * @param viewModel BusViewModel для работы с избранными
 * @param route маршрут для контекста
 * @param modifier модификатор для настройки внешнего вида
 */
@Composable
fun FilterableScheduleGrid(
    leftSchedules: List<BusSchedule>,
    rightSchedules: List<BusSchedule>,
    leftTitle: String,
    rightTitle: String,
    nextUpcomingLeftId: String?,
    nextUpcomingRightId: String?,
    viewModel: BusViewModel,
    route: com.example.lets_go_slavgorod.data.model.BusRoute,
    showOnlyFavorites: Boolean = false,
    modifier: Modifier = Modifier
) {
    val favoriteTimesList by viewModel.favoriteTimes.collectAsState()
    
    // Состояние фильтрации: null = показывать все, true = только левая, false = только правая
    var filterState by remember { mutableStateOf<Boolean?>(null) }
    
    // Фильтруем расписания по избранному если включен фильтр
    val filteredLeftSchedules = remember(leftSchedules, showOnlyFavorites, favoriteTimesList) {
        if (showOnlyFavorites) {
            leftSchedules.filter { schedule ->
                favoriteTimesList.any { it.id == schedule.id && it.isActive }
            }
        } else {
            leftSchedules
        }
    }
    
    val filteredRightSchedules = remember(rightSchedules, showOnlyFavorites, favoriteTimesList) {
        if (showOnlyFavorites) {
            rightSchedules.filter { schedule ->
                favoriteTimesList.any { it.id == schedule.id && it.isActive }
            }
        } else {
            rightSchedules
        }
    }
    
    if (filteredLeftSchedules.isEmpty() && filteredRightSchedules.isEmpty()) {
        // Показываем сообщение если нет избранных при активном фильтре
        if (showOnlyFavorites) {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.StarBorder,
                        contentDescription = "Нет избранных",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "Нет избранных времен",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        return
    }
    
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Заголовки колонок с возможностью фильтрации
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Заголовок левой колонки
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { 
                        filterState = when (filterState) {
                            null -> true
                            true -> null
                            false -> true
                        }
                    }
                    .background(
                        color = when (filterState) {
                            true -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            null -> MaterialTheme.colorScheme.surface
                            false -> MaterialTheme.colorScheme.surface
                        },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = leftTitle,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (filterState == true) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (filterState == true) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .alpha(if (filterState == false) 0.4f else 1f)
                )
            }
            
            // Заголовок правой колонки
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { 
                        filterState = when (filterState) {
                            null -> false
                            true -> false
                            false -> null
                        }
                    }
                    .background(
                        color = when (filterState) {
                            false -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            null -> MaterialTheme.colorScheme.surface
                            true -> MaterialTheme.colorScheme.surface
                        },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = rightTitle,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (filterState == false) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (filterState == false) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .alpha(if (filterState == true) 0.4f else 1f)
                )
            }
        }
        
        // Определяем максимальную длину для выравнивания
        val maxLength = maxOf(filteredLeftSchedules.size, filteredRightSchedules.size)
        
        // Отображаем расписания построчно
        for (i in 0 until maxLength) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Левая колонка
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .alpha(if (filterState == false) 0.4f else 1f)
                ) {
                    if (i < filteredLeftSchedules.size) {
                        val schedule = filteredLeftSchedules[i]
                        val isCurrentlyFavorite = favoriteTimesList.any { it.id == schedule.id && it.isActive }
                        val isNextUpcoming = schedule.id == nextUpcomingLeftId
                        
                        CompactScheduleCard(
                            schedule = schedule,
                            isFavorite = isCurrentlyFavorite,
                            onFavoriteClick = {
                                if (isCurrentlyFavorite) {
                                    viewModel.removeFavoriteTime(schedule.id)
                                } else {
                                    viewModel.addFavoriteTime(schedule)
                                }
                            },
                            isNextUpcoming = isNextUpcoming,
                            allSchedules = filteredLeftSchedules,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        // Пустое место для выравнивания
                        Spacer(modifier = Modifier.height(60.dp))
                    }
                }
                
                // Правая колонка
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .alpha(if (filterState == true) 0.4f else 1f)
                ) {
                    if (i < filteredRightSchedules.size) {
                        val schedule = filteredRightSchedules[i]
                        val isCurrentlyFavorite = favoriteTimesList.any { it.id == schedule.id && it.isActive }
                        val isNextUpcoming = schedule.id == nextUpcomingRightId
                        
                        CompactScheduleCard(
                            schedule = schedule,
                            isFavorite = isCurrentlyFavorite,
                            onFavoriteClick = {
                                if (isCurrentlyFavorite) {
                                    viewModel.removeFavoriteTime(schedule.id)
                                } else {
                                    viewModel.addFavoriteTime(schedule)
                                }
                            },
                            isNextUpcoming = isNextUpcoming,
                            allSchedules = filteredRightSchedules,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        // Пустое место для выравнивания
                        Spacer(modifier = Modifier.height(60.dp))
                    }
                }
            }
        }
    }
}
