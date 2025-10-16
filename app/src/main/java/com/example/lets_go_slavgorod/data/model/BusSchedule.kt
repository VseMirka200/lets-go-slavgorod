package com.example.lets_go_slavgorod.data.model

import com.example.lets_go_slavgorod.utils.ValidationUtils
import com.example.lets_go_slavgorod.utils.loge

/**
 * Модель расписания автобуса для конкретной остановки
 * 
 * Представляет собой единичное событие отправления автобуса с определенной остановки
 * в определенное время и день недели. Используется для построения расписания
 * и планирования уведомлений пользователей.
 * 
 * Модель содержит:
 * - Временную информацию (время отправления, день недели)
 * - Локационную информацию (название остановки, пункт отправления)
 * - Связь с маршрутом (routeId)
 * - Дополнительную информацию (заметки, тип дня)
 * 
 * @param id Уникальный идентификатор расписания в системе
 * @param routeId Идентификатор маршрута, к которому относится расписание
 * @param stopName Название остановки отправления
 * @param departureTime Время отправления в формате HH:mm (24-часовой формат)
 * @param dayOfWeek День недели (1=воскресенье, 2=понедельник, ..., 7=суббота)
 * @param isWeekend Флаг выходного дня (суббота/воскресенье)
 * @param notes Дополнительные заметки к расписанию (например, "только в будни")
 * @param departurePoint Пункт отправления (начальная остановка маршрута)
 * 
 * @author VseMirka200
 * @version 2.0
 * @since 1.0
 */
data class BusSchedule(
    val id: String,                     // Уникальный ID расписания
    val routeId: String,                // ID маршрута
    val stopName: String,               // Название остановки
    val departureTime: String,          // Время отправления (HH:mm)
    val dayOfWeek: Int,                 // День недели (1-7)
    val isWeekend: Boolean = false,     // Выходной день
    val notes: String? = null,          // Дополнительные заметки
    val departurePoint: String          // Пункт отправления
) {
    
    // Валидация данных расписания
    fun isValid(): Boolean {
        return try {
            val validations = listOf(
                ValidationUtils.isValidRouteId(id) to "Invalid schedule ID: '$id'",
                ValidationUtils.isValidRouteId(routeId) to "Invalid route ID: '$routeId'",
                ValidationUtils.isValidStopName(stopName) to "Invalid stop name: '$stopName'",
                ValidationUtils.isValidTime(departureTime) to "Invalid departure time: '$departureTime'",
                ValidationUtils.isValidDayOfWeek(dayOfWeek) to "Invalid day of week: $dayOfWeek",
                ValidationUtils.isValidStopName(departurePoint) to "Invalid departure point: '$departurePoint'"
            )
            
            val failedValidations = validations.filter { !it.first }
            if (failedValidations.isNotEmpty()) {
                failedValidations.forEach { (_, message) ->
                    loge(message)
                }
                return false
            }
            
            true
        } catch (e: Exception) {
            loge("Error validating BusSchedule", e)
            false
        }
    }
    
    /**
     * Создает очищенную и санитизированную копию расписания
     * 
     * Удаляет лишние пробелы, нормализует строки и подготавливает данные
     * для безопасного использования в приложении.
     * 
     * @return новая копия BusSchedule с очищенными данными
     */
    fun sanitized(): BusSchedule {
        return copy(
            id = ValidationUtils.sanitizeString(id),
            routeId = ValidationUtils.sanitizeString(routeId),
            stopName = ValidationUtils.sanitizeString(stopName),
            departureTime = ValidationUtils.sanitizeString(departureTime),
            departurePoint = ValidationUtils.sanitizeString(departurePoint),
            notes = notes?.let { ValidationUtils.sanitizeString(it) }
        )
    }
    
    /**
     * Проверяет, является ли расписание для буднего дня
     * 
     * Будние дни: понедельник (2) - пятница (6)
     * 
     * @return true если расписание для буднего дня
     */
    fun isWeekday(): Boolean {
        return dayOfWeek in 2..6
    }
    
    /**
     * Проверяет, является ли расписание для выходного дня
     * 
     * Выходные дни: суббота (7) и воскресенье (1)
     * 
     * @return true если расписание для выходного дня
     */
    fun isWeekendDay(): Boolean {
        return dayOfWeek == 1 || dayOfWeek == 7
    }
    
    /**
     * Получает название дня недели на русском языке
     * 
     * @return название дня недели
     */
    fun getDayName(): String {
        return when (dayOfWeek) {
            1 -> "Воскресенье"
            2 -> "Понедельник"
            3 -> "Вторник"
            4 -> "Среда"
            5 -> "Четверг"
            6 -> "Пятница"
            7 -> "Суббота"
            else -> "Неизвестный день"
        }
    }
    
    /**
     * Получает краткое название дня недели
     * 
     * @return краткое название дня недели (ПН, ВТ, СР, и т.д.)
     */
    fun getDayShortName(): String {
        return when (dayOfWeek) {
            1 -> "ВС"
            2 -> "ПН"
            3 -> "ВТ"
            4 -> "СР"
            5 -> "ЧТ"
            6 -> "ПТ"
            7 -> "СБ"
            else -> "??"
        }
    }
}