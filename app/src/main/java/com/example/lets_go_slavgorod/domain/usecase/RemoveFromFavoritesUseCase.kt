package com.example.lets_go_slavgorod.domain.usecase

import com.example.lets_go_slavgorod.data.local.dao.FavoriteTimeDao
import com.example.lets_go_slavgorod.data.model.AppError
import com.example.lets_go_slavgorod.data.model.Result
import com.example.lets_go_slavgorod.data.model.toResultError
import timber.log.Timber

/**
 * Use Case для удаления времени отправления из избранного
 * 
 * Инкапсулирует бизнес-логику удаления избранного времени в соответствии
 * с принципами Clean Architecture. Обеспечивает безопасное удаление с
 * валидацией и обработкой ошибок.
 * 
 * Основные функции:
 * - Валидация ID избранного времени
 * - Удаление записи из базы данных через DAO
 * - Обработка ошибок с преобразованием в Result
 * - Логирование ошибок для отладки
 * 
 * Валидация:
 * - Проверяет, что favoriteId не пустой
 * - Возвращает ошибку Validation.MissingField при пустом ID
 * 
 * Преимущества:
 * - Разделение ответственности (SRP)
 * - Легкость тестирования (можно мокировать DAO)
 * - Централизованная валидация
 * - Типобезопасная обработка ошибок
 * 
 * @param favoriteTimeDao DAO для работы с базой данных избранных времен
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.0
 */
class RemoveFromFavoritesUseCase(
    private val favoriteTimeDao: FavoriteTimeDao
) {
    /**
     * Удаляет время отправления из избранного пользователя
     * 
     * Выполняет полный цикл удаления избранного времени:
     * 1. Валидация ID избранного времени
     * 2. Удаление записи из базы данных через DAO
     * 3. Обработка ошибок с возвратом Result
     * 
     * Обработка ошибок:
     * - Validation.MissingField: если ID пустой
     * - Database.Generic: при ошибках базы данных
     * - Все ошибки логируются через Timber
     * 
     * Примечание: Удаление выполняется физически (DELETE), а не soft delete.
     * Все связанные уведомления должны быть отменены отдельно через AlarmScheduler.
     * 
     * @param favoriteId уникальный идентификатор избранного времени
     * @return Result.Success(Unit) при успешном удалении,
     *         Result.Error с описанием ошибки при неудаче
     */
    suspend operator fun invoke(favoriteId: String): Result<Unit> {
        return try {
            // Валидация
            if (favoriteId.isBlank()) {
                return AppError.Validation.MissingField("ID избранного").toResultError()
            }
            
            // Удаляем из базы данных
            favoriteTimeDao.removeFavoriteTime(favoriteId)
            
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка удаления из избранного")
            AppError.Database.Generic(
                message = "Не удалось удалить из избранного",
                cause = e
            ).toResultError()
        }
    }
}