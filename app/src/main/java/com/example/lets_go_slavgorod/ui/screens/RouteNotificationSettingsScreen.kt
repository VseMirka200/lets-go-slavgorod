package com.example.lets_go_slavgorod.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lets_go_slavgorod.core.Constants
import com.example.lets_go_slavgorod.data.local.AppDatabase
import com.example.lets_go_slavgorod.data.local.NotificationTimePreferences
import com.example.lets_go_slavgorod.data.model.BusRoute
import com.example.lets_go_slavgorod.data.repository.BusRouteRepository
import com.example.lets_go_slavgorod.domain.notification.NotificationTimeCalculator
import com.example.lets_go_slavgorod.ui.components.NextNotificationTimer
import com.example.lets_go_slavgorod.ui.components.NotificationTimeSelector
import com.example.lets_go_slavgorod.ui.util.TextFormattingUtils
import com.example.lets_go_slavgorod.ui.viewmodel.NotificationMode
import com.example.lets_go_slavgorod.ui.viewmodel.NotificationSettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.DayOfWeek

/**
 * Экран настроек уведомлений для конкретного маршрута
 * 
 * Позволяет настроить уведомления индивидуально для каждого маршрута.
 * Доступен из экрана расписания маршрута через кнопку уведомлений в шапке.
 * 
 * Функциональность:
 * 
 * 1. Режим уведомлений (модальный диалог):
 *    - Все дни: уведомления каждый день
 *    - Только будни: с понедельника по пятницу
 *    - Выбранные дни: уведомления в определенные дни недели
 *    - Отключено: уведомления для этого маршрута отключены
 * 
 * 2. Выбор дней недели (при режиме "Выбранные дни"):
 *    - Модальный диалог с чекбоксами для каждого дня
 *    - Отображается количество выбранных дней
 *    - Применение изменений по кнопке "Применить"
 * 
 * Применение настроек:
 * - Настройки применяются КО ВСЕМ избранным временам данного маршрута
 * - Автоматическое обновление запланированных уведомлений
 * - Сохранение в DataStore
 * 
 * Шапка:
 * - Заголовок "Уведомления"
 * - Подзаголовок с названием маршрута
 * - Стрелка назад
 * 
 * @param route маршрут для настройки уведомлений
 * @param notificationSettingsViewModel ViewModel для управления настройками
 * @param onBackClick callback для возврата назад
 * @param modifier модификатор для настройки внешнего вида
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteNotificationSettingsScreen(
    route: BusRoute,
    notificationSettingsViewModel: NotificationSettingsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Напрямую из DataStore без оптимистичного обновления
    val currentNotificationMode by notificationSettingsViewModel.getRouteNotificationMode(route.id).collectAsState()
    val selectedDays by notificationSettingsViewModel.getRouteSelectedDays(route.id).collectAsState()
    
    var showModeDropdown by remember { mutableStateOf(false) }
    var showDaysDialog by remember { mutableStateOf(false) }
    var pendingDaysDialog by remember { mutableStateOf(false) }
    var isProcessingDaysClick by remember { mutableStateOf(false) }

    val notificationModeOptions = arrayOf(
        NotificationMode.ALL_DAYS,
        NotificationMode.WEEKDAYS,
        NotificationMode.SELECTED_DAYS,
        NotificationMode.DISABLED
    )
    
    val dayOptions = DayOfWeek.entries
    
    // Отслеживаем изменения режима и открываем диалог когда режим станет SELECTED_DAYS
    LaunchedEffect(currentNotificationMode, pendingDaysDialog) {
        if (pendingDaysDialog && currentNotificationMode == NotificationMode.SELECTED_DAYS) {
            // Небольшая задержка для завершения обновления StateFlow
            kotlinx.coroutines.delay(100)
            showDaysDialog = true
            pendingDaysDialog = false
            isProcessingDaysClick = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Уведомления",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = route.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                windowInsets = WindowInsets(0)
            )
        },
        contentWindowInsets = WindowInsets(0)
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(Constants.SETTINGS_HORIZONTAL_PADDING.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Constants.SETTINGS_SECTION_SPACING.dp)
        ) {
            // Настройка времени уведомления
            val context = LocalContext.current
            val timePreferences = remember { NotificationTimePreferences(context) }
            val hasCustomTime by timePreferences.hasCustomLeadTime(route.id).collectAsState(initial = false)
            val leadTime by timePreferences.getLeadTimeForRoute(route.id).collectAsState(initial = Constants.DEFAULT_NOTIFICATION_LEAD_TIME)
            val coroutineScope = rememberCoroutineScope()
            
            
            // ═══════════════════════════════════════════════════════════
            // ЗАГРУЗКА И ФИЛЬТРАЦИЯ ИЗБРАННЫХ ВРЕМЕН
            // ═══════════════════════════════════════════════════════════
            
            // Загружаем ВСЕ избранные времена из Room БД (реактивно через Flow)
            val allFavoriteTimes by try {
                AppDatabase.getDatabase(context)
                    .favoriteTimeDao()
                    .getAllFavoriteTimes()  // Flow автоматически обновляется при изменениях в БД
            } catch (e: Exception) {
                Timber.e(e, "Ошибка получения избранных времен из базы данных")
                kotlinx.coroutines.flow.flowOf(emptyList())  // Fallback на пустой список
            }.collectAsState(initial = emptyList())
            
            // Фильтруем избранные для ЭТОГО маршрута (с детальной диагностикой)
            val routeFavoriteTimes by remember {
                derivedStateOf {
                    allFavoriteTimes.filter { entity ->
                        entity.routeId == route.id && entity.isActive
                    }
                }
            }
            
            // Создаем репозиторий один раз (для конвертации FavoriteTimeEntity → FavoriteTime)
            val repository = remember { BusRouteRepository(context) }
            
            // ═══════════════════════════════════════════════════════════
            // ВЫЧИСЛЕНИЕ СЛЕДУЮЩЕГО ВРЕМЕНИ УВЕДОМЛЕНИЯ
            // ═══════════════════════════════════════════════════════════
            
            // Пересчитывается при изменении ЛЮБОЙ зависимости:
            // - routeFavoriteTimes: добавление/удаление избранных
            // - leadTime: изменение времени уведомления (за сколько минут)
            // - currentNotificationMode: режим (ALL_DAYS/WEEKDAYS/SELECTED_DAYS/DISABLED)
            // - selectedDays: выбранные дни недели (для режима SELECTED_DAYS)
            val nextNotificationTime = remember(
                routeFavoriteTimes,
                leadTime,
                currentNotificationMode,
                selectedDays
            ) {
                val convertedTimes = routeFavoriteTimes.mapNotNull { entity ->
                    try {
                        val routeData = repository.getRouteById(entity.routeId)
                        com.example.lets_go_slavgorod.data.model.FavoriteTime(
                            id = entity.id,
                            routeId = entity.routeId,
                            routeNumber = routeData?.routeNumber ?: route.routeNumber,
                            routeName = routeData?.name ?: route.name,
                            stopName = entity.stopName,
                            departureTime = entity.departureTime,
                            dayOfWeek = entity.dayOfWeek,
                            departurePoint = entity.departurePoint,
                            addedDate = entity.addedDate,
                            isActive = entity.isActive
                        )
                    } catch (e: Exception) {
                        Timber.e(e, "Ошибка конвертации избранного времени ${entity.id}")
                        null
                    }
                }
                
                NotificationTimeCalculator.getNextNotificationTime(
                    favoriteTimes = convertedTimes,
                    context = context,
                    leadTimeMinutes = leadTime,
                    overrideNotificationMode = currentNotificationMode,
                    overrideSelectedDays = selectedDays
                )
            }
            
            if (routeFavoriteTimes.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NextNotificationTimer(
                        nextNotificationTime = nextNotificationTime,
                        leadTimeMinutes = leadTime
                    )
                    
                }
            } else {
                // Сообщение если нет избранных времен
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Нет запланированных уведомлений",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Добавьте избранные времена на экране расписания маршрута (нажмите на ⭐)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(Constants.SETTINGS_HORIZONTAL_PADDING.dp)) {
                    NotificationTimeSelector(
                        selectedMinutes = leadTime,
                        onMinutesSelected = { minutes ->
                            coroutineScope.launch {
                                timePreferences.setLeadTimeForRoute(route.id, minutes)
                            }
                        },
                        useGlobal = !hasCustomTime,
                        onUseGlobalChange = { useGlobal ->
                            coroutineScope.launch {
                                if (useGlobal) {
                                    timePreferences.removeCustomLeadTime(route.id)
                                } else {
                                    timePreferences.setLeadTimeForRoute(route.id, leadTime)
                                }
                            }
                        }
                    )
                }
            }
            
            // Режим уведомлений
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(Constants.SETTINGS_HORIZONTAL_PADDING.dp),
                    verticalArrangement = Arrangement.spacedBy(Constants.SETTINGS_ITEM_SPACING.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { showModeDropdown = true }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Режим уведомлений",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = when (currentNotificationMode) {
                                NotificationMode.ALL_DAYS -> "Все дни"
                                NotificationMode.WEEKDAYS -> "Только будни"
                                NotificationMode.SELECTED_DAYS -> "Выбранные дни"
                                NotificationMode.DISABLED -> "Отключено"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            // Выбор дней (только если выбран режим "Выбранные дни")
            if (currentNotificationMode == NotificationMode.SELECTED_DAYS) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { 
                                    // Защита от множественных кликов
                                    if (isProcessingDaysClick) return@clickable
                                    
                                    isProcessingDaysClick = true
                                    
                                    // Устанавливаем режим SELECTED_DAYS перед открытием диалога
                                    if (currentNotificationMode != NotificationMode.SELECTED_DAYS) {
                                        notificationSettingsViewModel.setRouteNotificationMode(route.id, NotificationMode.SELECTED_DAYS)
                                        pendingDaysDialog = true
                                    } else {
                                        showDaysDialog = true
                                        isProcessingDaysClick = false
                                    }
                                }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Дни недели",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (selectedDays.isEmpty()) {
                                    "Не выбрано"
                                } else {
                                    "${selectedDays.size} ${TextFormattingUtils.getDaysWord(selectedDays.size)}"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (selectedDays.isEmpty()) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                        }
                        
                        // Предупреждение если дни не выбраны
                        if (selectedDays.isEmpty()) {
                            Text(
                                text = "⚠️ Уведомления не будут приходить, пока не выбраны дни",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Модальный диалог выбора режима уведомлений
    if (showModeDropdown) {
        NotificationModeDialog(
            currentMode = currentNotificationMode,
            options = notificationModeOptions,
            onModeSelected = { mode ->
                notificationSettingsViewModel.setRouteNotificationMode(route.id, mode)
                showModeDropdown = false
            },
            onDismiss = { showModeDropdown = false }
        )
    }
    
    // Модальный диалог выбора дней недели
    if (showDaysDialog) {
        DaysSelectionDialog(
            selectedDays = selectedDays,
            onDaysSelected = { days ->
                // Сохраняем дни сразу - режим уже должен быть SELECTED_DAYS
                notificationSettingsViewModel.setRouteSelectedDays(route.id, days)
                showDaysDialog = false
            },
            onDismiss = { showDaysDialog = false }
        )
    }
}

/**
 * Модальный диалог выбора режима уведомлений для маршрута
 * 
 * Отображает список доступных режимов уведомлений с радио-кнопками.
 * При выборе режима изменения применяются сразу и диалог закрывается.
 * 
 * Режимы:
 * - Все дни: уведомления каждый день
 * - Только будни: только рабочие дни (Пн-Пт)
 * - Выбранные дни: пользователь выбирает конкретные дни
 * - Отключено: уведомления для маршрута не приходят
 * 
 * @param currentMode текущий выбранный режим
 * @param options список доступных режимов
 * @param onModeSelected callback при выборе режима
 * @param onDismiss callback при закрытии диалога
 */
