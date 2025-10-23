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

/**
 * Компонент таймера до следующего уведомления о избранном времени
 * 
 * Показывает время до ближайшего запланированного УВЕДОМЛЕНИЯ.
 * Отображает три типа информации:
 * 1. Относительное время до уведомления: "Через 45 мин." или "Через 2 мин. 35 сек."
 * 2. Абсолютное время уведомления: "Сегодня в 13:45"
 * 3. Подсказка: время отправления автобуса "Автобус в 14:00"
 * 
 * Умное обновление:
 * - Каждую СЕКУНДУ когда осталось меньше 5 минут (показывает секунды)
 * - Каждую МИНУТУ когда осталось больше 5 минут (экономия батареи)
 * 
 * ВАЖНО: nextNotificationTime - это время УВЕДОМЛЕНИЯ (уже вычитан leadTime).
 * Показываем именно время уведомления, чтобы пользователь знал когда придет notification!
 * 
 * Пример 1 (далеко до уведомления):
 * - Автобус отправляется в 14:00
 * - Уведомление за 15 минут → уведомление в 13:45
 * - Сейчас 13:00
 * - Компонент покажет:
 *   "Через 45 мин." (обновляется каждую минуту)
 *   "Сегодня в 13:45"
 *   "Автобус в 14:00"
 * 
 * Пример 2 (близко к уведомлению):
 * - Сейчас 13:43
 * - Компонент покажет:
 *   "Через 1 мин. 45 сек." (обновляется каждую секунду!)
 *   "Сегодня в 13:45"
 *   "Автобус в 14:00"
 * 
 * @param nextNotificationTime время срабатывания уведомления (уже с учетом leadTime)
 * @param leadTimeMinutes интервал в минутах до отправления (для вычисления времени отправления)
 * @param modifier модификатор для стилизации компонента
 * 
 * @author VseMirka200
 * @version 2.3
 * @since 2.1
 */
