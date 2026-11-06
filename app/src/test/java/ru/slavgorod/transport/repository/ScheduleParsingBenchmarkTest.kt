package ru.slavgorod.transport.repository

import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import ru.slavgorod.transport.data.repository.ScheduleJsonParser
import kotlin.system.measureNanoTime
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Запуск: SCHEDULE_BENCHMARK=1 ./gradlew testDebugUnitTest --tests '*ScheduleParsingBenchmarkTest'. */
class ScheduleParsingBenchmarkTest {
    @Test
    fun compareParsingPaths() = runBlocking {
        assumeTrue(System.getenv("SCHEDULE_BENCHMARK") == "1")
        val parser = ScheduleJsonParser()
        for (size in listOf(100, 1000)) {
            val routes = (1..size).joinToString(",") { id ->
                """{"id":"$id","routeNumber":"$id","name":"Route $id","description":"Route","color":"#123456","schedules":[{"id":"$id-a","departurePoint":"Stop","departureTime":"06:25"}]}"""
            }
            val json = """{"routes":[$routes]}"""
            suspend fun oldPath() {
                val parsedRoutes = parser.parseRoutes(json)
                val index = parser.buildSchedulesIndex(json)
                assertTrue(parser.isCompleteSnapshot(json, parsedRoutes, index))
            }
            repeat(3) { oldPath(); assertNotNull(parser.parseSnapshot(json)) }
            val iterations = 10
            val oldNanos = measureNanoTime { repeat(iterations) { oldPath() } }
            val newNanos = measureNanoTime { repeat(iterations) { assertNotNull(parser.parseSnapshot(json)) } }
            println("$size routes: previous=${oldNanos / iterations / 1_000_000.0}ms singleParse=${newNanos / iterations / 1_000_000.0}ms")
        }
    }
}
