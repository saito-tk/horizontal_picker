package com.saitotk.horizontalpicker

import kotlin.math.floor
import kotlin.math.roundToInt

private const val EPSILON = 1e-4f

internal data class PickerModel(
    val start: Float,
    val endInclusive: Float,
    val step: Float,
    val tickCount: Int
) {
    val lastIndex: Int = tickCount - 1

    fun valueToIndex(value: Float): Int {
        val clamped = value.coerceIn(start, endInclusive)
        val relative = (clamped - start) / step
        return relative.roundToInt().coerceIn(0, lastIndex)
    }

    fun indexToValue(index: Int): Float {
        val safeIndex = index.coerceIn(0, lastIndex)
        val computed = start + (safeIndex * step)
        return computed.coerceIn(start, endInclusive)
    }

    fun snapToStep(value: Float): Float = indexToValue(valueToIndex(value))
}

internal fun createPickerModel(
    valueRange: ClosedFloatingPointRange<Float>,
    step: Float
): PickerModel {
    require(step > 0f) { "step must be > 0" }
    require(valueRange.start <= valueRange.endInclusive) {
        "valueRange.start must be <= valueRange.endInclusive"
    }

    val span = valueRange.endInclusive - valueRange.start
    val steps = floor((span / step + EPSILON).toDouble()).toLong()
    require(steps >= 0) { "Computed step count must be >= 0" }
    require(steps < Int.MAX_VALUE) {
        "Too many ticks. Reduce range size or increase step."
    }

    return PickerModel(
        start = valueRange.start,
        endInclusive = valueRange.endInclusive,
        step = step,
        tickCount = steps.toInt() + 1
    )
}
