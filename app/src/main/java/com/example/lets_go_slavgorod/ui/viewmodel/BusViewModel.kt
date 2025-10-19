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

// Уведомления
import com.example.lets_go_slavgorod.notifications.AlarmScheduler

// Утилиты
import com.example.lets_go_slavgorod.utils.loge
import com.example.lets_go_slavgorod.utils.toFavoriteTime
import com.example.lets_go_slavgorod.utils.toFavoriteTimesBatch

// Compose импорты
import androidx.compose.runtime.Stable
import androidx.compose.runtime.Immutable

// Coroutines импорты
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * Состояние UI для избранных времен
 * 
 * @param routes список маршрутов (legacy, не используется)
 * @param isLoading флаг загрузки (legacy, не используется)
 * @param error сообщение об ошибке
 * @param isAddingFavorite флаг добавления в избранное (показывает прогресс)
 * @param isRemovingFavorite флаг удаления из избранного (показывает прогресс)
 * 
 * @author VseMirka200
 * @version 2.0
 * @since 1.0
 */
@Stable
data class BusUiState(
    val routes: List<BusRoute> = emptyList(), // Legacy: use RoutesViewModel instead
    val isLoading: Boolean = false,           // Legacy: use RoutesViewModel instead
    val error: String? = null,
    val isAddingFavorite: Boolean = false,
    val isRemovingFavorite: Boolean = false
)

