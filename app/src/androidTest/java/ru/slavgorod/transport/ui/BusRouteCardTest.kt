package ru.slavgorod.transport.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.slavgorod.transport.R
import ru.slavgorod.transport.data.model.BusRoute
import ru.slavgorod.transport.ui.components.BusRouteCard
import ru.slavgorod.transport.ui.theme.LetsGoSlavgorodTheme

@RunWith(AndroidJUnit4::class)
class BusRouteCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private val sampleRoute = BusRoute(
        id = "102",
        routeNumber = "102",
        name = context.getString(R.string.schedule_title_bus_format, "102"),
        description = "Market - Station",
        travelTime = "~40 minutes",
        pricePrimary = "38 / 55",
        paymentMethods = "Cash / Card",
        color = "#FF6200EE"
    )

    @Test
    fun busRouteCard_displaysRouteNumber() {
        composeTestRule.setContent {
            LetsGoSlavgorodTheme {
                BusRouteCard(
                    route = sampleRoute,
                    isGridMode = true,
                    gridColumns = 2,
                    onClick = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText(sampleRoute.routeNumber)
            .assertIsDisplayed()
    }

    @Test
    fun busRouteCard_isClickable() {
        var clicked = false

        composeTestRule.setContent {
            LetsGoSlavgorodTheme {
                BusRouteCard(
                    route = sampleRoute,
                    isGridMode = true,
                    gridColumns = 2,
                    onClick = { clicked = true }
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription(
                context.getString(
                    R.string.accessibility_route_card_description,
                    sampleRoute.routeNumber,
                    sampleRoute.name
                )
            )
            .performClick()

        assert(clicked) { "Card was not clicked" }
    }

    @Test
    fun busRouteCard_displaysAutobusLabel() {
        composeTestRule.setContent {
            LetsGoSlavgorodTheme {
                BusRouteCard(
                    route = sampleRoute,
                    isGridMode = true,
                    gridColumns = 2,
                    onClick = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.route_card_bus_label))
            .assertIsDisplayed()
    }

    @Test
    fun busRouteCard_listMode_displaysSeparator() {
        composeTestRule.setContent {
            LetsGoSlavgorodTheme {
                BusRouteCard(
                    route = sampleRoute,
                    isGridMode = false,
                    onClick = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("•")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(sampleRoute.name)
            .assertIsDisplayed()
    }
}
