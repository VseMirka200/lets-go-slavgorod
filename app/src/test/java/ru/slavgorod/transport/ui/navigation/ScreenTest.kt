package ru.slavgorod.transport.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenTest {

    @Test
    fun `Home route should be correct`() {
        assertEquals("home", Screen.Home.route)
    }

    @Test
    fun `Settings route should be correct`() {
        assertEquals("settings", Screen.Settings.route)
    }

    @Test
    fun `Schedule route pattern should be correct`() {
        assertEquals("schedule/{routeId}", Screen.Schedule.route)
    }

    @Test
    fun `Schedule route builder should create route with id`() {
        assertEquals("schedule/102", Screen.Schedule.createRoute("102"))
    }

    @Test
    fun `Routes destinations should include home and schedule`() {
        assertTrue(Screen.isRoutesDestination(Screen.Home.route))
        assertTrue(Screen.isRoutesDestination(Screen.Schedule.route))
        assertFalse(Screen.isRoutesDestination(Screen.Settings.route))
    }

    @Test
    fun `Settings destinations should include nested settings screens`() {
        assertTrue(Screen.isSettingsDestination(Screen.Settings.route))
        assertTrue(Screen.isSettingsDestination(Screen.DisplaySettings.route))
        assertTrue(Screen.isSettingsDestination(Screen.About.route))
        assertTrue(Screen.isSettingsDestination(Screen.ResetSettings.route))
    }
}
