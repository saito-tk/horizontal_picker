package com.saitotk.horizontalpicker

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
}
