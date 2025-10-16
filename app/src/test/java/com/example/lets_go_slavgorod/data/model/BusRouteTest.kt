package com.example.lets_go_slavgorod.data.model

import org.junit.Test
import org.junit.Assert.*

/**
 * Тесты для модели BusRoute
 */
class BusRouteTest {

    @Test
    fun `isValid should return true for valid route`() {
        // Given
        val route = BusRoute(
            id = "route_1",
            routeNumber = "102",
            name = "Славгород-Яровое",
            description = "Маршрут между городами",
            travelTime = "30 минут",
            paymentMethods = "Наличные, карта"
        )

        // When
        val isValid = route.isValid()

        // Then
        assertTrue(isValid)
    }

    @Test
    fun `isValid should return false for invalid route ID`() {
        // Given
        val route = BusRoute(
            id = "", // Invalid empty ID
            routeNumber = "102",
            name = "Славгород-Яровое",
            description = "Маршрут между городами",
            travelTime = "30 минут",
            paymentMethods = "Наличные, карта"
        )

        // When
        val isValid = route.isValid()

        // Then
        assertFalse(isValid)
    }

    @Test
    fun `isValid should return false for invalid route number`() {
        // Given
        val route = BusRoute(
            id = "route_1",
            routeNumber = "", // Invalid empty route number
            name = "Славгород-Яровое",
            description = "Маршрут между городами",
            travelTime = "30 минут",
            paymentMethods = "Наличные, карта"
        )

        // When
        val isValid = route.isValid()

        // Then
        assertFalse(isValid)
    }

    @Test
    fun `isValid should return false for invalid route name`() {
        // Given
        val route = BusRoute(
            id = "route_1",
            routeNumber = "102",
            name = "", // Invalid empty name
            description = "Маршрут между городами",
            travelTime = "30 минут",
            paymentMethods = "Наличные, карта"
        )

        // When
        val isValid = route.isValid()

        // Then
        assertFalse(isValid)
    }

    @Test
    fun `sanitized should return valid route with sanitized data`() {
        // Given
        val route = BusRoute(
            id = "route_1",
            routeNumber = "102",
            name = "Славгород-Яровое",
            description = "Маршрут между городами",
            travelTime = "30 минут",
            paymentMethods = "Наличные, карта"
        )

        // When
        val sanitized = route.sanitized()

        // Then
        assertNotNull(sanitized)
        assertTrue(sanitized.isValid())
    }

    @Test
    fun `default values should be set correctly`() {
        // Given
        val route = BusRoute(
            id = "route_1",
            routeNumber = "102",
            name = "Славгород-Яровое",
            description = "Маршрут между городами",
            travelTime = "30 минут",
            paymentMethods = "Наличные, карта"
        )

        // Then
        assertTrue(route.isActive)
        assertFalse(route.isFavorite)
        assertEquals("#1976D2", route.color)
        assertNull(route.pricePrimary)
        assertNull(route.priceSecondary)
        assertNull(route.directionDetails)
    }
}
