package ru.slavgorod.transport.core

object ScheduleConfig {

    val remoteJsonUrl: String = System.getProperty(REMOTE_SCHEDULE_URL_PROPERTY)
        ?: System.getenv(REMOTE_SCHEDULE_URL_ENV)
        ?: DEFAULT_REMOTE_SCHEDULE_URL

    private const val REMOTE_SCHEDULE_URL_PROPERTY = "remoteScheduleUrl"
    private const val REMOTE_SCHEDULE_URL_ENV = "REMOTE_SCHEDULE_URL"
    private const val DEFAULT_REMOTE_SCHEDULE_URL =
        "https://script.google.com/macros/s/AKfycbwKaCxx-FdDvlptqFCaWbg81ZWLvenzZ0e-sjgmgp8n2LYzzhCLokozPi9rTcbeXf2BNA/exec"
}
