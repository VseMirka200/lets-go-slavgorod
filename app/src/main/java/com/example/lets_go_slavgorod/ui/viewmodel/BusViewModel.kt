package com.example.lets_go_slavgorod.ui.viewmodel

// Android системные импорты
import android.app.Application
import android.annotation.SuppressLint
import timber.log.Timber
import kotlinx.coroutines.FlowPreview

// ViewModel импорты
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

// Модели данных
import com.example.lets_go_slavgorod.data.local.entity.FavoriteTimeEntity
import com.example.lets_go_slavgorod.data.model.BusRoute
import com.example.lets_go_slavgorod.data.model.BusSchedule
import com.example.lets_go_slavgorod.data.model.FavoriteTime

// Локальная база данных
import com.example.lets_go_slavgorod.data.local.AppDatabase
import com.example.lets_go_slavgorod.data.repository.BusRouteRepository

// Use Cases
import com.example.lets_go_slavgorod.domain.usecase.AddToFavoritesUseCase
import com.example.lets_go_slavgorod.domain.usecase.GetFavoriteTimesUseCase
import com.example.lets_go_slavgorod.domain.usecase.RemoveFromFavoritesUseCase

// Уведомления
import com.example.lets_go_slavgorod.notifications.AlarmScheduler

// Утилиты
import com.example.lets_go_slavgorod.utils.loge
import com.example.lets_go_slavgorod.utils.toFavoriteTime

// Coroutines импорты
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/**
 * Состояние UI для экрана с маршрутами автобусов
 * 
 * Оптимизированное состояние для эффективного управления UI:
 * - Минимальные перекомпозиции через неизменяемые данные
 * - Четкое разделение состояний загрузки и ошибок
 * - Флаги для отслеживания асинхронных операций
 * - Кэширование для улучшения производительности
 * 
 * @param routes список доступных маршрутов автобусов
 * @param isLoading флаг загрузки данных (показывает индикатор загрузки)
 * @param error сообщение об ошибке (если есть, блокирует UI)
 * @param isAddingFavorite флаг добавления в избранное (показывает прогресс)
 * @param isRemovingFavorite флаг удаления из избранного (показывает прогресс)
 * 
 * @author VseMirka200
 * @version 1.2
 * @since 1.0
 */
data class BusUiState(
    val routes: List<BusRoute> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAddingFavorite: Boolean = false,
    val isRemovingFavorite: Boolean = false
)

/**
 * ViewModel для управления данными маршрутов и избранными временами
 * 
 * Оптимизированный ViewModel для максимальной производительности:
 * - Кэширование данных для быстрого доступа
 * - Асинхронная загрузка без блокировки UI
 * - Эффективное управление состоянием через StateFlow
 * - Интеграция с Room базой данных и системой уведомлений
 * 
 * Основные функции:
 * - Загрузка и поиск маршрутов автобусов
 * - Управление избранными временами отправления
 * - Планирование уведомлений для избранных времен
 * - Валидация данных и обработка ошибок
 * 
 * Оптимизации производительности:
 * - Локальное кэширование маршрутов и избранных времен
 * - Минимизация запросов к базе данных
 * - Эффективные StateFlow с SharingStarted.WhileSubscribed
 * - Асинхронная обработка всех операций
 * 
 * @param application контекст приложения для доступа к базе данных
 * 
 * @author VseMirka200
 * @version 2.0
 * @since 1.0
 */
@OptIn(FlowPreview::class)
class BusViewModel(application: Application) : AndroidViewModel(application) {

    // =====================================================================================
    //                              РЕПОЗИТОРИИ И DAO
    // =====================================================================================
    
    // Используем applicationContext вместо Application для избежания утечек памяти
    private val appContext = application.applicationContext
    
    /** DAO для работы с избранными временами */
    private val favoriteTimeDao = AppDatabase.getDatabase(appContext).favoriteTimeDao()
    
    /** Репозиторий для работы с маршрутами */
    private val routeRepository = BusRouteRepository(appContext)
    
