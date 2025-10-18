package com.example.lets_go_slavgorod.utils

import com.example.lets_go_slavgorod.data.model.BusSchedule
import timber.log.Timber
import java.util.*

/**
 * Утилиты для работы с расписанием автобусов
 * 
 * Основные функции:
 * - Генерация статических расписаний для всех маршрутов
 * - Создание расписаний для маршрута №102 (Славгород — Яровое)
 * - Создание расписаний для маршрута №1 (Вокзал — Совхоз)
 * - Поддержка различных выходов (Выход 1, 2, 3)
 * 
 * @author VseMirka200
 * @version 1.0
 */
object ScheduleUtils {
    
    /**
     * Генерирует стандартные расписания для всех маршрутов
     * 
     * @param routeId ID маршрута (не может быть null или пустым)
     * @return список расписаний для маршрута
     * @throws IllegalArgumentException если routeId некорректный
     */
    fun generateSchedules(routeId: String): List<BusSchedule> {
        // Валидация входных данных (isNotBlank уже включает isNotEmpty, избыточность убрана)
        require(routeId.isNotBlank()) { "Route ID cannot be blank" }
        val currentDayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        
        return when (routeId) {
            "102" -> generateRoute102Schedules(currentDayOfWeek)
            "102B" -> generateRoute102BSchedules(currentDayOfWeek)
            "1" -> generateRoute1Schedules(currentDayOfWeek)
            else -> {
                // Для новых маршрутов (3, 4 и т.д.) возвращаем пустой список
                // Расписание должно загружаться из JSON (assets или GitHub)
                Timber.w("⚠️ No hardcoded schedules for route $routeId - should load from JSON")
                emptyList()
            }
        }
    }
    
