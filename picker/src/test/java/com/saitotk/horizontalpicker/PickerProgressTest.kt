package com.saitotk.horizontalpicker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PickerProgressTest {
    @Test
    fun progress_isInclusiveAndDoesNotRoundUp() {
        assertTrue(isTickReached(0f, 55f))
        assertTrue(isTickReached(55f, 55f))
        assertFalse(isTickReached(56f, 55f))
        assertFalse(isTickReached(55f, 54.99f))
    }

    @Test
    fun progress_handlesRangeEdgesAndReset() {
        assertFalse(isTickReached(0f, -1f))
        assertTrue(isTickReached(600f, 700f))
        assertTrue(isTickReached(55f, 60f))
        assertFalse(isTickReached(55f, 0f))
        assertTrue(isTickReached(0f, 0f))
    }

    @Test
    fun nonFiniteProgress_doesNotHighlight() {
        for (value in listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
            assertFalse(isTickReached(0f, value))
            assertFalse(isTickReached(-100f, value))
        }
    }

    @Test
    fun fractionalProgress_comparesValuesNotIndices() {
        val model = createPickerModel(-0.75f..2f, 0.25f)
        assertTrue(isTickReached(model.indexToValue(4), 0.3f)) // 0.25
        assertFalse(isTickReached(model.indexToValue(5), 0.3f)) // 0.5
        assertTrue(isTickReached(model.indexToValue(5), 0.5f))
    }

    @Test
    fun visibleTicks_doNotGrowWithTotalRange() {
        assertEquals(38..63, visibleTickIndices(50.5f, 240f, 10f, 600))
        assertEquals(38..63, visibleTickIndices(50.5f, 240f, 10f, 1_000_000))
        assertEquals(0..12, visibleTickIndices(0f, 240f, 10f, 600))
        assertEquals(588..600, visibleTickIndices(600f, 240f, 10f, 600))
        assertEquals(0..0, visibleTickIndices(0f, 240f, 10f, 0))
    }
}
