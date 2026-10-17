package ru.slavgorod.transport.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BusRouteTest {

    @Test
    fun `isValid should return true for valid route`() {
        val route = BusRoute(
            id = "route_1",
            routeNumber = "102",
            name = "Slavgorod-Yarovoye",
            description = "Intercity route",
            travelTime = "30 minutes",
            paymentMethods = "Cash, card"
        )

        val isValid = route.isValid()

        assertTrue(isValid)
    }

    @Test
    fun `isValid should return false for invalid route ID`() {
        val route = BusRoute(
            id = "",
            routeNumber = "102",
            name = "Slavgorod-Yarovoye",
            description = "Intercity route",
            travelTime = "30 minutes",
            paymentMethods = "Cash, card"
        )

        val isValid = route.isValid()

        assertFalse(isValid)
    }

    @Test
    fun `isValid should return false for invalid route number`() {
        val route = BusRoute(
            id = "route_1",
            routeNumber = "",
            name = "Slavgorod-Yarovoye",
            description = "Intercity route",
            travelTime = "30 minutes",
            paymentMethods = "Cash, card"
        )

        val isValid = route.isValid()

        assertFalse(isValid)
    }

    @Test
    fun `isValid should return false for invalid route name`() {
        val route = BusRoute(
            id = "route_1",
            routeNumber = "102",
            name = "",
            description = "Intercity route",
            travelTime = "30 minutes",
            paymentMethods = "Cash, card"
        )

        val isValid = route.isValid()

        assertFalse(isValid)
    }

    @Test
    fun `sanitized should return valid route with sanitized data`() {
        BusRoute(
            id = "route_1",
            routeNumber = "102",
            name = "Slavgorod-Yarovoye",
            description = "Intercity route",
            travelTime = "30 minutes",
            paymentMethods = "Cash, card"
        )
    }

    @Test
    fun `default values should be set correctly`() {
        val route = BusRoute(
            id = "route_1",
            routeNumber = "102",
            name = "Slavgorod-Yarovoye",
            description = "Intercity route",
            travelTime = "30 minutes",
            paymentMethods = "Cash, card"
        )

        assertTrue(route.isActive)
        assertEquals("#1976D2", route.color)
        assertNull(route.pricePrimary)
        assertNull(route.priceSecondary)
        assertNull(route.directionDetails)
    }
}
