package com.example.lets_go_slavgorod.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ExperimentalMaterial3Api
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.lets_go_slavgorod.data.model.BusRoute
import com.example.lets_go_slavgorod.data.model.BusSchedule
import com.example.lets_go_slavgorod.ui.components.schedule.ScheduleList
import com.example.lets_go_slavgorod.ui.viewmodel.BusViewModel
import com.example.lets_go_slavgorod.utils.ScheduleUtils
import com.example.lets_go_slavgorod.utils.ConditionalLogging
import com.example.lets_go_slavgorod.utils.Constants
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Calendar

/**
 * Экран расписания конкретного маршрута
 * 
 * Отображает полное расписание автобусного маршрута с детальной информацией,
 * разделением по точкам отправления и интерактивными элементами.
 * 
 * Структура экрана:
 * 1. Заголовок (UnifiedScheduleHeader):
 *    - Стрелка назад
 *    - Название маршрута
 *    - Кнопка настроек уведомлений
 *    - Детальная информация (время в пути, стоимость, оплата)
 * 
 * 2. Расписание (ScheduleList):
 *    - Варьируется в зависимости от маршрута
 *    - Добавление времен в избранное (звёздочка)
 *    - Подсветка ближайших рейсов
 * 
 * Функциональность:
 * - Загрузка расписания из JSON или fallback данных
 * - Фильтрация расписаний по точкам отправления
 * - Определение ближайших рейсов для каждой точки
 * - Добавление/удаление времен в избранное
 * - Навигация к настройкам уведомлений маршрута
 * - Анимация загрузки (минимум 1 секунда)
 * 
 * Точки отправления:
 * - Маршруты 102/102Б: Славгород (Рынок) ↔ Яровое (МСЧ-128/Зори)
 * - Маршрут №1: Вокзал ↔ Совхоз (по выходам)
 * 
 * @param route маршрут для отображения расписания (null = пустое состояние)
 * @param onBackClick callback для возврата на главный экран
 * @param viewModel BusViewModel для данных и избранного
 * @param onNotificationClick callback для открытия настроек уведомлений маршрута
 * 
 * @author VseMirka200
 * @version 3.0
 * @since 1.0
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    route: BusRoute?,
    onBackClick: () -> Unit,
    viewModel: BusViewModel,
    onNotificationClick: ((String) -> Unit)? = null
) {
    // Состояние загрузки и данных
    // Remember с зависимостью от route гарантирует сброс при смене маршрута
    var isLoading by remember(route) { mutableStateOf(true) }
    
    // Состояние для Pull-to-Refresh
    var isRefreshing by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    
    // Состояние для управления видимостью заголовка при скролле
    var isHeaderVisible by remember { mutableStateOf(true) }
    var lastScrollOffset by remember { mutableStateOf(0f) }
    
    // Расписания для каждой точки отправления
    var schedulesSlavgorod by remember { mutableStateOf<List<BusSchedule>>(emptyList()) }
    var schedulesYarovoe by remember { mutableStateOf<List<BusSchedule>>(emptyList()) }
    var schedulesVokzal by remember { mutableStateOf<List<BusSchedule>>(emptyList()) }
    var schedulesSovhoz by remember { mutableStateOf<List<BusSchedule>>(emptyList()) }
    
    // Названия точек отправления (динамически определяются из данных)
    var departurePoint1Name by remember { mutableStateOf("") }
    var departurePoint2Name by remember { mutableStateOf("") }
    var departurePoint3Name by remember { mutableStateOf("") }
    var departurePoint4Name by remember { mutableStateOf("") }
    
    // ID ближайших рейсов для каждой точки отправления
    // Используются для подсветки и анимации
    var nextUpcomingSlavgorodId by remember { mutableStateOf<String?>(null) }
    var nextUpcomingYarovoeId by remember { mutableStateOf<String?>(null) }
    var nextUpcomingVokzalId by remember { mutableStateOf<String?>(null) }
    var nextUpcomingSovhozId by remember { mutableStateOf<String?>(null) }
    
    // Динамическая загрузка и обработка данных расписания
    // LaunchedEffect с зависимостью от route и refreshTrigger перезапускает загрузку
    LaunchedEffect(route, refreshTrigger) {
        if (route != null) {
            isLoading = true
            ConditionalLogging.debug("Schedule") { "Starting schedule generation for route ${route.id}" }
            
            val startTime = System.currentTimeMillis()
            
            // Загружаем расписание через BusViewModel
            // Логика загрузки: сначала пытаемся загрузить из JSON, потом fallback на hardcoded
            val allSchedules = viewModel.getSchedulesForRoute(route.id)
            ConditionalLogging.debug("Schedule") { "Loaded ${allSchedules.size} schedules for route ${route.id}" }
            if (route.id == "102B") {
                ConditionalLogging.debug("Schedule") { "102B schedules: ${allSchedules.map { "${it.departurePoint} - ${it.departureTime}" }}" }
            }
            
            // Динамически определяем уникальные точки отправления из загруженных расписаний
            // Это позволяет автоматически поддерживать новые маршруты без изменения кода
            // Порядок сохраняется как в JSON (без сортировки по алфавиту)
            val uniqueDeparturePoints = allSchedules
                .map { it.departurePoint }
                .distinct()
            
            ConditionalLogging.debug("Schedule") { "Found ${uniqueDeparturePoints.size} unique departure points: $uniqueDeparturePoints" }
            
            // Фильтруем расписания для каждой уникальной точки отправления
            // Используем первую точку как "основную" (schedulesSlavgorod)
            // Вторую как "вторичную" (schedulesYarovoe), и т.д.
            val schedulesByPoint = uniqueDeparturePoints.map { point ->
                allSchedules.filter { it.departurePoint == point }.sortedBy { it.departureTime }
            }
            
            // Присваиваем расписания в переменные для совместимости с существующим UI
            schedulesSlavgorod = schedulesByPoint.getOrNull(0) ?: emptyList()
            schedulesYarovoe = schedulesByPoint.getOrNull(1) ?: emptyList()
            schedulesVokzal = schedulesByPoint.getOrNull(2) ?: emptyList()
            schedulesSovhoz = schedulesByPoint.getOrNull(3) ?: emptyList()
            
            // Сохраняем названия точек отправления
            departurePoint1Name = uniqueDeparturePoints.getOrNull(0) ?: ""
            departurePoint2Name = uniqueDeparturePoints.getOrNull(1) ?: ""
            departurePoint3Name = uniqueDeparturePoints.getOrNull(2) ?: ""
            departurePoint4Name = uniqueDeparturePoints.getOrNull(3) ?: ""
            
            ConditionalLogging.debug("Schedule") { "Point 1 ($departurePoint1Name): ${schedulesSlavgorod.size} schedules" }
            ConditionalLogging.debug("Schedule") { "Point 2 ($departurePoint2Name): ${schedulesYarovoe.size} schedules" }
            ConditionalLogging.debug("Schedule") { "Point 3 ($departurePoint3Name): ${schedulesVokzal.size} schedules" }
            ConditionalLogging.debug("Schedule") { "Point 4 ($departurePoint4Name): ${schedulesSovhoz.size} schedules" }
            
            // Определяем ID ближайших рейсов для каждой точки
            // Эти рейсы будут подсвечены и анимированы в UI
            nextUpcomingSlavgorodId = getNextUpcomingScheduleId(schedulesSlavgorod)
            nextUpcomingYarovoeId = getNextUpcomingScheduleId(schedulesYarovoe)
            nextUpcomingVokzalId = getNextUpcomingScheduleId(schedulesVokzal)
            nextUpcomingSovhozId = getNextUpcomingScheduleId(schedulesSovhoz)
            
            val elapsedTime = System.currentTimeMillis() - startTime
            ConditionalLogging.debug("Schedule") { "Schedule data fully loaded in ${elapsedTime}ms" }
            
            // Гарантируем показ анимации загрузки минимум указанное время
            // Это улучшает UX, не допуская "мерцания" при быстрой загрузке
            if (elapsedTime < Constants.MIN_LOADING_ANIMATION_MS) {
                delay(Constants.MIN_LOADING_ANIMATION_MS - elapsedTime)
            }
            
            isLoading = false
        } else {
            // Если маршрут null (ошибка навигации), сразу показываем пустое состояние
            isLoading = false
        }
    }

        if (route == null) {
            NoRouteSelectedMessage(Modifier.fillMaxSize())
        } else if (isLoading) {
            // Анимация загрузки расписания
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Загрузка расписания...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Pull-to-Refresh для обновления расписания
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    coroutineScope.launch {
                        try {
                            // Очищаем кэш расписания для этого маршрута
                            route.id.let { routeId ->
                                viewModel.refreshScheduleForRoute(routeId)
                            }
                            
                            // Небольшая задержка для анимации
                            delay(com.example.lets_go_slavgorod.utils.Constants.PULL_TO_REFRESH_MIN_DELAY_MS)
                            
                            // Триггерим перезагрузку
                            refreshTrigger++
                        } finally {
                            isRefreshing = false
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) {
                ScheduleList(
                    route = route,
                    schedulesSlavgorod = schedulesSlavgorod,
                    schedulesYarovoe = schedulesYarovoe,
                    schedulesVokzal = schedulesVokzal,
                    schedulesSovhoz = schedulesSovhoz,
                    departurePoint1Name = departurePoint1Name,
                    departurePoint2Name = departurePoint2Name,
                    departurePoint3Name = departurePoint3Name,
                    departurePoint4Name = departurePoint4Name,
                    nextUpcomingSlavgorodId = nextUpcomingSlavgorodId,
                    nextUpcomingYarovoeId = nextUpcomingYarovoeId,
                    nextUpcomingVokzalId = nextUpcomingVokzalId,
                    nextUpcomingSovhozId = nextUpcomingSovhozId,
                    viewModel = viewModel,
                    onBackClick = onBackClick,
                    onNotificationClick = if (onNotificationClick != null) {
                        { onNotificationClick(route.id) }
                    } else null,
                    onScrollOffsetChange = { offset ->
                        // Можно удалить, так как заголовок теперь скроллится естественным образом
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

/**
 * Определяет ID ближайшего предстоящего рейса из списка расписаний
 * 
 * Алгоритм определения ближайшего рейса:
 * 1. Проверяет наличие расписаний в списке
 * 2. Получает текущее время для сравнения
 * 3. Парсит время отправления каждого рейса
 * 4. Фильтрует только будущие рейсы (после текущего времени)
 * 5. Возвращает первый (ближайший) рейс из будущих
 * 6. Если будущих рейсов нет сегодня, возвращает первый рейс завтра
 * 
 * Используется для подсветки и анимации ближайшего рейса в UI.
 * Критично для UX - пользователь сразу видит когда будет следующий автобус.
 * 
 * @param schedules отсортированный по времени список расписаний для одной точки отправления
 * @return ID ближайшего рейса или null если расписаний нет
 * 
 * @sample
 * Текущее время: 14:30
 * Расписания: [14:00, 15:00, 16:00]
 * Результат: ID рейса в 15:00 (первый будущий рейс)
 */
