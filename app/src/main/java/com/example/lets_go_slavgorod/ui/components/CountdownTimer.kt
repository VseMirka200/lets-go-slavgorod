package com.example.lets_go_slavgorod.ui.components

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lets_go_slavgorod.data.model.BusSchedule
import com.example.lets_go_slavgorod.utils.Constants
import com.example.lets_go_slavgorod.utils.TimeUtils
import kotlinx.coroutines.delay
import timber.log.Timber
import java.util.Calendar

/**
 * Компонент обратного отсчета времени до отправления автобуса
 * 
 * Основные функции:
 * - Отображение времени до ближайшего рейса
 * - Анимация пульсации для привлечения внимания
 * - Поддержка различных состояний (ближайший рейс, обычный рейс)
 * - Автоматическое обновление каждую секунду
 * 
 * @param schedule расписание автобуса
 * @param allSchedules все расписания для определения ближайшего рейса
 * @param showLabel показывать ли лейбл "Ближайший рейс" в тексте
 * @param modifier модификатор для настройки внешнего вида
 * 
 * @author VseMirka200
 * @version 1.0
 */
@Composable
fun CountdownTimer(
    schedule: BusSchedule,
    allSchedules: List<BusSchedule>,
    showLabel: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Текущее время для вычисления обратного отсчета
    // Обновляется каждую секунду для ближайшего рейса
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    
    // Анимация пульсации для привлечения внимания к ближайшему рейсу
    // Плавно меняет прозрачность между 0.7 и 1.0 за 1 секунду
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    
    // Создаем Calendar из currentTime для использования в TimeUtils
    // Remember гарантирует что Calendar пересоздается только при изменении currentTime
    val currentCalendar = remember(currentTime) {
        Calendar.getInstance().apply {
            timeInMillis = currentTime
        }
    }
    
    // Определяем, является ли этот рейс ближайшим (оптимизировано)
    // Remember предотвращает пересчет при каждой рекомпозиции
    // Пересчет происходит только при изменении schedule.id или количества расписаний
    val isNextDeparture = remember(schedule.id, allSchedules.size) {
        if (allSchedules.isEmpty()) false
        else {
            val nextDeparture = TimeUtils.getNextDeparture(allSchedules, currentCalendar)
            nextDeparture?.id == schedule.id
        }
    }
    
    // Обновляем время каждую секунду только для ближайшего рейса
    // Для остальных рейсов таймер не запускается для экономии ресурсов
    LaunchedEffect(isNextDeparture) {
        if (isNextDeparture) {
            while (true) {
                delay(Constants.TIMER_UPDATE_INTERVAL_MS)
                currentTime = System.currentTimeMillis()
            }
        }
    }
    
    // Вычисляем время до отправления (оптимизировано)
    val timeWithSeconds = remember(schedule.departureTime, currentTime, isNextDeparture) {
        if (isNextDeparture) {
            TimeUtils.getTimeUntilDepartureWithSeconds(schedule.departureTime, currentCalendar)
        } else {
            null
        }
    }
    
    val timeUntilDeparture = remember(schedule.departureTime, currentTime, isNextDeparture) {
        if (isNextDeparture) {
            timeWithSeconds?.first
        } else {
            TimeUtils.getTimeUntilDeparture(schedule.departureTime, currentCalendar)
        }
    }
    
    val minutes = timeUntilDeparture ?: -1
    
    // Форматируем время для отображения (оптимизировано)
    val formattedTime = remember(minutes, timeWithSeconds, isNextDeparture) {
        if (isNextDeparture && timeWithSeconds != null) {
            // Для ближайших рейсов показываем секунды
            TimeUtils.formatTimeUntilDepartureWithSeconds(
                timeWithSeconds.first,
                timeWithSeconds.second
            )
        } else {
            // Для дальних рейсов показываем только минуты
            TimeUtils.formatTimeUntilDeparture(minutes)
        }
    }
    
    Timber.d("Formatted time: $formattedTime, minutes: $minutes")
    
    if (isNextDeparture && showLabel) {
        // Для ближайшего рейса показываем только плашку
        Row(
            modifier = modifier.background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha * 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = "Ближайший рейс",
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = pulseAlpha),
                modifier = Modifier.size(16.dp)
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Ближайший рейс",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = pulseAlpha)
                )
                Text(
                    text = "через ${formattedTime.replace("Через ", "")}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = pulseAlpha)
                )
            }
        }
    } else {
        // Для обычных рейсов показываем только время отсчёта
        Text(
            text = formattedTime,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (minutes < 0) FontWeight.Normal else FontWeight.Medium
            ),
            color = if (minutes < 0) {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = if (minutes >= 0) {
                Modifier.background(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
            } else {
                Modifier
            }
        )
    }
}

