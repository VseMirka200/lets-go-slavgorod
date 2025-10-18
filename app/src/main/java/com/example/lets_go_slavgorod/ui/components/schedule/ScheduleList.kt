package com.example.lets_go_slavgorod.ui.components.schedule

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lets_go_slavgorod.data.model.BusRoute
import com.example.lets_go_slavgorod.data.model.BusSchedule
import com.example.lets_go_slavgorod.ui.components.StickyDepartureHeader
import com.example.lets_go_slavgorod.ui.components.schedule.TwoColumnScheduleGrid
import com.example.lets_go_slavgorod.ui.components.schedule.FilterableScheduleGrid
import com.example.lets_go_slavgorod.ui.components.schedule.UnifiedScheduleHeader
import com.example.lets_go_slavgorod.ui.components.UniversalFilterRow
import com.example.lets_go_slavgorod.ui.components.FilterItem
import com.example.lets_go_slavgorod.ui.viewmodel.BusViewModel
import com.example.lets_go_slavgorod.utils.Constants
import com.example.lets_go_slavgorod.ui.utils.TextFormattingUtils
import com.example.lets_go_slavgorod.utils.ConditionalLogging
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

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
 * - **Маршрут №1**: сворачиваемые секции по выходам (Выход 1, 2, 3)
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleList(
    route: BusRoute,
    schedulesSlavgorod: List<BusSchedule>,
    schedulesYarovoe: List<BusSchedule>,
    schedulesVokzal: List<BusSchedule>,
    schedulesSovhoz: List<BusSchedule>,
    departurePoint1Name: String = "",
    departurePoint2Name: String = "",
    departurePoint3Name: String = "",
    departurePoint4Name: String = "",
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
    // Универсальные фильтры
    var selectedFilterId by remember { mutableStateOf<String?>(null) }
    val favoriteTimesList by viewModel.favoriteTimes.collectAsState()
    
    // Вычисляем производные состояния из selectedFilterId
    val showOnlyFavorites = selectedFilterId == "favorites"
    val showOnlyUpcoming = selectedFilterId == "upcoming"
    val selectedRoute1Exit = if (selectedFilterId?.startsWith("exit_") == true) selectedFilterId?.removePrefix("exit_") else null
    val selectedRoute4DeparturePoint = if (selectedFilterId?.startsWith("point4_") == true) selectedFilterId?.removePrefix("point4_") else null

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
        
        // Универсальные фильтры для всех маршрутов
        item(key = "filters") {
            val favCount = favoriteTimesList.count { it.isActive && it.routeId == route.id }
            
            UniversalFilterRow(
                filters = listOf(
                    FilterItem(
                        id = "favorites",
                        label = "Избранные",
                        icon = if (showOnlyFavorites) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        count = favCount
                    ),
                    FilterItem(
                        id = "upcoming",
                        label = "Следующий",
                        icon = if (showOnlyUpcoming) Icons.Filled.Schedule else Icons.Outlined.Schedule
                    )
                ),
                selectedFilterId = selectedFilterId,
                onFilterSelected = { selectedFilterId = it },
                useEqualWeights = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = Constants.PADDING_MEDIUM.dp, 
                        top = Constants.PADDING_FILTER_TOP.dp, 
                        end = Constants.PADDING_MEDIUM.dp, 
                        bottom = Constants.PADDING_FILTER_BOTTOM.dp
                    )
            )
        }
        
        // Маршрут №4 (Пригородный) - фильтрация по точкам отправления с приглушением
        if (route.id == "4") {
            // Кнопка для открытия модального окна выбора точки отправления
            item(key = "route_4_filters") {
                var showBottomSheet by remember { mutableStateOf(false) }
                
                // Определяем текущий выбранный пункт
                val selectedPointName = when {
                    selectedFilterId == "point4_$departurePoint1Name" -> departurePoint1Name
                    selectedFilterId == "point4_$departurePoint2Name" -> departurePoint2Name
                    selectedFilterId == "point4_$departurePoint3Name" -> departurePoint3Name
                    else -> "Все точки отправления"
                }
                
                // Кнопка для открытия меню
                OutlinedButton(
                    onClick = { showBottomSheet = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Constants.PADDING_MEDIUM.dp, vertical = 4.dp)
                        .height(56.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Точка отправления",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = selectedPointName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                                Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Выбрать точку отправления"
                        )
                    }
                }
                
                // Модальное окно с выбором
                if (showBottomSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showBottomSheet = false },
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            // Заголовок
                            Text(
                                text = "Выберите точку отправления",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                            )
                            
                            // Опция "Все"
                            ListItem(
                                headlineContent = {
                                Text(
                                        text = "Все точки отправления",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                },
                                leadingContent = {
                                    RadioButton(
                                        selected = selectedFilterId == null,
                                        onClick = null
                                    )
                                },
                                modifier = Modifier.clickable {
                                    selectedFilterId = null
                                    showBottomSheet = false
                                }
                            )
                            
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp))
                            
                            // Точка 1
                            if (schedulesSlavgorod.isNotEmpty()) {
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            text = departurePoint1Name,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    },
                                    leadingContent = {
                                        RadioButton(
                                            selected = selectedFilterId == "point4_$departurePoint1Name",
                                            onClick = null
                                        )
                                    },
                                    modifier = Modifier.clickable {
                                        selectedFilterId = "point4_$departurePoint1Name"
                                        showBottomSheet = false
                                    }
                                )
                            }
                            
                            // Точка 2
                            if (schedulesYarovoe.isNotEmpty()) {
                                ListItem(
                                    headlineContent = {
                                Text(
                                            text = departurePoint2Name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                    },
                                    leadingContent = {
                                        RadioButton(
                                            selected = selectedFilterId == "point4_$departurePoint2Name",
                                            onClick = null
                                        )
                                    },
                                    modifier = Modifier.clickable {
                                        selectedFilterId = "point4_$departurePoint2Name"
                                        showBottomSheet = false
                                    }
                                )
                            }
                            
                            // Точка 3
                            if (schedulesVokzal.isNotEmpty()) {
                                ListItem(
                                    headlineContent = {
                    Text(
                                            text = departurePoint3Name,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    },
                                    leadingContent = {
                                        RadioButton(
                                            selected = selectedFilterId == "point4_$departurePoint3Name",
                                            onClick = null
                                        )
                                    },
                                    modifier = Modifier.clickable {
                                        selectedFilterId = "point4_$departurePoint3Name"
                                        showBottomSheet = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
            
            // Объединяем все расписания маршрута 4 для проверки фильтров
            val allRoute4Schedules = schedulesSlavgorod + schedulesYarovoe + schedulesVokzal
            
            // Проверяем есть ли хоть что-то после применения фильтров
            val hasFilteredSchedules = when {
                showOnlyFavorites -> allRoute4Schedules.any { schedule -> 
                    favoriteTimesList.any { it.id == schedule.id && it.isActive }
                }
                showOnlyUpcoming -> allRoute4Schedules.any { schedule ->
                    schedule.id == nextUpcomingSlavgorodId || 
                    schedule.id == nextUpcomingYarovoeId || 
                    schedule.id == nextUpcomingVokzalId
                }
                else -> true
            }
            
            // Показываем расписания только если есть что показывать после фильтров
            if (hasFilteredSchedules) {
                // Точки 1 и 2 в двухколоночной сетке
                if (schedulesSlavgorod.isNotEmpty() || schedulesYarovoe.isNotEmpty()) {
                    item(key = "route_4_grid_1_2") {
                        // Определяем состояние фильтра для приглушения заголовков и расписаний
                        val externalFilterState = when (selectedRoute4DeparturePoint) {
                            departurePoint1Name -> true  // Левая активна, правая приглушена
                            departurePoint2Name -> false // Правая активна, левая приглушена
                            else -> null // Обе активны или обе приглушены
                        }
                        
                        // Приглушаем весь блок, если выбрана точка 3
                        val shouldDimGrid = selectedRoute4DeparturePoint == departurePoint3Name
                        
                        TwoColumnScheduleGrid(
                    leftSchedules = schedulesSlavgorod,
                    rightSchedules = schedulesYarovoe,
                            leftTitle = departurePoint1Name,
                            rightTitle = departurePoint2Name,
                    nextUpcomingLeftId = nextUpcomingSlavgorodId,
                    nextUpcomingRightId = nextUpcomingYarovoeId,
                    viewModel = viewModel,
                    route = route,
                    showOnlyFavorites = showOnlyFavorites,
                    showOnlyUpcoming = showOnlyUpcoming,
                            enableHeaderClick = false,
                            externalFilterState = externalFilterState,
                            modifier = Modifier
                                .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 8.dp)
                                .alpha(if (shouldDimGrid) 0.4f else 1f)
                        )
                    }
                }

                // Точка 3 отдельно
                if (schedulesVokzal.isNotEmpty()) {
                    item(key = "route_4_point3") {
                        // Определяем состояние приглушения для точки 3
                        val shouldDimPoint3 = selectedRoute4DeparturePoint != null && selectedRoute4DeparturePoint != departurePoint3Name
                        
                        TwoColumnScheduleGrid(
                            leftSchedules = schedulesVokzal,
                            rightSchedules = emptyList(),
                            leftTitle = departurePoint3Name,
                            rightTitle = "",
                            nextUpcomingLeftId = nextUpcomingVokzalId,
                            nextUpcomingRightId = null,
                            viewModel = viewModel,
                            route = route,
                            showOnlyFavorites = showOnlyFavorites,
                            showOnlyUpcoming = showOnlyUpcoming,
                            enableHeaderClick = false,
                            externalFilterState = if (shouldDimPoint3) false else null,
                            modifier = Modifier
                                .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 8.dp)
                                .alpha(if (shouldDimPoint3) 0.4f else 1f)
                        )
                    }
                }
            } else {
                // Показываем ОДНО сообщение для всего маршрута 4 если нет данных после фильтрации
                item(key = "route_4_no_filtered") {
                    Box(
                        modifier = Modifier
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
            }
        }
        
        // Универсальная логика для маршрутов 102, 102Б - двухколоночная сетка
        if ((route.id == "102" || route.id == "102B") && schedulesSlavgorod.isNotEmpty() && schedulesYarovoe.isNotEmpty()) {
            item(key = "route_${route.id}_schedule_grid") {
                FilterableScheduleGrid(
                    leftSchedules = schedulesSlavgorod,
                    rightSchedules = schedulesYarovoe,
                    leftTitle = departurePoint1Name,
                    rightTitle = departurePoint2Name,
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

        // Маршрут №1 (Вокзал — Совхоз) - фильтрация по выходам
        if (route.id == "1") {
            // Для маршрута 1 все расписания попадают в schedulesSlavgorod и schedulesYarovoe
            // (т.к. "вокзал" и "совхоз" - первые две точки по алфавиту)
            val allRoute1Schedules = schedulesSlavgorod + schedulesYarovoe
            val groupedByExit = allRoute1Schedules.groupBy { it.notes ?: "Без выхода" }
            
            // Сортируем выходы: "Выход 1", "Выход 2", "Выход 3"
            val sortedExits = groupedByExit.keys.sortedBy { exitName ->
                when {
                    exitName.contains("1") -> 1
                    exitName.contains("2") -> 2
                    exitName.contains("3") -> 3
                    else -> 99
                }
            }.toList()
            
            // Кнопки фильтрации по выходам
            item(key = "route_1_filters") {
                val filters = sortedExits.map { exitName ->
                    FilterItem(id = "exit_$exitName", label = exitName)
                }
                
                UniversalFilterRow(
                    filters = filters,
                    selectedFilterId = selectedFilterId,
                    onFilterSelected = { selectedFilterId = it },
                    useEqualWeights = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Constants.PADDING_MEDIUM.dp)
                )
            }
            
            // Показываем все выходы в две колонки: Вокзал | Совхоз
            sortedExits.forEach { exitName ->
                val exitSchedules = groupedByExit[exitName] ?: emptyList()
                val vokzalSchedules = exitSchedules.filter { it.stopName.contains("вокзал", ignoreCase = true) }
                val sovhozSchedules = exitSchedules.filter { it.stopName.contains("совхоз", ignoreCase = true) }
                
                if ((vokzalSchedules.isNotEmpty() || sovhozSchedules.isNotEmpty()) &&
                    (selectedRoute1Exit == null || selectedRoute1Exit == exitName)) {
                    item(key = "route_1_exit_$exitName") {
                        Column {
                            // Заголовок выхода - красивый с фоном
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = exitName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                            
                            TwoColumnScheduleGrid(
                                leftSchedules = vokzalSchedules,
                                rightSchedules = sovhozSchedules,
                                leftTitle = "Вокзал",
                                rightTitle = "Совхоз",
                                nextUpcomingLeftId = getNextUpcomingScheduleId(vokzalSchedules),
                                nextUpcomingRightId = getNextUpcomingScheduleId(sovhozSchedules),
                        viewModel = viewModel,
                        route = route,
                        showOnlyFavorites = showOnlyFavorites,
                                showOnlyUpcoming = showOnlyUpcoming,
                                modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                            )
                        }
                    }
                }
            }
        }
        
        // Маршрут №3 (Кольцевой) - две секции: Радиозавод и Мясокомбинат
        if (route.id == "3" && schedulesSlavgorod.isNotEmpty()) {
            // Фильтры для выбора направления
            item(key = "route_3_direction_filters") {
                val selectedRoute3Direction = if (selectedFilterId?.startsWith("dir3_") == true) selectedFilterId?.removePrefix("dir3_") else null
                
                val filters = listOf(
                    FilterItem(id = "dir3_radio", label = "Радиозавод"),
                    FilterItem(id = "dir3_meat", label = "Мясокомбинат")
                )
                
                UniversalFilterRow(
                    filters = filters,
                    selectedFilterId = selectedFilterId,
                    onFilterSelected = { selectedFilterId = it },
                    useEqualWeights = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Constants.PADDING_MEDIUM.dp)
                )
            }
            
            // Группируем расписания по направлениям
            val selectedRoute3Direction = if (selectedFilterId?.startsWith("dir3_") == true) selectedFilterId?.removePrefix("dir3_") else null
            
            val allRadioSchedules = schedulesSlavgorod.filter { it.notes?.contains("Радиозавод") == true }
            val allMeatSchedules = schedulesSlavgorod.filter { it.notes?.contains("Мясокомбинат") == true }
            
            // Разделяем по дням для Радиозавода
            val radioWeekday = allRadioSchedules.filter { it.notes?.contains("будни") == true }
            val radioWeekend = allRadioSchedules.filter { it.notes?.contains("суббота") == true || it.notes?.contains("выходные") == true }
            
            // Секция Радиозавода
            if ((radioWeekday.isNotEmpty() || radioWeekend.isNotEmpty()) && 
                (selectedRoute3Direction == null || selectedRoute3Direction == "radio")) {
                item(key = "route_3_radio_header") {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Радиозавод",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                
                item(key = "route_3_radio_grid") {
                    TwoColumnScheduleGrid(
                        leftSchedules = radioWeekday,
                        rightSchedules = radioWeekend,
                        leftTitle = "Будни",
                        rightTitle = "Выходные",
                        nextUpcomingLeftId = getNextUpcomingScheduleId(radioWeekday),
                        nextUpcomingRightId = getNextUpcomingScheduleId(radioWeekend),
                        viewModel = viewModel,
                        route = route,
                        showOnlyFavorites = showOnlyFavorites,
                        showOnlyUpcoming = showOnlyUpcoming,
                        modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 16.dp)
                    )
                }
            }
            
            // Разделяем по дням для Мясокомбината
            val meatWeekday = allMeatSchedules.filter { it.notes?.contains("будни") == true }
            val meatWeekend = allMeatSchedules.filter { it.notes?.contains("выходные") == true }
            
            // Секция Мясокомбината
            if ((meatWeekday.isNotEmpty() || meatWeekend.isNotEmpty()) && 
                (selectedRoute3Direction == null || selectedRoute3Direction == "meat")) {
                item(key = "route_3_meat_header") {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Мясокомбинат",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                
                item(key = "route_3_meat_grid") {
                    TwoColumnScheduleGrid(
                        leftSchedules = meatWeekday,
                        rightSchedules = meatWeekend,
                        leftTitle = "Будни",
                        rightTitle = "Выходные",
                        nextUpcomingLeftId = getNextUpcomingScheduleId(meatWeekday),
                        nextUpcomingRightId = getNextUpcomingScheduleId(meatWeekend),
                        viewModel = viewModel,
                        route = route,
                        showOnlyFavorites = showOnlyFavorites,
                        showOnlyUpcoming = showOnlyUpcoming,
                        modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                    )
                }
            }
        }

        // Сообщение об отсутствии расписания (только если действительно нет расписаний)
        if (shouldShowNoScheduleMessage(route, schedulesSlavgorod, schedulesYarovoe, schedulesVokzal, schedulesSovhoz)) {
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
 * Проверяет реально есть ли хоть какие-то расписания для маршрута
 */
private fun shouldShowNoScheduleMessage(
    route: BusRoute,
    schedulesSlavgorod: List<BusSchedule>,
    schedulesYarovoe: List<BusSchedule>,
    schedulesVokzal: List<BusSchedule>,
    schedulesSovhoz: List<BusSchedule>
): Boolean {
    // Показываем сообщение только если НЕТ вообще никаких расписаний
    val totalSchedules = schedulesSlavgorod.size + schedulesYarovoe.size + 
                        schedulesVokzal.size + schedulesSovhoz.size
    return totalSchedules == 0
}

/**
 * Определяет ID ближайшего предстоящего рейса из списка расписаний
 */
private fun getNextUpcomingScheduleId(schedules: List<BusSchedule>): String? {
    if (schedules.isEmpty()) return null
    
    val currentTime = Calendar.getInstance()
    val timeFormat = SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    
    val upcomingToday = schedules.filter { schedule ->
        try {
            val departureTime = timeFormat.parse(schedule.departureTime)
            if (departureTime != null) {
                val scheduleCalendar = Calendar.getInstance().apply {
                    time = departureTime
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                scheduleCalendar.after(currentTime)
            } else {
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Error parsing time: ${schedule.departureTime}")
            false
        }
    }
    
    if (upcomingToday.isNotEmpty()) {
        return upcomingToday.first().id
    }
    
    return schedules.firstOrNull()?.id
}
