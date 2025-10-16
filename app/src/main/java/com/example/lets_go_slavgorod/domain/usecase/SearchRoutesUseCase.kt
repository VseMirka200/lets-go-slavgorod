package com.example.lets_go_slavgorod.domain.usecase

import com.example.lets_go_slavgorod.data.model.BusRoute
import com.example.lets_go_slavgorod.data.repository.BusRouteRepository
import com.example.lets_go_slavgorod.utils.search
import timber.log.Timber

/**
 * Use Case для поиска маршрутов по запросу
 * 
 * Инкапсулирует бизнес-логику поиска маршрутов.
 * Использует нечеткий поиск по номеру и названию маршрута.
 * 
 * @param repository репозиторий маршрутов
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
     * @return List<BusRoute> список найденных маршрутов
     * 
     * @sample
     * ```kotlin
     * val routes = searchRoutesUseCase("1")
     * // Вернет все маршруты содержащие "1" в номере или названии
     * ```
     */
    suspend operator fun invoke(query: String): List<BusRoute> {
        return try {
            val allRoutes = repository.getAllRoutes()
            
            if (query.isBlank()) {
                Timber.d("Empty query, returning all routes")
                return allRoutes
            }
            
            val results = allRoutes.search(query)
            Timber.d("Search '$query' found ${results.size} routes")
            
            results
        } catch (e: Exception) {
            Timber.e(e, "Error searching routes")
            emptyList()
        }
    }
}

