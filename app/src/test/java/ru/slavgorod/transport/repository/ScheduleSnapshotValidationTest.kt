package ru.slavgorod.transport.repository

import kotlinx.coroutines.test.runTest
import org.junit.Test
import ru.slavgorod.transport.data.repository.ScheduleJsonParser
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScheduleSnapshotValidationTest {
    private val parser = ScheduleJsonParser()
    private val valid = """{"routes":[{"id":"1","routeNumber":"1","name":"Route","description":"Description","color":"#123456","schedules":[{"id":"a","departurePoint":"Stop","departureTime":"06:25"}]}]}"""

    private suspend fun isValid(json: String): Boolean = parser.isCompleteSnapshot(
        json, parser.parseRoutes(json), parser.buildSchedulesIndex(json)
    )

    @Test fun `accepts complete snapshot`() = runTest { assertTrue(isValid(valid)) }

    @Test fun `rejects invalid departure and incomplete entry`() = runTest {
        assertFalse(isValid(valid.replace("06:25", "25:25")))
        assertFalse(isValid(valid.replace("departurePoint", "missingPoint")))
    }

    @Test fun `rejects duplicate schedule ids`() = runTest {
        val duplicate = valid.replace(
            """{"id":"a","departurePoint":"Stop","departureTime":"06:25"}""",
            """{"id":"a","departurePoint":"Stop","departureTime":"06:25"},{"id":"a","departurePoint":"Stop","departureTime":"07:25"}"""
        )
        assertFalse(isValid(duplicate))
    }
}