private fun getNextUpcomingScheduleId(schedules: List<BusSchedule>): String? {
    if (schedules.isEmpty()) return null
    
    val currentTime = Calendar.getInstance()
    val timeFormat = SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    
    // Находим все рейсы, которые еще не прошли сегодня
    // Парсим время и сравниваем с текущим
    val upcomingToday = schedules.filter { schedule ->
        try {
            val departureTime = timeFormat.parse(schedule.departureTime)
            if (departureTime != null) {
                // Создаем Calendar для времени отправления
                val scheduleCalendar = Calendar.getInstance().apply {
                    time = departureTime
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                // Проверяем что рейс в будущем
                scheduleCalendar.after(currentTime)
            } else {
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Error parsing time: ${schedule.departureTime}")
            false
        }
    }
    
    // Если есть рейсы сегодня, возвращаем ближайший (первый в отфильтрованном списке)
    if (upcomingToday.isNotEmpty()) {
        ConditionalLogging.debug("Schedule") { "Found ${upcomingToday.size} upcoming departures today. Next: ${upcomingToday.first().departureTime}" }
        return upcomingToday.first().id
    }
    
    // Если рейсов сегодня больше нет, возвращаем первый рейс завтра
    // (расписание циклическое - первый рейс в списке будет завтра)
    val firstTomorrow = schedules.firstOrNull()
    ConditionalLogging.debug("Schedule") { "No departures today. First tomorrow: ${firstTomorrow?.departureTime}" }
    return firstTomorrow?.id
}

/**
 * Сообщение об отсутствии выбранного маршрута
 */
@Composable
private fun NoRouteSelectedMessage(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Пожалуйста, выберите маршрут для просмотра расписания и деталей.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp)
            )
        }
    }
}