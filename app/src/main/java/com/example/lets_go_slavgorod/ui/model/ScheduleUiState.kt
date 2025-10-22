package com.example.lets_go_slavgorod.ui.model

import androidx.compose.runtime.Immutable
import com.example.lets_go_slavgorod.data.model.BusRoute
import com.example.lets_go_slavgorod.data.model.BusSchedule

/**
 * Состояние UI для экрана расписания
 * 
 * Объединяет все данные необходимые для отображения расписания маршрута
 * в единый неизменяемый объект. Упрощает сигнатуру ScheduleList с 17 параметров до 4.
 * 
 * Преимущества:
 * - Type-safe передача данных
 * - Проще добавлять новые данные
 * - Легче тестировать
 * - @Immutable для оптимизации Compose
 * 
 * @param route маршрут для отображения
 * @param departurePoints список точек отправления с расписаниями
 * @param favoriteIds set ID избранных времен для быстрой проверки
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.1
 */
@Immutable
data class ScheduleUiState(
    val route: BusRoute,
    val departurePoints: List<DeparturePointSchedules>,
    val favoriteIds: Set<String> = emptySet()
) {
    /**
     * Получает расписание для определенной точки отправления
     */
    fun getSchedulesFor(departurePoint: String): List<BusSchedule> {
        return departurePoints.find { it.name == departurePoint }?.schedules ?: emptyList()
    }
    
    /**
     * Получает ID ближайшего рейса для точки отправления
     */
    fun getNextUpcomingId(departurePoint: String): String? {
        return departurePoints.find { it.name == departurePoint }?.nextUpcomingId
    }
    
    /**
     * Проверяет, является ли время избранным
     */
    fun isFavorite(scheduleId: String): Boolean {
        return scheduleId in favoriteIds
    }
}

/**
 * Расписания для одной точки отправления
 * 
 * Группирует все расписания отправлений из одной точки
 * с метаданными (название точки, ближайший рейс).
 * 
 * @param name название точки отправления (например, "Славгород", "Яровое")
 * @param schedules список расписаний для этой точки
 * @param nextUpcomingId ID ближайшего предстоящего рейса (для подсветки)
 */
@Immutable
data class DeparturePointSchedules(
    val name: String,
    val schedules: List<BusSchedule>,
    val nextUpcomingId: String? = null
) {
    /**
     * Получает количество расписаний
     */
    val count: Int get() = schedules.size
    
    /**
     * Проверяет, есть ли расписания
     */
    val isEmpty: Boolean get() = schedules.isEmpty()
    val isNotEmpty: Boolean get() = schedules.isNotEmpty()
}

/**
 * Extension функции для создания ScheduleUiState
 */

/**
 * Создает ScheduleUiState из списков расписаний
 */
fun createScheduleUiState(
    route: BusRoute,
    schedulesByPoint: Map<String, List<BusSchedule>>,
    nextUpcomingIds: Map<String, String?> = emptyMap(),
    favoriteIds: Set<String> = emptySet()
): ScheduleUiState {
    val departurePoints = schedulesByPoint.map { (name, schedules) ->
        DeparturePointSchedules(
            name = name,
            schedules = schedules,
            nextUpcomingId = nextUpcomingIds[name]
        )
    }
    
    return ScheduleUiState(
        route = route,
        departurePoints = departurePoints,
        favoriteIds = favoriteIds
    )
}