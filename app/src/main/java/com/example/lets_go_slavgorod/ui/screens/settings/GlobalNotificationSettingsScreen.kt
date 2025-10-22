package com.example.lets_go_slavgorod.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.lets_go_slavgorod.ui.components.SettingsTopBar
import com.example.lets_go_slavgorod.ui.util.TextFormattingUtils
import com.example.lets_go_slavgorod.ui.viewmodel.NotificationMode
import com.example.lets_go_slavgorod.ui.viewmodel.NotificationSettingsViewModel
import java.time.DayOfWeek

/**
 * Экран глобальных настроек уведомлений
 * 
 * Позволяет настроить уведомления ПО УМОЛЧАНИЮ для всех маршрутов.
 * Эти настройки применяются ко всем маршрутам, у которых НЕ установлены
 * индивидуальные настройки.
 * 
 * Функциональность:
 * 
 * 1. Глобальный режим уведомлений (модальный диалог):
 *    - Все дни: уведомления каждый день (по умолчанию)
 *    - Только будни: с понедельника по пятницу
 *    - Выбранные дни: уведомления в определенные дни недели
 *    - Отключено: глобально отключить уведомления
 * 
 * 2. Глобальные дни недели (при режиме "Выбранные дни"):
 *    - Модальный диалог с чекбоксами для каждого дня
 *    - Отображается количество выбранных дней
 *    - Применение изменений по кнопке "Применить"
 * 
 * Применение настроек:
 * - Глобальные настройки применяются ко ВСЕМ маршрутам БЕЗ индивидуальных настроек
 * - Маршруты с индивидуальными настройками продолжают использовать свои
 * - Автоматическое обновление запланированных уведомлений
 * - Сохранение в DataStore
 * 
 * Информация:
 * - Подсказка о том, что это настройки "по умолчанию"
 * - Пояснение что индивидуальные настройки маршрутов имеют приоритет
 * - Ссылка на настройки конкретного маршрута (через экран расписания)
 * 
 * Дизайн v4.0:
 * - Отключена анимация касания (ripple effect) для всех кликабельных элементов
 * 
 * @param notificationSettingsViewModel ViewModel для управления настройками
 * @param onNavigateBack callback для возврата назад
 * @param modifier модификатор для настройки внешнего вида
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.1
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalNotificationSettingsScreen(
    notificationSettingsViewModel: NotificationSettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Напрямую из DataStore без оптимистичного обновления
    val currentGlobalMode by notificationSettingsViewModel.currentNotificationMode.collectAsState()
    val globalSelectedDays by notificationSettingsViewModel.selectedNotificationDays.collectAsState()
    
    var showModeDialog by remember { mutableStateOf(false) }
    var showDaysDialog by remember { mutableStateOf(false) }
    
    val notificationModeOptions = arrayOf(
        NotificationMode.ALL_DAYS,
        NotificationMode.WEEKDAYS,
        NotificationMode.SELECTED_DAYS,
        NotificationMode.DISABLED
    )
    
    Scaffold(
        topBar = {
            SettingsTopBar(
                title = "Уведомления по умолчанию",
                subtitle = "Для всех маршрутов",
                onBackClick = onNavigateBack
            )
        },
        contentWindowInsets = WindowInsets(0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Информационная карточка
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ℹ️ Глобальные настройки",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Эти настройки применяются ко всем маршрутам ПО УМОЛЧАНИЮ. " +
                                "Индивидуальные настройки конкретного маршрута (доступны через экран расписания) " +
                                "имеют приоритет над глобальными.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Режим уведомлений
            Text(
                text = "Режим уведомлений",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    // Кликабельность без ripple эффекта
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null // Отключена анимация касания
                    ) { showModeDialog = true },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Текущий режим",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = when (currentGlobalMode) {
                                NotificationMode.ALL_DAYS -> "Все дни"
                                NotificationMode.WEEKDAYS -> "Только будни"
                                NotificationMode.SELECTED_DAYS -> "Выбранные дни"
                                NotificationMode.DISABLED -> "Отключено"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            // Выбор дней (только если режим SELECTED_DAYS)
            if (currentGlobalMode == NotificationMode.SELECTED_DAYS) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Выбранные дни",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Кликабельность без ripple эффекта
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null // Отключена анимация касания
                        ) { showDaysDialog = true },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (globalSelectedDays.isEmpty()) {
                                "Дни не выбраны"
                            } else {
                                "${globalSelectedDays.size} ${TextFormattingUtils.getDaysWord(globalSelectedDays.size)}"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        
                        if (globalSelectedDays.isEmpty()) {
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
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Дополнительная информация
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "💡 Как это работает?",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Глобальные настройки применяются к маршрутам автоматически\n" +
                                "• Для индивидуальных настроек откройте расписание маршрута → кнопка уведомлений\n" +
                                "• Тихий режим блокирует ВСЕ уведомления (высший приоритет)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
    
    // Диалог выбора режима уведомлений
    // Отключена анимация касания (indication = null) для чистого вида
    if (showModeDialog) {
        AlertDialog(
            onDismissRequest = { showModeDialog = false },
            title = { Text("Выберите режим уведомлений") },
            text = {
                Column {
                    notificationModeOptions.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                // Кликабельность без ripple эффекта
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null // Отключена анимация касания
                                ) {
                                    notificationSettingsViewModel.setGlobalNotificationMode(mode)
                                    showModeDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentGlobalMode == mode,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = when (mode) {
                                    NotificationMode.ALL_DAYS -> "Все дни"
                                    NotificationMode.WEEKDAYS -> "Только будни"
                                    NotificationMode.SELECTED_DAYS -> "Выбранные дни"
                                    NotificationMode.DISABLED -> "Отключено"
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showModeDialog = false }) {
                    Text("Закрыть")
                }
            }
        )
    }
    
    // Диалог выбора дней недели для уведомлений
    // Временное состояние для изменений до подтверждения, синхронизируется с globalSelectedDays
    if (showDaysDialog) {
        var tempSelectedDays by remember(globalSelectedDays) { mutableStateOf(globalSelectedDays) }
        
        AlertDialog(
            onDismissRequest = { showDaysDialog = false },
            title = { Text("Выберите дни недели") },
            text = {
                Column {
                    DayOfWeek.entries.forEach { day ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                // Кликабельность без ripple эффекта для чистого вида
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null // Отключена анимация касания
                                ) {
                                    // Переключение выбора дня (добавление/удаление из списка)
                                    tempSelectedDays = if (day in tempSelectedDays) {
                                        tempSelectedDays - day
                                    } else {
                                        tempSelectedDays + day
                                    }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = day in tempSelectedDays,
                                onCheckedChange = null
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = when (day) {
                                    DayOfWeek.MONDAY -> "Понедельник"
                                    DayOfWeek.TUESDAY -> "Вторник"
                                    DayOfWeek.WEDNESDAY -> "Среда"
                                    DayOfWeek.THURSDAY -> "Четверг"
                                    DayOfWeek.FRIDAY -> "Пятница"
                                    DayOfWeek.SATURDAY -> "Суббота"
                                    DayOfWeek.SUNDAY -> "Воскресенье"
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        notificationSettingsViewModel.setGlobalSelectedDays(tempSelectedDays)
                        showDaysDialog = false
                    }
                ) {
                    Text("Применить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDaysDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}