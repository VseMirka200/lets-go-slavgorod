package com.example.lets_go_slavgorod.utils

import timber.log.Timber
import java.util.Calendar

/**
 * Комплексные утилиты для валидации данных в приложении
 * 
 * Предоставляет централизованную систему валидации для всех типов данных,
 * используемых в приложении. Включает проверки форматов, диапазонов значений
 * и бизнес-правил.
 * 
 * Поддерживаемые типы валидации:
 * - Идентификаторы (ID маршрутов, расписаний)
 * - Номера маршрутов (формат, длина, символы)
 * - Названия остановок и маршрутов (пустые строки, специальные символы)
 * - Время отправления (формат HH:mm, валидные часы/минуты)
 * - Дни недели (диапазон 1-7)
 * - Email адреса (RFC стандарт)
 * - URL адреса (HTTP/HTTPS протоколы)
 * - Цвета (ARGB формат #AARRGGBB)
 * 
 * Все методы включают подробное логирование для отладки и мониторинга.
 * 
 * @author VseMirka200
 * @version 2.0
 * @since 1.0
 */
object ValidationUtils {

    /**
     * Валидирует время в формате HH:MM
     * 
     * Проверяет корректность времени отправления автобуса.
     * Поддерживает 24-часовой формат с разделителем ":"
     * 
     * @param time строка времени для валидации (например, "08:30", "14:15")
     * @return true если время в корректном формате, false иначе
     */
    fun isValidTime(time: String): Boolean {
        if (time.isBlank()) {
            Timber.w("Time is blank")
            return false
        }
        
        val timeParts = time.split(":")
        if (timeParts.size != 2) {
            Timber.w("Invalid time format: '$time' - expected HH:MM")
            return false
        }
        
        return try {
            val hour = timeParts[0].trim().toInt()
            val minute = timeParts[1].trim().toInt()
            
            val isValid = hour in 0..23 && minute in 0..59
            if (!isValid) {
                Timber.w("Invalid time values: hour=$hour, minute=$minute")
            }
            isValid
        } catch (e: NumberFormatException) {
            Timber.w(e, "Invalid number format in time: '$time'")
            false
        }
    }
    
    /**
     * Валидирует день недели
     */
    fun isValidDayOfWeek(dayOfWeek: Int): Boolean {
        val isValid = dayOfWeek in Calendar.SUNDAY..Calendar.SATURDAY
        if (!isValid) {
            Timber.w("Invalid day of week: $dayOfWeek - expected ${Calendar.SUNDAY}-${Calendar.SATURDAY}")
        }
        return isValid
    }
    
    /**
     * Валидирует ID маршрута
     */
    fun isValidRouteId(routeId: String?): Boolean {
        if (routeId.isNullOrBlank()) {
            Timber.w("Route ID is null or blank")
            return false
        }
        
        val isValid = routeId.trim().isNotBlank()
        if (!isValid) {
            Timber.w("Route ID is empty after trimming: '$routeId'")
        }
        return isValid
    }
    
    /**
     * Валидирует название остановки
     */
    fun isValidStopName(stopName: String?): Boolean {
        if (stopName.isNullOrBlank()) {
            Timber.w("Stop name is null or blank")
            return false
        }
        
        val trimmed = stopName.trim()
        val isValid = trimmed.isNotBlank() && trimmed.length >= 2
        if (!isValid) {
            Timber.w("Stop name is too short or empty: '$stopName'")
        }
        return isValid
    }
    
    /**
     * Валидирует название маршрута
     */
    fun isValidRouteName(routeName: String?): Boolean {
        if (routeName.isNullOrBlank()) {
            Timber.w("Route name is null or blank")
            return false
        }
        
        val trimmed = routeName.trim()
        val isValid = trimmed.isNotBlank() && trimmed.length >= 3
        if (!isValid) {
            Timber.w("Route name is too short or empty: '$routeName'")
        }
        return isValid
    }
    
    /**
     * Валидирует номер маршрута
     */
    fun isValidRouteNumber(routeNumber: String?): Boolean {
        if (routeNumber.isNullOrBlank()) {
            Timber.w("Route number is null or blank")
            return false
        }
        
        // isNotBlank() уже включает проверку isNotEmpty(), избыточность убрана
        val trimmed = routeNumber.trim()
        val isValid = trimmed.isNotBlank()
        if (!isValid) {
            Timber.w("Route number is empty: '$routeNumber'")
        }
        return isValid
    }

    /**
     * Очищает и нормализует строку
     */
    fun sanitizeString(input: String?): String {
        return input?.trim()?.takeIf { it.isNotBlank() } ?: ""
    }

    /**
     * Валидирует email адрес
     */
    fun isValidEmail(email: String?): Boolean {
        if (email.isNullOrBlank()) {
            Timber.w("Email is null or blank")
            return false
        }
        
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$")
        val isValid = emailRegex.matches(email.trim())
        
        if (!isValid) {
            Timber.w("Invalid email format: '$email'")
        }
        return isValid
    }

    /**
     * Валидирует URL
     */
    fun isValidUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) {
            Timber.w("URL is null or blank")
            return false
        }
        
        return try {
            val urlObj = java.net.URL(url.trim())
            val isValid = urlObj.protocol in listOf("http", "https")
            if (!isValid) {
                Timber.w("Invalid URL protocol: '$url'")
            }
            isValid
        } catch (e: Exception) {
            Timber.w(e, "Invalid URL format: '$url'")
            false
        }
    }

    /**
     * Валидирует цвет в формате ARGB
     */
    fun isValidColor(color: String?): Boolean {
        if (color.isNullOrBlank()) {
            Timber.w("Color is null or blank")
            return false
        }
        
        val colorRegex = Regex("^#[0-9A-Fa-f]{8}$")
        val isValid = colorRegex.matches(color.trim())
        
        if (!isValid) {
            Timber.w("Invalid color format: '$color' - expected #AARRGGBB")
        }
        return isValid
    }

}