    /**
     * Генерирует расписания для маршрута №102 (Славгород — Яровое)
     * Расписание действует с 09.01.2019г.
     */
    private fun generateRoute102Schedules(dayOfWeek: Int): List<BusSchedule> {
        return listOf(
            // Рынок (Славгород)
            BusSchedule("102_slav_1", "102", "Рынок (Славгород)", "06:25", dayOfWeek, notes = null, departurePoint = "Рынок (Славгород)"),
            BusSchedule("102_slav_2", "102", "Рынок (Славгород)", "06:45", dayOfWeek, notes = null, departurePoint = "Рынок (Славгород)"),
            BusSchedule("102_slav_3", "102", "Рынок (Славгород)", "07:00", dayOfWeek, notes = null, departurePoint = "Рынок (Славгород)"),
            BusSchedule("102_slav_4", "102", "Рынок (Славгород)", "07:20", dayOfWeek, notes = null, departurePoint = "Рынок (Славгород)"),
            BusSchedule("102_slav_5", "102", "Рынок (Славгород)", "07:40", dayOfWeek, notes = null, departurePoint = "Рынок (Славгород)"),
            BusSchedule("102_slav_6", "102", "Рынок (Славгород)", "08:00", dayOfWeek, notes = null, departurePoint = "Рынок (Славгород)"),
            BusSchedule("102_slav_7", "102", "Рынок (Славгород)", "08:25", dayOfWeek, notes = null, departurePoint = "Рынок (Славгород)"),
            BusSchedule("102_slav_8", "102", "Рынок (Славгород)", "08:40", dayOfWeek, notes = null, departurePoint = "Рынок (Славгород)"),
            BusSchedule("102_slav_9", "102", "Рынок (Славгород)", "09:00", dayOfWeek, notes = null, departurePoint = "Рынок (Славгород)"),
            BusSchedule("102_slav_10", "102", "Рынок (Славгород)", "09:20", dayOfWeek, notes = null, departurePoint = "Рынок (Славгород)"),
            BusSchedule("102_slav_11", "102", "Рынок (Славгород)", "09:35", dayOfWeek, notes = null, departurePoint = "Рынок (Славгород)"),
            BusSchedule("102_slav_12", "102", "Рынок (Славгород)", "10:00", dayOfWeek, notes = null, departurePoint = "Рынок (Славгород)"),
            BusSchedule("102_slav_13", "102", "Рынок (Славгород)", "10:25", dayOfWeek, notes = null, departurePoint = "Рынок (Славгород)"),
            BusSchedule("102_slav_14", "102", "Рынок (Славгород)", "10:50", dayOfWeek, notes = null, departurePoint = "Рынок (Славгород)"),
            BusSchedule("102_slav_15", "102", "Рынок (Славгород)", "11:10", dayOfWeek, notes = null, departurePoint = "Рынок (Славгород)"),
            BusSchedule("102_slav_16", "102", "Рынок (Славгород)", "11:35", dayOfWeek, notes = null, departurePoint = "Рынок (Славгород)"),
            BusSchedule("102_slav_17", "102", "Рынок (Славгород)", "12:05", dayOfWeek, notes = null, departurePoint = "Рынок (Славгород)"),
            BusSchedule("102_slav_18", "102", "Рынок (Славгород)", "12:30", dayOfWeek, notes = null, departurePoint = "Рынок (Славгород)"),
            BusSchedule("102_slav_19", "102", "Рынок (Славгород)", "12:55", dayOfWeek, notes = null, departurePoint = "Рынок (Славгород)"),
            BusSchedule("102_slav_20", "102", "Рынок (Славгород)", "13:15", dayOfWeek, notes = null, departurePoint = "Рынок (Славгород)"),
            BusSchedule("102_slav_21", "102", "Рынок (Славгород)", "13:35", dayOfWeek, notes = null, departurePoint = "Рынок (Славгород)"),
            BusSchedule("102_slav_22", "102", "Рынок (Славгород)", "14:05", dayOfWeek, notes = null, departurePoint = "Рынок (Славгород)"),
            BusSchedule("102_slav_23", "102", "Рынок (Славгород)", "14:30", dayOfWeek, notes = null, departurePoint = "Рынок (Славгород)"),
            BusSchedule("102_slav_24", "102", "Рынок (Славгород)", "14:55", dayOfWeek, notes = null, departurePoint = "Рынок (Славгород)"),
            BusSchedule("102_slav_25", "102", "Рынок (Славгород)", "15:20", dayOfWeek, notes = null, departurePoint = "Рынок (Славгород)"),
            BusSchedule("102_slav_26", "102", "Рынок (Славгород)", "15:45", dayOfWeek, notes = null, departurePoint = "Рынок (Славгород)"),
            BusSchedule("102_slav_27", "102", "Рынок (Славгород)", "16:10", dayOfWeek, notes = null, departurePoint = "Рынок (Славгород)"),
            BusSchedule("102_slav_28", "102", "Рынок (Славгород)", "16:35", dayOfWeek, notes = null, departurePoint = "Рынок (Славгород)"),
            BusSchedule("102_slav_29", "102", "Рынок (Славгород)", "17:05", dayOfWeek, notes = null, departurePoint = "Рынок (Славгород)"),
            BusSchedule("102_slav_30", "102", "Рынок (Славгород)", "17:25", dayOfWeek, notes = null, departurePoint = "Рынок (Славгород)"),
            BusSchedule("102_slav_31", "102", "Рынок (Славгород)", "17:50", dayOfWeek, notes = null, departurePoint = "Рынок (Славгород)"),
            BusSchedule("102_slav_32", "102", "Рынок (Славгород)", "18:20", dayOfWeek, notes = null, departurePoint = "Рынок (Славгород)"),
            BusSchedule("102_slav_33", "102", "Рынок (Славгород)", "18:50", dayOfWeek, notes = null, departurePoint = "Рынок (Славгород)"),
            BusSchedule("102_slav_34", "102", "Рынок (Славгород)", "19:20", dayOfWeek, notes = null, departurePoint = "Рынок (Славгород)"),
            BusSchedule("102_slav_35", "102", "Рынок (Славгород)", "20:00", dayOfWeek, notes = null, departurePoint = "Рынок (Славгород)"),
            BusSchedule("102_slav_36", "102", "Рынок (Славгород)", "20:30", dayOfWeek, notes = null, departurePoint = "Рынок (Славгород)"),
            BusSchedule("102_slav_37", "102", "Рынок (Славгород)", "21:00", dayOfWeek, notes = null, departurePoint = "Рынок (Славгород)"),


            // МСЧ-128 (Яровое)
            BusSchedule("102_yar_1", "102", "МСЧ-128 (Яровое)", "07:00", dayOfWeek, notes = null, departurePoint = "МСЧ-128 (Яровое)"),
            BusSchedule("102_yar_2", "102", "МСЧ-128 (Яровое)", "07:20", dayOfWeek, notes = null, departurePoint = "МСЧ-128 (Яровое)"),
            BusSchedule("102_yar_3", "102", "МСЧ-128 (Яровое)", "07:35", dayOfWeek, notes = null, departurePoint = "МСЧ-128 (Яровое)"),
            BusSchedule("102_yar_4", "102", "МСЧ-128 (Яровое)", "07:55", dayOfWeek, notes = null, departurePoint = "МСЧ-128 (Яровое)"),
            BusSchedule("102_yar_5", "102", "МСЧ-128 (Яровое)", "08:20", dayOfWeek, notes = null, departurePoint = "МСЧ-128 (Яровое)"),
            BusSchedule("102_yar_6", "102", "МСЧ-128 (Яровое)", "08:40", dayOfWeek, notes = null, departurePoint = "МСЧ-128 (Яровое)"),
            BusSchedule("102_yar_7", "102", "МСЧ-128 (Яровое)", "09:00", dayOfWeek, notes = null, departurePoint = "МСЧ-128 (Яровое)"),
            BusSchedule("102_yar_8", "102", "МСЧ-128 (Яровое)", "09:20", dayOfWeek, notes = null, departurePoint = "МСЧ-128 (Яровое)"),
            BusSchedule("102_yar_9", "102", "МСЧ-128 (Яровое)", "09:40", dayOfWeek, notes = null, departurePoint = "МСЧ-128 (Яровое)"),
            BusSchedule("102_yar_10", "102", "МСЧ-128 (Яровое)", "10:00", dayOfWeek, notes = null, departurePoint = "МСЧ-128 (Яровое)"),
            BusSchedule("102_yar_11", "102", "МСЧ-128 (Яровое)", "10:15", dayOfWeek, notes = null, departurePoint = "МСЧ-128 (Яровое)"),
            BusSchedule("102_yar_12", "102", "МСЧ-128 (Яровое)", "10:35", dayOfWeek, notes = null, departurePoint = "МСЧ-128 (Яровое)"),
            BusSchedule("102_yar_13", "102", "МСЧ-128 (Яровое)", "11:10", dayOfWeek, notes = null, departurePoint = "МСЧ-128 (Яровое)"),
            BusSchedule("102_yar_14", "102", "МСЧ-128 (Яровое)", "11:30", dayOfWeek, notes = null, departurePoint = "МСЧ-128 (Яровое)"),
            BusSchedule("102_yar_15", "102", "МСЧ-128 (Яровое)", "11:55", dayOfWeek, notes = null, departurePoint = "МСЧ-128 (Яровое)"),
            BusSchedule("102_yar_16", "102", "МСЧ-128 (Яровое)", "12:20", dayOfWeek, notes = null, departurePoint = "МСЧ-128 (Яровое)"),
            BusSchedule("102_yar_17", "102", "МСЧ-128 (Яровое)", "12:40", dayOfWeek, notes = null, departurePoint = "МСЧ-128 (Яровое)"),
            BusSchedule("102_yar_18", "102", "МСЧ-128 (Яровое)", "13:05", dayOfWeek, notes = null, departurePoint = "МСЧ-128 (Яровое)"),
            BusSchedule("102_yar_19", "102", "МСЧ-128 (Яровое)", "13:30", dayOfWeek, notes = null, departurePoint = "МСЧ-128 (Яровое)"),
            BusSchedule("102_yar_20", "102", "МСЧ-128 (Яровое)", "13:55", dayOfWeek, notes = null, departurePoint = "МСЧ-128 (Яровое)"),
            BusSchedule("102_yar_21", "102", "МСЧ-128 (Яровое)", "14:15", dayOfWeek, notes = null, departurePoint = "МСЧ-128 (Яровое)"),
            BusSchedule("102_yar_22", "102", "МСЧ-128 (Яровое)", "14:45", dayOfWeek, notes = null, departurePoint = "МСЧ-128 (Яровое)"),
            BusSchedule("102_yar_23", "102", "МСЧ-128 (Яровое)", "15:10", dayOfWeek, notes = null, departurePoint = "МСЧ-128 (Яровое)"),
            BusSchedule("102_yar_24", "102", "МСЧ-128 (Яровое)", "15:30", dayOfWeek, notes = null, departurePoint = "МСЧ-128 (Яровое)"),
            BusSchedule("102_yar_25", "102", "МСЧ-128 (Яровое)", "15:55", dayOfWeek, notes = null, departurePoint = "МСЧ-128 (Яровое)"),
            BusSchedule("102_yar_26", "102", "МСЧ-128 (Яровое)", "16:20", dayOfWeek, notes = null, departurePoint = "МСЧ-128 (Яровое)"),
            BusSchedule("102_yar_27", "102", "МСЧ-128 (Яровое)", "16:45", dayOfWeek, notes = null, departurePoint = "МСЧ-128 (Яровое)"),
            BusSchedule("102_yar_28", "102", "МСЧ-128 (Яровое)", "17:10", dayOfWeek, notes = null, departurePoint = "МСЧ-128 (Яровое)"),
            BusSchedule("102_yar_29", "102", "МСЧ-128 (Яровое)", "17:40", dayOfWeek, notes = null, departurePoint = "МСЧ-128 (Яровое)"),
            BusSchedule("102_yar_30", "102", "МСЧ-128 (Яровое)", "18:10", dayOfWeek, notes = null, departurePoint = "МСЧ-128 (Яровое)"),
            BusSchedule("102_yar_31", "102", "МСЧ-128 (Яровое)", "18:35", dayOfWeek, notes = null, departurePoint = "МСЧ-128 (Яровое)"),
            BusSchedule("102_yar_32", "102", "МСЧ-128 (Яровое)", "19:00", dayOfWeek, notes = null, departurePoint = "МСЧ-128 (Яровое)"),
            BusSchedule("102_yar_33", "102", "МСЧ-128 (Яровое)", "19:25", dayOfWeek, notes = null, departurePoint = "МСЧ-128 (Яровое)"),
            BusSchedule("102_yar_34", "102", "МСЧ-128 (Яровое)", "20:00", dayOfWeek, notes = null, departurePoint = "МСЧ-128 (Яровое)"),
            BusSchedule("102_yar_35", "102", "МСЧ-128 (Яровое)", "20:30", dayOfWeek, notes = null, departurePoint = "МСЧ-128 (Яровое)"),
            BusSchedule("102_yar_36", "102", "МСЧ-128 (Яровое)", "21:00", dayOfWeek, notes = null, departurePoint = "МСЧ-128 (Яровое)")
        )
    }
    