@Composable
private fun NotificationModeDialog(
    currentMode: NotificationMode,
    options: Array<NotificationMode>,
    onModeSelected: (NotificationMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Режим уведомлений",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                options.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = mode == currentMode,
                            onClick = {
                                if (mode != currentMode) {
                                    onModeSelected(mode)
                                }
                            }
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = when (mode) {
                                NotificationMode.ALL_DAYS -> "Все дни"
                                NotificationMode.WEEKDAYS -> "Только будни"
                                NotificationMode.SELECTED_DAYS -> "Выбранные дни"
                                NotificationMode.DISABLED -> "Отключено"
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}

/**
 * Модальный диалог выбора дней недели для уведомлений
 * 
 * Позволяет выбрать конкретные дни недели, когда должны приходить уведомления.
 * Используется при режиме уведомлений "Выбранные дни".
 * 
 * Функциональность:
 * - Чекбоксы для каждого дня недели (Пн-Вс)
 * - Клик на всю строку для выбора/снятия чекбокса
 * - Временное состояние: изменения применяются только по кнопке "Применить"
 * - Кнопка "Отмена" для отмены изменений
 * 
 * @param selectedDays текущий набор выбранных дней
 * @param onDaysSelected callback при подтверждении выбора (передается Set<DayOfWeek>)
 * @param onDismiss callback при закрытии без сохранения
 */
@Composable
private fun DaysSelectionDialog(
    selectedDays: Set<DayOfWeek>,
    onDaysSelected: (Set<DayOfWeek>) -> Unit,
    onDismiss: () -> Unit
) {
    var tempSelectedDays by remember { mutableStateOf(selectedDays) }
    var showError by remember { mutableStateOf(false) }
    
    val dayOptions = listOf(
        DayOfWeek.MONDAY to "Понедельник",
        DayOfWeek.TUESDAY to "Вторник",
        DayOfWeek.WEDNESDAY to "Среда",
        DayOfWeek.THURSDAY to "Четверг",
        DayOfWeek.FRIDAY to "Пятница",
        DayOfWeek.SATURDAY to "Суббота",
        DayOfWeek.SUNDAY to "Воскресенье"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Выберите дни недели",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                if (showError) {
                    Text(
                        text = "Выберите хотя бы один день",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                dayOptions.forEach { (day, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                tempSelectedDays = if (tempSelectedDays.contains(day)) {
                                    tempSelectedDays - day
                                } else {
                                    tempSelectedDays + day
                                }
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = tempSelectedDays.contains(day),
                            onCheckedChange = { isChecked ->
                                tempSelectedDays = if (isChecked) {
                                    tempSelectedDays + day
                                } else {
                                    tempSelectedDays - day
                                }
                            }
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (tempSelectedDays.isEmpty()) {
                        showError = true
                    } else {
                        showError = false
                        onDaysSelected(tempSelectedDays)
                    }
                }
            ) {
                Text("Применить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}