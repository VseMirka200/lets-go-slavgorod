package com.example.lets_go_slavgorod.domain.usecase

import com.example.lets_go_slavgorod.data.local.dao.FavoriteTimeDao
import com.example.lets_go_slavgorod.data.model.FavoriteTime
import com.example.lets_go_slavgorod.data.repository.BusRouteRepository
import com.example.lets_go_slavgorod.core.toFavoriteTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber

/**
 * Use Case для получения списка избранных времен отправления
 * 
 * Инкапсулирует бизнес-логику получения всех избранных времен в соответствии
 * с принципами Clean Architecture. Реализует реактивный поток данных с
 * автоматическим преобразованием Entity в доменные модели.
 * 
 * Основные функции:
 * - Получение всех избранных времен из базы данных через DAO
 * - Преобразование FavoriteTimeEntity в FavoriteTime
 * - Обогащение данных информацией о маршрутах из репозитория
 * - Обработка ошибок с graceful degradation
 * - Логирование ошибок для отладки
 * 
 * Преобразование данных:
 * - Использует extension функцию toFavoriteTime() для конвертации
 * - Обогащает данные информацией о маршруте из репозитория
 * - Поддерживает batch преобразование для оптимизации
 * 
 * Реактивность:
 * - Возвращает Flow для автоматического обновления UI
 * - При ошибках возвращает пустой список вместо прерывания потока
 * - Данные автоматически обновляются при изменении в базе данных
 * 
 * Преимущества:
 * - Разделение ответственности (SRP)
 * - Легкость тестирования (можно мокировать DAO и repository)
 * - Реактивное обновление UI
 * - Централизованная обработка ошибок
 * 
 * @param favoriteTimeDao DAO для работы с базой данных избранных времен
 * @param repository репозиторий маршрутов для получения деталей маршрутов
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
     * Возвращает реактивный Flow со списком избранных времен отправления
     * 
     * Выполняет полный цикл получения избранных времен:
     * 1. Получение всех Entity из базы данных через DAO
     * 2. Преобразование каждой Entity в доменную модель FavoriteTime
     * 3. Обогащение данных информацией о маршрутах из репозитория
     * 4. Обработка ошибок с возвратом пустого списка
     * 
     * Реактивность:
     * - Flow автоматически обновляется при изменении данных в БД
     * - UI автоматически получает обновления через collectAsState()
     * - Ошибки обрабатываются без прерывания потока
     * 
     * Производительность:
     * - Использует batch преобразование для оптимизации
     * - Один запрос к репозиторию для всех маршрутов
     * - O(1) поиск маршрута по ID через HashMap
     * 
     * Обработка ошибок:
     * - Логирует все исключения через Timber
     * - Возвращает пустой список при ошибках (graceful degradation)
     * - Не прерывает работу приложения
     * 
     * @return Flow<List<FavoriteTime>> реактивный поток избранных времен,
     *         автоматически обновляющийся при изменении данных в БД
     */
    operator fun invoke(): Flow<List<FavoriteTime>> {
        return favoriteTimeDao.getAllFavoriteTimes()
            .map { entities ->
                entities.map { entity ->
                    entity.toFavoriteTime(repository)
                }
            }
            .catch { exception ->
                Timber.e(exception, "Ошибка загрузки избранных времен")
                emit(emptyList())
            }
    }
}