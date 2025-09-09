package com.example.lets_go_slavgorod.domain.usecase

import com.example.lets_go_slavgorod.data.local.dao.FavoriteTimeDao
import com.example.lets_go_slavgorod.data.local.entity.FavoriteTimeEntity
import com.example.lets_go_slavgorod.data.model.AppError
import com.example.lets_go_slavgorod.data.model.FavoriteTime
import com.example.lets_go_slavgorod.data.model.Result
import com.example.lets_go_slavgorod.data.model.toResultError
import timber.log.Timber

/**
 * Use Case для добавления времени отправления в избранное
 * 
 * Инкапсулирует бизнес-логику добавления времени отправления автобуса
 * в избранное пользователя в соответствии с принципами Clean Architecture.
 * 
 * Основные функции:
 * - Валидация всех обязательных полей
 * - Преобразование доменной модели в Entity
 * - Сохранение в базу данных через DAO
 * - Обработка ошибок с преобразованием в Result
 * - Логирование ошибок для отладки
 * 
 * Валидация:
 * - Проверяет наличие ID избранного времени
 * - Проверяет наличие ID маршрута
 * - Проверяет наличие времени отправления
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
class AddToFavoritesUseCase(
    private val favoriteTimeDao: FavoriteTimeDao
) {
    /**
     * Добавляет время отправления в избранное пользователя
     * 
     * Выполняет полный цикл добавления избранного времени:
     * 1. Валидация всех обязательных полей
     * 2. Преобразование FavoriteTime в FavoriteTimeEntity
     * 3. Установка текущего времени как даты добавления
     * 4. Сохранение в базу данных через DAO
     * 5. Обработка ошибок с возвратом Result
     * 
     * Обработка ошибок:
     * - Validation.MissingField: если отсутствуют обязательные поля
     * - Database.Generic: при ошибках базы данных
     * - Все ошибки логируются через Timber
     * 
     * @param favoriteTime доменная модель избранного времени с данными маршрута
     * @return Result.Success(Unit) при успешном добавлении,
     *         Result.Error с описанием ошибки при неудаче
     * 
     * @sample
     * ```kotlin
     * val favoriteTime = FavoriteTime(
     *     id = "route_1_08:30",
     *     routeId = "1",
     *     routeNumber = "1",
     *     routeName = "Автобус №1",
     *     stopName = "Рынок",
     *     departureTime = "08:30",
     *     dayOfWeek = 2,
     *     departurePoint = "Славгород",
     *     isActive = true
     * )
     * val result = addToFavoritesUseCase(favoriteTime)
     * when (result) {
     *     is Result.Success -> println("Успешно добавлено в избранное")
     *     is Result.Error -> println("Ошибка: ${result.error}")
     * }
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
            
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка добавления в избранное")
            AppError.Database.Generic(
                message = "Не удалось добавить в избранное",
                cause = e
            ).toResultError()
        }
    }
}