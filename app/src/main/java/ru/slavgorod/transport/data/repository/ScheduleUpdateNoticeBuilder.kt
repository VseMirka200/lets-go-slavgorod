package ru.slavgorod.transport.data.repository

import ru.slavgorod.transport.R
import ru.slavgorod.transport.domain.util.DateTimeFormatterUtils

internal fun buildScheduleUpdateNotice(updatedAtMillis: Long): ScheduleUpdateNotice =
    ScheduleUpdateNotice(
        textResId = R.string.schedule_update_notice_with_date,
        textArgs = listOf(DateTimeFormatterUtils.formatFullDateTime(updatedAtMillis)),
        updatedAtMillis = updatedAtMillis
    )
