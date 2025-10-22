package com.example.lets_go_slavgorod.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.Stable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lets_go_slavgorod.data.local.AppDatabase
import com.example.lets_go_slavgorod.data.local.dao.FavoriteTimeDao
import com.example.lets_go_slavgorod.data.local.entity.FavoriteTimeEntity
import com.example.lets_go_slavgorod.data.model.BusSchedule
import com.example.lets_go_slavgorod.data.model.FavoriteTime
import com.example.lets_go_slavgorod.data.repository.BusRouteRepository
import com.example.lets_go_slavgorod.domain.notification.AlarmScheduler
import com.example.lets_go_slavgorod.core.Constants
import com.example.lets_go_slavgorod.core.toFavoriteTime
import com.example.lets_go_slavgorod.core.toFavoriteTimesBatch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Состояние UI для избранного
 */
@Stable
data class FavoritesUiState(
    val isAddingFavorite: Boolean = false,
    val isRemovingFavorite: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel для управления избранными временами
 * 
 * Специализированный ViewModel с единственной ответственностью:
 * управление списком избранных времен отправления.
 * 
 * Функции:
 * - Добавление времени в избранное
 * - Удаление избранного времени
 * - Обновление активности
 * - Планирование/отмена уведомлений
 * 
 * Преимущества:
 * - Четкая ответственность (SRP)
 * - Легко тестировать
 * - Переиспользуемый
 * - Низкая связанность
 * 
 * @param application контекст приложения
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.1
 */
class FavoritesViewModel(application: Application) : AndroidViewModel(application) {
    
    private val appContext = application.applicationContext
    private val favoriteTimeDao: FavoriteTimeDao = AppDatabase.getDatabase(appContext).favoriteTimeDao()
    private val routeRepository = BusRouteRepository(appContext)
    
    // UI состояние
    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()
    
    // Список избранных времен
    val favoriteTimes: StateFlow<List<FavoriteTime>> =
        favoriteTimeDao.getAllFavoriteTimes()
            .map { entities ->
                entities.toFavoriteTimesBatch(routeRepository)
            }
            .catch { exception ->
                Timber.e(exception, "Error collecting favorite times")
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(Constants.STATE_FLOW_TIMEOUT_MS),
                initialValue = emptyList()
            )
    
    /**
     * Добавляет время отправления в избранное
     */
    fun addFavoriteTime(schedule: BusSchedule) {
        viewModelScope.launch {
            try {
                Timber.d("═══════════════════════════════════════════════════")
                Timber.d("🌟 ADDING FAVORITE TIME")
                Timber.d("Input schedule:")
                Timber.d("  id: ${schedule.id}")
                Timber.d("  routeId: ${schedule.routeId}")
                Timber.d("  time: ${schedule.departureTime}")
                Timber.d("  day: ${schedule.dayOfWeek}")
                
                _uiState.update { it.copy(isAddingFavorite = true, error = null) }
                
                // Валидация
                val sanitizedSchedule = schedule.sanitized()
                if (!sanitizedSchedule.isValid()) {
                    Timber.e("❌ Invalid schedule data after sanitization")
                    _uiState.update {
                        it.copy(isAddingFavorite = false, error = "Некорректные данные")
                    }
                    return@launch
                }
                
                // Получаем информацию о маршруте
                val route = routeRepository.getRouteById(sanitizedSchedule.routeId)
                val currentTime = System.currentTimeMillis()
                
                Timber.d("Route info loaded: ${route?.name ?: "null"}")
                
                // Создаем entity
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
                
                Timber.d("Created FavoriteTimeEntity:")
                Timber.d("  id: ${favoriteTimeEntity.id}")
                Timber.d("  routeId: ${favoriteTimeEntity.routeId}")
                Timber.d("  isActive: ${favoriteTimeEntity.isActive}")
                Timber.d("  time: ${favoriteTimeEntity.departureTime}")
                
                // Сохраняем в БД
                favoriteTimeDao.addFavoriteTime(favoriteTimeEntity)
                Timber.d("✓ Saved to database")
                
                // Проверяем что сохранилось
                val savedEntity = favoriteTimeDao.getFavoriteTimeById(favoriteTimeEntity.id).firstOrNull()
                if (savedEntity != null) {
                    Timber.d("✅ VERIFICATION: Favorite saved successfully!")
                    Timber.d("   Saved entity routeId: ${savedEntity.routeId}")
                    Timber.d("   Saved entity isActive: ${savedEntity.isActive}")
                } else {
                    Timber.e("❌ VERIFICATION FAILED: Favorite not found in DB after save!")
                }
                
                // Планируем уведомление
                val favoriteTime = favoriteTimeEntity.toFavoriteTime(routeRepository)
                AlarmScheduler.checkAndUpdateNotifications(getApplication(), favoriteTime)
                
                Timber.d("═══════════════════════════════════════════════════")
                
                _uiState.update { it.copy(isAddingFavorite = false, error = null) }
            } catch (e: Exception) {
                Timber.e(e, "Error adding favorite time")
                _uiState.update {
                    it.copy(isAddingFavorite = false, error = e.message)
                }
            }
        }
    }
    
    /**
     * Удаляет время из избранного
     */
    fun removeFavoriteTime(scheduleId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isRemovingFavorite = true, error = null) }
                
                favoriteTimeDao.removeFavoriteTime(scheduleId)
                AlarmScheduler.cancelAlarm(getApplication(), scheduleId)
                
                _uiState.update { it.copy(isRemovingFavorite = false, error = null) }
            } catch (e: Exception) {
                Timber.e(e, "Error removing favorite time")
                _uiState.update {
                    it.copy(isRemovingFavorite = false, error = e.message)
                }
            }
        }
    }
    
    /**
     * Обновляет активность избранного времени
     */
    fun updateFavoriteActiveState(favoriteTime: FavoriteTime, newActiveState: Boolean) {
        viewModelScope.launch {
            val entityInDb = favoriteTimeDao.getFavoriteTimeById(favoriteTime.id).firstOrNull()
                ?: run {
                    Timber.w("Favorite time not found: ${favoriteTime.id}")
                    return@launch
                }
            
            if (!newActiveState) {
                // Деактивация - удаляем
                favoriteTimeDao.removeFavoriteTime(favoriteTime.id)
                try {
                    AlarmScheduler.cancelAlarm(getApplication(), favoriteTime.id)
                } catch (e: Exception) {
                    Timber.e(e, "Error cancelling alarm")
                }
            } else {
                // Активация - обновляем
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
}