package ru.slavgorod.transport.core

object ScheduleConfig {

    val remoteJsonUrl: String = System.getProperty(REMOTE_SCHEDULE_URL_PROPERTY)
        ?: System.getenv(REMOTE_SCHEDULE_URL_ENV)
        ?: DEFAULT_REMOTE_SCHEDULE_URL

    // The stops catalog lives beside schedule.json; custom timetable hosts can override it.
    val remoteStopsUrl: String = System.getProperty("remoteStopsUrl")
        ?: System.getenv("REMOTE_STOPS_URL")
        ?: remoteJsonUrl.substringBeforeLast("/") + "/route-stops.json"

    private const val REMOTE_SCHEDULE_URL_PROPERTY = "remoteScheduleUrl"
    private const val REMOTE_SCHEDULE_URL_ENV = "REMOTE_SCHEDULE_URL"
    private const val DEFAULT_REMOTE_SCHEDULE_URL =
        "https://raw.githubusercontent.com/VseMirka200/lets-go-slavgorod/main/schedule-data/schedule.json"
}
