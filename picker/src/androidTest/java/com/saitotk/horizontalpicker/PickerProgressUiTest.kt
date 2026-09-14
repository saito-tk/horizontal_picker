package com.saitotk.horizontalpicker

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PickerProgressUiTest {
    @get:Rule val composeRule = createComposeRule()

    private val normal = Color(0xFF404040)
    private val reached = Color.Red
    private val ticks = TickStyle(
        spacing = 10.dp, thickness = 4.dp,
        minorHeight = 20.dp, mediumHeight = 20.dp, majorHeight = 20.dp,
        minorColor = normal, mediumColor = normal, majorColor = normal
    )

    @Test
    fun horizontalProgress_redrawsTicksWithoutCompositionLayoutLabelsOrSelectionChanges() {
        val progress = mutableFloatStateOf(54f)
        var compositions = 0
        var measurements = 0
        var labels = 0
        var callbacks = 0
        var progressReads = 0
        composeRule.setContent {
            ProgressTestContent {
                SideEffect { compositions++ }
                HorizontalPicker(
                    value = 50, onValueChange = { callbacks++ }, range = 0..600, step = 1,
                    modifier = Modifier.width(240.dp).testTag("picker").layout { measurable, constraints ->
                        measurements++
                        val placeable = measurable.measure(constraints)
                        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                    },
                    contentPadding = PaddingValues(0.dp), tick = ticks,
                    label = LabelStyle(showEvery = 10, formatter = { labels++; it.toString() }),
                    centerMarker = CenterMarkerStyle(color = Color.Transparent),
                    valueBadge = { _, _ -> SideEffect { compositions++ } },
                    progress = PickerProgress(value = { progressReads++; progress.floatValue }, color = reached)
                )
            }
        }
        val node = composeRule.onNodeWithTag("picker")
        val initial = node.captureToImage()
        assertHorizontalTick(initial, 54, reached)
        assertHorizontalTick(initial, 55, normal)
        val before = composeRule.runOnIdle { listOf(compositions, measurements, labels, callbacks) }
        val initialReads = progressReads
        val selection = node.fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo]

        for (value in listOf(55f, 56f, 54f, 700f, -1f, Float.NaN)) {
            composeRule.runOnIdle { progress.floatValue = value }
            val bitmap = node.captureToImage()
            assertHorizontalTick(bitmap, 55, if (value.isFinite() && value >= 55f) reached else normal)
            assertHorizontalTick(bitmap, 56, if (value.isFinite() && value >= 56f) reached else normal)
            composeRule.runOnIdle {
                assertEquals("Progress must only invalidate tick drawing", before, listOf(compositions, measurements, labels, callbacks))
            }
            assertEquals(selection, node.fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo])
        }
        assertTrue(progressReads > initialReads)
    }

    @Test
    fun verticalFloatProgress_usesActualValuesAndCanBeRemoved() {
        val progress = mutableFloatStateOf(0.74f)
        val showProgress = mutableStateOf(true)
        composeRule.setContent {
            ProgressTestContent {
                VerticalPicker(
                    value = 0.25f, onValueChange = {}, valueRange = -2.25f..5f, step = 0.25f,
                    modifier = Modifier.height(240.dp).testTag("picker"),
                    contentPadding = PaddingValues(0.dp), tick = ticks,
                    label = LabelStyle(width = 48.dp, topPadding = 8.dp),
                    centerMarker = CenterMarkerStyle(color = Color.Transparent, showValueBadge = false),
                    progress = if (showProgress.value) PickerProgress(value = { progress.floatValue }) else null
                )
            }
        }
        val node = composeRule.onNodeWithTag("picker")
        val bright = lerp(normal, Color.White, 0.35f)
        // Tick area: label width 48 + padding 8 + half tick height 10 = x 66.
        assertColor(normal, node.captureToImage().toPixelMap()[66, 140]) // 0.75, not yet reached
        composeRule.runOnIdle { progress.floatValue = 0.75f }
        assertColor(bright, node.captureToImage().toPixelMap()[66, 140])
        assertColor(normal, node.captureToImage().toPixelMap()[66, 150]) // 1.0
        composeRule.runOnIdle { showProgress.value = false }
        assertColor(normal, node.captureToImage().toPixelMap()[66, 140])
    }

    private fun assertHorizontalTick(bitmap: ImageBitmap, value: Int, expected: Color) {
        assertColor(expected, bitmap.toPixelMap()[bitmap.width / 2 + (value - 50) * 10, 10])
    }

    @Composable
    private fun ProgressTestContent(content: @Composable () -> Unit) {
        MaterialTheme {
            // Avoid both system bars and the test Activity's ActionBar shadow in pixel captures.
            Box(Modifier.windowInsetsPadding(WindowInsets.safeDrawing).padding(16.dp)) {
                CompositionLocalProvider(LocalDensity provides Density(1f), content = content)
            }
        }
    }

    private fun assertColor(expected: Color, actual: Color) {
        assertEquals("Expected $expected, actual $actual", expected.red, actual.red, 0.01f)
        assertEquals(expected.green, actual.green, 0.01f)
        assertEquals(expected.blue, actual.blue, 0.01f)
        assertEquals(expected.alpha, actual.alpha, 0.01f)
    }
}
