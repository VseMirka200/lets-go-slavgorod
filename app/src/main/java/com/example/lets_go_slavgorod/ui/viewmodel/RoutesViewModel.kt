package com.example.lets_go_slavgorod.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.Stable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lets_go_slavgorod.data.model.BusRoute
import com.example.lets_go_slavgorod.data.repository.BusRouteRepository
import com.example.lets_go_slavgorod.core.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Состояние UI для маршрутов
 */
@Stable
data class RoutesUiState(
    val routes: List<BusRoute> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel для управления маршрутами и поиском
 * 
 * Специализированный ViewModel с единственной ответственностью:
 * управление списком маршрутов и поиском по ним.
 * 
 * Основные возможности:
 * - **Реактивный поиск**: Debounced поиск с задержкой 300ms
 * - **Кэширование**: LRU кэш для предотвращения утечек памяти
 * - **Умная фильтрация**: Поиск по номеру, названию, описанию
 * - **Состояние загрузки**: Отслеживание состояния операций
 * - **Обработка ошибок**: Централизованная обработка ошибок
 * - **Принудительное обновление**: Метод refreshRoutes() для актуальности данных
 * 
 * Архитектура:
 * ```
 * UI (HomeScreen) → RoutesViewModel → BusRouteRepository → Data Sources
 * ```
 * 
 * Потоки данных:
 * - **uiState**: RoutesUiState - состояние UI
 * - **searchQuery**: String - текущий поисковый запрос
 * - **filteredRoutes**: List<BusRoute> - отфильтрованные маршруты
 * 
 * Преимущества разделения:
 * - **Единственная ответственность** (SRP)
 * - **Легко тестировать** (меньше зависимостей)
 * - **Переиспользуемый** (можно использовать в других экранах)
 * - **Производительность** (оптимизированный кэш)
 * 
 * @param application Контекст приложения для создания репозитория
 * 
 * @author VseMirka200
 * @version 3.1
 * @since 2.1
 */
@OptIn(FlowPreview::class)
class RoutesViewModel(application: Application) : AndroidViewModel(application) {
    
    private val appContext = application.applicationContext
    private val routeRepository = BusRouteRepository(appContext)
    
    // UI состояние
    private val _uiState = MutableStateFlow(RoutesUiState(isLoading = true))
    val uiState: StateFlow<RoutesUiState> = _uiState.asStateFlow()
    
    // Состояние Pull-to-Refresh
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    // Поисковый запрос
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    // Кэш маршрутов (List для UI, HashMap для быстрого поиска)
    private var cachedRoutes: List<BusRoute> = emptyList()
    private var cachedRoutesMap: Map<String, BusRoute> = emptyMap()
    
    // Job для отслеживания корутин и предотвращения утечек
    private val cacheJobs = mutableMapOf<String, Job>()
    
    // Ограничения кэша для предотвращения утечек памяти
    private val maxCacheSize = 50
    private val cacheExpirationTime = 30 * 60 * 1000L // 30 минут
    private var lastCacheTime = 0L
    
    // SupervisorJob для безопасной отмены всех корутин
    private val supervisorJob = SupervisorJob()
    private val cacheScope = CoroutineScope(supervisorJob + Dispatchers.IO)
    
    // Константы для управления кэшем (используем централизованные константы)
    companion object {
        private const val MAX_CACHE_SIZE = Constants.ROUTES_MAX_CACHE_SIZE
        private const val CACHE_CLEANUP_THRESHOLD = Constants.ROUTES_CACHE_CLEANUP_THRESHOLD
    }
    
    // Поисковые результаты с debounce
    private val debouncedSearchResults = _searchQuery
        .debounce(Constants.SEARCH_DEBOUNCE_MS)
        .map { query ->
            if (query.isBlank()) {
                cachedRoutes
            } else {
                cachedRoutes.filter { route ->
                    route.name.contains(query, ignoreCase = true) ||
                    route.routeNumber.contains(query, ignoreCase = true)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(Constants.STATE_FLOW_TIMEOUT_MS),
            initialValue = emptyList()
        )
    
    init {
        cachedRoutes = emptyList()
        
        // Подписка на маршруты из Repository (реактивная загрузка)
        viewModelScope.launch {
            routeRepository.routes.collect { routes ->
                
                // Проверяем нужно ли обновить кэш
                if (shouldRefreshCache()) {
                    clearCache()
                }
                
                cachedRoutes = routes
                cachedRoutesMap = routes.associateBy { it.id }
                updateCacheTime()
                
                // Обновляем UI если нет активного поиска
                if (_searchQuery.value.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            routes = routes,
                            isLoading = false,
                            error = if (routes.isEmpty()) "Маршруты не найдены" else null
                        )
                    }
                }
            }
        }
        
        // Подписка на поисковые результаты
        viewModelScope.launch {
            debouncedSearchResults.collect { results ->
                _uiState.update { currentState ->
                    currentState.copy(
                        routes = results,
                        isLoading = false,
                        error = null
                    )
                }
            }
        }
    }
    
    
    /**
     * Обновляет поисковый запрос
     */
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }
    
    /**
     * Обновляет маршруты (Pull-to-Refresh)
     */
    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                
                // Принудительно обновляем из Repository
                val success = routeRepository.refreshRoutesFromRemote()
                
                if (success) {
                } else {
                }
                
                delay(Constants.PULL_TO_REFRESH_MIN_DELAY_MS)
            } finally {
                _isRefreshing.value = false
            }
        }
    }
    
    /**
     * Принудительно обновляет маршруты для актуальности данных
     * 
     * Используется при навигации к экрану расписания для обеспечения
     * актуальности данных о следующих рейсах.
     * 
     * Алгоритм:
     * 1. Принудительно обновляет данные из удалённого источника
     * 2. Очищает внутренний кэш для гарантии свежести данных
     * 3. Выполняется в фоновом режиме без блокировки UI
     * 
     * Применение:
     * - При переходе к ScheduleScreen для точности расписания
     * - При обновлении данных после изменений в настройках
     * - При восстановлении после ошибок сети
     * 
     * @see BusRouteRepository.refreshRoutesFromRemote()
     * @see clearCache()
     */
    fun refreshRoutes() {
        viewModelScope.launch {
            try {
                
                // Принудительно обновляем из Repository без UI индикации
                routeRepository.refreshRoutesFromRemote()
                
                // Очищаем кэш для принудительного обновления
                clearCache()
                
            } catch (e: Exception) {
                Timber.e(e, "Ошибка принудительного обновления маршрутов")
            }
        }
    }
    
    /**
     * Получает маршрут по ID с O(1) сложностью
     * 
     * Оптимизация: использует HashMap вместо линейного поиска
     * 
     * @param routeId ID маршрута
     * @return маршрут или null если не найден
     */
    fun getRouteById(routeId: String?): BusRoute? {
        if (routeId == null) return null
        
        // O(1) lookup из HashMap вместо O(n) из Repository
        return cachedRoutesMap[routeId] ?: run {
            routeRepository.getRouteById(routeId)
        }
    }
    
    /**
     * Очищает кэш маршрутов для предотвращения утечек памяти
     */
    private fun clearCache() {
        
        // Отменяем все активные корутины
        cacheJobs.values.forEach { job ->
            if (job.isActive) {
                job.cancel()
            }
        }
        cacheJobs.clear()
        
        // Очищаем кэш
        cachedRoutes = emptyList()
        cachedRoutesMap = emptyMap()
        lastCacheTime = 0L
    }
    
    /**
     * Проверяет, нужно ли обновить кэш
     */
    private fun shouldRefreshCache(): Boolean {
        val currentTime = System.currentTimeMillis()
        return cachedRoutes.isEmpty() || 
               (currentTime - lastCacheTime) > cacheExpirationTime ||
               cachedRoutes.size > maxCacheSize
    }
    
    /**
     * Обновляет время кэша
     */
    private fun updateCacheTime() {
        lastCacheTime = System.currentTimeMillis()
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
        clearCache()
    }
}