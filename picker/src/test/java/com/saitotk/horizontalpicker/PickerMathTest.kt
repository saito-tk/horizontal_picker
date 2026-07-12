package com.saitotk.horizontalpicker

import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PickerMathTest {

    @Test
    fun valueToIndex_and_indexToValue_roundTrip() {
        val model = createPickerModel(0f..10f, step = 0.5f)

        val index = model.valueToIndex(6.4f)
        assertEquals(13, index)
        assertEquals(6.5f, model.indexToValue(index), 0.0001f)
    }

    @Test
    fun valueToIndex_clampsLowerAndUpperBounds() {
        val model = createPickerModel(5f..15f, step = 1f)

        assertEquals(0, model.valueToIndex(-999f))
        assertEquals(model.lastIndex, model.valueToIndex(999f))
    }

    @Test
    fun snapToStep_returnsNearestCanonicalValue() {
        val model = createPickerModel(0f..2f, step = 0.25f)

        val snapped = model.snapToStep(0.63f)
        assertEquals(0.75f, snapped, 0.0001f)
    }

    @Test
    fun createPickerModel_createsExpectedTickCount() {
        val model = createPickerModel(0f..100f, step = 1f)

        assertEquals(101, model.tickCount)
        assertTrue(model.lastIndex == 100)
    }

    @Test
    fun pickerModel_lastSelectableValue_doesNotExposeAnUnreachableRangeEnd() {
        val model = createPickerModel(0f..10f, step = 3f)

        assertEquals(9f, model.indexToValue(model.lastIndex), 0.0001f)
    }

    @Test
    fun badgeFormatting_accountsForFractionalRangeStart() {
        assertEquals(2, decimalPlaces(0.25f))
        assertEquals(1, decimalPlaces(0.1f))
        assertEquals("0.25", formatSelectedValueForBadge(0.25f, decimals = 2))
    }

    @Test
    fun intPicker_requiresValuesThatFloatCanRepresentExactly() {
        requireFloatExactIntRange(-16_777_216..16_777_216)

        try {
            requireFloatExactIntRange(16_777_217..16_777_217)
            throw AssertionError("Expected requireFloatExactIntRange to reject an inexact Float value")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
    }

    @Test
    fun verticalPickerWidth_reservesTheCorrectLabelAxisForRotation() {
        val label = LabelStyle(width = 48.dp, topPadding = 8.dp)
        val tick = TickStyle(majorHeight = 16.dp)
        val padding = PaddingValues(start = 4.dp, end = 6.dp)

        assertEquals(
            82.dp,
            verticalPickerWidth(tick, label, padding, LayoutDirection.Ltr, 20.dp, PickerContentRotation.None)
        )
        assertEquals(
            54.dp,
            verticalPickerWidth(tick, label, padding, LayoutDirection.Ltr, 20.dp, PickerContentRotation.Clockwise)
        )
        assertEquals(
            82.dp,
            verticalPickerWidth(tick, label, padding, LayoutDirection.Ltr, 20.dp, PickerContentRotation.UpsideDown)
        )
    }

    @Test
    fun nextEdgeTapAnchorIndex_accumulatesSequentialForwardTaps() {
        val first = nextEdgeTapAnchorIndex(anchorIndex = 10, delta = 1, maxIndex = 20)
        val second = nextEdgeTapAnchorIndex(anchorIndex = first, delta = 1, maxIndex = 20)

        assertEquals(11, first)
        assertEquals(12, second)
    }

    @Test
    fun nextEdgeTapAnchorIndex_clampsWithinBounds() {
        assertEquals(0, nextEdgeTapAnchorIndex(anchorIndex = 0, delta = -1, maxIndex = 20))
        assertEquals(20, nextEdgeTapAnchorIndex(anchorIndex = 20, delta = 1, maxIndex = 20))
    }

    @Test
    fun effectiveEdgeTapAnchorIndex_usesCurrentPositionWhileScrolling() {
        val anchor = effectiveEdgeTapAnchorIndex(
            settledAnchorIndex = 10,
            currentIndexFloat = 103.6f,
            isScrolling = true,
            maxIndex = 200
        )

        assertEquals(104, anchor)
    }

    @Test
    fun effectiveEdgeTapAnchorIndex_usesSettledAnchorWhenIdle() {
        val anchor = effectiveEdgeTapAnchorIndex(
            settledAnchorIndex = 10,
            currentIndexFloat = 103.6f,
            isScrolling = false,
            maxIndex = 200
        )

        assertEquals(10, anchor)
    }

    @Test
    fun edgeTapStepDelta_detectsOnlyTopEdgeZones() {
        assertEquals(
            -1,
            edgeTapStepDelta(
                downPosition = Offset(x = 10f, y = 10f),
                orientation = PickerOrientation.Horizontal,
                mainAxisSize = 200f,
                crossAxisSize = 60f,
                zoneSize = 40f,
                overlayCrossAxisSize = 30f
            )
        )
        assertEquals(
            1,
            edgeTapStepDelta(
                downPosition = Offset(x = 190f, y = 10f),
                orientation = PickerOrientation.Horizontal,
                mainAxisSize = 200f,
                crossAxisSize = 60f,
                zoneSize = 40f,
                overlayCrossAxisSize = 30f
            )
        )
        assertEquals(
            0,
            edgeTapStepDelta(
                downPosition = Offset(x = 10f, y = 50f),
                orientation = PickerOrientation.Horizontal,
                mainAxisSize = 200f,
                crossAxisSize = 60f,
                zoneSize = 40f,
                overlayCrossAxisSize = 30f
            )
        )
    }

    @Test
    fun edgeTapStepDelta_detectsOnlyEndEdgeZonesForVerticalPicker() {
        assertEquals(
            -1,
            edgeTapStepDelta(
                downPosition = Offset(x = 190f, y = 10f),
                orientation = PickerOrientation.Vertical,
                mainAxisSize = 200f,
                crossAxisSize = 200f,
                zoneSize = 40f,
                overlayCrossAxisSize = 30f
            )
        )
        assertEquals(
            1,
            edgeTapStepDelta(
                downPosition = Offset(x = 190f, y = 190f),
                orientation = PickerOrientation.Vertical,
                mainAxisSize = 200f,
                crossAxisSize = 200f,
                zoneSize = 40f,
                overlayCrossAxisSize = 30f
            )
        )
        assertEquals(
            0,
            edgeTapStepDelta(
                downPosition = Offset(x = 150f, y = 10f),
                orientation = PickerOrientation.Vertical,
                mainAxisSize = 200f,
                crossAxisSize = 200f,
                zoneSize = 40f,
                overlayCrossAxisSize = 30f
            )
        )
    }

    @Test
    fun badgeReservedSize_atDefaultFontScale_matchesOriginalUnscaledLayout() {
        assertEquals(20.dp, badgeReservedSize(1f))
    }

    @Test
    fun badgeReservedSize_growsProportionallyWithLargerFontScale() {
        assertEquals(30.dp, badgeReservedSize(1.5f))
        assertEquals(40.dp, badgeReservedSize(2f))
    }

    @Test
    fun badgeReservedSize_neverShrinksBelowTheDefault() {
        assertEquals(20.dp, badgeReservedSize(0.85f))
        assertEquals(20.dp, badgeReservedSize(0.5f))
    }

    @Test
    fun edgeTapIndicatorPositions_horizontal_centersWithinZoneAndOverlayBand() {
        val positions = edgeTapIndicatorPositions(
            orientation = PickerOrientation.Horizontal,
            mainAxisSize = 300f,
            crossAxisSize = 80f,
            zoneSize = 60f,
            overlayCrossAxisSize = 40f
        )

        assertEquals(Offset(30f, 20f), positions.start)
        assertEquals(Offset(270f, 20f), positions.end)
    }

    @Test
    fun edgeTapIndicatorPositions_vertical_alignsWithEndSideOverlayBand() {
        val positions = edgeTapIndicatorPositions(
            orientation = PickerOrientation.Vertical,
            mainAxisSize = 300f,
            crossAxisSize = 80f,
            zoneSize = 60f,
            overlayCrossAxisSize = 40f
        )

        // The vertical picker's overlay band sits against the end (right) edge of the cross
        // axis, not the start, matching where the marker/badge are actually drawn.
        assertEquals(Offset(60f, 30f), positions.start)
        assertEquals(Offset(60f, 270f), positions.end)
    }

    @Test
    fun edgeTapStepDelta_usesWidthForVerticalCrossAxis() {
        assertEquals(
            -1,
            edgeTapStepDelta(
                downPosition = Offset(x = 50f, y = 10f),
                orientation = PickerOrientation.Vertical,
                mainAxisSize = 280f,
                crossAxisSize = 56f,
                zoneSize = 84f,
                overlayCrossAxisSize = 30f
            )
        )
        assertEquals(
            0,
            edgeTapStepDelta(
                downPosition = Offset(x = 20f, y = 10f),
                orientation = PickerOrientation.Vertical,
                mainAxisSize = 280f,
                crossAxisSize = 56f,
                zoneSize = 84f,
                overlayCrossAxisSize = 30f
            )
        )
    }
}
