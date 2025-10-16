package com.example.lets_go_slavgorod.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lets_go_slavgorod.data.model.BusRoute
import com.example.lets_go_slavgorod.ui.viewmodel.NotificationMode
import com.example.lets_go_slavgorod.ui.viewmodel.NotificationSettingsViewModel
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
    val currentNotificationMode by notificationSettingsViewModel.getRouteNotificationMode(route.id).collectAsState()
    val selectedDays by notificationSettingsViewModel.getRouteSelectedDays(route.id).collectAsState()
    
    var showModeDropdown by remember { mutableStateOf(false) }
    var showDaysDialog by remember { mutableStateOf(false) }

    val notificationModeOptions = arrayOf(
        NotificationMode.ALL_DAYS,
        NotificationMode.WEEKDAYS,
        NotificationMode.SELECTED_DAYS,
        NotificationMode.DISABLED
    )
    
    val dayOptions = DayOfWeek.entries
    
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Режим уведомлений
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showModeDropdown = true }
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDaysDialog = true }
                            .padding(16.dp),
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
                                "${selectedDays.size} ${getDaysWord(selectedDays.size)}"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
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
                notificationSettingsViewModel.setRouteSelectedDays(route.id, days)
                showDaysDialog = false
            },
            onDismiss = { showDaysDialog = false }
        )
    }
}

/**
 * Склонение слова "день" для русского языка
 * 
 * Возвращает правильную форму слова в зависимости от числа:
 * - 1, 21, 31... -> "день"
 * - 2-4, 22-24... -> "дня"
 * - 5-20, 25-30... -> "дней"
 * 
 * @param count количество дней
 * @return правильно склоненное слово
 */
private fun getDaysWord(count: Int): String {
    return when {
        count % 10 == 1 && count % 100 != 11 -> "день"
        count % 10 in 2..4 && (count % 100 < 10 || count % 100 >= 20) -> "дня"
        else -> "дней"
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
                            .clickable { onModeSelected(mode) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = mode == currentMode,
                            onClick = { onModeSelected(mode) }
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
                dayOptions.forEach { (day, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
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
                    onDaysSelected(tempSelectedDays)
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
