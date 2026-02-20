package com.saitotk.horizontalpicker

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs
import kotlin.math.round

class HorizontalPickerUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun swipe_updatesValue() {
        var value by mutableFloatStateOf(10f)

        composeRule.setContent {
            MaterialTheme {
                HorizontalPicker(
                    value = value,
                    onValueChange = { value = it },
                    valueRange = 0f..20f,
                    step = 1f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("picker")
                )
            }
        }

        composeRule.onNodeWithTag("picker").performTouchInput {
            swipeLeft()
        }
        composeRule.waitForIdle()

        assertNotEquals(10f, value)
    }

    @Test
    fun swipe_snapsToStep() {
        var value by mutableFloatStateOf(5f)

        composeRule.setContent {
            MaterialTheme {
                HorizontalPicker(
                    value = value,
                    onValueChange = { value = it },
                    valueRange = 0f..20f,
                    step = 0.5f,
                    valueChangeMode = ValueChangeMode.OnScrollFinished,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("picker")
                )
            }
        }

        composeRule.onNodeWithTag("picker").performTouchInput {
            swipeLeft()
        }
        composeRule.waitForIdle()

        val doubled = value * 2f
        assertTrue(abs(doubled - round(doubled)) < 0.0001f)
    }
}
