package ru.slavgorod.transport.integration

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso
import kotlinx.coroutines.runBlocking
import org.junit.rules.ExternalResource
import org.koin.core.context.GlobalContext
import org.koin.dsl.module
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.slavgorod.transport.R
import ru.slavgorod.transport.app.bootstrap.MainActivity
import ru.slavgorod.transport.data.local.CachedScheduleSnapshot
import ru.slavgorod.transport.data.local.DisclaimerManager
import ru.slavgorod.transport.data.repository.RoutesTableDataSource
import ru.slavgorod.transport.data.repository.ScheduleFetcher
import ru.slavgorod.transport.data.repository.ScheduleSnapshotStore

@RunWith(AndroidJUnit4::class)
class NavigationFlowTest {

    @get:Rule(order = 0)
    val testData = object : ExternalResource() {
        private lateinit var previousRepository: RoutesTableDataSource

        override fun before() {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val koin = GlobalContext.get()
            previousRepository = koin.get()
            val json = """{"routes":[{"id":"102","routeNumber":"102","name":"Славгород — Яровое","description":"Рынок — вокзал","color":"#1976D2","schedules":[{"id":"102_1","departurePoint":"Рынок","departureTime":"06:25"}]}]}"""
            val fetcher = object : ScheduleFetcher {
                override fun isOnline() = true
                override suspend fun fetchWithRetry() = json
            }
            val store = object : ScheduleSnapshotStore {
                override suspend fun readSnapshot(): CachedScheduleSnapshot? = null
                override suspend fun writeSnapshot(json: String) = Unit
                override suspend fun clear() = Unit
                override suspend fun hasSnapshot() = false
                override fun formatSavedAt(timestampMillis: Long) = ""
            }
            val repository = RoutesTableDataSource(
                context = context,
                remoteScheduleFetcher = fetcher,
                scheduleSnapshotRepository = store,
                autoLoadOnInit = false
            )
            runBlocking {
                DisclaimerManager.markDisclaimerAccepted(context)
                repository.refreshRoutesFromLocal(notifyUser = false)
            }
            koin.loadModules(listOf(module { single { repository } }))
        }

        override fun after() {
            val original = previousRepository
            GlobalContext.get().loadModules(listOf(module { single { original } }))
        }
    }

    @get:Rule(order = 1)
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
        openSettings()

        composeTestRule
            .onNodeWithText(context.getString(R.string.settings_title))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(context.getString(R.string.settings_appearance_title))
            .assertIsDisplayed()
    }

    @Test
    fun settingsScreen_canOpenInterfaceSettings() {
        openSettings()

        composeTestRule.onNodeWithText(context.getString(R.string.settings_appearance_title)).performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.settings_theme_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.settings_columns_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.settings_theme_title)).performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.theme_light)).assertIsDisplayed()
        Espresso.pressBack()
        composeTestRule.onNodeWithText(context.getString(R.string.settings_columns_title)).performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.grid_columns_1)).assertIsDisplayed()
        Espresso.pressBack()
        composeTestRule.onNodeWithText(context.getString(R.string.settings_schedule_day_mode_title))
            .performScrollTo().performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.settings_schedule_day_mode_day_first)).assertIsDisplayed()
    }

    @Test
    fun settingsScreen_canNavigateBack() {
        openSettings()

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

        routeCard().assertIsDisplayed()
    }

    @Test
    fun homeScreen_searchRemainsAfterActivityRecreation() {
        composeTestRule.onNodeWithText(context.getString(R.string.search_placeholder)).performTextInput("102")
        routeCard().assertIsDisplayed()

        composeTestRule.activityRule.scenario.recreate()

        composeTestRule.onNode(hasSetTextAction() and hasText("102")).assertIsDisplayed()
        routeCard().assertIsDisplayed()
    }

    @Test
    fun homeScreen_canOpenRouteSchedule() {
        composeTestRule.waitForIdle()

        routeCard().performClick()

        composeTestRule
            .onNodeWithText(context.getString(R.string.schedule_title_bus_format, "102"))
            .assertIsDisplayed()
    }

    @Test
    fun scheduleScreen_systemBackReturnsToRoutes() {
        routeCard().performClick()
        composeTestRule
            .onNodeWithText(context.getString(R.string.schedule_title_bus_format, "102"))
            .assertIsDisplayed()

        composeTestRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }

        routeCard().assertIsDisplayed()
    }

    private fun openSettings() {
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.home_menu_more))
            .performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.home_settings)).performClick()
    }

    private fun routeCard() = composeTestRule.onNodeWithContentDescription(
        context.getString(R.string.accessibility_route_card_description, "102", "Славгород — Яровое")
    )
}
