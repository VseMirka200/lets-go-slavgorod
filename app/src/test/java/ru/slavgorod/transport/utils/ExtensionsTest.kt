package ru.slavgorod.transport.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.slavgorod.transport.core.search
import ru.slavgorod.transport.data.model.BusRoute

class ExtensionsTest {

    @Test
    fun `search should return all routes when query is blank`() {
        val routes = listOf(
            BusRoute(
                "1",
                "102",
                "Slavgorod-Yarovoye",
                "Description 1",
                travelTime = "30 min",
                paymentMethods = "Cash"
            ),
            BusRoute(
                "2",
                "103",
                "Slavgorod-Barnaul",
                "Description 2",
                travelTime = "2 hours",
                paymentMethods = "Card"
            )
        )
        val query = ""

        val result = routes.search(query)

        assertEquals(routes, result)
    }

    @Test
    fun `search should filter routes by route number`() {
        val routes = listOf(
            BusRoute(
                "1",
                "102",
                "Slavgorod-Yarovoye",
                "Description 1",
                travelTime = "30 min",
                paymentMethods = "Cash"
            ),
            BusRoute(
                "2",
                "103",
                "Slavgorod-Barnaul",
                "Description 2",
                travelTime = "2 hours",
                paymentMethods = "Card"
            )
        )
        val query = "102"

        val result = routes.search(query)

        assertEquals(1, result.size)
        assertEquals("102", result[0].routeNumber)
    }

    @Test
    fun `search should filter routes by name`() {
        val routes = listOf(
            BusRoute(
                "1",
                "102",
                "Slavgorod-Yarovoye",
                "Description 1",
                travelTime = "30 min",
                paymentMethods = "Cash"
            ),
            BusRoute(
                "2",
                "103",
                "Slavgorod-Barnaul",
                "Description 2",
                travelTime = "2 hours",
                paymentMethods = "Card"
            )
        )
        val query = "Yarovoye"

        val result = routes.search(query)

        assertEquals(1, result.size)
        assertEquals("Slavgorod-Yarovoye", result[0].name)
    }

    @Test
    fun `search should filter routes by description`() {
        val routes = listOf(
            BusRoute(
                "1",
                "102",
                "Slavgorod-Yarovoye",
                "Description 1",
                travelTime = "30 min",
                paymentMethods = "Cash"
            ),
            BusRoute(
                "2",
                "103",
                "Slavgorod-Barnaul",
                "Description 2",
                travelTime = "2 hours",
                paymentMethods = "Card"
            )
        )
        val query = "Description 2"

        val result = routes.search(query)

        assertEquals(1, result.size)
        assertEquals("Slavgorod-Barnaul", result[0].name)
    }

    @Test
    fun `search should be case insensitive`() {
        val routes = listOf(
            BusRoute(
                "1",
                "102",
                "Slavgorod-Yarovoye",
                "Description 1",
                travelTime = "30 min",
                paymentMethods = "Cash"
            )
        )
        val query = "SLAVGOROD"

        val result = routes.search(query)

        assertEquals(1, result.size)
        assertEquals("Slavgorod-Yarovoye", result[0].name)
    }

    @Test
    fun `search should work with case insensitive queries`() {
        val routes = listOf(
            BusRoute(
                "1",
                "102",
                "Slavgorod-Yarovoye",
                "Description 1",
                travelTime = "30 min",
                paymentMethods = "Cash"
            ),
            BusRoute(
                "2",
                "103",
                "Slavgorod-Barnaul",
                "Description 2",
                travelTime = "2 hours",
                paymentMethods = "Card"
            )
        )
        val query = "slavgorod"

        val searchResult = routes.search(query)

        assertEquals(2, searchResult.size)
        assertTrue("Should find both routes", searchResult.any { it.routeNumber == "102" })
        assertTrue("Should find both routes", searchResult.any { it.routeNumber == "103" })
    }
}
