package ru.slavgorod.transport.ui

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import ru.slavgorod.transport.ui.components.DisclaimerDialog
import ru.slavgorod.transport.ui.theme.LetsGoSlavgorodTheme

class DisclaimerDialogTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun requiresAcknowledgementAndCompletesOnlyOnLastStep() {
        var accepted = 0
        compose.setContent {
            LetsGoSlavgorodTheme {
                DisclaimerDialog(onDismiss = {}, onAccept = { accepted++ })
            }
        }
        compose.onNodeWithText("Продолжить").assertIsNotEnabled()
        compose.onNodeWithText("Я ознакомился(лась)").performScrollTo().performClick()
        compose.onNodeWithText("Продолжить").assertIsEnabled()
        compose.onNodeWithText("Я ознакомился(лась)").performScrollTo().performClick()
        compose.onNodeWithText("Продолжить").assertIsNotEnabled()
        compose.onNodeWithText("Я ознакомился(лась)").performScrollTo().performClick()
        compose.onNodeWithText("Продолжить").performClick()
        compose.onNodeWithText("ВКонтакте").assertExists()
        compose.onNodeWithText("МАКС").assertExists()
        compose.runOnIdle { assertEquals(0, accepted) }
        compose.onNodeWithText("Продолжить").performClick()
        compose.onNodeWithText("RuStore").assertExists()
        compose.onNodeWithText("Google Play").assertExists()
        compose.runOnIdle { assertEquals(0, accepted) }
        compose.onNodeWithText("Начать пользоваться").performClick()
        compose.runOnIdle { assertEquals(1, accepted) }
    }

    @Test
    fun preservesCurrentStepAfterRecreation() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            LetsGoSlavgorodTheme { DisclaimerDialog(onDismiss = {}, onAccept = {}) }
        }
        compose.onNodeWithText("Я ознакомился(лась)").performScrollTo().performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Продолжить").assertIsEnabled().performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Будьте в курсе новостей").assertExists()
    }

    @Test
    fun noticeRemainsScrollableWithLargeText() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                LetsGoSlavgorodTheme { DisclaimerDialog(onDismiss = {}, onAccept = {}) }
            }
        }
        compose.onNodeWithText("Подробнее об организациях").assertDoesNotExist()
        compose.onNodeWithText("Приложение не является официальным", substring = true).assertExists()
        compose.onNodeWithText("Я ознакомился(лась)").performScrollTo().performClick()
        compose.onNodeWithText("Продолжить").assertIsEnabled()
    }
}
