package ru.slavgorod.transport.data.repository

import androidx.annotation.StringRes

data class ScheduleUpdateNotice(
    @param:StringRes val textResId: Int,
    val textArgs: List<String>,
    val updatedAtMillis: Long
)
