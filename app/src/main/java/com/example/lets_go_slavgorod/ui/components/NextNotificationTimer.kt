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
 * Компонент таймера до следующего уведомления
 * 
 * Показывает время до срабатывания ближайшего запланированного уведомления.
 * Отображает три типа информации:
 * 1. Относительное время: "Через 2 ч. 15 мин."
 * 2. Абсолютное время: "Сегодня в 13:45" или "Завтра в 08:30"
 * 3. Подсказка: "За 15 мин до отправления"
 * 
 * Обновляется каждую минуту автоматически.
 * 
 * ВАЖНО: nextNotificationTime - это время УВЕДОМЛЕНИЯ (уже вычитан leadTime),
 * а не время отправления автобуса!
 * 
 * Пример:
 * - Автобус отправляется в 14:00
 * - Пользователь выбрал уведомление за 15 минут
 * - nextNotificationTime = 13:45 (время когда придет уведомление)
 * - Компонент покажет:
 *   "Через 2 ч. 15 мин."
 *   "Сегодня в 13:45"
 *   "За 15 мин до отправления"
 * 
 * @param nextNotificationTime время срабатывания уведомления (уже с учетом leadTime)
 * @param leadTimeMinutes интервал в минутах до отправления (для отображения подсказки)
 * @param modifier модификатор для стилизации компонента
 * 
 * @author VseMirka200
 * @version 2.0
 * @since 2.1
 */
@Composable
fun NextNotificationTimer(
    nextNotificationTime: LocalDateTime?,
    leadTimeMinutes: Int,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf(LocalDateTime.now()) }
    
    // Логируем при изменении nextNotificationTime
    LaunchedEffect(nextNotificationTime) {
        if (nextNotificationTime != null) {
            Timber.d("═══════════════════════════════════════════════════")
            Timber.d("NextNotificationTimer updated:")
            Timber.d("  Current time: ${LocalDateTime.now()}")
            Timber.d("  Notification time: $nextNotificationTime")
            Timber.d("  Lead time: $leadTimeMinutes min")
            
            val now = LocalDateTime.now()
            val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(now.toLocalDate(), nextNotificationTime.toLocalDate())
            val hoursDiff = java.time.temporal.ChronoUnit.HOURS.between(now, nextNotificationTime) % 24
            val minDiff = java.time.temporal.ChronoUnit.MINUTES.between(now, nextNotificationTime) % 60
            
            Timber.d("  Time until: $daysDiff days, $hoursDiff hours, $minDiff minutes")
            Timber.d("═══════════════════════════════════════════════════")
        }
    }
    
    // Обновляем текущее время каждую минуту
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalDateTime.now()
            delay(60000L) // 1 минута
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
                contentDescription = null,
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
                    // Время до уведомления (относительное)
                    val timeUntil = formatTimeUntil(currentTime, nextNotificationTime)
                    Text(
                        text = timeUntil,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    // Точное время уведомления
                    val exactTime = formatExactTime(nextNotificationTime)
                    Text(
                        text = exactTime,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    // Подсказка: за сколько минут до отправления
                    Text(
                        text = "За $leadTimeMinutes мин до отправления",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                } else {
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
 * Форматирует время до события (относительное: "Через 2 ч. 15 мин.")
 */
private fun formatTimeUntil(now: LocalDateTime, target: LocalDateTime): String {
    if (target.isBefore(now)) {
        Timber.w("Target time $target is before current time $now")
        return "Просрочено"
    }
    
    val totalDays = ChronoUnit.DAYS.between(now.toLocalDate(), target.toLocalDate())
    val totalHours = ChronoUnit.HOURS.between(now, target)
    val hours = totalHours % 24
    val minutes = ChronoUnit.MINUTES.between(now, target) % 60
    
    Timber.d("formatTimeUntil: now=$now, target=$target -> $totalDays days, $hours hours, $minutes min")
    
    return buildString {
        append("Через ")
        if (totalDays > 0) {
            append("$totalDays д. ")
        }
        if (hours > 0 || totalDays > 0) {
            append("$hours ч. ")
        }
        append("$minutes мин.")
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

