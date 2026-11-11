package ru.slavgorod.transport.data.repository

import org.junit.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ScheduleInputLimitsTest {
    private val parser = ScheduleJsonParser()

    private fun snapshot(name: String) =
        """{"routes":[{"id":"1","routeNumber":"1","name":"$name","description":"Маршрут","color":"#123456","schedules":[]}]}"""

    @Test
    fun `normal snapshot remains valid`() {
        assertNotNull(parser.parseSnapshot(snapshot("Маршрут")))
    }

    @Test
    fun `oversized string is rejected`() {
        assertNull(parser.parseSnapshot(snapshot("x".repeat(8193))))
    }

    @Test
    fun `excessive route count is rejected`() {
        val route = """{"id":"1","routeNumber":"1","name":"Маршрут","description":"Маршрут","color":"#123456"}"""
        assertNull(parser.parseSnapshot("{\"routes\":[${List(1001) { route }.joinToString(",")}]}"))
    }
}
