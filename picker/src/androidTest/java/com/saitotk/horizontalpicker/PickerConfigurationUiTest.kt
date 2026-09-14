package com.saitotk.horizontalpicker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PickerConfigurationUiTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun labels_stillObserveFormatterStateAfterMeasurementCacheHits() {
        var suffix by mutableStateOf("A")
        val formatted = mutableListOf<String>()
        val label = LabelStyle(showEvery = 1, formatter = { "$it$suffix".also { formatted += it } })
        composeRule.setContent {
            MaterialTheme {
                Box(Modifier.padding(32.dp)) {
                    HorizontalPicker(
                        value = 50, onValueChange = {}, range = 0..100, step = 1,
                        label = label, modifier = Modifier.width(300.dp).testTag("picker")
                    )
                }
            }
        }
        val node = composeRule.onNodeWithTag("picker")
        node.captureToImage()
        composeRule.runOnIdle {
            assertTrue(formatted.any { it.endsWith("A") })
            formatted.clear()
            suffix = "B"
        }
        node.captureToImage()
        composeRule.runOnIdle {
            assertTrue("Cached text must not freeze a state-backed formatter", formatted.any { it.endsWith("B") })
        }
    }

    @Test
    fun fling_afterRangeAndSpacingChange_usesCurrentModel() {
        var range by mutableStateOf(0..10)
        var value by mutableIntStateOf(5)
        var spacing by mutableStateOf(12.dp)
        composeRule.setContent {
            MaterialTheme {
                Box(Modifier.padding(32.dp)) {
                    HorizontalPicker(
                        value = value, onValueChange = { value = it }, range = range, step = 1,
                        tick = TickStyle(spacing = spacing),
                        modifier = Modifier.width(300.dp).testTag("picker")
                    )
                }
            }
        }
        composeRule.runOnIdle {
            range = 0..200
            value = 50
            spacing = 18.dp
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("picker").performTouchInput { swipeLeft() }
        composeRule.runOnIdle {
            assertTrue("Fling must move forward from 50, not snap toward the obsolete 0..10 model: $value", value > 50)
            assertTrue(value in range)
        }
    }
}
