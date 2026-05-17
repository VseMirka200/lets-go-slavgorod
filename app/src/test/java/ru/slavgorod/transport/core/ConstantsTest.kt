package ru.slavgorod.transport.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConstantsTest {

    @Test
    fun `APP_VERSION should be valid`() {
        val version = Constants.APP_VERSION

        assertTrue("Version should start with 'v'", version.startsWith("v"))
        assertTrue("Version should contain numbers", version.any { it.isDigit() })
    }

    @Test
    fun `remote json url should be configured`() {
        val remoteJsonUrl = Constants.REMOTE_JSON_URL

        assertFalse("Remote json url should not be empty", remoteJsonUrl.isBlank())
        assertTrue("Remote json url should use https", remoteJsonUrl.startsWith("https://"))
    }

    @Test
    fun `padding constants should be positive and ordered`() {
        assertTrue("Small padding should be positive", Constants.PADDING_SMALL > 0)
        assertTrue("Medium padding should be positive", Constants.PADDING_MEDIUM > 0)
        assertTrue("Large padding should be positive", Constants.PADDING_LARGE > 0)
        assertTrue(
            "Medium padding should be at least small padding",
            Constants.PADDING_MEDIUM >= Constants.PADDING_SMALL
        )
        assertTrue(
            "Large padding should be at least medium padding",
            Constants.PADDING_LARGE >= Constants.PADDING_MEDIUM
        )
    }

    @Test
    fun `timeout constants should be positive`() {
        assertTrue("Connection timeout should be positive", Constants.REMOTE_CONNECTION_TIMEOUT > 0)
        assertTrue("Read timeout should be positive", Constants.REMOTE_READ_TIMEOUT > 0)
        assertTrue(
            "Pull-to-refresh delay should be positive",
            Constants.PULL_TO_REFRESH_MIN_DELAY_MS > 0
        )
        assertTrue("Search debounce should be positive", Constants.SEARCH_DEBOUNCE_MS > 0)
        assertTrue(
            "Auto refresh interval should be positive",
            Constants.SCHEDULE_AUTO_REFRESH_INTERVAL_MS > 0
        )
        assertTrue("Route cache size should be positive", Constants.ROUTES_MAX_CACHE_SIZE > 0)
    }
}
