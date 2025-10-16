package com.example.lets_go_slavgorod.domain.usecase

import com.example.lets_go_slavgorod.data.local.dao.FavoriteTimeDao
import com.example.lets_go_slavgorod.data.model.FavoriteTime
import com.example.lets_go_slavgorod.data.repository.BusRouteRepository
import com.example.lets_go_slavgorod.utils.toFavoriteTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber

/**
 * Use Case для получения списка избранных времен
 * 
 * Инкапсулирует бизнес-логику получения избранных времен отправления.
 * Преобразует Entity в доменные модели и обрабатывает ошибки.
 * 
 * @param favoriteTimeDao DAO для работы с базой данных
 * @param repository репозиторий маршрутов для получения деталей
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.0
 */
class GetFavoriteTimesUseCase(
    private val favoriteTimeDao: FavoriteTimeDao,
    private val repository: BusRouteRepository
) {
    /**
     * Возвращает Flow со списком избранных времен
     * 
     * @return Flow<List<FavoriteTime>> реактивный поток избранных времен
     */
    operator fun invoke(): Flow<List<FavoriteTime>> {
        return favoriteTimeDao.getAllFavoriteTimes()
            .map { entities ->
                entities.map { entity ->
                    entity.toFavoriteTime(repository)
                }
            }
            .catch { exception ->
                Timber.e(exception, "Error loading favorite times")
                emit(emptyList())
            }
    }
}

