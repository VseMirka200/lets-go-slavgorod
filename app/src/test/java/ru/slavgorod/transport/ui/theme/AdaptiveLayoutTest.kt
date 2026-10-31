package ru.slavgorod.transport.ui.theme

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class AdaptiveLayoutTest {
    @Test
    fun `reduces columns as display width decreases or font size increases`() {
        assertEquals(3, adaptiveColumnCount(360.dp, 1f, 4, 100.dp, 12.dp))
        assertEquals(2, adaptiveColumnCount(360.dp, 1.25f, 4, 100.dp, 12.dp))
        assertEquals(2, adaptiveColumnCount(360.dp, 1.5f, 4, 100.dp, 12.dp))
        assertEquals(1, adaptiveColumnCount(240.dp, 1.5f, 4, 100.dp, 12.dp))
    }

    @Test
    fun `respects chosen single column and keeps at least one column`() {
        assertEquals(1, adaptiveColumnCount(800.dp, 1f, 1, 100.dp, 12.dp))
        assertEquals(1, adaptiveColumnCount(80.dp, 2f, 4, 100.dp, 12.dp))
        assertEquals(4, adaptiveColumnCount(800.dp, 1.5f, 4, 100.dp, 12.dp))
    }
}
