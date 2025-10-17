package com.example.lets_go_slavgorod.utils

import android.annotation.SuppressLint
import timber.log.Timber
import com.example.lets_go_slavgorod.data.model.BusSchedule
import java.text.SimpleDateFormat
import java.util.*

/**
 * Утилиты для работы со временем и датами
 * 
 * Предоставляет функции для:
 * - Парсинга и форматирования времени
 * - Вычисления интервалов между временами
 * - Фильтрации расписаний по времени суток
 * - Группировки расписаний по дням недели
 * - Определения текущего дня недели
 * 
 * Все функции используют локальный часовой пояс и учитывают
 * особенности 24-часового формата времени.
 * 
 * @author VseMirka200
 * @version 2.0
 * @since 1.0
 */
object TimeUtils {

    @SuppressLint("ConstantLocale")
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    /**
     * Вычисляет время до отправления в минутах
     * 
     * Определяет сколько минут осталось до указанного времени отправления.
     * Если время уже прошло сегодня, возвращает null (не считает на завтра).
     * 
     * Особенности:
     * - Учитывает переход через полночь в рамках суток
     * - Обнуляет секунды и миллисекунды для точного сравнения
     * - Возвращает null для прошедшего времени
     * - Логирует процесс вычисления для отладки
     * 
     * Примеры:
     * - Текущее время 10:00, отправление 10:30 → возвращает 30
     * - Текущее время 10:00, отправление 09:00 → возвращает null (уже прошло)
     * - Текущее время 23:50, отправление 00:10 → возвращает null (считается прошедшим)
     * 
     * @param departureTime время отправления в формате "HH:mm"
     * @param currentTime текущее время для сравнения (по умолчанию системное)
     * @return количество минут до отправления или null если время прошло
     * 
     * @see getTimeUntilDepartureWithSeconds для получения с секундами
     * @see parseTime для парсинга времени из строки
     */
    fun getTimeUntilDeparture(departureTime: String, currentTime: Calendar = Calendar.getInstance()): Int? {
        return try {
            Timber.d("Calculating time until departure: $departureTime")
            val departureCalendar = parseTime(departureTime)
            val current = currentTime.clone() as Calendar
            
            // Устанавливаем секунды и миллисекунды в 0 для точного сравнения
            current.set(Calendar.SECOND, 0)
            current.set(Calendar.MILLISECOND, 0)
            
            Timber.d("Current time: ${current.get(Calendar.HOUR_OF_DAY)}:${current.get(Calendar.MINUTE)}")
            Timber.d("Departure time: ${departureCalendar.get(Calendar.HOUR_OF_DAY)}:${departureCalendar.get(Calendar.MINUTE)}")
            
            // Если время отправления уже прошло сегодня, считаем его на завтра
            if (departureCalendar.before(current)) {
                Timber.d("Departure time is in the past, adding one day")
                departureCalendar.add(Calendar.DAY_OF_MONTH, 1)
            }
            
            val diffInMillis = departureCalendar.timeInMillis - current.timeInMillis
            val diffInMinutes = (diffInMillis / (1000 * 60)).toInt()
            
            Timber.d("Time difference in minutes: $diffInMinutes")
            
            if (diffInMinutes >= 0) {
                diffInMinutes
            } else {
                Timber.d("Departure time is in the past, returning null")
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error calculating time until departure")
            null
        }
    }
    
    // Вычисление времени до отправления (с секундами)
    fun getTimeUntilDepartureWithSeconds(departureTime: String, currentTime: Calendar = Calendar.getInstance()): Pair<Int, Int>? {
        return try {
            val departureCalendar = parseTime(departureTime)
            val current = currentTime.clone() as Calendar
            
            // Если время отправления уже прошло сегодня, считаем его на завтра
            if (departureCalendar.before(current)) {
                departureCalendar.add(Calendar.DAY_OF_MONTH, 1)
            }
            
            val diffInMillis = departureCalendar.timeInMillis - current.timeInMillis
            
            if (diffInMillis >= 0) {
                val totalSeconds = (diffInMillis / 1000).toInt()
                val minutes = totalSeconds / 60
                val seconds = totalSeconds % 60
                Pair(minutes, seconds)
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error calculating time until departure with seconds")
            null
        }
    }
    
    /**
     * Парсит время из строки в Calendar
     * 
     * @param timeString время в формате HH:mm
     * @return Calendar с установленным временем
     */
    fun parseTime(timeString: String): Calendar {
        return try {
            val time = timeFormat.parse(timeString)
            val calendar = Calendar.getInstance()
            if (time != null) {
                calendar.time = time
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                // Устанавливаем дату на сегодня
                val today = Calendar.getInstance()
                calendar.set(Calendar.YEAR, today.get(Calendar.YEAR))
                calendar.set(Calendar.MONTH, today.get(Calendar.MONTH))
                calendar.set(Calendar.DAY_OF_MONTH, today.get(Calendar.DAY_OF_MONTH))
            }
            calendar
        } catch (e: Exception) {
            Timber.e(e, "Error parsing time: $timeString")
            Calendar.getInstance()
        }
    }
    
    /**
     * Форматирует время до отправления в читаемый вид
     * 
     * Преобразует количество минут в естественную строку на русском языке.
     * Используется для отображения времени до прибытия автобуса в UI.
     * 
     * Примеры форматирования:
     * - 0 минут: "Сейчас"
     * - 1 минута: "1 минуту"
     * - 5 минут: " 5 мин"
     * - 60 минут: "Через 1 ч"
     * - 90 минут: "Через 1 ч 30 мин"
     * - 120 минут: "Через 2 ч"
     * 
     * @param minutes количество минут до отправления (неотрицательное число)
     * @return отформатированная строка времени
     */
    fun formatTimeUntilDeparture(minutes: Int): String {
        return when {
            minutes < 1 -> "Сейчас"
            minutes == 1 -> "1 минуту"
            minutes < 60 -> " $minutes мин"
            else -> formatHoursAndMinutes(minutes)
        }
    }
    
    /**
     * Внутренняя функция для форматирования часов и минут
     * 
     * Избегает дублирования кода между функциями форматирования.
     * 
     * @param totalMinutes общее количество минут
     * @return отформатированная строка вида "Через X ч Y мин"
     */
    private fun formatHoursAndMinutes(totalMinutes: Int): String {
        val hours = totalMinutes / 60
        val remainingMinutes = totalMinutes % 60
        return when {
            remainingMinutes == 0 -> "Через $hours ч"
            hours == 1 -> "Через 1 ч $remainingMinutes мин"
            else -> "Через $hours ч $remainingMinutes мин"
        }
    }
    
    /**
     * Форматирует время до отправления с секундами для ближайших рейсов
     * 
     * Используется для отображения точного времени до отправления с учетом секунд.
     * Показывает секунды только для рейсов отправляющихся в ближайшие минуты.
     * 
     * Примеры форматирования:
     * - 0 мин 45 сек: "Через 45 сек"
     * - 1 мин 30 сек: "Через 1 мин 30 сек"
     * - 5 мин 10 сек: "Через 5 мин 10 сек"
     * - 60 мин 0 сек: "Через 1 ч"
     * 
     * @param minutes количество минут до отправления
     * @param seconds количество секунд до отправления (0-59)
     * @return отформатированная строка с точным временем
     */
    fun formatTimeUntilDepartureWithSeconds(minutes: Int, seconds: Int): String {
        return when {
            minutes < 1 -> {
                if (seconds <= 0) "Сейчас"
                else "Через $seconds сек"
            }
            minutes == 1 -> "Через 1 мин $seconds сек"
            minutes < 60 -> "Через $minutes мин $seconds сек"
            else -> {
                val hours = minutes / 60
                val remainingMinutes = minutes % 60
                when {
                    remainingMinutes == 0 -> "Через $hours ч"
                    hours == 1 -> "Через 1 ч $remainingMinutes мин"
                    else -> "Через $hours ч $remainingMinutes мин"
                }
            }
        }
    }
    
    /**
     * Получает ближайший рейс из списка расписаний
     * 
     * Находит расписание с минимальным временем до отправления.
     * Если рейс уже прошел сегодня, он считается как "на завтра"
     * и получает соответствующий приоритет.
     * 
     * Алгоритм:
     * 1. Для каждого расписания вычисляет время до отправления
     * 2. Если время положительное - использует его как приоритет
     * 3. Если время отрицательное (уже прошло) - добавляет 24 часа
     * 4. Находит расписание с минимальным приоритетом
     * 
     * Оптимизация:
     * - Использует getTimeUntilDeparture для единообразной логики
     * - Избегает дублирования кода вычисления времени
     * - Использует minByOrNull для эффективного поиска
     * 
     * @param schedules список расписаний для поиска
     * @param currentTime текущее время (по умолчанию - системное)
     * @return ближайшее расписание или null если список пуст
     */
    fun getNextDeparture(schedules: List<BusSchedule>, currentTime: Calendar = Calendar.getInstance()): BusSchedule? {
        if (schedules.isEmpty()) return null
        
        // Находим рейс с минимальным временем до отправления
        return schedules.minByOrNull { schedule ->
            getTimeUntilDeparture(schedule.departureTime, currentTime) 
                ?: Int.MAX_VALUE // Если рейс прошел сегодня, даем минимальный приоритет
        }
    }
    
    /**
     * Проверяет, является ли рейс ближайшим
     * 
     * @param schedule расписание для проверки
     * @param allSchedules все расписания
     * @param currentTime текущее время
     * @return true если это ближайший рейс
     */
    fun isNextDeparture(
        schedule: BusSchedule,
        allSchedules: List<BusSchedule>,
        currentTime: Calendar = Calendar.getInstance()
    ): Boolean {
        val nextDeparture = getNextDeparture(allSchedules, currentTime)
        return nextDeparture?.id == schedule.id
    }
    
    /**
     * Получает время до ближайшего рейса в читаемом формате
     * 
     * @param schedule расписание
     * @param currentTime текущее время
     * @return отформатированное время до отправления или null
     */
    fun getFormattedTimeUntilDeparture(
        schedule: BusSchedule,
        currentTime: Calendar = Calendar.getInstance()
    ): String? {
        val minutes = getTimeUntilDeparture(schedule.departureTime, currentTime)
        return minutes?.let { formatTimeUntilDeparture(it) }
    }
    
    /**
     * Получает текущий день недели
     * 
     * @return день недели (1-7, где 1 - воскресенье, 7 - суббота)
     */
    fun getCurrentDayOfWeek(): Int {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.DAY_OF_WEEK)
    }
    
    /**
     * Фильтрует расписания, оставляя только будущие
     * 
     * @param schedules список расписаний
     * @param currentTime текущее время
     * @return отфильтрованный список расписаний
     */
    fun filterSchedulesByTime(
        schedules: List<BusSchedule>,
        currentTime: Calendar = Calendar.getInstance()
    ): List<BusSchedule> {
        return schedules.filter { schedule ->
            val timeUntilDeparture = getTimeUntilDeparture(schedule.departureTime, currentTime)
            timeUntilDeparture != null && timeUntilDeparture > 0
        }
    }
    
    /**
     * Группирует расписания по дням недели
     * 
     * @param schedules список расписаний
     * @return карта, где ключ - день недели, значение - список расписаний
     */
    fun groupSchedulesByDayOfWeek(schedules: List<BusSchedule>): Map<Int, List<BusSchedule>> {
        return schedules.groupBy { it.dayOfWeek }
    }
}
