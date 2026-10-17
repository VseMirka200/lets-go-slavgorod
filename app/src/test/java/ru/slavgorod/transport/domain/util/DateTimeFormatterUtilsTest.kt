package ru.slavgorod.transport.domain.util

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DateTimeFormatterUtilsTest {

    @Test
    fun `formatLogTimestamp uses stable log precision`() {
        val formatted = DateTimeFormatterUtils.formatLogTimestamp(KNOWN_TIMESTAMP)

        assertTrue(Regex("""\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3}""").matches(formatted))
    }

    @Test
    fun `formatFullDateTime uses date and minute precision`() {
        val formatted = DateTimeFormatterUtils.formatFullDateTime(KNOWN_TIMESTAMP)

        assertTrue(Regex("""\d{2}\.\d{2}\.\d{4} \d{2}:\d{2}""").matches(formatted))
    }

    @Test
    fun `formatShortDateTime uses compact date and minute precision`() {
        val formatted = DateTimeFormatterUtils.formatShortDateTime(KNOWN_TIMESTAMP)

        assertTrue(Regex("""\d{2}\.\d{2} \d{2}:\d{2}""").matches(formatted))
    }

    @Test
    fun `formatters are deterministic for repeated calls`() {
        assertEquals(
            DateTimeFormatterUtils.formatFullDateTime(KNOWN_TIMESTAMP),
            DateTimeFormatterUtils.formatFullDateTime(KNOWN_TIMESTAMP)
        )
    }

    private companion object {
        private const val KNOWN_TIMESTAMP = 1_700_000_000_000L
    }
}
