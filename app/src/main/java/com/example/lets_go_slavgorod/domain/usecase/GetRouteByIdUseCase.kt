package com.example.lets_go_slavgorod.domain.usecase

import com.example.lets_go_slavgorod.data.model.BusRoute
import com.example.lets_go_slavgorod.data.repository.BusRouteRepository
import timber.log.Timber

/**
 * Use Case для получения маршрута по ID
 * 
 * Инкапсулирует бизнес-логику получения маршрута по идентификатору.
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.0
 */
class GetRouteByIdUseCase(
    private val repository: BusRouteRepository
) {
    
    /**
     * Получает маршрут по ID
     * 
     * @param routeId идентификатор маршрута
     * @return маршрут или null если не найден
     */
    suspend operator fun invoke(routeId: String): BusRoute? {
        
        return repository.getRouteById(routeId)
    }
}
