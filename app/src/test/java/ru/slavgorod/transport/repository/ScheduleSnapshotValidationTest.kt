package ru.slavgorod.transport.repository

import org.junit.Test
import ru.slavgorod.transport.data.repository.ScheduleJsonParser
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ScheduleSnapshotValidationTest {
    private val parser = ScheduleJsonParser()
    private val valid = """{"routes":[{"id":"1","routeNumber":"1","name":"Route","description":"Description","color":"#123456","schedules":[{"id":"a","departurePoint":"Stop","departureTime":"06:25"}]}]}"""

    @Test fun `accepts complete snapshot and indexes its departures`() {
        val snapshot = assertNotNull(parser.parseSnapshot(valid))
        assertEquals(listOf("1"), snapshot.routes.map { it.id })
        assertEquals("06:25", snapshot.schedules.getValue("1").single().departureTime)
    }

    @Test fun `rejects invalid departure and incomplete entry`() {
        assertNull(parser.parseSnapshot(valid.replace("06:25", "25:25")))
        assertNull(parser.parseSnapshot(valid.replace("departurePoint", "missingPoint")))
    }

    @Test fun `rejects duplicate schedule ids`() {
        val duplicate = valid.replace(
            """{"id":"a","departurePoint":"Stop","departureTime":"06:25"}""",
            """{"id":"a","departurePoint":"Stop","departureTime":"06:25"},{"id":"a","departurePoint":"Stop","departureTime":"07:25"}"""
        )
        assertNull(parser.parseSnapshot(duplicate))
    }

    @Test fun `rejects duplicate routes and malformed snapshot`() {
        val route = valid.removePrefix("""{"routes":[""").removeSuffix("]}")
        assertNull(parser.parseSnapshot("""{"routes":[$route,$route]}"""))
        assertNull(parser.parseSnapshot("""{"routes":[]}"""))
        assertNull(parser.parseSnapshot("""{"routes":[{"id":"1"}]}"""))
        assertNull(parser.parseSnapshot("not json"))
    }
}
