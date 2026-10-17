package ru.slavgorod.transport.ui.components.schedule

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleLayoutUtilsTest {

    @Test
    fun `two column schedule layout is disabled for narrow widths`() {
        val layout = resolveScheduleResponsiveLayout(320.dp)

        assertFalse(shouldUseTwoColumnScheduleLayout(480.dp, 6, layout))
        assertFalse(shouldUseTwoColumnScheduleLayout(520.dp, 3, layout))
    }

    @Test
    fun `two column schedule layout is enabled for wide enough lists`() {
        val layout = resolveScheduleResponsiveLayout(600.dp)

        assertTrue(shouldUseTwoColumnScheduleLayout(520.dp, 4, layout))
        assertTrue(shouldUseTwoColumnScheduleLayout(720.dp, 12, layout))
    }

    @Test
    fun `list bottom reserve is larger for short schedules`() {
        assertEquals(120.dp, resolveScheduleListBottomReserve(5))
        assertEquals(80.dp, resolveScheduleListBottomReserve(8))
        assertEquals(40.dp, resolveScheduleListBottomReserve(11))
    }
}
