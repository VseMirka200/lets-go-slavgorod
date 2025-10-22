package com.example.lets_go_slavgorod.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lets_go_slavgorod.data.model.BusSchedule
import com.example.lets_go_slavgorod.data.repository.BusRouteRepository
import com.example.lets_go_slavgorod.core.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
    
    // Кэш расписаний по routeId - используем StateFlow для кэширования
    private val schedulesCache = mutableMapOf<String, StateFlow<List<BusSchedule>>>()
    
    // Job для отслеживания корутин и предотвращения утечек
    private val cacheJobs = mutableMapOf<String, Job>()
    
    // SupervisorJob для безопасной отмены всех корутин
    private val supervisorJob = SupervisorJob()
    private val cacheScope = CoroutineScope(supervisorJob + Dispatchers.IO)
    
    // Константы для управления кэшем (используем централизованные константы)
    companion object {
        private const val MAX_SCHEDULE_CACHE_SIZE = Constants.SCHEDULE_MAX_CACHE_SIZE
    }
    
    /**
     * Получает расписание для маршрута
     * 
     * @param routeId ID маршрута
     * @return StateFlow с расписанием
     */
    fun getScheduleFor(routeId: String): StateFlow<List<BusSchedule>> {
        // Проверяем размер кэша и очищаем при необходимости
        if (schedulesCache.size > MAX_SCHEDULE_CACHE_SIZE) {
            Timber.w("Schedule cache size exceeded: ${schedulesCache.size} > $MAX_SCHEDULE_CACHE_SIZE")
            clearScheduleCache()
        }
        
        // Проверяем, есть ли уже кэшированный StateFlow
        schedulesCache[routeId]?.let { return it }
        
        // Создаем новый StateFlow и кэшируем его
        val newStateFlow = flow {
            try {
                // Асинхронная загрузка через suspend функцию
                val loadedSchedules = repository.getSchedulesForRoute(routeId)
                emit(loadedSchedules)
                Timber.d("Loaded ${loadedSchedules.size} schedules for route $routeId")
            } catch (e: Exception) {
                Timber.e(e, "Error loading schedules for route $routeId")
                emit(emptyList())
            }
        }.flowOn(Dispatchers.IO).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        
        // Кэшируем StateFlow
        schedulesCache[routeId] = newStateFlow
        return newStateFlow
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
                
                // Удаляем старый кэш и создаем новый
                schedulesCache.remove(routeId)
                val newStateFlow = MutableStateFlow(loadedSchedules)
                schedulesCache[routeId] = newStateFlow
                
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
    
    /**
     * Очищает кэш расписаний для предотвращения утечек памяти
     */
    private fun clearScheduleCache() {
        Timber.d("🧹 Clearing schedule cache to prevent memory leaks")
        
        // Отменяем все активные корутины
        cacheJobs.values.forEach { job ->
            if (job.isActive) {
                job.cancel()
            }
        }
        cacheJobs.clear()
        
        // Очищаем кэш
        schedulesCache.clear()
    }
    
    /**
     * Очистка ресурсов при уничтожении ViewModel
     */
    override fun onCleared() {
        super.onCleared()
        Timber.d("🧹 ScheduleViewModel cleared, cleaning up resources")
        
        // Отменяем все корутины
        supervisorJob.cancel()
        cacheScope.cancel()
        
        // Очищаем кэш
        clearScheduleCache()
    }
}