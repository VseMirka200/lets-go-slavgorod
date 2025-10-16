package com.example.lets_go_slavgorod.ui.components.schedule

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lets_go_slavgorod.data.model.BusRoute
import com.example.lets_go_slavgorod.data.model.BusSchedule
import com.example.lets_go_slavgorod.ui.components.ScheduleCard
import com.example.lets_go_slavgorod.ui.components.StickyDepartureHeader
import com.example.lets_go_slavgorod.ui.components.schedule.TwoColumnScheduleGrid
import com.example.lets_go_slavgorod.ui.components.schedule.FilterableScheduleGrid
import com.example.lets_go_slavgorod.ui.components.schedule.UnifiedScheduleHeader
import com.example.lets_go_slavgorod.ui.viewmodel.BusViewModel

/**
 * Список расписаний с возможностью сворачивания секций
 * 
 * Функциональность:
 * - Отображение расписаний по секциям (отправления из разных точек)
 * - Возможность сворачивания/разворачивания секций
 * - Интеграция с избранными временами
 * - Подсветка ближайшего рейса
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScheduleList(
    route: BusRoute,
    schedulesSlavgorod: List<BusSchedule>,
    schedulesYarovoe: List<BusSchedule>,
    schedulesVokzal: List<BusSchedule>,
    schedulesSovhoz: List<BusSchedule>,
    nextUpcomingSlavgorodId: String?,
    nextUpcomingYarovoeId: String?,
    nextUpcomingVokzalId: String?,
    nextUpcomingSovhozId: String?,
    viewModel: BusViewModel,
    onBackClick: () -> Unit = {},
    onScrollOffsetChange: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isSlavgorodSectionExpanded by remember { mutableStateOf(true) }
    var isYarovoeSectionExpanded by remember { mutableStateOf(true) }
    var isVokzalSectionExpanded by remember { mutableStateOf(true) }
    var isSovhozSectionExpanded by remember { mutableStateOf(true) }

    // Состояние для отслеживания скролла
    val listState = rememberLazyListState()
    
    // Отслеживаем изменения позиции скролла
    LaunchedEffect(listState.firstVisibleItemScrollOffset, listState.firstVisibleItemIndex) {
        val offset = listState.firstVisibleItemIndex * 100f + listState.firstVisibleItemScrollOffset
        onScrollOffsetChange(offset)
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
        // Оптимизация производительности
        userScrollEnabled = true
    ) {
        // Заголовок маршрута как первый элемент списка
        item(key = "route_header") {
            UnifiedScheduleHeader(
                route = route,
                onBackClick = onBackClick,
                isVisible = true
            )
        }
        
        // Маршрут №102 (Славгород — Яровое) - прямое отображение с фильтрацией
        if (route.id == "102" && schedulesSlavgorod.isNotEmpty() && schedulesYarovoe.isNotEmpty()) {
            item(key = "route_102_schedule") {
                FilterableScheduleGrid(
                    leftSchedules = schedulesSlavgorod,
                    rightSchedules = schedulesYarovoe,
                    leftTitle = "Отправление из Рынок (Славгород)",
                    rightTitle = "Отправление из МСЧ-128 (Яровое)",
                    nextUpcomingLeftId = nextUpcomingSlavgorodId,
                    nextUpcomingRightId = nextUpcomingYarovoeId,
                    viewModel = viewModel,
                    route = route,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                )
            }
        }

        // Маршрут №102Б (Славгород — Яровое) - прямое отображение с фильтрацией
        if (route.id == "102B" && schedulesSlavgorod.isNotEmpty() && schedulesYarovoe.isNotEmpty()) {
            item(key = "route_102B_schedule") {
                FilterableScheduleGrid(
                    leftSchedules = schedulesSlavgorod,
                    rightSchedules = schedulesYarovoe,
                    leftTitle = "Отправление из Рынок (Славгород)",
                    rightTitle = "Отправление из Ст. Зори (Яровое)",
                    nextUpcomingLeftId = nextUpcomingSlavgorodId,
                    nextUpcomingRightId = nextUpcomingYarovoeId,
                    viewModel = viewModel,
                    route = route,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                )
            }
        }

        // Секции для маршрута №1 (Вокзал — Совхоз) - группировка по выходам
        if (route.id == "1") {
            // Группируем все расписания по выходам
            val allRoute1Schedules = schedulesVokzal + schedulesSovhoz
            val groupedByExit = allRoute1Schedules.groupBy { it.notes ?: "Без выхода" }
            
            // Сортируем выходы: "1 выход", "2 выход", "3 выход"
            val sortedExits = groupedByExit.keys.sortedBy { exitName ->
                when {
                    exitName.contains("1") -> 1
                    exitName.contains("2") -> 2
                    exitName.contains("3") -> 3
                    else -> 99
                }
            }.toList()
            
            // Создаем секцию для каждого выхода
            sortedExits.forEach { exitName ->
                val exitSchedules = groupedByExit[exitName] ?: emptyList()
                val vokzalSchedules = exitSchedules.filter { it.departurePoint == "вокзал" }
                val sovhozSchedules = exitSchedules.filter { it.departurePoint == "совхоз" }
                
                if (vokzalSchedules.isNotEmpty() && sovhozSchedules.isNotEmpty()) {
                    // Используем ключ для состояния секции
                    val sectionKey = "exit_${exitName.replace(" ", "_")}"
                    
                    ExpandableScheduleSection(
                        title = exitName.replaceFirstChar { it.uppercase() }, // "1 выход" -> "1 Выход"
                        schedules = vokzalSchedules,
                        nextUpcomingScheduleId = nextUpcomingVokzalId,
                        isExpanded = when(exitName) {
                            "1 выход" -> isVokzalSectionExpanded
                            "2 выход" -> isYarovoeSectionExpanded
                            "3 выход" -> isSovhozSectionExpanded
                            else -> true
                        },
                        onToggleExpand = {
                            when(exitName) {
                                "1 выход" -> isVokzalSectionExpanded = !isVokzalSectionExpanded
                                "2 выход" -> isYarovoeSectionExpanded = !isYarovoeSectionExpanded
                                "3 выход" -> isSovhozSectionExpanded = !isSovhozSectionExpanded
                            }
                        },
                        viewModel = viewModel,
                        route = route,
                        departurePointForCheck = "вокзал",
                        leftSchedules = vokzalSchedules,
                        rightSchedules = sovhozSchedules,
                        leftTitle = "Из вокзала",
                        rightTitle = "Из совхоза",
                        nextUpcomingLeftId = nextUpcomingVokzalId,
                        nextUpcomingRightId = nextUpcomingSovhozId
                    )
                }
            }
        }

        // Сообщение об отсутствии расписания
        if (shouldShowNoScheduleMessage(route)) {
            item {
                NoScheduleMessage(
                    departurePoint = "выбранного маршрута",
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

/**
 * Секция расписания с возможностью сворачивания и sticky header
 */
