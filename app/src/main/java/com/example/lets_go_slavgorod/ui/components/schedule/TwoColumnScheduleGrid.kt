package com.example.lets_go_slavgorod.ui.components.schedule

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lets_go_slavgorod.data.model.BusSchedule
import com.example.lets_go_slavgorod.ui.components.CompactScheduleCard
import com.example.lets_go_slavgorod.ui.viewmodel.BusViewModel

/**
 * Компонент для отображения расписания в две колонки
 * 
 * Отображает времена отправления из двух разных точек в виде двух колонок,
 * как показано на примере изображения с автобусным расписанием.
 * 
 * @param leftSchedules список расписаний для левой колонки (например, отправления из Славгорода)
 * @param rightSchedules список расписаний для правой колонки (например, отправления из Ярового)
 * @param leftTitle заголовок для левой колонки
 * @param rightTitle заголовок для правой колонки
 * @param nextUpcomingLeftId ID ближайшего предстоящего рейса в левой колонке
 * @param nextUpcomingRightId ID ближайшего предстоящего рейса в правой колонке
 * @param viewModel BusViewModel для работы с избранными
 * @param route маршрут для контекста
 * @param modifier модификатор для настройки внешнего вида
 */
@Composable
fun TwoColumnScheduleGrid(
    leftSchedules: List<BusSchedule>,
    rightSchedules: List<BusSchedule>,
    leftTitle: String,
    rightTitle: String,
    nextUpcomingLeftId: String?,
    nextUpcomingRightId: String?,
    viewModel: BusViewModel,
    route: com.example.lets_go_slavgorod.data.model.BusRoute,
    modifier: Modifier = Modifier
) {
    val favoriteTimesList by viewModel.favoriteTimes.collectAsState()
    
    if (leftSchedules.isEmpty() && rightSchedules.isEmpty()) {
        return
    }
    
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Заголовки колонок
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Заголовок левой колонки
            Box(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = leftTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            // Заголовок правой колонки
            Box(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = rightTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
        
        // Определяем максимальную длину для выравнивания
        val maxLength = maxOf(leftSchedules.size, rightSchedules.size)
        
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
                    modifier = Modifier.weight(1f)
                ) {
                    if (i < leftSchedules.size) {
                        val schedule = leftSchedules[i]
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
                            allSchedules = leftSchedules,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        // Пустое место для выравнивания
                        Spacer(modifier = Modifier.height(60.dp))
                    }
                }
                
                // Правая колонка
                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    if (i < rightSchedules.size) {
                        val schedule = rightSchedules[i]
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
                            allSchedules = rightSchedules,
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

