package com.example.lets_go_slavgorod.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import timber.log.Timber
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/**
 * Компонент таймера до следующего уведомления
 * 
 * Отображает информацию о ближайшем запланированном уведомлении с учетом времени опережения.
 * Показывает три типа информации:
 * 1. Относительное время до уведомления: "Через 45 мин." или "Через 2 мин. 35 сек."
 * 2. Абсолютное время уведомления: "13:45" (только время, без даты)
 * 3. Дополнительно: время отправления автобуса "Автобус в 14:00" (если доступно)
 * 4. Для режима "выбранные дни": показывает день недели когда будет уведомление
 * 
 * Оптимизация производительности:
 * - Обновление каждую секунду при остатке < 5 минут (показ секунд)
 * - Обновление каждую минуту при остатке ≥ 5 минут (экономия батареи)
 * 
 * Архитектурные особенности:
 * - Использует remember для кэширования вычислений
 * - LaunchedEffect для управления обновлениями
 * - Реактивность на изменения nextNotificationTime
 * 
 * @param nextNotificationTime время уведомления (рассчитанное с учетом leadTime)
 * @param nextDepartureTime время отправления автобуса (опционально, для контекста)
 * @param notificationMode режим уведомлений (для отображения дополнительной информации)
 * @param selectedDays выбранные дни недели (для режима SELECTED_DAYS)
 * @param modifier модификатор для стилизации
 * 
 * v3.0 Changes (Октябрь 2025):
 * - Оптимизированы импорты и зависимости
 * - Улучшена производительность рендеринга
 * - Обновлены комментарии и документация
 */
@Composable
fun NextNotificationTimer(
    nextNotificationTime: LocalDateTime?,
    nextDepartureTime: LocalDateTime? = null,
    notificationMode: String? = null,
    selectedDays: Set<DayOfWeek>? = null,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf(LocalDateTime.now()) }
    
    // Отслеживание изменений времени уведомления для логирования
    LaunchedEffect(nextNotificationTime) {
        if (nextNotificationTime != null) {
            val now = LocalDateTime.now()
            val minutesUntilNotification = ChronoUnit.MINUTES.between(now, nextNotificationTime)
            
            Timber.d("NextNotificationTimer: nextNotificationTime = $nextNotificationTime, minutesUntil = $minutesUntilNotification")
            
            // Логируем критическую ситуацию: уведомление в прошлом
            if (minutesUntilNotification <= 0) {
                Timber.e("Время уведомления в прошлом или сейчас: $nextNotificationTime")
            }
        } else {
            Timber.d("NextNotificationTimer: nextNotificationTime is null")
        }
    }
    
    // Вычисляем оставшееся время до уведомления (кэшируется)
    val minutesUntilNotification = remember(currentTime, nextNotificationTime) {
        if (nextNotificationTime != null) {
            ChronoUnit.MINUTES.between(currentTime, nextNotificationTime)
        } else {
            Long.MAX_VALUE
        }
    }
    
    // Адаптивное обновление времени для оптимизации производительности
    LaunchedEffect(minutesUntilNotification) {
        while (nextNotificationTime != null) {
            currentTime = LocalDateTime.now()
            // Высокая частота обновлений для точного отображения секунд
            val updateInterval = if (minutesUntilNotification < 5 && minutesUntilNotification != Long.MAX_VALUE) {
                1000L // 1 секунда для точности
            } else {
                60000L // 1 минута для экономии батареи
            }
            delay(updateInterval)
        }
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Иконка уведомления",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(32.dp)
            )
            
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Следующее уведомление",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                if (nextNotificationTime != null) {
                    // Основная информация: относительное время до уведомления
                    val timeUntilNotification = remember(currentTime, nextNotificationTime, minutesUntilNotification) {
                        // Адаптивное форматирование: секунды для близких уведомлений
                        if (minutesUntilNotification < 5) {
                            formatTimeUntilWithSeconds(currentTime, nextNotificationTime)
                        } else {
                            formatTimeUntil(currentTime, nextNotificationTime)
                        }
                    }
                    Text(
                        text = timeUntilNotification,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    // Абсолютное время уведомления (формат HH:mm)
                    val exactNotificationTime = remember(nextNotificationTime) {
                        formatExactTime(nextNotificationTime)
                    }
                    
                    // Дополнительная информация для режима "выбранные дни"
                    val dayInfo = remember(nextNotificationTime, notificationMode, selectedDays) {
                        if (notificationMode == "SELECTED_DAYS" && nextNotificationTime != null) {
                            val dayOfWeek = nextNotificationTime.dayOfWeek
                            val dayName = dayOfWeek.getDisplayName(TextStyle.FULL, Locale("ru"))
                            
                            // Проверяем, сегодня ли это
                            val today = LocalDateTime.now().dayOfWeek
                            if (dayOfWeek == today) {
                                "Сегодня"
                            } else {
                                dayName
                            }
                        } else {
                            null
                        }
                    }
                    
                    Text(
                        text = if (dayInfo != null) "$exactNotificationTime ($dayInfo)" else exactNotificationTime,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    // Контекстная информация: время отправления автобуса
                    if (nextDepartureTime != null) {
                        val exactDepartureTime = remember(nextDepartureTime) {
                            formatExactTime(nextDepartureTime)
                        }
                        Text(
                            text = "Автобус в $exactDepartureTime",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                } else {
                    // Состояние отсутствия уведомлений
                    Text(
                        text = "Нет запланированных уведомлений",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

/**
 * Форматирует относительное время до события
 * 
 * Специализированное форматирование для отображения времени до уведомления.
 * Не показывает дни, только часы и минуты для лучшей читаемости.
 * 
 * @param now текущее время
 * @param target целевое время
 * @return отформатированная строка времени
 */
private fun formatTimeUntil(now: LocalDateTime, target: LocalDateTime): String {
    if (target.isBefore(now)) {
        return "Просрочено"
    }
    
    // Вычисляем полное количество часов и минут
    val totalHours = ChronoUnit.HOURS.between(now, target)
    val totalMinutes = ChronoUnit.MINUTES.between(now, target)
    
    // Оптимизированное отображение: часы и минуты без дней
    return when {
        totalHours > 0 -> {
            // Ограничиваем отображение часов до 24 для читаемости
            val hours = totalHours % 24
            val minutes = totalMinutes % 60
            "Через $hours ч. $minutes мин."
        }
        else -> {
            // Меньше часа - показываем только минуты
            val minutes = totalMinutes
            "Через $minutes мин."
        }
    }
}

/**
 * Форматирует время до события с отображением секунд
 * 
 * Используется для точного отображения времени при приближении к уведомлению.
 * Обновляется каждую секунду для максимальной точности.
 * 
 * @param now текущее время
 * @param target целевое время
 * @return отформатированная строка с секундами
 */
private fun formatTimeUntilWithSeconds(now: LocalDateTime, target: LocalDateTime): String {
    if (target.isBefore(now)) {
        return "Просрочено"
    }
    
    val totalSeconds = ChronoUnit.SECONDS.between(now, target)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    
    
    return when {
        minutes > 0 -> "Через $minutes мин. $seconds сек."
        seconds > 0 -> "Через $seconds сек."
        else -> "Сейчас!"
    }
}

/**
 * Форматирует абсолютное время в формате HH:mm
 * 
 * Упрощенное отображение времени без даты для лучшей читаемости.
 * 
 * @param target время для форматирования
 * @return строка времени в формате HH:mm
 */
private fun formatExactTime(target: LocalDateTime): String {
    val formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
    return target.format(formatter)
}