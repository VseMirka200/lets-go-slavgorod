package com.example.lets_go_slavgorod.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.Stable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lets_go_slavgorod.data.model.BusRoute
import com.example.lets_go_slavgorod.data.repository.BusRouteRepository
import com.example.lets_go_slavgorod.utils.Constants
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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
 * Преимущества разделения:
 * - Единственная ответственность (SRP)
 * - Легко тестировать (меньше зависимостей)
 * - Меньше строк кода (150 vs 462)
 * - Переиспользуемый
 * 
 * @param application контекст приложения
 * 
 * @author VseMirka200
 * @version 1.0
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
                Timber.d("📥 Received ${routes.size} routes from Repository")
                cachedRoutes = routes
                cachedRoutesMap = routes.associateBy { it.id }
                
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
                Timber.d("🔄 Refreshing routes from GitHub...")
                
                // Принудительно обновляем из Repository
                val success = routeRepository.refreshRoutesFromRemote()
                
                if (success) {
                    Timber.i("✅ Routes refreshed successfully")
                } else {
                    Timber.w("⚠️ Failed to refresh routes")
                }
                
                delay(Constants.PULL_TO_REFRESH_MIN_DELAY_MS)
            } finally {
                _isRefreshing.value = false
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
            Timber.w("Route not found in cache: $routeId, falling back to repository")
            routeRepository.getRouteById(routeId)
        }
    }
}