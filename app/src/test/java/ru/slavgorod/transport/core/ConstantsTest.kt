package ru.slavgorod.transport.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConstantsTest {

    @Test
    fun `APP_VERSION should be valid`() {
        val version = Constants.APP_VERSION

        assertTrue("Версия должна начинаться с 'v'", version.startsWith("v"))
        assertTrue("Версия должна содержать цифры", version.any { it.isDigit() })
    }

    @Test
    fun `remote json url should be configured`() {
        val remoteJsonUrl = Constants.REMOTE_JSON_URL

        assertFalse("URL удалённого JSON не должен быть пустым", remoteJsonUrl.isBlank())
        assertTrue("URL удалённого JSON должен использовать https", remoteJsonUrl.startsWith("https://"))
    }

    @Test
    fun `padding constants should be positive and ordered`() {
        assertTrue("Малый отступ должен быть положительным", Constants.PADDING_SMALL > 0)
        assertTrue("Средний отступ должен быть положительным", Constants.PADDING_MEDIUM > 0)
        assertTrue("Большой отступ должен быть положительным", Constants.PADDING_LARGE > 0)
        assertTrue(
            "Средний отступ должен быть не меньше малого",
            Constants.PADDING_MEDIUM >= Constants.PADDING_SMALL
        )
        assertTrue(
            "Большой отступ должен быть не меньше среднего",
            Constants.PADDING_LARGE >= Constants.PADDING_MEDIUM
        )
    }

    @Test
    fun `timeout constants should be positive`() {
        assertTrue("Тайм-аут подключения должен быть положительным", Constants.REMOTE_CONNECTION_TIMEOUT > 0)
        assertTrue("Тайм-аут чтения должен быть положительным", Constants.REMOTE_READ_TIMEOUT > 0)
        assertTrue(
            "Задержка обновления жестом должна быть положительной",
            Constants.PULL_TO_REFRESH_MIN_DELAY_MS > 0
        )
        assertTrue("Задержка debounce поиска должна быть положительной", Constants.SEARCH_DEBOUNCE_MS > 0)
        assertTrue(
            "Интервал автообновления должен быть положительным",
            Constants.SCHEDULE_AUTO_REFRESH_INTERVAL_MS > 0
        )
    }
}
