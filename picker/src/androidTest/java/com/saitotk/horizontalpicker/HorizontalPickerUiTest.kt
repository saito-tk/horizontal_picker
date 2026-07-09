package com.saitotk.horizontalpicker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertFalse
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

    @Test
    fun secondSwipe_isStillAccepted() {
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
        val afterFirstSwipe = value

        composeRule.onNodeWithTag("picker").performTouchInput {
            swipeLeft()
        }
        composeRule.waitForIdle()

        assertNotEquals(afterFirstSwipe, value)
    }

    @Test
    fun edgeTap_movesByOneStepOnBothSides() {
        var value by mutableFloatStateOf(10f)

        composeRule.setContent {
            MaterialTheme {
                HorizontalPicker(
                    value = value,
                    onValueChange = { value = it },
                    valueRange = 0f..20f,
                    step = 1f,
                    edgeTapZoneFraction = 0.3f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("picker")
                )
            }
        }

        composeRule.onNodeWithTag("picker").performTouchInput {
            click(Offset(1f, 1f))
        }
        composeRule.waitForIdle()
        assertTrue(value < 10f)

        val afterLeftTap = value
        composeRule.onNodeWithTag("picker").performTouchInput {
            click(Offset(width - 1f, 1f))
        }
        composeRule.waitForIdle()
        assertTrue(value > afterLeftTap)
    }

    @Test
    fun edgeTapIndicator_visibleTrue_drawsChevronsNearBothEdges() {
        composeRule.setContent {
            MaterialTheme {
                HorizontalPicker(
                    value = 10f,
                    onValueChange = {},
                    valueRange = 0f..20f,
                    step = 1f,
                    edgeTapZoneFraction = 0.3f,
                    edgeTapIndicator = EdgeTapIndicatorStyle(
                        visible = true,
                        color = Color.Red,
                        size = 16.dp,
                        strokeWidth = 4.dp
                    ),
                    modifier = Modifier
                        .width(300.dp)
                        .testTag("picker")
                )
            }
        }

        val bitmap = composeRule.onNodeWithTag("picker").captureToImage()
        val leftRegionX = 0..(bitmap.width / 4)
        val rightRegionX = (bitmap.width * 3 / 4)..(bitmap.width - 1)
        val topBandY = 0..(bitmap.height * 2 / 3)

        assertTrue(bitmap.containsColorNear(Color.Red, leftRegionX, topBandY))
        assertTrue(bitmap.containsColorNear(Color.Red, rightRegionX, topBandY))
    }

    @Test
    fun edgeTapIndicator_defaultHidden_drawsNoChevronEvenWithColorSet() {
        composeRule.setContent {
            MaterialTheme {
                HorizontalPicker(
                    value = 10f,
                    onValueChange = {},
                    valueRange = 0f..20f,
                    step = 1f,
                    edgeTapZoneFraction = 0.3f,
                    // visible defaults to false; color/size are set anyway to prove that
                    // *nothing* red is drawn even though the style would be visually loud if
                    // the visible flag were ignored.
                    edgeTapIndicator = EdgeTapIndicatorStyle(
                        color = Color.Red,
                        size = 16.dp,
                        strokeWidth = 4.dp
                    ),
                    modifier = Modifier
                        .width(300.dp)
                        .testTag("picker")
                )
            }
        }

        val bitmap = composeRule.onNodeWithTag("picker").captureToImage()
        val leftRegionX = 0..(bitmap.width / 4)
        val rightRegionX = (bitmap.width * 3 / 4)..(bitmap.width - 1)
        val topBandY = 0..(bitmap.height * 2 / 3)

        assertFalse(bitmap.containsColorNear(Color.Red, leftRegionX, topBandY))
        assertFalse(bitmap.containsColorNear(Color.Red, rightRegionX, topBandY))
    }

    @Test
    fun disabledPicker_isReportedAsDisabledToAccessibilityServices() {
        composeRule.setContent {
            MaterialTheme {
                HorizontalPicker(
                    value = 10f,
                    onValueChange = {},
                    valueRange = 0f..20f,
                    step = 1f,
                    enabled = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("picker")
                )
            }
        }

        composeRule.onNodeWithTag("picker").assertIsNotEnabled()
    }

    @Test
    fun enabledPicker_isReportedAsEnabledToAccessibilityServices() {
        composeRule.setContent {
            MaterialTheme {
                HorizontalPicker(
                    value = 10f,
                    onValueChange = {},
                    valueRange = 0f..20f,
                    step = 1f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("picker")
                )
            }
        }

        composeRule.onNodeWithTag("picker").assertIsEnabled()
    }

    @Test
    fun edgeTap_usesScaledOverlayHeightAtLargerFontScale() {
        var value by mutableFloatStateOf(10f)
        val fontScale = 1.5f

        composeRule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(density = LocalDensity.current.density, fontScale = fontScale)
            ) {
                MaterialTheme {
                    HorizontalPicker(
                        value = value,
                        onValueChange = { value = it },
                        valueRange = 0f..20f,
                        step = 1f,
                        edgeTapZoneFraction = 0.3f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("picker")
                    )
                }
            }
        }

        // At fontScale=1.5, the badge-reserved overlay band is
        // stemHeight(26dp) + badgeReservedSize(1.5f)=30dp = 56dp, vs 46dp at the default font
        // scale. A tap at y=50dp only registers as an edge tap if the *scaled* band is used,
        // proving the edge-tap hit-test and the badge offset stay in sync under scaling.
        val tapYPx = with(composeRule.density) { 50.dp.toPx() }
        composeRule.onNodeWithTag("picker").performTouchInput {
            click(Offset(1f, tapYPx))
        }
        composeRule.waitForIdle()
        assertTrue(value < 10f)
    }

    @Test
    fun badge_sitsFartherAboveThePickerAtLargerFontScale() {
        // Both pickers pin an explicit fontScale (1f and 1.5f) rather than relying on the ambient
        // system font scale, so the comparison is deterministic regardless of the test device's
        // accessibility settings.
        composeRule.setContent {
            MaterialTheme {
                Column {
                    CompositionLocalProvider(
                        LocalDensity provides Density(
                            density = LocalDensity.current.density,
                            fontScale = 1f
                        )
                    ) {
                        HorizontalPicker(
                            value = 10,
                            onValueChange = {},
                            range = 0..20,
                            step = 1,
                            label = LabelStyle(enabled = false),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("normalPicker")
                        )
                    }
                    CompositionLocalProvider(
                        LocalDensity provides Density(
                            density = LocalDensity.current.density,
                            fontScale = 1.5f
                        )
                    ) {
                        HorizontalPicker(
                            value = 11,
                            onValueChange = {},
                            range = 0..20,
                            step = 1,
                            label = LabelStyle(enabled = false),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("scaledPicker")
                        )
                    }
                }
            }
        }

        val normalPickerTop = composeRule.onNodeWithTag("normalPicker").getBoundsInRoot().top
        val scaledPickerTop = composeRule.onNodeWithTag("scaledPicker").getBoundsInRoot().top
        val normalBadgeTop = composeRule.onNodeWithText("10", useUnmergedTree = true).getBoundsInRoot().top
        val scaledBadgeTop = composeRule.onNodeWithText("11", useUnmergedTree = true).getBoundsInRoot().top

        val normalOffsetAboveTrack = normalPickerTop - normalBadgeTop
        val scaledOffsetAboveTrack = scaledPickerTop - scaledBadgeTop

        assertTrue(scaledOffsetAboveTrack > normalOffsetAboveTrack)
    }

    @Test
    fun verticalPicker_swipe_updatesValue() {
        var value by mutableFloatStateOf(10f)

        composeRule.setContent {
            MaterialTheme {
                VerticalPicker(
                    value = value,
                    onValueChange = { value = it },
                    valueRange = 0f..20f,
                    step = 1f,
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(120.dp)
                        .testTag("picker")
                )
            }
        }

        composeRule.onNodeWithTag("picker").performTouchInput {
            swipeUp()
        }
        composeRule.waitForIdle()

        assertNotEquals(10f, value)
    }
}

private fun ImageBitmap.containsColorNear(
    target: Color,
    xRange: IntRange,
    yRange: IntRange,
    tolerance: Float = 0.15f
): Boolean {
    val pixelMap = toPixelMap()
    for (x in xRange) {
        if (x !in 0 until width) continue
        for (y in yRange) {
            if (y !in 0 until height) continue
            val pixel = pixelMap[x, y]
            val isClose = abs(pixel.red - target.red) < tolerance &&
                abs(pixel.green - target.green) < tolerance &&
                abs(pixel.blue - target.blue) < tolerance
            if (isClose) return true
        }
    }
    return false
}