@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.ExpandableScheduleSection(
    title: String,
    schedules: List<BusSchedule>,
    nextUpcomingScheduleId: String?,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    viewModel: BusViewModel,
    route: BusRoute,
    departurePointForCheck: String,
    leftSchedules: List<BusSchedule>? = null,
    rightSchedules: List<BusSchedule>? = null,
    leftTitle: String? = null,
    rightTitle: String? = null,
    nextUpcomingLeftId: String? = null,
    nextUpcomingRightId: String? = null
) {
    // Sticky header для заголовка секции
    stickyHeader(key = "header_$departurePointForCheck") {
        StickyDepartureHeader(
            title = title,
            isExpanded = isExpanded,
            onToggleExpand = onToggleExpand
        )
    }
    
    // Превью ближайшего рейса в свернутом виде
    if (!isExpanded && nextUpcomingScheduleId != null) {
        item(key = "preview_$departurePointForCheck") {
            val favoriteTimesList by viewModel.favoriteTimes.collectAsState()
            val nextSchedule = schedules.find { it.id == nextUpcomingScheduleId }
            if (nextSchedule != null) {
                ScheduleCard(
                    schedule = nextSchedule,
                    isFavorite = favoriteTimesList.any { it.id == nextSchedule.id && it.isActive },
                    onFavoriteClick = {
                        val isCurrentlyFavorite = favoriteTimesList.any { it.id == nextSchedule.id && it.isActive }
                        if (isCurrentlyFavorite) {
                            viewModel.removeFavoriteTime(nextSchedule.id)
                        } else {
                            viewModel.addFavoriteTime(nextSchedule)
                        }
                    },
                    isNextUpcoming = true,
                    allSchedules = schedules,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }
    }
    
    // Содержимое секции (только если развернуто)
    if (isExpanded && schedules.isNotEmpty()) {
        if (leftSchedules != null && rightSchedules != null && leftTitle != null && rightTitle != null) {
            // Двухколоночное отображение
            item(key = "schedule_grid_$departurePointForCheck") {
                TwoColumnScheduleGrid(
                    leftSchedules = leftSchedules,
                    rightSchedules = rightSchedules,
                    leftTitle = leftTitle,
                    rightTitle = rightTitle,
                    nextUpcomingLeftId = nextUpcomingLeftId,
                    nextUpcomingRightId = nextUpcomingRightId,
                    viewModel = viewModel,
                    route = route,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                )
            }
        } else {
            // Обычное отображение списком - используем items вместо forEach
            items(
                items = schedules,
                key = { schedule -> schedule.id }
            ) { schedule ->
                val favoriteTimesList by viewModel.favoriteTimes.collectAsState()
                
                val isCurrentlyFavorite = remember(favoriteTimesList, schedule.id) {
                    favoriteTimesList.any { it.id == schedule.id && it.isActive }
                }

                ScheduleCard(
                    schedule = schedule,
                    isFavorite = isCurrentlyFavorite,
                    onFavoriteClick = {
                        if (isCurrentlyFavorite) {
                            viewModel.removeFavoriteTime(schedule.id)
                        } else {
                            viewModel.addFavoriteTime(schedule)
                        }
                    },
                    isNextUpcoming = schedule.id == nextUpcomingScheduleId,
                    allSchedules = schedules,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }
    } else if (isExpanded && schedules.isEmpty() && shouldShowNoScheduleMessage(route)) {
        item(key = "no_schedule_$departurePointForCheck") {
            NoScheduleMessage(
                departurePoint = departurePointForCheck,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

/**
 * Сообщение об отсутствии расписания
 */
@Composable
private fun NoScheduleMessage(
    departurePoint: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Для $departurePoint расписание отсутствует.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp)
        )
    }
}

/**
 * Проверяет, нужно ли показать сообщение об отсутствии расписания
 */
private fun shouldShowNoScheduleMessage(route: BusRoute): Boolean {
    return when (route.id) {
        "102" -> false // Маршрут 102 всегда имеет расписание
        "102B" -> false // Маршрут 102Б всегда имеет расписание
        "1" -> false  // Маршрут 1 всегда имеет расписание
        else -> true
    }
}