    // Use Cases для улучшения архитектуры
    private val getFavoriteTimesUseCase = GetFavoriteTimesUseCase(favoriteTimeDao, routeRepository)
    private val addToFavoritesUseCase = AddToFavoritesUseCase(favoriteTimeDao)
    private val removeFavoritesUseCase = RemoveFromFavoritesUseCase(favoriteTimeDao)

    // =====================================================================================
    //                              СОСТОЯНИЕ UI
    // =====================================================================================
    
    /** Текущее состояние UI с маршрутами */
    private val _uiState = MutableStateFlow(BusUiState(isLoading = true))
    val uiState: StateFlow<BusUiState> = _uiState.asStateFlow()
    
    /** Состояние Pull-to-Refresh */
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    /** Поисковый запрос пользователя */
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    /** Поисковые результаты с debounce для оптимизации */
    private val debouncedSearchResults = _searchQuery
        .debounce(300) // Задержка 300мс перед поиском
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
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // =====================================================================================
    //                              КЭШИРОВАНИЕ ДАННЫХ
    // =====================================================================================
    
    /** Кэш маршрутов для быстрого доступа */
    private var cachedRoutes: List<BusRoute> = emptyList()
    
    /** Кэш избранных времен для оптимизации */
    private var cachedFavoriteTimes: List<FavoriteTime> = emptyList()
    