/**
 * Legacy ViewModel для управления избранными временами
 * 
 * ⚠️ УСТАРЕВШИЙ КЛАСС - используйте FavoritesViewModel для нового кода
 * 
 * Этот класс сохранён для обратной совместимости с существующими UI компонентами
 * (ScheduleList, TwoColumnScheduleGrid, FilterableScheduleGrid).
 * 
 * Функционал маршрутов перенесён в:
 * - RoutesViewModel - управление маршрутами и поиском
 * - ScheduleViewModel - управление расписаниями
 * 
 * Оставшийся функционал:
 * - Управление избранными временами отправления
 * - Планирование уведомлений для избранных времен
 * - Валидация данных и обработка ошибок
 * 
 * @param application контекст приложения для доступа к базе данных
 * 
 * @author VseMirka200
 * @version 3.0 (Legacy)
 * @since 1.0
 * 
 * @deprecated Используйте FavoritesViewModel для нового кода
 * @see FavoritesViewModel
 * @see RoutesViewModel
 * @see ScheduleViewModel
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
    

    // =====================================================================================
    //                              СОСТОЯНИЕ UI (LEGACY - для обратной совместимости)
    // =====================================================================================
    
    /** Legacy: используйте RoutesViewModel.uiState */
    private val _uiState = MutableStateFlow(BusUiState(isLoading = false))
    val uiState: StateFlow<BusUiState> = _uiState.asStateFlow()
    
    /** Legacy: используйте RoutesViewModel.isRefreshing */
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    /** Legacy: используйте RoutesViewModel.searchQuery */
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // =====================================================================================
    //                              ИЗБРАННЫЕ ВРЕМЕНА (ОСНОВНОЙ ФУНКЦИОНАЛ)
    // =====================================================================================
    
    /**
     * StateFlow со списком избранных времен
     * 
     * Автоматически обновляется при изменениях в базе данных.
     * Используется во всех UI компонентах для отображения звёздочек и фильтров.
     */
    val favoriteTimes: StateFlow<List<FavoriteTime>> =
        favoriteTimeDao.getAllFavoriteTimes()
            .map { entities ->
                // Используем batch преобразование для оптимизации (избегаем N+1 запросов)
                entities.toFavoriteTimesBatch(routeRepository)
            }
            .catch { exception ->
                loge("Error collecting favorite times", exception)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(com.example.lets_go_slavgorod.utils.Constants.STATE_FLOW_TIMEOUT_MS),
                initialValue = emptyList()
            )

    /**
     * Получает маршрут по ID
     * 
     * @param routeId ID маршрута
     * @return маршрут или null если не найден
     */
    fun getRouteById(routeId: String?): BusRoute? {
        return routeRepository.getRouteById(routeId)
    }
    
    /**
     * Принудительно обновляет расписание для конкретного маршрута
     * 
     * Legacy: используйте ScheduleViewModel.refreshSchedule()
     * 
     * @param routeId ID маршрута для обновления
     */
    fun refreshScheduleForRoute(routeId: String) {
        viewModelScope.launch {
            try {
                Timber.d("Force refreshing schedule for route: $routeId")
                routeRepository.getSchedulesForRoute(routeId, forceRefresh = true)
                Timber.d("Schedule force refreshed for route: $routeId")
            } catch (e: Exception) {
                Timber.e(e, "Error refreshing schedule for route: $routeId")
            }
        }
    }
    
    /**
     * Добавляет время отправления в избранное
     * 
     * Оптимизации:
     * - Валидация данных перед сохранением
     * - Единое создание объекта FavoriteTime для БД и уведомлений
     * - Обработка ошибок с информативными сообщениями
     * - Автоматическое планирование уведомлений
     * 
     * @param schedule расписание для добавления в избранное
     */
    fun addFavoriteTime(schedule: BusSchedule) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isAddingFavorite = true, error = null) }
                
                // Валидация и санитизация данных
                val sanitizedSchedule = schedule.sanitized()
                if (!sanitizedSchedule.isValid()) {
                    Timber.tag("BusViewModel").e("Invalid schedule data")
                    _uiState.update { it.copy(isAddingFavorite = false, error = "Некорректные данные") } // TODO: strings.xml
                    return@launch
                }
                
                // Получаем информацию о маршруте для заполнения данных
                val route = getRouteById(sanitizedSchedule.routeId)
                val currentTime = System.currentTimeMillis()
                
                // Создаём entity для сохранения в БД
                val favoriteTimeEntity = FavoriteTimeEntity(
                    id = sanitizedSchedule.id,
                    routeId = sanitizedSchedule.routeId,
                    routeNumber = route?.routeNumber ?: "N/A",
                    routeName = route?.name ?: "Маршрут",
                    stopName = sanitizedSchedule.stopName,
                    departureTime = sanitizedSchedule.departureTime,
                    dayOfWeek = sanitizedSchedule.dayOfWeek,
                    departurePoint = sanitizedSchedule.departurePoint,
                    addedDate = currentTime,
                    isActive = true
                )
                
                // Сохраняем в БД
                favoriteTimeDao.addFavoriteTime(favoriteTimeEntity)
                
                // Конвертируем для планировщика уведомлений
                val favoriteTime = favoriteTimeEntity.toFavoriteTime(routeRepository)
                
                // Планируем уведомления
                AlarmScheduler.checkAndUpdateNotifications(getApplication(), favoriteTime)
                
                _uiState.update { it.copy(isAddingFavorite = false, error = null) }
            } catch (e: Exception) {
                Timber.e(e, "Error adding favorite time")
                _uiState.update { it.copy(isAddingFavorite = false, error = e.message) }
            }
        }
    }

    /**
     * Удаляет время отправления из избранного
     * 
     * Автоматически отменяет связанные уведомления.
     * 
     * @param scheduleId ID расписания для удаления
     */
    fun removeFavoriteTime(scheduleId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isRemovingFavorite = true, error = null) }
                
                // Удаляем из БД
                favoriteTimeDao.removeFavoriteTime(scheduleId)
                
                // Отменяем запланированные уведомления
                AlarmScheduler.cancelAlarm(getApplication(), scheduleId)
                
                _uiState.update { it.copy(isRemovingFavorite = false, error = null) }
            } catch (e: Exception) {
                Timber.e(e, "Error removing favorite time")
                _uiState.update { it.copy(isRemovingFavorite = false, error = e.message) }
            }
        }
    }

    /**
     * Обновляет состояние активности избранного времени
     * 
     * При деактивации (newActiveState = false):
     * - Удаляет запись из БД
     * - Отменяет уведомления
     * 
     * При активации (newActiveState = true):
     * - Обновляет запись в БД
     * - Перепланирует уведомления
     * 
     * @param favoriteTime избранное время для обновления
     * @param newActiveState новое состояние активности
     */
    fun updateFavoriteActiveState(favoriteTime: FavoriteTime, newActiveState: Boolean) {
        viewModelScope.launch {
            // Проверяем существование записи в БД
            val entityInDb = favoriteTimeDao.getFavoriteTimeById(favoriteTime.id).firstOrNull()
                ?: run {
                    Timber.w("Favorite time not found in DB: ${favoriteTime.id}")
                    return@launch
                }

            if (!newActiveState) {
                // Деактивация: удаляем из избранного
                favoriteTimeDao.removeFavoriteTime(favoriteTime.id)
                try {
                    AlarmScheduler.cancelAlarm(getApplication(), favoriteTime.id)
                } catch (e: Exception) {
                    Timber.e(e, "Error cancelling alarm for ${favoriteTime.id}")
                }
            } else {
                // Активация: обновляем запись и перепланируем уведомления
                if (!entityInDb.isActive) {
                    favoriteTimeDao.updateFavoriteTime(entityInDb.copy(isActive = true))
                    val updatedFavorite = favoriteTime.copy(isActive = true)
                    try {
                        AlarmScheduler.checkAndUpdateNotifications(getApplication(), updatedFavorite)
                    } catch (e: Exception) {
                        Timber.e(e, "Error rescheduling alarm for ${favoriteTime.id}")
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