package com.example.lets_go_slavgorod.domain.usecase

import com.example.lets_go_slavgorod.data.local.dao.FavoriteTimeDao
import com.example.lets_go_slavgorod.data.model.AppError
import com.example.lets_go_slavgorod.data.model.Result
import com.example.lets_go_slavgorod.data.model.toResultError
import timber.log.Timber

/**
 * Use Case для удаления времени из избранного
 * 
 * Инкапсулирует бизнес-логику удаления времени отправления из избранного.
 * Обрабатывает валидацию и ошибки базы данных.
 * 
 * @param favoriteTimeDao DAO для работы с базой данных
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.0
 */
class RemoveFromFavoritesUseCase(
    private val favoriteTimeDao: FavoriteTimeDao
) {
    /**
     * Удаляет время отправления из избранного
     * 
     * @param favoriteId ID избранного времени
     * @return Result.Success при успехе или Result.Error при ошибке
     */
    suspend operator fun invoke(favoriteId: String): Result<Unit> {
        return try {
            // Валидация
            if (favoriteId.isBlank()) {
                return AppError.Validation.MissingField("ID избранного").toResultError()
            }
            
            // Удаляем из базы данных
            favoriteTimeDao.removeFavoriteTime(favoriteId)
            
            Timber.d("Removed from favorites: $favoriteId")
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error removing from favorites")
            AppError.Database.Generic(
                message = "Не удалось удалить из избранного",
                cause = e
            ).toResultError()
        }
    }
}

