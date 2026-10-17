package ru.slavgorod.transport.core

object ScheduleConfig {

    val remoteJsonUrl: String = System.getProperty(REMOTE_SCHEDULE_URL_PROPERTY)
        ?: System.getenv(REMOTE_SCHEDULE_URL_ENV)
        ?: DEFAULT_REMOTE_SCHEDULE_URL

    private const val REMOTE_SCHEDULE_URL_PROPERTY = "remoteScheduleUrl"
    private const val REMOTE_SCHEDULE_URL_ENV = "REMOTE_SCHEDULE_URL"
    private const val DEFAULT_REMOTE_SCHEDULE_URL =
        "https://raw.githubusercontent.com/VseMirka200/lets-go-slavgorod/main/docs/data/schedule.json"
}