@Composable
fun NextNotificationTimer(
    nextNotificationTime: LocalDateTime?,
    leadTimeMinutes: Int,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf(LocalDateTime.now()) }
    
    // Вычисляем время отправления автобуса (для справки)
    // Пересчитывается при изменении nextNotificationTime ИЛИ leadTimeMinutes
    val nextDepartureTime = remember(nextNotificationTime, leadTimeMinutes) {
        val result = nextNotificationTime?.plusMinutes(leadTimeMinutes.toLong())
        result
    }
    
    // Логируем при изменении nextNotificationTime
    LaunchedEffect(nextNotificationTime, leadTimeMinutes) {
        if (nextNotificationTime != null && nextDepartureTime != null) {
            
            val now = LocalDateTime.now()
            val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(now.toLocalDate(), nextDepartureTime.toLocalDate())
            val hoursDiff = java.time.temporal.ChronoUnit.HOURS.between(now, nextDepartureTime) % 24
            val minDiff = java.time.temporal.ChronoUnit.MINUTES.between(now, nextDepartureTime) % 60
            
            
            // Проверяем, если уведомление должно сработать в ближайшие 5 минут
            val minutesUntilNotification = java.time.temporal.ChronoUnit.MINUTES.between(now, nextNotificationTime)
            if (minutesUntilNotification <= 5 && minutesUntilNotification > 0) {
            }
            
            // КРИТИЧЕСКИ ВАЖНО: Если время уведомления в прошлом или сейчас
            val secondsUntilNotification = java.time.temporal.ChronoUnit.SECONDS.between(now, nextNotificationTime)
            if (secondsUntilNotification <= 0) {
                Timber.e("Время уведомления в прошлом или сейчас")
            }
            
        }
    }
    
    // Проверяем сколько времени осталось до уведомления
    val minutesUntilNotification = remember(currentTime, nextNotificationTime) {
        if (nextNotificationTime != null) {
            ChronoUnit.MINUTES.between(currentTime, nextNotificationTime)
        } else {
            Long.MAX_VALUE
        }
    }
    
    // Обновляем текущее время:
    // - Каждую секунду если осталось меньше 5 минут (для показа секунд)
    // - Каждую минуту если осталось больше 5 минут
    LaunchedEffect(minutesUntilNotification) {
        while (true) {
            currentTime = LocalDateTime.now()
            val updateInterval = if (minutesUntilNotification < 5) {
                1000L // 1 секунда
            } else {
                60000L // 1 минута
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
                    // Показываем время ДО УВЕДОМЛЕНИЯ (не до отправления автобуса!)
                    // Время до уведомления (относительное, с умным форматированием)
                    val timeUntilNotification = remember(currentTime, nextNotificationTime, minutesUntilNotification) {
                        // Показываем секунды если осталось меньше 5 минут
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
                    
                    // Точное время уведомления
                    val exactNotificationTime = remember(nextNotificationTime) {
                        formatExactTime(nextNotificationTime)
                    }
                    Text(
                        text = exactNotificationTime,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    // Подсказка: время отправления автобуса (для справки)
                    if (nextDepartureTime != null) {
                        val departureTimeFormatted = remember(nextDepartureTime) {
                            nextDepartureTime.toLocalTime().toString()
                        }
                        Text(
                            text = "Автобус в $departureTimeFormatted",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                } else {
                    Text(
                        text = "Нет запланированных избранных времен",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

/**
 * Форматирует время до события (относительное: "Через 2 ч. 15 мин.")
 * 
 * Умная логика отображения:
 * - Если > 24 часов: показывает дни + часы + минуты
 * - Если < 24 часов: показывает только часы + минуты (без дней)
 * 
 * Примеры:
 * - До события 8 ч 35 мин → "Через 8 ч. 35 мин." (а не "Через 1 д. 8 ч. 35 мин.")
 * - До события 1 д 10 ч → "Через 1 д. 10 ч. 0 мин."
 */
private fun formatTimeUntil(now: LocalDateTime, target: LocalDateTime): String {
    if (target.isBefore(now)) {
        return "Просрочено"
    }
    
    // Вычисляем полное количество часов и минут
    val totalHours = ChronoUnit.HOURS.between(now, target)
    val totalMinutes = ChronoUnit.MINUTES.between(now, target)
    
    
    return when {
        totalHours >= 24 -> {
            // Больше суток - показываем дни, часы, минуты
            val days = totalHours / 24
            val hours = totalHours % 24
            val minutes = totalMinutes % 60
            
            buildString {
                append("Через ")
                append("$days д. ")
                if (hours > 0) {
                    append("$hours ч. ")
                }
                append("$minutes мин.")
            }
        }
        totalHours > 0 -> {
            // Меньше суток - показываем только часы и минуты
            val hours = totalHours
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
 * Форматирует время до события с секундами (относительное: "Через 2 мин. 35 сек.")
 * 
 * Используется когда осталось меньше 5 минут до уведомления.
 * Обновляется каждую секунду для точного отображения.
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
 * Форматирует точное время уведомления (абсолютное: "Сегодня в 13:45" или "Завтра в 08:30")
 */
private fun formatExactTime(target: LocalDateTime): String {
    val now = LocalDateTime.now()
    val formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
    val timeStr = target.format(formatter)
    
    return when {
        target.toLocalDate() == now.toLocalDate() -> {
            "Сегодня в $timeStr"
        }
        target.toLocalDate() == now.toLocalDate().plusDays(1) -> {
            "Завтра в $timeStr"
        }
        target.toLocalDate().isBefore(now.toLocalDate().plusDays(7)) -> {
            // Для дней на этой неделе показываем день недели
            val dayOfWeek = when (target.dayOfWeek) {
                java.time.DayOfWeek.MONDAY -> "Понедельник"
                java.time.DayOfWeek.TUESDAY -> "Вторник"
                java.time.DayOfWeek.WEDNESDAY -> "Среда"
                java.time.DayOfWeek.THURSDAY -> "Четверг"
                java.time.DayOfWeek.FRIDAY -> "Пятница"
                java.time.DayOfWeek.SATURDAY -> "Суббота"
                java.time.DayOfWeek.SUNDAY -> "Воскресенье"
            }
            "$dayOfWeek в $timeStr"
        }
        else -> {
            // Для дальних дат показываем дату
            val dateFormatter = java.time.format.DateTimeFormatter.ofPattern("dd.MM")
            "${target.format(dateFormatter)} в $timeStr"
        }
    }
}