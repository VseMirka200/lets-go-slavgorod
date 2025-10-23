package com.example.lets_go_slavgorod.domain.usecase

import com.example.lets_go_slavgorod.data.model.BusRoute
import com.example.lets_go_slavgorod.data.repository.BusRouteRepository
import timber.log.Timber

/**
 * Use Case для поиска маршрутов
 * 
 * Инкапсулирует бизнес-логику поиска маршрутов по запросу.
 * Отделяет бизнес-логику от UI слоя.
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.0
 */
class SearchRoutesUseCase(
    private val repository: BusRouteRepository
) {
    
    /**
     * Выполняет поиск маршрутов по запросу
     * 
     * @param query поисковый запрос
     * @return список найденных маршрутов
     */
    suspend operator fun invoke(query: String): List<BusRoute> {
        
        return if (query.isBlank()) {
            // Если запрос пустой, возвращаем все маршруты
            repository.getAllRoutes()
        } else {
            // Выполняем поиск
            repository.searchRoutes(query)
        }
    }
}