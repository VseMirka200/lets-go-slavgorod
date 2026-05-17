package ru.slavgorod.transport.repository

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.slavgorod.transport.R
import ru.slavgorod.transport.data.repository.buildScheduleUpdateNotice

class ScheduleUpdateNoticeBuilderTest {

    @Test
    fun `buildScheduleUpdateNotice returns short update message with date`() {
        val notice = buildScheduleUpdateNotice(updatedAtMillis = 1_700_000_000_000L)

        assertEquals(R.string.schedule_update_notice_with_date, notice.textResId)
        assertEquals(1, notice.textArgs.size)
    }
}
