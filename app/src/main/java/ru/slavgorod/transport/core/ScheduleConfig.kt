package ru.slavgorod.transport.core

import ru.slavgorod.transport.BuildConfig

object ScheduleConfig {

    const val backupJsonUrl =
        "https://raw.githubusercontent.com/VseMirka200/lets-go-slavgorod/main/backup/schedule.json"

    val remoteJsonUrl: String = System.getProperty(REMOTE_SCHEDULE_URL_PROPERTY)
        ?: System.getenv(REMOTE_SCHEDULE_URL_ENV)
        ?: BuildConfig.SCHEDULE_SOURCE_URL

    private const val REMOTE_SCHEDULE_URL_PROPERTY = "remoteScheduleUrl"
    private const val REMOTE_SCHEDULE_URL_ENV = "REMOTE_SCHEDULE_URL"
}
