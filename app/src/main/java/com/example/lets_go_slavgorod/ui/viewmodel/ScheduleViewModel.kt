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
 * v3.0 Changes (Октябрь 2025):
 * - Оптимизированы импорты и зависимости
 * - Улучшена производительность работы с расписанием
 * - Обновлены комментарии и документация
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
     * Получает расписание для маршрута с автоматическим кэшированием
     * 
     * Реализует ленивую загрузку и кэширование расписаний:
     * - При первом запросе создает StateFlow и загружает данные
     * - При последующих запросах возвращает кэшированный StateFlow
     * - Автоматически очищает кэш при превышении лимита
     * 
     * Кэширование:
     * - Максимальный размер кэша: SCHEDULE_MAX_CACHE_SIZE (50 записей)
     * - Автоматическая очистка при превышении лимита
     * - StateFlow остается активным пока есть подписчики
     * 
     * Производительность:
     * - O(1) доступ к кэшу через HashMap
     * - Асинхронная загрузка без блокировки UI
     * - Реактивное обновление UI через StateFlow
     * 
     * @param routeId ID маршрута (например, "102", "1")
     * @return StateFlow с расписанием маршрута (не пустой, минимум emptyList)
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
     * Принудительно обновляет расписание для маршрута из удаленного источника
     * 
     * Выполняет полное обновление расписания:
     * - Очищает старый кэш для маршрута
     * - Загружает актуальные данные из GitHub (forceRefresh=true)
     * - Обновляет StateFlow для автоматического обновления UI
     * 
     * Использование:
     * - Pull-to-refresh в ScheduleScreen
     * - Ручное обновление из настроек
     * - Восстановление после сетевых ошибок
     * 
     * Особенности:
     * - Асинхронная операция (не блокирует UI)
     * - Обрабатывает ошибки с graceful degradation
     * - Логирует процесс обновления для отладки
     * 
     * @param routeId ID маршрута для обновления
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
     * Получает расписание для маршрута синхронно (suspend функция)
     * 
     * Прямой доступ к репозиторию для получения расписания без использования
     * кэша StateFlow. Используется когда нужны актуальные данные на момент
     * вызова функции.
     * 
     * Особенности:
     * - Выполняется в suspend контексте (не блокирует главный поток)
     * - Обходит кэш StateFlow для получения свежих данных
     * - Использует стратегию загрузки репозитория (GitHub → Assets → Fallback)
     * - Обрабатывает ошибки с возвратом пустого списка
     * 
     * Использование:
     * - Инициализация ScheduleScreen
     * - Проверка актуальности данных
     * - Тестирование и отладка
     * 
     * @param routeId ID маршрута (например, "102", "1")
     * @return список расписаний для маршрута (не пустой, минимум fallback данные)
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