    /**
     * Генерирует упрощенное расписание для маршрута №102Б (Славгород — Яровое)
     * Согласно изображению расписания
     */
    private fun generateRoute102BSchedules(dayOfWeek: Int): List<BusSchedule> {
        return listOf(
            // Рынок (Славгород)
            BusSchedule("102B_slav_1", "102B", "Рынок (Славгород)", "06:30", dayOfWeek, notes = null, departurePoint = "Рынок (Славгород)"),
            BusSchedule("102B_slav_2", "102B", "Рынок (Славгород)", "07:50", dayOfWeek, notes = null, departurePoint = "Рынок (Славгород)"),
            BusSchedule("102B_slav_3", "102B", "Рынок (Славгород)", "14:40", dayOfWeek, notes = null, departurePoint = "Рынок (Славгород)"),
            BusSchedule("102B_slav_4", "102B", "Рынок (Славгород)", "16:00", dayOfWeek, notes = null, departurePoint = "Рынок (Славгород)"),
            BusSchedule("102B_slav_5", "102B", "Рынок (Славгород)", "17:20", dayOfWeek, notes = null, departurePoint = "Рынок (Славгород)"),

            // Ст. Зори (Яровое)
            BusSchedule("102B_yar_1", "102B", "Ст. Зори (Яровое)", "07:10", dayOfWeek, notes = null, departurePoint = "Ст. Зори (Яровое)"),
            BusSchedule("102B_yar_2", "102B", "Ст. Зори (Яровое)", "08:30", dayOfWeek, notes = null, departurePoint = "Ст. Зори (Яровое)"),
            BusSchedule("102B_yar_3", "102B", "Ст. Зори (Яровое)", "15:20", dayOfWeek, notes = null, departurePoint = "Ст. Зори (Яровое)"),
            BusSchedule("102B_yar_4", "102B", "Ст. Зори (Яровое)", "16:40", dayOfWeek, notes = null, departurePoint = "Ст. Зори (Яровое)"),
            BusSchedule("102B_yar_5", "102B", "Ст. Зори (Яровое)", "18:00", dayOfWeek, notes = null, departurePoint = "Ст. Зори (Яровое)")
        )
    }
    
