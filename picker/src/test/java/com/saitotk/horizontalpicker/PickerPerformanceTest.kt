package com.saitotk.horizontalpicker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.ceil
import kotlin.math.floor

class PickerPerformanceTest {
    @Test
    fun tickBuffer_reusesStorageAcrossOneThousandScrollFrames() {
        val model = createPickerModel(0f..1_000_000f, 1f)
        val buffer = PickerTickBuffer(model, 360f, 12f)
        buffer.update(0f)
        val positions = buffer.positions
        val values = buffer.values
        for (frame in 0..1_000) {
            val center = frame * 0.375f
            buffer.update(center)
            assertSame(positions, buffer.positions)
            assertSame(values, buffer.values)
            val expected = visibleTickIndices(center, 360f, 12f, model.lastIndex)
            assertEquals(expected.count(), buffer.count)
            for (slot in 0 until buffer.count) {
                val index = expected.first + slot
                assertEquals(180f + (index - center) * 12f, buffer.positions[slot], 0f)
                assertEquals(model.indexToValue(index), buffer.values[slot], 0f)
            }
        }
        assertTrue(positions.size <= 33) // Independent of the million-tick range.
    }

    @Test
    fun tickBuffer_preservesSingleTickFractionalRangeAndEndGeometry() {
        for (model in listOf(createPickerModel(5f..5f, 1f), createPickerModel(-2.25f..5f, 0.25f))) {
            val buffer = PickerTickBuffer(model, 240f, 10f)
            for (center in listOf(0f, model.lastIndex / 2f, model.lastIndex.toFloat())) {
                buffer.update(center)
                for (slot in 0 until buffer.count) {
                    val index = buffer.firstIndex + slot
                    assertEquals(120f + (index - center) * 10f, buffer.positions[slot], 0f)
                    assertEquals(model.indexToValue(index), buffer.values[slot], 0f)
                }
                assertTrue(buffer.positions.size <= minOf(27, model.tickCount))
            }
        }
    }

    @Test
    fun labelIteration_matchesOldPerTickFilter() {
        for (start in 0..30) for (end in start..50) for (every in -1..12) {
            val expected = (start..end).filter { every > 0 && it % every == 0 }
            assertEquals(expected, visibleLabelIndices(start..end, true, every).toList())
            assertTrue(visibleLabelIndices(start..end, false, every).isEmpty())
        }
        assertTrue(visibleLabelIndices(IntRange.EMPTY, true, 1).isEmpty())
        assertEquals(listOf(Int.MAX_VALUE), visibleLabelIndices((Int.MAX_VALUE - 5)..Int.MAX_VALUE, true, Int.MAX_VALUE).toList())
    }

    @Test
    fun constantTimeHapticSelection_matchesOldWalkInBothDirections() {
        for (fromQuarter in 0..40) for (toQuarter in 0..40) {
            val from = fromQuarter / 4f
            val to = toQuarter / 4f
            val reference = buildList {
                if (to > from) {
                    for (i in (floor(from).toInt() + 1)..floor(to).toInt()) add(i)
                } else if (to < from) {
                    for (i in (ceil(from).toInt() - 1) downTo ceil(to).toInt()) add(i)
                }
            }
            val crossed = crossedAlignedIndices(from, to)
            assertEquals(reference, crossed.toList())
            for (previous in 0..10) {
                assertEquals(reference.lastOrNull { it != previous }, lastCrossedIndexOtherThan(crossed, previous))
            }
        }
    }
}
