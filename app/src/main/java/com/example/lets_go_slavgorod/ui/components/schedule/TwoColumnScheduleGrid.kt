package com.example.lets_go_slavgorod.ui.components.schedule

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lets_go_slavgorod.data.model.BusSchedule
import com.example.lets_go_slavgorod.ui.components.CompactScheduleCard
import com.example.lets_go_slavgorod.ui.viewmodel.BusViewModel

/**
 * Компонент для отображения расписания в две колонки
 * 
 * Версия: 2.1
 * Последнее обновление: Октябрь 2025
 * 
 * Отображает времена отправления из двух разных точек в виде двух колонок
 * с поддержкой фильтрации и автоматическим выравниванием. Используется для
 * сворачиваемых секций маршрута №1 (вокзал ↔ совхоз).
 * 
 * Функциональность:
 * - Двухколоночная сетка с заголовками (левая | правая точка отправления)
 * - Подсветка ближайших рейсов с меткой "Следующий"
 * - Кнопки избранного (звездочка) для каждого времени
 * - Поддержка фильтров "Избранные" и "Следующий" (передаются извне)
 * - Автоматическое выравнивание высоты колонок
 * - Порядковые номера рейсов для удобной навигации (1., 2., 3...)
 * - Пустые состояния с подсказками при активных фильтрах
 * 
 * Изменения v2.1:
 * - Добавлены порядковые номера рейсов в CompactScheduleCard
 * - Используются remember для оптимизации фильтрации
 * - Улучшена документация и комментарии
 * 
 * Изменения v2.0:
 * - Добавлена поддержка фильтра "Следующий" (showOnlyUpcoming)
 * - Улучшена логика фильтрации с использованием when
 * - Добавлены пустые состояния для фильтров с информативными сообщениями
 * 
 * @param leftSchedules список расписаний для левой колонки (например, из Вокзала)
 * @param rightSchedules список расписаний для правой колонки (например, из Совхоза)
 * @param leftTitle заголовок для левой колонки
 * @param rightTitle заголовок для правой колонки
 * @param nextUpcomingLeftId ID ближайшего предстоящего рейса в левой колонке
 * @param nextUpcomingRightId ID ближайшего предстоящего рейса в правой колонке
 * @param viewModel BusViewModel для работы с избранными
 * @param route маршрут для контекста
 * @param showOnlyFavorites если true, показывать только избранные времена
 * @param showOnlyUpcoming если true, показывать только следующий рейс
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
    showOnlyFavorites: Boolean = false,
    showOnlyUpcoming: Boolean = false,
    modifier: Modifier = Modifier
) {
    val favoriteTimesList by viewModel.favoriteTimes.collectAsState()
    
    // Фильтруем расписания по избранному или следующему если включен фильтр
    val filteredLeftSchedules = remember(leftSchedules, showOnlyFavorites, showOnlyUpcoming, favoriteTimesList, nextUpcomingLeftId) {
        when {
            showOnlyFavorites -> leftSchedules.filter { schedule ->
                favoriteTimesList.any { it.id == schedule.id && it.isActive }
            }
            showOnlyUpcoming -> leftSchedules.filter { schedule ->
                schedule.id == nextUpcomingLeftId
            }
            else -> leftSchedules
        }
    }
    
    val filteredRightSchedules = remember(rightSchedules, showOnlyFavorites, showOnlyUpcoming, favoriteTimesList, nextUpcomingRightId) {
        when {
            showOnlyFavorites -> rightSchedules.filter { schedule ->
                favoriteTimesList.any { it.id == schedule.id && it.isActive }
            }
            showOnlyUpcoming -> rightSchedules.filter { schedule ->
                schedule.id == nextUpcomingRightId
            }
            else -> rightSchedules
        }
    }
    
    if (filteredLeftSchedules.isEmpty() && filteredRightSchedules.isEmpty()) {
        // Показываем сообщение если нет данных при активном фильтре
        if (showOnlyFavorites || showOnlyUpcoming) {
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
                        contentDescription = if (showOnlyFavorites) "Нет избранных" else "Нет следующих",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Text(
                        text = if (showOnlyFavorites) "Нет избранных времен" else "Нет предстоящих рейсов",
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
        // Заголовки колонок
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Заголовок левой колонки
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = leftTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            // Заголовок правой колонки
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = rightTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
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
                    modifier = Modifier.weight(1f)
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
                            orderNumber = i + 1,
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
                            orderNumber = i + 1,
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

