package com.example.lets_go_slavgorod.domain.usecase

import com.example.lets_go_slavgorod.data.model.BusRoute
import com.example.lets_go_slavgorod.data.repository.BusRouteRepository
import timber.log.Timber

/**
 * Use Case для поиска маршрутов по текстовому запросу
 * 
 * Инкапсулирует бизнес-логику поиска маршрутов в соответствии с принципами
 * Clean Architecture. Реализует единую точку доступа для поисковых операций.
 * 
 * Основные функции:
 * - Обработка пустых запросов (возврат всех маршрутов)
 * - Делегирование поиска в репозиторий
 * - Упрощение логики поиска для UI слоя
 * 
 * Преимущества:
 * - Разделение ответственности (SRP)
 * - Легкость тестирования (можно мокировать repository)
 * - Переиспользуемость в разных частях приложения
 * - Единообразная обработка поисковых запросов
 * 
 * @param repository репозиторий маршрутов для выполнения поиска
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.0
 */
class SearchRoutesUseCase(
    private val repository: BusRouteRepository
) {
    
    /**
     * Выполняет поиск маршрутов по текстовому запросу
     * 
     * Реализует умную логику поиска:
     * - Пустой запрос: возвращает все маршруты (для сброса фильтра)
     * - Непустой запрос: выполняет поиск через репозиторий
     * 
     * Особенности:
     * - Поиск без учета регистра
     * - Поддержка частичного совпадения
     * - Результаты автоматически сортируются по релевантности
     * 
     * @param query поисковый запрос (например, "102", "Яровое", "Вокзал")
     * @return список найденных маршрутов (все маршруты если query пустой)
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