package ru.slavgorod.transport.data.local

import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsonDataSourceTest {

    private val dataSource = JsonDataSource()

    @Test
    fun `parseRoutesFromJson accepts the strict contract`() = runTest {
        val json = """
            {
              "routes": [
                {
                  "id": "102",
                  "routeNumber": "102",
                  "name": "Slavgorod - Yarovoye",
                  "notes": "Market (Slavgorod) - MCS-128 (Yarovoye)",
                  "color": "#1976D2",
                  "travelTime": "~40 minutes",
                  "pricePrimary": "40 city",
                  "priceSecondary": "60 intercity",
                  "paymentMethods": "Cash / Card"
                }
              ]
            }
        """.trimIndent()

        val routes = dataSource.parseRoutesFromJson(json)

        assertEquals(1, routes.size)
        assertEquals("102", routes.first().id)
        assertEquals("102", routes.first().routeNumber)
        assertEquals("Slavgorod - Yarovoye", routes.first().name)
        assertEquals("Market (Slavgorod) - MCS-128 (Yarovoye)", routes.first().description)
        assertEquals("Market (Slavgorod) - MCS-128 (Yarovoye)", routes.first().notes)
    }

    @Test
    fun `parseRoutesFromJson skips routes with missing required fields`() = runTest {
        val json = """
            {
              "routes": [
                {
                  "id": "broken",
                  "name": "No number",
                  "description": "Invalid route",
                  "color": "#1976D2",
                  "travelTime": "15",
                  "pricePrimary": "",
                  "priceSecondary": "",
                  "paymentMethods": ""
                }
              ]
            }
        """.trimIndent()

        val routes = dataSource.parseRoutesFromJson(json)

        assertTrue(routes.isEmpty())
    }

    @Test
    fun `parseRoutesFromJson keeps routes when optional fields are missing`() = runTest {
        val json = """
            {
              "routes": [
                {
                  "id": "105",
                  "routeNumber": "105",
                  "name": "Slavgorod",
                  "description": "Rail depot",
                  "color": "#1976D2"
                }
              ]
            }
        """.trimIndent()

        val routes = dataSource.parseRoutesFromJson(json)

        assertEquals(1, routes.size)
        assertEquals("105", routes.first().id)
        assertEquals(routes.first().travelTime, null)
        assertEquals(routes.first().paymentMethods, null)
    }

    @Test
    fun `parseRoutesFromJson skips routes without notes`() = runTest {
        val json = """
            {
              "routes": [
                {
                  "id": "102",
                  "routeNumber": "102",
                  "name": "Slavgorod - Yarovoye",
                  "color": "#1976D2"
                }
              ]
            }
        """.trimIndent()

        val routes = dataSource.parseRoutesFromJson(json)

        assertTrue(routes.isEmpty())
    }
}
