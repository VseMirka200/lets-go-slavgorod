package com.example.lets_go_slavgorod.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity класс для таблицы избранных времен отправления в Room базе данных
 * 
 * Представляет строку в таблице favorite_times SQLite базы данных.
 * Хранит информацию о сохраненных пользователем временах отправления автобусов
 * для быстрого доступа и планирования уведомлений.
 * 
 * Структура таблицы:
 * - id: первичный ключ, уникальный идентификатор записи
 * - route_id: внешний ключ к маршруту
 * - route_number: денормализованный номер маршрута (для производительности)
 * - route_name: денормализованное название маршрута (для производительности)
 * - stop_name: название остановки отправления
 * - departure_time: время отправления в формате HH:mm
 * - day_of_week: день недели (1-7, где 1=воскресенье)
 * - departure_point: пункт отправления (начальная остановка)
 * - added_date: timestamp добавления в избранное
 * - is_active: флаг активности (для soft delete)
 * 
 * @author VseMirka200
 * @version 2.0
 * @since 1.0
 */
@Entity(tableName = "favorite_times")
data class FavoriteTimeEntity(
    /** Уникальный идентификатор записи */
    @PrimaryKey
    val id: String,

    /** ID маршрута, к которому относится время */
    @ColumnInfo(name = "route_id")
    val routeId: String,

    /** Номер маршрута для отображения пользователю */
    @ColumnInfo(name = "route_number")
    val routeNumber: String,

    /** Название маршрута для отображения пользователю */
    @ColumnInfo(name = "route_name")
    val routeName: String,

    /** Название остановки отправления */
    @ColumnInfo(name = "stop_name")
    val stopName: String,

    /** Время отправления в формате HH:mm (24-часовой формат) */
    @ColumnInfo(name = "departure_time")
    val departureTime: String,

    /** День недели (1=воскресенье, 2=понедельник, ..., 7=суббота) */
    @ColumnInfo(name = "day_of_week")
    val dayOfWeek: Int,

    /** Пункт отправления (начальная остановка маршрута) */
    @ColumnInfo(name = "departure_point")
    val departurePoint: String,

    /** Timestamp добавления записи в избранное (System.currentTimeMillis()) */
    @ColumnInfo(name = "added_date")
    val addedDate: Long,

    /** Флаг активности записи (false = soft delete, не показывается в UI) */
    @ColumnInfo(name = "is_active", defaultValue = "true")
    val isActive: Boolean = true
)