package com.example.lets_go_slavgorod.domain.usecase

import com.example.lets_go_slavgorod.data.model.AppError
import com.example.lets_go_slavgorod.data.model.BusRoute
import com.example.lets_go_slavgorod.data.model.Result
import com.example.lets_go_slavgorod.data.model.toResultError
import com.example.lets_go_slavgorod.data.repository.BusRouteRepository
import timber.log.Timber

/**
 * Use Case для получения списка маршрутов автобусов
 * 
 * Инкапсулирует бизнес-логику загрузки и получения маршрутов в соответствии
 * с принципами Clean Architecture. Реализует единую точку доступа к данным
 * маршрутов для UI слоя.
 * 
 * Основные функции:
 * - Загрузка маршрутов из репозитория
 * - Валидация наличия данных
 * - Обработка ошибок с преобразованием в Result
 * - Логирование ошибок для отладки
 * 
 * Преимущества использования Use Case:
 * - Разделение ответственности (SRP)
 * - Легкость тестирования (можно мокировать repository)
 * - Переиспользуемость в разных частях приложения
 * - Централизованная обработка ошибок
 * 
 * @param repository репозиторий маршрутов для получения данных
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.0
 */
class GetBusRoutesUseCase(
    private val repository: BusRouteRepository
) {
    /**
     * Получает список всех доступных маршрутов из репозитория
     * 
     * Выполняет загрузку маршрутов с обработкой различных сценариев:
     * - Успешная загрузка: возвращает Result.Success с маршрутами
     * - Пустой список: возвращает Result.Error с NotFound ошибкой
     * - Исключение: возвращает Result.Error с Unknown ошибкой
     * 
     * Валидация:
     * - Проверяет, что список маршрутов не пустой
     * - Возвращает ошибку если маршруты не найдены
     * 
     * Обработка ошибок:
     * - Логирует все исключения через Timber
     * - Преобразует исключения в типобезопасный Result
     * - Не прерывает выполнение приложения
     * 
     * @return Result<List<BusRoute>> содержащий либо список маршрутов,
     *         либо ошибку (NotFound если список пуст, Unknown при исключении)
     */
    suspend operator fun invoke(): Result<List<BusRoute>> {
        return try {
            val routes = repository.getAllRoutes()
            
            if (routes.isEmpty()) {
                AppError.Database.NotFound("routes").toResultError()
            } else {
                Result.Success(routes)
            }
        } catch (e: Exception) {
            Timber.e(e, "Ошибка загрузки маршрутов")
            AppError.Unknown(
                message = "Не удалось загрузить маршруты",
                cause = e
            ).toResultError()
        }
    }
}