    /**
     * Генерирует расписания для маршрута №1 (Вокзал — Совхоз)
     * Расписание по сменам (выходам) согласно официальному графику
     */
    private fun generateRoute1Schedules(dayOfWeek: Int): List<BusSchedule> {
        return listOf(
            // ВЫХОД 1 - вокзал
            BusSchedule("1_vokzal_1_1", "1", "вокзал", "07:00", dayOfWeek, notes = "Выход 1", departurePoint = "вокзал"),
            BusSchedule("1_vokzal_1_2", "1", "вокзал", "07:48", dayOfWeek, notes = "Выход 1", departurePoint = "вокзал"),
            BusSchedule("1_vokzal_1_3", "1", "вокзал", "08:36", dayOfWeek, notes = "Выход 1", departurePoint = "вокзал"),
            BusSchedule("1_vokzal_1_4", "1", "вокзал", "09:24", dayOfWeek, notes = "Выход 1", departurePoint = "вокзал"),
            BusSchedule("1_vokzal_1_5", "1", "вокзал", "10:12", dayOfWeek, notes = "Выход 1", departurePoint = "вокзал"),
            BusSchedule("1_vokzal_1_6", "1", "вокзал", "11:00", dayOfWeek, notes = "Выход 1", departurePoint = "вокзал"),
            BusSchedule("1_vokzal_1_7", "1", "вокзал", "11:48", dayOfWeek, notes = "Выход 1", departurePoint = "вокзал"),
            BusSchedule("1_vokzal_1_8", "1", "вокзал", "12:36", dayOfWeek, notes = "Выход 1", departurePoint = "вокзал"),
            // Перерыв с 12:36 по 13:24
            BusSchedule("1_vokzal_1_9", "1", "вокзал", "13:24", dayOfWeek, notes = "Выход 1", departurePoint = "вокзал"),
            BusSchedule("1_vokzal_1_10", "1", "вокзал", "14:12", dayOfWeek, notes = "Выход 1", departurePoint = "вокзал"),
            BusSchedule("1_vokzal_1_11", "1", "вокзал", "15:00", dayOfWeek, notes = "Выход 1", departurePoint = "вокзал"),
            BusSchedule("1_vokzal_1_12", "1", "вокзал", "15:48", dayOfWeek, notes = "Выход 1", departurePoint = "вокзал"),
            BusSchedule("1_vokzal_1_13", "1", "вокзал", "16:36", dayOfWeek, notes = "Выход 1", departurePoint = "вокзал"),
            BusSchedule("1_vokzal_1_14", "1", "вокзал", "17:24", dayOfWeek, notes = "Выход 1", departurePoint = "вокзал"),
            BusSchedule("1_vokzal_1_15", "1", "вокзал", "18:12", dayOfWeek, notes = "Выход 1", departurePoint = "вокзал"),
            BusSchedule("1_vokzal_1_16", "1", "вокзал", "19:00", dayOfWeek, notes = "Выход 1", departurePoint = "вокзал"),
            BusSchedule("1_vokzal_1_17", "1", "вокзал", "19:48", dayOfWeek, notes = "Выход 1", departurePoint = "вокзал"),
            
            // ВЫХОД 1 - совхоз
            BusSchedule("1_sovhoz_1_1", "1", "совхоз", "07:24", dayOfWeek, notes = "Выход 1", departurePoint = "совхоз"),
            BusSchedule("1_sovhoz_1_2", "1", "совхоз", "08:12", dayOfWeek, notes = "Выход 1", departurePoint = "совхоз"),
            BusSchedule("1_sovhoz_1_3", "1", "совхоз", "09:00", dayOfWeek, notes = "Выход 1", departurePoint = "совхоз"),
            BusSchedule("1_sovhoz_1_4", "1", "совхоз", "09:48", dayOfWeek, notes = "Выход 1", departurePoint = "совхоз"),
            BusSchedule("1_sovhoz_1_5", "1", "совхоз", "10:36", dayOfWeek, notes = "Выход 1", departurePoint = "совхоз"),
            BusSchedule("1_sovhoz_1_6", "1", "совхоз", "11:24", dayOfWeek, notes = "Выход 1", departurePoint = "совхоз"),
            BusSchedule("1_sovhoz_1_7", "1", "совхоз", "12:12", dayOfWeek, notes = "Выход 1", departurePoint = "совхоз"),
            // Перерыв
            BusSchedule("1_sovhoz_1_8", "1", "совхоз", "13:48", dayOfWeek, notes = "Выход 1", departurePoint = "совхоз"),
            BusSchedule("1_sovhoz_1_9", "1", "совхоз", "14:36", dayOfWeek, notes = "Выход 1", departurePoint = "совхоз"),
            BusSchedule("1_sovhoz_1_10", "1", "совхоз", "15:24", dayOfWeek, notes = "Выход 1", departurePoint = "совхоз"),
            BusSchedule("1_sovhoz_1_11", "1", "совхоз", "16:12", dayOfWeek, notes = "Выход 1", departurePoint = "совхоз"),
            BusSchedule("1_sovhoz_1_12", "1", "совхоз", "17:00", dayOfWeek, notes = "Выход 1", departurePoint = "совхоз"),
            BusSchedule("1_sovhoz_1_13", "1", "совхоз", "17:48", dayOfWeek, notes = "Выход 1", departurePoint = "совхоз"),
            BusSchedule("1_sovhoz_1_14", "1", "совхоз", "18:36", dayOfWeek, notes = "Выход 1", departurePoint = "совхоз"),
            BusSchedule("1_sovhoz_1_15", "1", "совхоз", "19:24", dayOfWeek, notes = "Выход 1", departurePoint = "совхоз"),
            
            // ВЫХОД 2 - вокзал
            BusSchedule("1_vokzal_2_1", "1", "вокзал", "07:15", dayOfWeek, notes = "Выход 2", departurePoint = "вокзал"),
            BusSchedule("1_vokzal_2_2", "1", "вокзал", "08:03", dayOfWeek, notes = "Выход 2", departurePoint = "вокзал"),
            BusSchedule("1_vokzal_2_3", "1", "вокзал", "08:51", dayOfWeek, notes = "Выход 2", departurePoint = "вокзал"),
            BusSchedule("1_vokzal_2_4", "1", "вокзал", "09:39", dayOfWeek, notes = "Выход 2", departurePoint = "вокзал"),
            BusSchedule("1_vokzal_2_5", "1", "вокзал", "10:27", dayOfWeek, notes = "Выход 2", departurePoint = "вокзал"),
            BusSchedule("1_vokzal_2_6", "1", "вокзал", "11:15", dayOfWeek, notes = "Выход 2", departurePoint = "вокзал"),
            BusSchedule("1_vokzal_2_7", "1", "вокзал", "12:03", dayOfWeek, notes = "Выход 2", departurePoint = "вокзал"),
            BusSchedule("1_vokzal_2_8", "1", "вокзал", "12:51", dayOfWeek, notes = "Выход 2", departurePoint = "вокзал"),
            BusSchedule("1_vokzal_2_9", "1", "вокзал", "13:39", dayOfWeek, notes = "Выход 2", departurePoint = "вокзал"),
            // Перерыв с 13:39 по 14:27
            BusSchedule("1_vokzal_2_10", "1", "вокзал", "14:27", dayOfWeek, notes = "Выход 2", departurePoint = "вокзал"),
            BusSchedule("1_vokzal_2_11", "1", "вокзал", "15:15", dayOfWeek, notes = "Выход 2", departurePoint = "вокзал"),
            BusSchedule("1_vokzal_2_12", "1", "вокзал", "16:03", dayOfWeek, notes = "Выход 2", departurePoint = "вокзал"),
            BusSchedule("1_vokzal_2_13", "1", "вокзал", "16:51", dayOfWeek, notes = "Выход 2", departurePoint = "вокзал"),
            BusSchedule("1_vokzal_2_14", "1", "вокзал", "17:39", dayOfWeek, notes = "Выход 2", departurePoint = "вокзал"),
            BusSchedule("1_vokzal_2_15", "1", "вокзал", "18:27", dayOfWeek, notes = "Выход 2", departurePoint = "вокзал"),
            
            // ВЫХОД 2 - совхоз
            BusSchedule("1_sovhoz_2_1", "1", "совхоз", "07:39", dayOfWeek, notes = "Выход 2", departurePoint = "совхоз"),
            BusSchedule("1_sovhoz_2_2", "1", "совхоз", "08:27", dayOfWeek, notes = "Выход 2", departurePoint = "совхоз"),
            BusSchedule("1_sovhoz_2_3", "1", "совхоз", "09:15", dayOfWeek, notes = "Выход 2", departurePoint = "совхоз"),
            BusSchedule("1_sovhoz_2_4", "1", "совхоз", "10:03", dayOfWeek, notes = "Выход 2", departurePoint = "совхоз"),
            BusSchedule("1_sovhoz_2_5", "1", "совхоз", "10:51", dayOfWeek, notes = "Выход 2", departurePoint = "совхоз"),
            BusSchedule("1_sovhoz_2_6", "1", "совхоз", "11:39", dayOfWeek, notes = "Выход 2", departurePoint = "совхоз"),
            BusSchedule("1_sovhoz_2_7", "1", "совхоз", "12:27", dayOfWeek, notes = "Выход 2", departurePoint = "совхоз"),
            BusSchedule("1_sovhoz_2_8", "1", "совхоз", "13:15", dayOfWeek, notes = "Выход 2", departurePoint = "совхоз"),
            // Перерыв
            BusSchedule("1_sovhoz_2_9", "1", "совхоз", "14:51", dayOfWeek, notes = "Выход 2", departurePoint = "совхоз"),
            BusSchedule("1_sovhoz_2_10", "1", "совхоз", "15:39", dayOfWeek, notes = "Выход 2", departurePoint = "совхоз"),
            BusSchedule("1_sovhoz_2_11", "1", "совхоз", "16:27", dayOfWeek, notes = "Выход 2", departurePoint = "совхоз"),
            BusSchedule("1_sovhoz_2_12", "1", "совхоз", "17:15", dayOfWeek, notes = "Выход 2", departurePoint = "совхоз"),
            BusSchedule("1_sovhoz_2_13", "1", "совхоз", "18:03", dayOfWeek, notes = "Выход 2", departurePoint = "совхоз"),
            
            // ВЫХОД 3 - вокзал
            BusSchedule("1_vokzal_3_1", "1", "вокзал", "07:30", dayOfWeek, notes = "Выход 3", departurePoint = "вокзал"),
            BusSchedule("1_vokzal_3_2", "1", "вокзал", "08:18", dayOfWeek, notes = "Выход 3", departurePoint = "вокзал"),
            BusSchedule("1_vokzal_3_3", "1", "вокзал", "09:06", dayOfWeek, notes = "Выход 3", departurePoint = "вокзал"),
            BusSchedule("1_vokzal_3_4", "1", "вокзал", "09:54", dayOfWeek, notes = "Выход 3", departurePoint = "вокзал"),
            BusSchedule("1_vokzal_3_5", "1", "вокзал", "10:42", dayOfWeek, notes = "Выход 3", departurePoint = "вокзал"),
            BusSchedule("1_vokzal_3_6", "1", "вокзал", "11:30", dayOfWeek, notes = "Выход 3", departurePoint = "вокзал"),
            // Перерыв с 11:30 по 12:18
            BusSchedule("1_vokzal_3_7", "1", "вокзал", "12:18", dayOfWeek, notes = "Выход 3", departurePoint = "вокзал"),
            BusSchedule("1_vokzal_3_8", "1", "вокзал", "13:06", dayOfWeek, notes = "Выход 3", departurePoint = "вокзал"),
            BusSchedule("1_vokzal_3_9", "1", "вокзал", "13:54", dayOfWeek, notes = "Выход 3", departurePoint = "вокзал"),
            BusSchedule("1_vokzal_3_10", "1", "вокзал", "14:42", dayOfWeek, notes = "Выход 3", departurePoint = "вокзал"),
            BusSchedule("1_vokzal_3_11", "1", "вокзал", "15:30", dayOfWeek, notes = "Выход 3", departurePoint = "вокзал"),
            BusSchedule("1_vokzal_3_12", "1", "вокзал", "16:18", dayOfWeek, notes = "Выход 3", departurePoint = "вокзал"),
            BusSchedule("1_vokzal_3_13", "1", "вокзал", "17:06", dayOfWeek, notes = "Выход 3", departurePoint = "вокзал"),
            
            // ВЫХОД 3 - совхоз
            BusSchedule("1_sovhoz_3_1", "1", "совхоз", "07:54", dayOfWeek, notes = "Выход 3", departurePoint = "совхоз"),
            BusSchedule("1_sovhoz_3_2", "1", "совхоз", "08:42", dayOfWeek, notes = "Выход 3", departurePoint = "совхоз"),
            BusSchedule("1_sovhoz_3_3", "1", "совхоз", "09:30", dayOfWeek, notes = "Выход 3", departurePoint = "совхоз"),
            BusSchedule("1_sovhoz_3_4", "1", "совхоз", "10:18", dayOfWeek, notes = "Выход 3", departurePoint = "совхоз"),
            BusSchedule("1_sovhoz_3_5", "1", "совхоз", "11:06", dayOfWeek, notes = "Выход 3", departurePoint = "совхоз"),
            // Перерыв
            BusSchedule("1_sovhoz_3_6", "1", "совхоз", "12:42", dayOfWeek, notes = "Выход 3", departurePoint = "совхоз"),
            BusSchedule("1_sovhoz_3_7", "1", "совхоз", "13:30", dayOfWeek, notes = "Выход 3", departurePoint = "совхоз"),
            BusSchedule("1_sovhoz_3_8", "1", "совхоз", "14:18", dayOfWeek, notes = "Выход 3", departurePoint = "совхоз"),
            BusSchedule("1_sovhoz_3_9", "1", "совхоз", "15:06", dayOfWeek, notes = "Выход 3", departurePoint = "совхоз"),
            BusSchedule("1_sovhoz_3_10", "1", "совхоз", "15:54", dayOfWeek, notes = "Выход 3", departurePoint = "совхоз"),
            BusSchedule("1_sovhoz_3_11", "1", "совхоз", "16:42", dayOfWeek, notes = "Выход 3", departurePoint = "совхоз")
        )
    }
}
