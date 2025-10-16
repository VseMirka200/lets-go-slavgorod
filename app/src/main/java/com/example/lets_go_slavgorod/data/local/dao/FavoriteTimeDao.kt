package com.example.lets_go_slavgorod.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.lets_go_slavgorod.data.local.entity.FavoriteTimeEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) для работы с избранными временами в базе данных
 * 
 * Предоставляет типобезопасный интерфейс для выполнения SQL операций
 * с таблицей favorite_times. Все операции выполняются асинхронно через
 * Kotlin Coroutines и Flow.
 * 
 * Основные функции:
 * - Получение всех избранных времен (реактивное через Flow)
 * - Добавление нового избранного времени (с заменой при конфликте)
 * - Удаление избранного времени по ID
 * - Обновление существующего избранного времени
 * - Проверка существования и активности избранного времени
 * - Получение конкретного избранного времени по ID
 * 
 * Все запросы оптимизированы и используют индексы для быстрого поиска.
 * 
 * @author VseMirka200
 * @version 2.0
 * @since 1.0
 */
@Dao
interface FavoriteTimeDao {
    /**
     * Получает все избранные времена, отсортированные по дате добавления (новые сначала)
     * 
     * @return Flow со списком всех избранных времен
     */
    @Query("SELECT * FROM favorite_times ORDER BY added_date DESC")
    fun getAllFavoriteTimes(): Flow<List<FavoriteTimeEntity>>

    /**
     * Добавляет новое избранное время в базу данных
     * 
     * @param favoriteTime избранное время для добавления
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavoriteTime(favoriteTime: FavoriteTimeEntity)

    /**
     * Удаляет избранное время по его ID
     * 
     * @param scheduleId ID избранного времени для удаления
     */
    @Query("DELETE FROM favorite_times WHERE id = :scheduleId")
    suspend fun removeFavoriteTime(scheduleId: String)

    /**
     * Получает избранное время по его ID
     * 
     * @param id ID избранного времени
     * @return Flow с избранным временем или null
     */
    @Query("SELECT * FROM favorite_times WHERE id = :id")
    fun getFavoriteTimeById(id: String): Flow<FavoriteTimeEntity?>

    /**
     * Обновляет существующее избранное время
     * 
     * @param favoriteTime избранное время для обновления
     */
    @Update
    suspend fun updateFavoriteTime(favoriteTime: FavoriteTimeEntity)

    /**
     * Проверяет, является ли время избранным и активным
     * 
     * @param scheduleId ID времени для проверки
     * @return Flow с результатом проверки (true если избранное и активное)
     */
    @Query("SELECT EXISTS(SELECT 1 FROM favorite_times WHERE id = :scheduleId AND is_active = 1 LIMIT 1)")
    fun isFavorite(scheduleId: String): Flow<Boolean>

    /**
     * Удаляет все избранные времена для указанного маршрута
     * 
     * @param routeId ID маршрута
     * @return количество удалённых записей
     */
    @Query("DELETE FROM favorite_times WHERE route_id = :routeId")
    suspend fun deleteByRouteId(routeId: String): Int
}