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
 * Инкапсулирует бизнес-логику загрузки и получения маршрутов.
 * Обрабатывает кэширование, валидацию и ошибки.
 * 
 * @param repository репозиторий маршрутов
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.0
 */
class GetBusRoutesUseCase(
    private val repository: BusRouteRepository
) {
    /**
     * Получает список всех доступных маршрутов
     * 
     * @return Result<List<BusRoute>> результат с списком маршрутов или ошибкой
     */
    suspend operator fun invoke(): Result<List<BusRoute>> {
        return try {
            val routes = repository.getAllRoutes()
            
            if (routes.isEmpty()) {
                Timber.w("No routes available")
                AppError.Database.NotFound("routes").toResultError()
            } else {
                Timber.d("Loaded ${routes.size} routes")
                Result.Success(routes)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error loading routes")
            AppError.Unknown(
                message = "Не удалось загрузить маршруты",
                cause = e
            ).toResultError()
        }
    }
}

