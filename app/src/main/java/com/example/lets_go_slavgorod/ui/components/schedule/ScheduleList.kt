package com.example.lets_go_slavgorod.ui.components.schedule

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lets_go_slavgorod.data.model.BusRoute
import com.example.lets_go_slavgorod.data.model.BusSchedule
import com.example.lets_go_slavgorod.ui.components.StickyDepartureHeader
import com.example.lets_go_slavgorod.ui.components.schedule.TwoColumnScheduleGrid
import com.example.lets_go_slavgorod.ui.components.schedule.FilterableScheduleGrid
import com.example.lets_go_slavgorod.ui.components.schedule.UnifiedScheduleHeader
import com.example.lets_go_slavgorod.ui.viewmodel.BusViewModel
import com.example.lets_go_slavgorod.utils.Constants
import com.example.lets_go_slavgorod.ui.utils.TextFormattingUtils

/**
 * Основной компонент списка расписаний маршрута
 * 
 * Версия: 3.1
 * Последнее обновление: Октябрь 2025
 * 
 * Центральный компонент для отображения расписаний автобусных маршрутов.
 * Поддерживает различные варианты компоновки, фильтрацию и интерактивное управление.
 * 
 * Изменения v3.1:
 * - Унифицированы отступы между фильтрами и содержимым (8dp сверху, 8dp снизу)
 * - Добавлены порядковые номера рейсов в компактных карточках
 * - Оптимизирована компоновка фильтров на всю ширину экрана
 * - Используются константы из Constants.kt для всех отступов
 * 
 * Изменения v3.0:
 * - Добавлены фильтры "Избранные" и "Следующий" с иконками
 * - Фильтры взаимоисключающие (можно выбрать только один)
 * - Счетчик избранных времен с правильным склонением
 * - Улучшена компоновка фильтров на всю ширину
 * - Оптимизирована работа с избранными временами через TextFormattingUtils
 * 
 * Варианты отображения:
 * - **Маршрут №102**: двухколоночная сетка Славгород ↔ Яровое (МСЧ-128) с фильтрацией
 * - **Маршрут №102Б**: двухколоночная сетка Славгород ↔ Яровое (Ст. Зори) с фильтрацией
 * - **Маршрут №1**: сворачиваемые секции по выходам (1 выход, 2 выход, 3 выход)
 * 
 * Функциональность:
 * - Унифицированный заголовок с информацией о маршруте (UnifiedScheduleHeader)
 * - Фильтрация по избранным временам и следующим рейсам
 * - Добавление/удаление времен в избранное (звёздочка на карточках)
 * - Подсветка ближайших рейсов с обратным отсчетом
 * - Сворачивание/разворачивание секций (для маршрута №1)
 * - Порядковые номера для быстрой навигации
 * 
 * Оптимизации:
 * - LazyColumn для эффективной прокрутки больших списков
 * - Sticky headers для заголовков секций (остаются видимыми при прокрутке)
 * - Уникальные ключи для всех элементов (избегаем перерисовки)
 * - Remember для кэширования состояний фильтров и секций
 * 
 * @param route маршрут для отображения расписания
 * @param schedulesSlavgorod расписания отправлений из Славгорода
 * @param schedulesYarovoe расписания отправлений из Яровое
 * @param schedulesVokzal расписания отправлений с вокзала (маршрут №1)
 * @param schedulesSovhoz расписания отправлений из совхоза (маршрут №1)
 * @param nextUpcomingSlavgorodId ID ближайшего рейса из Славгорода
 * @param nextUpcomingYarovoeId ID ближайшего рейса из Яровое
 * @param nextUpcomingVokzalId ID ближайшего рейса с вокзала
 * @param nextUpcomingSovhozId ID ближайшего рейса из совхоза
 * @param viewModel ViewModel для управления избранными временами
 * @param onBackClick callback для кнопки "Назад" в заголовке
 * @param onNotificationClick callback для кнопки уведомлений в заголовке (может быть null)
 * @param onScrollOffsetChange callback при изменении позиции прокрутки (используется для анимаций)
 * @param modifier модификатор для настройки внешнего вида
 * 
 * Фильтры:
 * - **Избранные**: показывает только избранные пользователем времена
 * - **Следующий**: показывает только ближайшие предстоящие рейсы
 * - Фильтры взаимоисключающие (активен только один или ни одного)
 */
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
    onNotificationClick: (() -> Unit)? = null,
    onScrollOffsetChange: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Состояния для секций выходов маршрута №1
    var isExit1Expanded by remember { mutableStateOf(true) }
    var isExit2Expanded by remember { mutableStateOf(true) }
    var isExit3Expanded by remember { mutableStateOf(true) }
    
    // Фильтры
    var showOnlyFavorites by remember { mutableStateOf(false) }
    var showOnlyUpcoming by remember { mutableStateOf(false) }
    val favoriteTimesList by viewModel.favoriteTimes.collectAsState()

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
                isVisible = true,
                onNotificationClick = onNotificationClick
            )
        }
        
        // Фильтры
        item(key = "filters") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = Constants.PADDING_MEDIUM.dp, 
                        top = Constants.PADDING_FILTER_TOP.dp, 
                        end = Constants.PADDING_MEDIUM.dp, 
                        bottom = Constants.PADDING_FILTER_BOTTOM.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Фильтр "Избранные"
                    FilterChip(
                        selected = showOnlyFavorites,
                        onClick = { 
                            showOnlyFavorites = !showOnlyFavorites
                            if (showOnlyFavorites) showOnlyUpcoming = false
                        },
                        label = { 
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (showOnlyFavorites) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                    contentDescription = null,
                                    modifier = Modifier.size(Constants.FILTER_ICON_SIZE.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Избранные",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(Constants.FILTER_CHIP_HEIGHT.dp)
                    )
                    
                    // Фильтр "Следующий"
                    FilterChip(
                        selected = showOnlyUpcoming,
                        onClick = { 
                            showOnlyUpcoming = !showOnlyUpcoming
                            if (showOnlyUpcoming) showOnlyFavorites = false
                        },
                        label = { 
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (showOnlyUpcoming) Icons.Filled.Schedule else Icons.Outlined.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(Constants.FILTER_ICON_SIZE.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Следующий",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(Constants.FILTER_CHIP_HEIGHT.dp)
                    )
                }
                
                // Счетчик избранных
                if (showOnlyFavorites) {
                    val favCount = favoriteTimesList.count { it.isActive && it.routeId == route.id }
                    Text(
                        text = TextFormattingUtils.formatCounter(
                            favCount,
                            "время",
                            "времени",
                            "времен"
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }
            }
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
                    showOnlyFavorites = showOnlyFavorites,
                    showOnlyUpcoming = showOnlyUpcoming,
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 0.dp, bottom = 8.dp)
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
                    showOnlyFavorites = showOnlyFavorites,
                    showOnlyUpcoming = showOnlyUpcoming,
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 0.dp, bottom = 8.dp)
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
            sortedExits.forEachIndexed { index, exitName ->
                val exitSchedules = groupedByExit[exitName] ?: emptyList()
                val vokzalSchedules = exitSchedules.filter { it.departurePoint == Constants.STOP_ROUTE1_VOKZAL }
                val sovhozSchedules = exitSchedules.filter { it.departurePoint == Constants.STOP_ROUTE1_SOVHOZ }
                
                if (vokzalSchedules.isNotEmpty() && sovhozSchedules.isNotEmpty()) {
                    ExpandableScheduleSection(
                        title = exitName.replaceFirstChar { it.uppercase() },
                        isExpanded = when(exitName) {
                            "1 выход" -> isExit1Expanded
                            "2 выход" -> isExit2Expanded
                            "3 выход" -> isExit3Expanded
                            else -> true
                        },
                        onToggleExpand = {
                            when(exitName) {
                                "1 выход" -> isExit1Expanded = !isExit1Expanded
                                "2 выход" -> isExit2Expanded = !isExit2Expanded
                                "3 выход" -> isExit3Expanded = !isExit3Expanded
                            }
                        },
                        viewModel = viewModel,
                        route = route,
                        departurePointForCheck = "exit_$index",
                        leftSchedules = vokzalSchedules,
                        rightSchedules = sovhozSchedules,
                        leftTitle = "Из вокзала",
                        rightTitle = "Из совхоза",
                        nextUpcomingLeftId = nextUpcomingVokzalId,
                        nextUpcomingRightId = nextUpcomingSovhozId,
                        showOnlyFavorites = showOnlyFavorites,
                        showOnlyUpcoming = showOnlyUpcoming
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
 * 
 * Версия: 2.1
 * Последнее обновление: Октябрь 2025
 * 
 * Создает раздел в списке расписаний с заголовком, который "прилипает" к верхней части
 * экрана при прокрутке (sticky header). Секция может быть свернута/развернута одним кликом.
 * 
 * Используется для:
 * - Секций выходов маршрута №1 ("1 Выход", "2 Выход", "3 Выход")
 * - Группировки расписаний по точкам отправления (вокзал ↔ совхоз)
 * 
 * Функциональность:
 * - Sticky header с названием секции и кнопкой сворачивания/разворачивания
 * - Полное расписание в развернутом состоянии (двухколоночная сетка)
 * - Двухколоночная сетка (вокзал | совхоз) с порядковыми номерами
 * - При сворачивании расписание полностью скрывается
 * - Поддержка фильтров "Избранные" и "Следующий"
 * 
 * Изменения v2.1:
 * - Добавлены порядковые номера рейсов в TwoColumnScheduleGrid
 * - Используются константы из Constants.kt для отступов
 * 
 * Изменения v2.0:
 * - Убрано превью ближайшего рейса в свернутом состоянии
 * - При сворачивании секция полностью скрывается (без preview)
 * - Добавлена поддержка фильтров "Избранные" и "Следующий"
 * 
 * @param title название секции (например, "1 Выход")
 * @param isExpanded развернута ли секция
 * @param onToggleExpand callback для сворачивания/разворачивания
 * @param viewModel ViewModel для управления избранным
 * @param route маршрут (для добавления в избранное)
 * @param departurePointForCheck уникальный ключ секции
 * @param leftSchedules расписания левой колонки (вокзал)
 * @param rightSchedules расписания правой колонки (совхоз)
 * @param leftTitle заголовок левой колонки
 * @param rightTitle заголовок правой колонки
 * @param nextUpcomingLeftId ID ближайшего рейса слева
 * @param nextUpcomingRightId ID ближайшего рейса справа
 * @param showOnlyFavorites фильтр избранных
 * @param showOnlyUpcoming фильтр следующего рейса
 */
@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.ExpandableScheduleSection(
    title: String,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    viewModel: BusViewModel,
    route: BusRoute,
    departurePointForCheck: String,
    leftSchedules: List<BusSchedule>,
    rightSchedules: List<BusSchedule>,
    leftTitle: String,
    rightTitle: String,
    nextUpcomingLeftId: String?,
    nextUpcomingRightId: String?,
    showOnlyFavorites: Boolean = false,
    showOnlyUpcoming: Boolean = false
) {
    // Sticky header для заголовка секции
    stickyHeader(key = "header_$departurePointForCheck") {
        StickyDepartureHeader(
            title = title,
            isExpanded = isExpanded,
            onToggleExpand = onToggleExpand
        )
    }
    
    // Содержимое секции (только если развернуто)
    if (isExpanded) {
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
                        showOnlyFavorites = showOnlyFavorites,
                        showOnlyUpcoming = showOnlyUpcoming,
                        modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
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