    val favoriteTimes: StateFlow<List<FavoriteTime>> =
        favoriteTimeDao.getAllFavoriteTimes()
            .map { entities ->
                entities.map { entity ->
                    entity.toFavoriteTime(routeRepository)
                }
            }
            .catch { exception ->
                loge("Error collecting favorite times", exception)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    init {
        // Принудительно очищаем кэш при инициализации
        cachedRoutes = emptyList()
        loadInitialRoutes()
    }

    /**
     * Загружает начальные маршруты с оптимизацией производительности
     * 
     * Оптимизации:
     * - Использует кэшированные данные при наличии
     * - Минимизирует количество обновлений UI
     * - Обрабатывает ошибки gracefully
     * - Избегает ненужных повторных загрузок
     */
    private fun loadInitialRoutes() {
        Timber.d("Starting to load initial routes")
        
        // Оптимизация: используем кэш если доступен
        if (cachedRoutes.isNotEmpty()) {
            Timber.d("Using cached routes: ${cachedRoutes.size} routes")
            _uiState.update { currentState ->
                currentState.copy(
                    routes = cachedRoutes,
                    isLoading = false,
                    error = null
                )
            }
            return
        }
        
        // Загружаем маршруты из репозитория
        val routes = routeRepository.getAllRoutes()
        Timber.d("Loading routes: ${routes.size} routes found")
        
        // Кэшируем маршруты для последующих обращений
        cachedRoutes = routes

        if (routes.isEmpty()) {
            Timber.w("No routes found! Repository may not be initialized yet.")
            // Попробуем загрузить еще раз через небольшую задержку
            viewModelScope.launch {
                kotlinx.coroutines.delay(100)
                val retryRoutes = routeRepository.getAllRoutes()
                Timber.d("Retry loading routes: ${retryRoutes.size} routes found")
                cachedRoutes = retryRoutes
                _uiState.update { currentState ->
                    currentState.copy(
                        routes = retryRoutes,
                        isLoading = false,
                        error = if (retryRoutes.isEmpty()) "Маршруты не найдены" else null
                    )
                }
            }
        } else {
            _uiState.update { currentState ->
                currentState.copy(
                    routes = routes,
                    isLoading = false,
                    error = null
                )
            }
        }
    }

    /**
     * Обрабатывает изменение поискового запроса с debounce оптимизацией
     * 
     * Оптимизации:
     * - Debounce 300мс для избежания лишних операций
     * - Использует кэшированные данные для быстрого поиска
     * - Автоматическое обновление UI через Flow
     * 
     * @param query поисковый запрос пользователя
     */
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        
        // Результаты автоматически обновляются через debouncedSearchResults
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

    fun getRouteById(routeId: String?): BusRoute? {
        return routeRepository.getRouteById(routeId)
    }
    
    /**
     * Обновляет данные маршрутов (для Pull-to-Refresh)
     */
    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                // Перезагружаем маршруты
                loadInitialRoutes()
                kotlinx.coroutines.delay(500) // Минимальная задержка для плавности
            } finally {
                _isRefreshing.value = false
            }
        }
    }
    
    fun addFavoriteTime(schedule: BusSchedule) {
        viewModelScope.launch {
            try {
                _uiState.update { currentState ->
                    currentState.copy(isAddingFavorite = true, error = null)
                }
                
                val sanitizedSchedule = schedule.sanitized()
                if (!sanitizedSchedule.isValid()) {
                    Timber.tag("BusViewModel").e("Invalid schedule data")
                    _uiState.update { it.copy(isAddingFavorite = false, error = "Некорректные данные") }
                    return@launch
                }
                
                val route = getRouteById(sanitizedSchedule.routeId)
                val favoriteTimeEntity = FavoriteTimeEntity(
                    id = sanitizedSchedule.id,
                    routeId = sanitizedSchedule.routeId,
                    routeNumber = route?.routeNumber ?: "N/A",
                    routeName = route?.name ?: "Маршрут",
                    departureTime = sanitizedSchedule.departureTime,
                    stopName = sanitizedSchedule.stopName,
                    departurePoint = sanitizedSchedule.departurePoint,
                    dayOfWeek = sanitizedSchedule.dayOfWeek,
                    addedDate = System.currentTimeMillis(),
                    isActive = true
                )
                favoriteTimeDao.addFavoriteTime(favoriteTimeEntity)

                val favoriteForScheduler = FavoriteTime(
                    id = sanitizedSchedule.id,
                    routeId = sanitizedSchedule.routeId,
                    routeNumber = route?.routeNumber ?: "N/A",
                    routeName = route?.name ?: "Маршрут",
                    stopName = sanitizedSchedule.stopName,
                    departureTime = sanitizedSchedule.departureTime,
                    dayOfWeek = sanitizedSchedule.dayOfWeek,
                    departurePoint = sanitizedSchedule.departurePoint,
                    addedDate = System.currentTimeMillis(),
                    isActive = true
                )
                
                AlarmScheduler.checkAndUpdateNotifications(getApplication(), favoriteForScheduler)
                _uiState.update { it.copy(isAddingFavorite = false, error = null) }
            } catch (e: Exception) {
                Timber.e(e, "Error adding favorite time")
                _uiState.update { it.copy(isAddingFavorite = false, error = e.message) }
            }
        }
    }

    fun removeFavoriteTime(scheduleId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isRemovingFavorite = true, error = null) }
                favoriteTimeDao.removeFavoriteTime(scheduleId)
                AlarmScheduler.cancelAlarm(getApplication(), scheduleId)
                _uiState.update { it.copy(isRemovingFavorite = false, error = null) }
            } catch (e: Exception) {
                Timber.e(e, "Error removing favorite time")
                _uiState.update { it.copy(isRemovingFavorite = false, error = e.message) }
            }
        }
    }

    fun updateFavoriteActiveState(favoriteTime: FavoriteTime, newActiveState: Boolean) {
        viewModelScope.launch {
            val entityInDb = favoriteTimeDao.getFavoriteTimeById(favoriteTime.id).firstOrNull()
            if (entityInDb == null) return@launch

            if (!newActiveState) {
                favoriteTimeDao.removeFavoriteTime(favoriteTime.id)
                try {
                    AlarmScheduler.cancelAlarm(getApplication(), favoriteTime.id)
                } catch (e: Exception) {
                    Timber.e(e, "Error cancelling alarm")
                }
            } else {
                if (!entityInDb.isActive) {
                    favoriteTimeDao.updateFavoriteTime(entityInDb.copy(isActive = true))
                    val updatedFavorite = favoriteTime.copy(isActive = true)
                    try {
                        AlarmScheduler.checkAndUpdateNotifications(getApplication(), updatedFavorite)
                    } catch (e: Exception) {
                        Timber.e(e, "Error rescheduling alarm")
                    }
                }
            }
        }
    }
    
    /**
     * Получает расписание для маршрута из repository
     * 
     * @param routeId ID маршрута
     * @return список расписаний
     */
    suspend fun getSchedulesForRoute(routeId: String): List<BusSchedule> {
        return routeRepository.getSchedulesForRoute(routeId)
    }
}