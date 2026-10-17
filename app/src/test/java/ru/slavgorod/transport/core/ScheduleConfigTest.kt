package ru.slavgorod.transport.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleConfigTest {

    @Test
    fun `remoteJsonUrl should be configured`() {
        assertFalse(ScheduleConfig.remoteJsonUrl.isBlank())
        assertTrue(ScheduleConfig.remoteJsonUrl.startsWith("https://"))
    }
}
