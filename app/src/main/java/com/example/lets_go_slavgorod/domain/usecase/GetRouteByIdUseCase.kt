package com.example.lets_go_slavgorod.domain.usecase

import com.example.lets_go_slavgorod.data.model.BusRoute
import com.example.lets_go_slavgorod.data.repository.BusRouteRepository
import timber.log.Timber

/**
 * Use Case для получения маршрута по идентификатору
 * 
 * Инкапсулирует бизнес-логику получения конкретного маршрута по его ID
 * в соответствии с принципами Clean Architecture. Предоставляет единую
 * точку доступа для получения маршрута из различных частей приложения.
 * 
 * Основные функции:
 * - Получение маршрута из репозитория
 * - Валидация входных данных (выполняется в репозитории)
 * - Упрощение доступа к данным для UI слоя
 * 
 * Преимущества:
 * - Разделение ответственности (SRP)
 * - Легкость тестирования (можно мокировать repository)
 * - Переиспользуемость в разных частях приложения
 * - Единообразный доступ к маршрутам
 * 
 * @param repository репозиторий маршрутов для получения данных
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.0
 */
class GetRouteByIdUseCase(
    private val repository: BusRouteRepository
) {
    
    /**
     * Получает маршрут по его идентификатору из репозитория
     * 
     * Выполняет поиск маршрута в кэше репозитория с быстрым доступом O(1).
     * Кэш автоматически обновляется при загрузке маршрутов из любого источника.
     * 
     * Валидация:
     * - Проверка выполняется в репозитории
     * - Возвращает null для невалидных или несуществующих ID
     * 
     * Производительность:
     * - O(1) доступ к кэшу через HashMap
     * - Не выполняет запросы к базе данных или сети
     * 
     * @param routeId уникальный идентификатор маршрута (например, "102", "1")
     * @return объект BusRoute или null если маршрут не найден
     */
    suspend operator fun invoke(routeId: String): BusRoute? {
        
        return repository.getRouteById(routeId)
    }
}
