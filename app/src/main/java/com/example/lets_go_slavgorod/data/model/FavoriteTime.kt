package com.example.lets_go_slavgorod.data.model

/**
 * Модель избранного времени отправления автобуса
 * 
 * Представляет собой сохраненное пользователем время отправления автобуса
 * для быстрого доступа и планирования уведомлений. Содержит всю необходимую
 * информацию для отображения в списке избранного и настройки напоминаний.
 * 
 * Модель включает в себя:
 * - Идентификационную информацию (ID, routeId)
 * - Отображаемую информацию (номер маршрута, название, остановка)
 * - Временную информацию (время отправления, день недели)
 * - Метаданные (дата добавления, активность)
 * 
 * @param id Уникальный идентификатор записи в избранном
 * @param routeId Идентификатор маршрута, к которому относится время
 * @param routeNumber Номер маршрута для отображения пользователю
 * @param routeName Название маршрута для отображения пользователю
 * @param stopName Название остановки отправления
 * @param departureTime Время отправления в формате HH:mm
 * @param dayOfWeek День недели (1=воскресенье, 2=понедельник, ..., 7=суббота)
 * @param departurePoint Пункт отправления (начальная остановка)
 * @param addedDate Timestamp добавления записи в избранное
 * @param isActive Флаг активности записи (неактивные записи не показываются)
 * 
 * @author VseMirka200
 * @version 2.0
 * @since 1.0
 */
data class FavoriteTime(
    val id: String,                     // Уникальный ID
    val routeId: String,                // ID маршрута
    val routeNumber: String,            // Номер маршрута
    val routeName: String,              // Название маршрута
    val stopName: String,               // Название остановки
    val departureTime: String,          // Время отправления
    val dayOfWeek: Int,                 // День недели (1-7)
    val departurePoint: String,         // Пункт отправления
    val addedDate: Long,                // Дата добавления в избранное (timestamp)
    val isActive: Boolean = true        // Активность записи
) {
    
    /**
     * Проверяет, является ли избранное время для буднего дня
     * 
     * @return true если время для буднего дня
     */
    fun isWeekday(): Boolean {
        return dayOfWeek in 2..6
    }
    
    /**
     * Проверяет, является ли избранное время для выходного дня
     * 
     * @return true если время для выходного дня
     */
    fun isWeekend(): Boolean {
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
     * Получает краткое описание избранного времени
     * 
     * Формат: "Маршрут №X в HH:mm (День недели)"
     * 
     * @return краткое описание
     */
    fun getShortDescription(): String {
        return "$routeNumber в $departureTime (${getDayName()})"
    }
}
