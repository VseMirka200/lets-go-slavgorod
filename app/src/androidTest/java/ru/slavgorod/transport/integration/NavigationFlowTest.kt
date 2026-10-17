package ru.slavgorod.transport.integration

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.slavgorod.transport.R
import ru.slavgorod.transport.app.bootstrap.MainActivity

@RunWith(AndroidJUnit4::class)
class NavigationFlowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun app_startsOnHomeScreen() {
        composeTestRule
            .onNodeWithText(context.getString(R.string.app_name))
            .assertIsDisplayed()
    }

    @Test
    fun homeScreen_canNavigateToSettings() {
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.action_open_settings))
            .assertIsDisplayed()
            .performClick()

        composeTestRule
            .onNodeWithText(context.getString(R.string.settings_title))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(context.getString(R.string.settings_theme_title))
            .assertIsDisplayed()
    }

    @Test
    fun settingsScreen_canNavigateBack() {
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.action_open_settings))
            .performClick()

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.accessibility_back_button))
            .performClick()

        composeTestRule
            .onNodeWithText(context.getString(R.string.app_name))
            .assertIsDisplayed()
    }

    @Test
    fun homeScreen_displaysSearchBar() {
        composeTestRule
            .onNodeWithText(context.getString(R.string.search_placeholder))
            .assertIsDisplayed()
    }

    @Test
    fun homeScreen_searchFunctionality_works() {
        composeTestRule
            .onNodeWithText(context.getString(R.string.search_placeholder))
            .performTextInput("102")

        composeTestRule
            .onNodeWithText("102")
            .assertIsDisplayed()
    }

    @Test
    fun homeScreen_canOpenRouteSchedule() {
        composeTestRule.waitForIdle()

        composeTestRule
            .onAllNodesWithText("102")
            .onFirst()
            .performClick()

        composeTestRule
            .onNodeWithText(context.getString(R.string.schedule_title_bus_format, "102"))
            .assertIsDisplayed()
    }
}
