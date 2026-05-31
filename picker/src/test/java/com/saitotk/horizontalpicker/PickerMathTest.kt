package com.saitotk.horizontalpicker

import androidx.compose.ui.geometry.Offset
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
                zoneSize = 40f,
                overlayCrossAxisSize = 30f
            )
        )
    }

    @Test
    fun edgeTapStepDelta_detectsOnlyStartEdgeZonesForVerticalPicker() {
        assertEquals(
            -1,
            edgeTapStepDelta(
                downPosition = Offset(x = 10f, y = 10f),
                orientation = PickerOrientation.Vertical,
                mainAxisSize = 200f,
                zoneSize = 40f,
                overlayCrossAxisSize = 30f
            )
        )
        assertEquals(
            1,
            edgeTapStepDelta(
                downPosition = Offset(x = 10f, y = 190f),
                orientation = PickerOrientation.Vertical,
                mainAxisSize = 200f,
                zoneSize = 40f,
                overlayCrossAxisSize = 30f
            )
        )
        assertEquals(
            0,
            edgeTapStepDelta(
                downPosition = Offset(x = 50f, y = 10f),
                orientation = PickerOrientation.Vertical,
                mainAxisSize = 200f,
                zoneSize = 40f,
                overlayCrossAxisSize = 30f
            )
        )
    }
}
