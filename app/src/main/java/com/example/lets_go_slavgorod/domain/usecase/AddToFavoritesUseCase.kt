package com.example.lets_go_slavgorod.domain.usecase

import com.example.lets_go_slavgorod.data.local.dao.FavoriteTimeDao
import com.example.lets_go_slavgorod.data.local.entity.FavoriteTimeEntity
import com.example.lets_go_slavgorod.data.model.AppError
import com.example.lets_go_slavgorod.data.model.FavoriteTime
import com.example.lets_go_slavgorod.data.model.Result
import com.example.lets_go_slavgorod.data.model.toResultError
import timber.log.Timber

/**
 * Use Case для добавления времени в избранное
 * 
 * Инкапсулирует бизнес-логику добавления времени отправления в избранное.
 * Обрабатывает валидацию, преобразование данных и ошибки.
 * 
 * @param favoriteTimeDao DAO для работы с базой данных
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.0
 */
class AddToFavoritesUseCase(
    private val favoriteTimeDao: FavoriteTimeDao
) {
    /**
     * Добавляет время отправления в избранное
     * 
     * @param favoriteTime данные избранного времени
     * @return Result.Success при успехе или Result.Error при ошибке
     * 
     * @sample
     * ```kotlin
     * val result = addToFavoritesUseCase(
     *     FavoriteTime(
     *         routeId = "1",
     *         departureTime = "08:30",
     *         departurePoint = "Славгород"
     *     )
     * )
     * ```
     */
    suspend operator fun invoke(favoriteTime: FavoriteTime): Result<Unit> {
        return try {
            // Валидация данных
            if (favoriteTime.id.isBlank()) {
                return AppError.Validation.MissingField("ID избранного времени").toResultError()
            }
            
            if (favoriteTime.routeId.isBlank()) {
                return AppError.Validation.MissingField("ID маршрута").toResultError()
            }
            
            if (favoriteTime.departureTime.isBlank()) {
                return AppError.Validation.MissingField("Время отправления").toResultError()
            }
            
            // Создаем entity для сохранения
            val entity = FavoriteTimeEntity(
                id = favoriteTime.id,
                routeId = favoriteTime.routeId,
                routeNumber = favoriteTime.routeNumber,
                routeName = favoriteTime.routeName,
                stopName = favoriteTime.stopName,
                departureTime = favoriteTime.departureTime,
                dayOfWeek = favoriteTime.dayOfWeek,
                departurePoint = favoriteTime.departurePoint,
                addedDate = System.currentTimeMillis(),
                isActive = true
            )
            
            // Сохраняем в базу данных
            favoriteTimeDao.addFavoriteTime(entity)
            
            Timber.d("Added to favorites: ${favoriteTime.routeId} at ${favoriteTime.departureTime}")
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error adding to favorites")
            AppError.Database.Generic(
                message = "Не удалось добавить в избранное",
                cause = e
            ).toResultError()
        }
    }
}