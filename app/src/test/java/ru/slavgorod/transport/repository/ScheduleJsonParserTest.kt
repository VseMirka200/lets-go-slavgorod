package ru.slavgorod.transport.repository

import org.junit.Test
import ru.slavgorod.transport.data.repository.ScheduleJsonParser
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScheduleJsonParserTest {

    private val parser = ScheduleJsonParser()

    @Test
    fun `buildSchedulesIndex groups schedules by route id`() {
        val index = parser.buildSchedulesIndex(buildRoutesJson())

        assertEquals(setOf("102", "105"), index.keys)
        assertEquals(listOf("102_1", "102_2"), index.getValue("102").map { it.id })
        assertEquals(listOf("105_1"), index.getValue("105").map { it.id })
    }

    @Test
    fun `buildSchedulesIndex skips invalid schedule entries`() {
        val index = parser.buildSchedulesIndex(
            """
                {
                  "routes": [
                    {
                      "id": "102",
                      "routeNumber": "102",
                      "name": "Slavgorod - Yarovoye",
                      "description": "Market",
                      "color": "#1976D2",
                      "schedules": [
                        { "id": "missing_time", "departurePoint": "Market" },
                        { "id": "valid", "departurePoint": "Market", "departureTime": "09:00" }
                      ]
                    }
                  ]
                }
            """.trimIndent()
        )

        assertEquals(listOf("valid"), index.getValue("102").map { it.id })
    }

    @Test
    fun `buildSchedulesIndex preserves schedule metadata used for label sync`() {
        val index = parser.buildSchedulesIndex(
            """
                {
                  "routes": [
                    {
                      "id": "1",
                      "routeNumber": "1",
                      "name": "Route 1",
                      "description": "Route 1",
                      "color": "#1976D2",
                      "schedules": [
                        {
                          "id": "1_1",
                          "departurePoint": "Station",
                          "departureTime": "06:10",
                          "dayType": "weekday",
                          "variant": "1",
                          "platform": "1",
                          "notes": "First trip"
                        }
                      ]
                    }
                  ]
                }
            """.trimIndent()
        )

        val schedule = index.getValue("1").single()

        assertEquals("weekday", schedule.dayType)
        assertEquals("1", schedule.variant)
        assertEquals("1", schedule.platform)
        assertEquals("First trip", schedule.notes)
    }

    @Test
    fun `buildSchedulesIndex returns empty map for invalid json`() {
        val index = parser.buildSchedulesIndex("{broken")

        assertTrue(index.isEmpty())
    }

    @Test
    fun `buildSchedulesIndex parses service calendar for temporary and weekend trips`() {
        val index = parser.buildSchedulesIndex(
            """
                { "routes": [{ "id": "102", "schedules": [{
                  "id": "weekend", "departurePoint": "Station", "departureTime": "08:00",
                  "serviceCalendar": {
                    "daysOfWeek": ["SATURDAY", "SUNDAY"],
                    "validFrom": "2026-10-01", "validUntil": "2026-10-31",
                    "includedDates": ["2026-10-12"], "excludedDates": ["2026-10-18"]
                  }
                }]}] }
            """.trimIndent()
        )

        val calendar = index.getValue("102").single().serviceCalendar

        assertEquals(setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY), calendar.operatingDays)
        assertTrue(calendar.runsOn(LocalDate.parse("2026-10-12")))
        assertTrue(!calendar.runsOn(LocalDate.parse("2026-10-18")))
        assertTrue(!calendar.runsOn(LocalDate.parse("2026-11-01")))
    }

    @Test
    fun `buildSchedulesIndex applies production calendar to weekday and weekend trips`() {
        val index = parser.buildSchedulesIndex(
            """
                {
                  "productionCalendar": {
                    "years": {
                      "2026": { "nonWorkingDates": ["2026-11-04"] }
                    }
                  },
                  "routes": [{ "id": "102", "schedules": [
                    {
                      "id": "weekday", "departurePoint": "Station", "departureTime": "08:00",
                      "serviceCalendar": { "daysOfWeek": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"] }
                    },
                    {
                      "id": "weekend", "departurePoint": "Station", "departureTime": "09:00",
                      "serviceCalendar": { "daysOfWeek": ["SATURDAY", "SUNDAY"] }
                    }
                  ]}]
                }
            """.trimIndent()
        )

        val schedules = index.getValue("102").associateBy { it.id }
        val holiday = LocalDate.parse("2026-11-04")

        assertTrue(!schedules.getValue("weekday").serviceCalendar.runsOn(holiday))
        assertTrue(schedules.getValue("weekend").serviceCalendar.runsOn(holiday))
    }

    private fun buildRoutesJson(): String {
        return """
            {
              "routes": [
                {
                  "id": "102",
                  "routeNumber": "102",
                  "name": "Slavgorod - Yarovoye",
                  "description": "Market",
                  "color": "#1976D2",
                  "schedules": [
                    { "id": "102_1", "departurePoint": "Market", "departureTime": "06:25" },
                    { "id": "102_2", "departurePoint": "Market", "departureTime": "08:25" }
                  ]
                },
                {
                  "id": "105",
                  "routeNumber": "105",
                  "name": "Slavgorod",
                  "description": "Rail depot",
                  "color": "#1976D2",
                  "schedules": [
                    { "id": "105_1", "departurePoint": "Station", "departureTime": "07:10" }
                  ]
                }
              ]
            }
        """.trimIndent()
    }
}
