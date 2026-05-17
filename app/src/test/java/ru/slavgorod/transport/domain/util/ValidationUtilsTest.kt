package ru.slavgorod.transport.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidationUtilsTest {

    @Test
    fun `isValidRouteId should return true for valid IDs`() {
        val validIds = listOf("102", "102B", "1", "3", "4")

        validIds.forEach { id ->
            assertTrue("ID '$id' should be valid", ValidationUtils.isValidRouteId(id))
        }
    }

    @Test
    fun `isValidRouteId should return false for invalid IDs`() {
        val invalidIds = listOf("", " ")

        invalidIds.forEach { id ->
            assertFalse("ID '$id' should be invalid", ValidationUtils.isValidRouteId(id))
        }
    }

    @Test
    fun `isValidRouteNumber should return true for valid numbers`() {
        val validNumbers = listOf("102", "102A", "1", "3", "4")

        validNumbers.forEach { number ->
            assertTrue(
                "Number '$number' should be valid",
                ValidationUtils.isValidRouteNumber(number)
            )
        }
    }

    @Test
    fun `isValidRouteNumber should return false for invalid numbers`() {
        val invalidNumbers = listOf("", " ")

        invalidNumbers.forEach { number ->
            assertFalse(
                "Number '$number' should be invalid",
                ValidationUtils.isValidRouteNumber(number)
            )
        }
    }

    @Test
    fun `isValidRouteName should return true for valid names`() {
        val validNames = listOf("Bus 102", "Route 1", "Loop")

        validNames.forEach { name ->
            assertTrue("Name '$name' should be valid", ValidationUtils.isValidRouteName(name))
        }
    }

    @Test
    fun `isValidRouteName should return false for invalid names`() {
        val invalidNames = listOf("", " ", "a", "ab")

        invalidNames.forEach { name ->
            assertFalse("Name '$name' should be invalid", ValidationUtils.isValidRouteName(name))
        }
    }

    @Test
    fun `isValidTime should return true for valid times`() {
        val validTimes = listOf("06:25", "14:30", "23:59", "00:00")

        validTimes.forEach { time ->
            assertTrue("Time '$time' should be valid", ValidationUtils.isValidTime(time))
        }
    }

    @Test
    fun `isValidTime should return false for invalid times`() {
        val invalidTimes = listOf("", " ", "25:00", "12:60", "abc")

        invalidTimes.forEach { time ->
            assertFalse("Time '$time' should be invalid", ValidationUtils.isValidTime(time))
        }
    }

    @Test
    fun `isValidUrl should return true for valid URLs`() {
        val validUrls = listOf(
            "https://api.github.com",
            "https://raw.githubusercontent.com/user/repo/main/file.json",
            "https://example.com"
        )

        validUrls.forEach { url ->
            assertTrue("URL '$url' should be valid", ValidationUtils.isValidUrl(url))
        }
    }

    @Test
    fun `isValidUrl should return false for invalid URLs`() {
        val invalidUrls = listOf("", " ", "ftp://example.com", "not-a-url")

        invalidUrls.forEach { url ->
            assertFalse("URL '$url' should be invalid", ValidationUtils.isValidUrl(url))
        }
    }

    @Test
    fun `sanitizeString should clean input correctly`() {
        val input = "  Test String  \n\t"
        val expected = "Test String"

        val result = ValidationUtils.sanitizeString(input)

        assertEquals(expected, result)
    }

    @Test
    fun `sanitizeString should handle null input`() {
        val input: String? = null

        val result = ValidationUtils.sanitizeString(input)

        assertEquals("", result)
    }

    @Test
    fun `isValidColor should return true for valid colors`() {
        val validColors = listOf("#FF6200EE", "#FF1976D2", "#FF4CAF50", "#4CAF50", "#abcdef")

        validColors.forEach { color ->
            assertTrue("Color '$color' should be valid", ValidationUtils.isValidColor(color))
        }
    }

    @Test
    fun `isValidColor should return false for invalid colors`() {
        val invalidColors =
            listOf("", " ", "red", "#FF", "#FFF", "#FF6200EEFF", "#GG5722FF", "not-a-color")

        invalidColors.forEach { color ->
            assertFalse("Color '$color' should be invalid", ValidationUtils.isValidColor(color))
        }
    }

    @Test
    fun `normalizeColor should add alpha channel for RGB colors`() {
        val input = "#4caf50"

        val result = ValidationUtils.normalizeColor(input)

        assertEquals("#FF4CAF50", result)
    }

    @Test
    fun `normalizeColor should return null for invalid colors`() {
        val inputs = listOf("", " ", "blue", "#123", "#12", "#ZZZZZZ", null)

        inputs.forEach {
            assertNull(
                "Color '$it' should be null after normalization",
                ValidationUtils.normalizeColor(it)
            )
        }
    }
}
