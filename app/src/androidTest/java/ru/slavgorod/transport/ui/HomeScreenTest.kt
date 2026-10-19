package ru.slavgorod.transport.ui

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.slavgorod.transport.R
import ru.slavgorod.transport.data.model.BusRoute
import ru.slavgorod.transport.data.repository.RoutesTableDataSource
import ru.slavgorod.transport.data.repository.ScheduleFetcher
import ru.slavgorod.transport.notifications.AppForegroundTracker
import ru.slavgorod.transport.ui.screens.EmptyState
import ru.slavgorod.transport.ui.screens.ErrorState
import ru.slavgorod.transport.ui.screens.HomeScreen
import ru.slavgorod.transport.ui.screens.LoadingState
import ru.slavgorod.transport.ui.screens.RoutesListStateForTest
import ru.slavgorod.transport.ui.viewmodel.RoutesViewModel

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private lateinit var routesViewModel: RoutesViewModel

    @Before
    fun setup() {
        val repository = RoutesTableDataSource(
            context = context,
            onlineChecker = { true },
            remoteScheduleFetcher = FakeScheduleFetcher { routesJson() },
            autoLoadOnInit = false
        )
        runBlocking {
            repository.refreshRoutesFromLocal(notifyUser = false)
        }
        routesViewModel = RoutesViewModel(
            routeRepository = repository,
            appForegroundTracker = AppForegroundTracker().apply {
                setForegroundForTesting(true)
            },
            loadRoutesOnInit = false
        )
    }

    @Test
    fun emptyState_displaysCorrectMessageWhenSearchQueryEmpty() {
        composeTestRule.setContent {
            EmptyState(searchQuery = "")
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.home_no_routes_title))
            .assertIsDisplayed()
    }

    @Test
    fun emptyState_displaysCorrectMessageWhenSearchQueryNotEmpty() {
        val searchQuery = "102"
        composeTestRule.setContent {
            EmptyState(searchQuery = searchQuery)
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.home_no_results_title))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(
                context.getString(
                    R.string.home_no_results_body,
                    searchQuery
                )
            )
            .assertIsDisplayed()
    }

    @Test
    fun errorState_displaysErrorMessage() {
        val errorMessage = "Тестовая ошибка"
        composeTestRule.setContent {
            ErrorState(errorMessage = errorMessage)
        }

        composeTestRule
            .onNodeWithText(errorMessage)
            .assertIsDisplayed()
    }

    @Test
    fun loadingState_displaysLoadingMessage() {
        composeTestRule.setContent {
            LoadingState()
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.home_loading_routes))
            .assertIsDisplayed()
    }

    @Test
    fun emptyState_showsPullToRefreshHintWhenNoSearchQuery() {
        composeTestRule.setContent {
            EmptyState(searchQuery = "")
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.home_no_routes_hint))
            .assertIsDisplayed()
    }

    @Test
    fun emptyState_doesNotShowPullToRefreshHintWhenSearchQueryExists() {
        composeTestRule.setContent {
            EmptyState(searchQuery = "102")
        }

        composeTestRule
            .onAllNodesWithText(context.getString(R.string.home_no_routes_hint))
            .assertCountEquals(0)
    }

    @Test
    fun routesListState_displaysRoutes() {
        val routes = listOf(
            route(id = "102", routeNumber = "102", name = "Yarovoye"),
            route(id = "105", routeNumber = "105", name = "Железнодорожное депо")
        )

        composeTestRule.setContent {
            RoutesListStateForTest(
                routes = routes,
                navController = rememberNavController(),
                gridColumns = 1
            )
        }

        composeTestRule
            .onNodeWithText("102")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("105")
            .assertIsDisplayed()
    }

    @Test
    fun homeScreen_backPressClosesMenu() {
        composeTestRule.setContent {
            HomeScreen(
                navController = rememberNavController(),
                routesViewModel = routesViewModel
            )
        }

        val menuDescription = context.getString(R.string.home_menu_more)
        val refreshScheduleText = context.getString(R.string.home_refresh_schedule)

        composeTestRule
            .onNodeWithContentDescription(menuDescription)
            .performClick()

        composeTestRule
            .onNodeWithText(refreshScheduleText)
            .assertIsDisplayed()

        composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        composeTestRule.waitForIdle()

        composeTestRule
            .onAllNodesWithText(refreshScheduleText)
            .assertCountEquals(0)

    }

    @Test
    fun homeScreen_preservesSearchAndMenuStateAcrossRecreationLikeRerender() {
        val menuDescription = context.getString(R.string.home_menu_more)
        val refreshScheduleText = context.getString(R.string.home_refresh_schedule)
        val searchQuery = "Yarovoye"
        val filteredOutRouteText = "Железнодорожное депо"

        composeTestRule.setContent {
            HomeScreen(
                navController = rememberNavController(),
                routesViewModel = routesViewModel
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            routesViewModel.onSearchQueryChange(searchQuery)
            routesViewModel.openHeaderMenu()
        }
        composeTestRule.waitUntilNodeMissing(filteredOutRouteText)

        composeTestRule
            .onNodeWithText(searchQuery)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithContentDescription(menuDescription)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(refreshScheduleText)
            .assertIsDisplayed()

        composeTestRule.setContent {
            HomeScreen(
                navController = rememberNavController(),
                routesViewModel = routesViewModel
            )
        }
        composeTestRule.waitUntilNodeMissing(filteredOutRouteText)

        composeTestRule
            .onNodeWithText(searchQuery)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithContentDescription(menuDescription)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(refreshScheduleText)
            .assertIsDisplayed()
    }

    private fun androidx.compose.ui.test.junit4.AndroidComposeTestRule<*, *>.waitUntilNodeMissing(
        text: String
    ) {
        waitUntil(5_000L) {
            onAllNodesWithText(text).fetchSemanticsNodes().isEmpty()
        }
    }

    private fun route(id: String, routeNumber: String, name: String): BusRoute {
        return BusRoute(
            id = id,
            routeNumber = routeNumber,
            name = name,
            description = name,
            color = "#1976D2",
            travelTime = null,
            paymentMethods = null
        )
    }

    private fun routesJson(): String {
        return """
            {
              "routes": [
                {
                  "id": "102",
                  "routeNumber": "102",
                  "name": "Yarovoye",
                  "description": "Slavgorod - Yarovoye",
                  "color": "#1976D2",
                  "travelTime": "40 min",
                  "pricePrimary": "40",
                  "priceSecondary": "60",
                  "paymentMethods": "Cash / Card"
                },
                {
                  "id": "105",
                  "routeNumber": "105",
                  "name": "Железнодорожное депо",
                  "description": "Железнодорожное депо",
                  "color": "#F2B705",
                  "travelTime": "15 min",
                  "pricePrimary": "20",
                  "priceSecondary": "30",
                  "paymentMethods": "Cash"
                }
              ]
            }
        """.trimIndent()
    }

    private class FakeScheduleFetcher(
        private val jsonProvider: () -> String
    ) : ScheduleFetcher {

        override fun isOnline(): Boolean = true

        override suspend fun fetchWithRetry(): String = jsonProvider()
    }
}
