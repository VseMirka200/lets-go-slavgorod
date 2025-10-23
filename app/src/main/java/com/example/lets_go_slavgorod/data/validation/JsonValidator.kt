package com.example.lets_go_slavgorod.data.validation

import com.example.lets_go_slavgorod.data.model.BusRoute
import com.example.lets_go_slavgorod.data.model.BusSchedule
import timber.log.Timber

/**
 * Валидатор JSON данных
 * 
 * Обеспечивает валидацию данных, загруженных из JSON,
 * для предотвращения некорректных данных в приложении.
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.0
 */
object JsonValidator {
    
    /**
     * Валидирует список маршрутов
     * 
     * @param routes список маршрутов для валидации
     * @return список валидных маршрутов
     * @throws IllegalArgumentException если нет валидных маршрутов
     */
    fun validateRoutes(routes: List<BusRoute>): List<BusRoute> {
        
        val validRoutes = routes.filter { route ->
            isValidRoute(route)
        }
        
        if (validRoutes.isEmpty()) {
            throw IllegalArgumentException("No valid routes found in JSON data")
        }
        
        val invalidCount = routes.size - validRoutes.size
        if (invalidCount > 0) {
        }
        
        return validRoutes
    }
    
    /**
     * Валидирует список расписаний
     * 
     * @param schedules список расписаний для валидации
     * @return список валидных расписаний
     */
    fun validateSchedules(schedules: List<BusSchedule>): List<BusSchedule> {
        
        val validSchedules = schedules.filter { schedule ->
            isValidSchedule(schedule)
        }
        
        val invalidCount = schedules.size - validSchedules.size
        if (invalidCount > 0) {
        }
        
        return validSchedules
    }
    
    /**
     * Проверяет валидность маршрута
     */
    private fun isValidRoute(route: BusRoute): Boolean {
        return route.id.isNotBlank() &&
               route.name.isNotBlank() &&
               route.routeNumber.isNotBlank() &&
               route.color.isNotBlank() &&
               (route.pricePrimary?.isNotBlank() != false) &&
               (route.priceSecondary?.isNotBlank() != false)
    }
    
    /**
     * Проверяет валидность расписания
     */
    private fun isValidSchedule(schedule: BusSchedule): Boolean {
        return schedule.routeId.isNotBlank() &&
               schedule.departurePoint.isNotBlank() &&
               schedule.departureTime.isNotBlank() &&
               schedule.dayOfWeek in 1..7 &&
               isValidTimeFormat(schedule.departureTime)
    }
    
    /**
     * Проверяет формат времени (HH:mm)
     */
    private fun isValidTimeFormat(time: String): Boolean {
        return try {
            val parts = time.split(":")
            parts.size == 2 &&
            parts[0].toInt() in 0..23 &&
            parts[1].toInt() in 0..59
        } catch (e: Exception) {
            false
        }
    }
}
