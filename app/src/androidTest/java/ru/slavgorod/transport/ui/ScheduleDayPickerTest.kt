package ru.slavgorod.transport.ui

import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import ru.slavgorod.transport.ui.components.schedule.ScheduleDayPicker
import java.time.DayOfWeek

class ScheduleDayPickerTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun narrowPickerScrollsToLastDayAndSelectsIt() {
        var selectedDay by mutableStateOf(DayOfWeek.MONDAY)
        composeRule.setContent {
            MaterialTheme {
                ScheduleDayPicker(
                    selectedDay = selectedDay,
                    currentDay = DayOfWeek.MONDAY,
                    onDaySelected = { selectedDay = it },
                    modifier = Modifier.width(240.dp)
                )
            }
        }

        composeRule.onNodeWithContentDescription("Понедельник").assertIsDisplayed().assertIsSelected()
        composeRule.onNodeWithTag("schedule_day_picker").performTouchInput { swipeLeft() }
        composeRule.onNodeWithContentDescription("Воскресенье").assertIsDisplayed().performClick().assertIsSelected()
    }
}
