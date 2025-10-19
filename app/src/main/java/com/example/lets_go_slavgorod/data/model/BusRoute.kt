package com.example.lets_go_slavgorod.data.model

import com.example.lets_go_slavgorod.utils.ValidationUtils
import com.example.lets_go_slavgorod.utils.loge

/**
 * Основная модель данных автобусного маршрута
 * 
 * Представляет собой неизменяемую (immutable) модель маршрута автобуса,
 * которая содержит всю необходимую информацию для отображения и работы с маршрутом.
 * 
 * Модель включает в себя:
 * - Идентификационные данные (ID, номер маршрута)
 * - Описательную информацию (название, описание, детали направления)
 * - Временные характеристики (время в пути)
 * - Финансовую информацию (цены, способы оплаты)
 * - UI-параметры (цвет, состояние активности)
 * 
 * Все поля проходят валидацию при создании объекта.
 * 
 * @param id Уникальный идентификатор маршрута в системе (например, "102", "1")
 * @param routeNumber Отображаемый номер маршрута для пользователя (например, "102", "1")
 * @param name Человекочитаемое название маршрута (например, "Автобус №102")
 * @param description Подробное описание маршрута с остановками
 * @param isActive Флаг активности маршрута (активные маршруты отображаются пользователю)
 * @param isFavorite Флаг избранности маршрута (для быстрого доступа)
 * @param color Цвет маршрута в формате ARGB (#AARRGGBB) для UI
 * @param pricePrimary Основная стоимость проезда
 * @param priceSecondary Дополнительная стоимость (например, для межгородских маршрутов)
 * @param directionDetails Детальная информация о направлении движения
 * @param travelTime Примерное время в пути
 * @param paymentMethods Доступные способы оплаты проезда
 * 
 * @author VseMirka200
 * @version 2.0
 * @since 1.0
 */
data class BusRoute(
    val id: String,                        // Уникальный ID маршрута
    val routeNumber: String,               // Номер маршрута (например, "1", "2А")
    val name: String,                      // Название маршрута
    val description: String,               // Описание маршрута
    val isActive: Boolean = true,          // Активен ли маршрут
    val isFavorite: Boolean = false,       // Добавлен ли в избранное
    val color: String = "#1976D2",         // Цвет маршрута в интерфейсе
    val pricePrimary: String? = null,      // Основная цена
    val priceSecondary: String? = null,    // Дополнительная цена
    val directionDetails: String? = null,  // Детали направления
    val travelTime: String?,               // Время в пути
    val paymentMethods: String?            // Способы оплаты
) {
    
    /**
     * Проверяет валидность данных маршрута
     * 
     * Выполняет комплексную валидацию всех критически важных полей маршрута.
     * Проверяет корректность ID, номера маршрута, названия и описания.
     * 
     * @return true если все данные валидны, false если найдены ошибки
     */
    fun isValid(): Boolean {
        return try {
            // Список всех валидаций с соответствующими сообщениями об ошибках
            val validations = listOf(
                ValidationUtils.isValidRouteId(id) to "Invalid route ID: '$id'",
                ValidationUtils.isValidRouteNumber(routeNumber) to "Invalid route number: '$routeNumber'",
                ValidationUtils.isValidRouteName(name) to "Invalid route name: '$name'",
                ValidationUtils.isValidStopName(description) to "Invalid description: '$description'"
            )
            
            // Фильтруем только неудачные валидации
            val failedValidations = validations.filter { !it.first }
            if (failedValidations.isNotEmpty()) {
                // Логируем все ошибки валидации
                failedValidations.forEach { (_, message) ->
                    loge(message)
                }
                return false
            }
            
            true
        } catch (e: Exception) {
            loge("Error validating BusRoute", e)
            false
        }
    }
    
    /**
     * Создает очищенную и санитизированную копию маршрута
     * 
     * Удаляет лишние пробелы, нормализует строки и подготавливает данные
     * для безопасного использования в приложении.
     * 
     * @return новая копия BusRoute с очищенными данными
     */
    fun sanitized(): BusRoute {
        return copy(
            id = ValidationUtils.sanitizeString(id),
            routeNumber = ValidationUtils.sanitizeString(routeNumber),
            name = ValidationUtils.sanitizeString(name),
            description = ValidationUtils.sanitizeString(description),
            pricePrimary = pricePrimary?.let { ValidationUtils.sanitizeString(it) },
            priceSecondary = priceSecondary?.let { ValidationUtils.sanitizeString(it) },
            directionDetails = directionDetails?.let { ValidationUtils.sanitizeString(it) },
            travelTime = travelTime?.let { ValidationUtils.sanitizeString(it) },
            paymentMethods = paymentMethods?.let { ValidationUtils.sanitizeString(it) }
        )
    }
    
}