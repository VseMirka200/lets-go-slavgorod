package com.example.lets_go_slavgorod.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lets_go_slavgorod.data.model.BusSchedule
import com.example.lets_go_slavgorod.data.repository.BusRouteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel для управления расписаниями маршрутов
 * 
 * Специализированный ViewModel с единственной ответственностью:
 * загрузка и обновление расписаний для конкретных маршрутов.
 * 
 * Функции:
 * - Получение расписания для маршрута
 * - Обновление расписания (force refresh)
 * - Кэширование расписаний
 * 
 * Преимущества:
 * - Единственная ответственность (SRP)
 * - Независимый от других ViewModels
 * - Легко тестировать
 * - Переиспользуемый
 * 
 * @param application контекст приложения
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.1
 */
class ScheduleViewModel(application: Application) : AndroidViewModel(application) {
    
    private val appContext = application.applicationContext
    private val repository = BusRouteRepository(appContext)
    
    // Кэш расписаний по routeId
    private val schedulesCache = mutableMapOf<String, MutableStateFlow<List<BusSchedule>>>()
    
    /**
     * Получает расписание для маршрута
     * 
     * @param routeId ID маршрута
     * @return StateFlow с расписанием
     */
    fun getScheduleFor(routeId: String): StateFlow<List<BusSchedule>> {
        return schedulesCache.getOrPut(routeId) {
            val flow = MutableStateFlow<List<BusSchedule>>(emptyList())
            viewModelScope.launch {
                try {
                    // Асинхронная загрузка через suspend функцию
                    val loadedSchedules = repository.getSchedulesForRoute(routeId)
                    flow.value = loadedSchedules
                    Timber.d("Loaded ${loadedSchedules.size} schedules for route $routeId")
                } catch (e: Exception) {
                    Timber.e(e, "Error loading schedules for route $routeId")
                    flow.value = emptyList()
                }
            }
            flow
        }
    }
    
    /**
     * Принудительно обновляет расписание для маршрута
     * 
     * @param routeId ID маршрута
     */
    fun refreshSchedule(routeId: String) {
        viewModelScope.launch {
            try {
                Timber.d("Force refreshing schedule for route $routeId")
                // Асинхронная загрузка с принудительным обновлением
                val loadedSchedules = repository.getSchedulesForRoute(routeId, forceRefresh = true)
                
                val existingFlow = schedulesCache[routeId]
                if (existingFlow != null) {
                    existingFlow.value = loadedSchedules
                } else {
                    schedulesCache[routeId] = MutableStateFlow(loadedSchedules)
                }
                
                Timber.d("Schedule refreshed for route $routeId: ${loadedSchedules.size} items")
            } catch (e: Exception) {
                Timber.e(e, "Error refreshing schedule for route $routeId")
            }
        }
    }
    
    /**
     * Получает расписание для маршрута (suspend)
     * 
     * Используется в ScheduleScreen для синхронной загрузки расписания.
     * Эта функция правильно работает в suspend контексте и не блокирует поток.
     * 
     * @param routeId ID маршрута
     * @return список расписаний
     */
    suspend fun getSchedulesForRoute(routeId: String): List<BusSchedule> {
        return try {
            repository.getSchedulesForRoute(routeId)
        } catch (e: Exception) {
            Timber.e(e, "Error getting schedules for route $routeId")
            emptyList()
        }
    }
}

