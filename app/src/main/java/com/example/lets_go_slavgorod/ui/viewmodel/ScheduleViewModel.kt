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
 * Основные возможности:
 * - **Загрузка расписаний**: Получение расписаний для конкретных маршрутов
 * - **Принудительное обновление**: Метод refreshSchedulesForRoute() для актуальности
 * - **Умное кэширование**: LRU кэш с автоматической очисткой
 * - **Управление памятью**: Предотвращение утечек памяти
 * - **Асинхронная загрузка**: Неблокирующие операции
 * 
 * Архитектура:
 * ```
 * UI (ScheduleScreen) → ScheduleViewModel → BusRouteRepository → Data Sources
 * ```
 * 
 * Потоки данных:
 * - **schedulesCache**: Map<String, List<BusSchedule>> - кэш расписаний
 * - **cacheJobs**: Map<String, Job> - активные задачи загрузки
 * 
 * Преимущества:
 * - **Единственная ответственность** (SRP)
 * - **Независимый** от других ViewModels
 * - **Легко тестировать** (изолированная логика)
 * - **Переиспользуемый** (можно использовать в других экранах)
 * - **Производительность** (оптимизированный кэш)
 * 
 * @param application контекст приложения
 * 
 * @author VseMirka200
 * @version 2.0
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
            } catch (e: Exception) {
                Timber.e(e, "Ошибка загрузки расписаний для маршрута $routeId")
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
                // Асинхронная загрузка с принудительным обновлением
                val loadedSchedules = repository.getSchedulesForRoute(routeId, forceRefresh = true)
                
                // Удаляем старый кэш и создаем новый
                schedulesCache.remove(routeId)
                val newStateFlow = MutableStateFlow(loadedSchedules)
                schedulesCache[routeId] = newStateFlow
                
            } catch (e: Exception) {
                Timber.e(e, "Ошибка обновления расписания для маршрута $routeId")
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
            Timber.e(e, "Ошибка получения расписаний для маршрута $routeId")
            emptyList()
        }
    }
    
    /**
     * Принудительно обновляет расписание для маршрута
     * 
     * Используется для обеспечения актуальности данных при навигации
     * к экрану расписания.
     * 
     * Алгоритм обновления:
     * 1. Очищает кэш для указанного маршрута
     * 2. Отменяет активные задачи загрузки
     * 3. Принудительно загружает свежие данные из репозитория
     * 4. Возвращает обновлённые расписания
     * 
     * Применение:
     * - При переходе к ScheduleScreen для актуальности
     * - При обновлении данных после изменений
     * - При восстановлении после ошибок сети
     * 
     * @param routeId ID маршрута для обновления
     * @return список обновлённых расписаний
     * @see BusRouteRepository.getSchedulesForRoute()
     * @see clearScheduleCache()
     */
    suspend fun refreshSchedulesForRoute(routeId: String): List<BusSchedule> {
        return try {
            
            // Очищаем кэш для данного маршрута
            schedulesCache.remove(routeId)
            cacheJobs[routeId]?.cancel()
            cacheJobs.remove(routeId)
            
            // Принудительно загружаем свежие данные
            val schedules = repository.getSchedulesForRoute(routeId)
            
            schedules
        } catch (e: Exception) {
            Timber.e(e, "Ошибка принудительного обновления расписаний для маршрута $routeId")
            emptyList()
        }
    }
    
    /**
     * Очищает кэш расписаний для предотвращения утечек памяти
     */
    private fun clearScheduleCache() {
        
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
        
        // Отменяем все корутины
        supervisorJob.cancel()
        cacheScope.cancel()
        
        // Очищаем кэш
        clearScheduleCache()
    }
}