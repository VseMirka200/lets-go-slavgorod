package ru.slavgorod.transport.core

object Constants {

    const val APP_VERSION = "v3.0.1"
    const val CARD_CORNER_RADIUS = 12
    const val SETTINGS_ITEM_SPACING = 12
    const val SETTINGS_HORIZONTAL_PADDING = 12
    const val SETTINGS_SCREEN_EDGE_PADDING = 12
    const val PADDING_SMALL = 8
    const val PADDING_MEDIUM = 16
    const val PADDING_LARGE = 24
    val REMOTE_JSON_URL = ScheduleConfig.remoteJsonUrl
    const val REMOTE_CONNECTION_TIMEOUT = 10_000
    const val REMOTE_READ_TIMEOUT = 15_000
    const val PULL_TO_REFRESH_MIN_DELAY_MS = 500L
    const val SEARCH_DEBOUNCE_MS = 300L
    const val SCHEDULE_AUTO_REFRESH_INTERVAL_MS = 30 * 60 * 1000L
    const val STATE_FLOW_TIMEOUT_MS = 5000L
    const val DATA_OPERATION_COMPLETION_DELAY_MS = 500L
    const val ROUTES_MAX_CACHE_SIZE = 100
}
