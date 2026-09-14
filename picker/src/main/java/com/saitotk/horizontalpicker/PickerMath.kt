package com.saitotk.horizontalpicker

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

private const val EPSILON = 1e-4f

/** Same order and boundary inclusion as walking every crossed tick center. */
internal fun crossedAlignedIndices(from: Float, to: Float): IntProgression = when {
    to > from -> (floor(from).toInt() + 1)..floor(to).toInt()
    to < from -> (ceil(from).toInt() - 1) downTo ceil(to).toInt()
    else -> IntRange.EMPTY
}

/** Haptics notify once per frame, unlike value callbacks which still notify every crossed tick. */
internal fun lastCrossedIndexOtherThan(indices: IntProgression, previous: Int): Int? = when {
    indices.isEmpty() -> null
    indices.last != previous -> indices.last
    indices.first != indices.last -> indices.last - indices.step
    else -> null
}

internal fun visibleLabelIndices(visible: IntRange, enabled: Boolean, every: Int): IntProgression {
    if (!enabled || every <= 0 || visible.isEmpty()) return IntRange.EMPTY
    val first = ((visible.first.toLong() + every - 1) / every) * every
    if (first > visible.last) return IntRange.EMPTY
    return first.toInt()..visible.last step every
}

/** One extra tick at each edge covers partially visible ticks; no full-range allocation. */
internal fun visibleTickIndices(currentIndex: Float, mainAxisSize: Float, spacingPx: Float, lastIndex: Int): IntRange {
    val radius = mainAxisSize / spacingPx.coerceAtLeast(1f) / 2f
    return floor(currentIndex - radius).toInt().coerceAtLeast(0)..
        ceil(currentIndex + radius).toInt().coerceAtMost(lastIndex)
}

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

/**
 * Reserved height for the default value badge above the center-marker stem, and the matching
 * edge-tap hit-test exclusion band above the ticks. Both must agree on this value: it is both a
 * layout offset (how far above the stem the badge sits) and a hit-test boundary (how tall the
 * "don't treat this as an edge tap" zone is), so they can't be derived independently.
 *
 * Fixed at 20dp for the default system font scale (1.0), matching the picker's original,
 * unscaled layout exactly. Grows proportionally for larger accessibility font scales so the
 * badge has room for larger text; never shrinks below 20dp.
 */
internal fun badgeReservedSize(fontScale: Float): Dp = 20.dp * fontScale.coerceAtLeast(1f)

/** Centers of the start-edge and end-edge chevron indicators for the edge-tap zone. */
internal data class EdgeTapIndicatorPositions(
    val start: Offset,
    val end: Offset
)

/**
 * Computes where the two edge-tap chevrons should be centered so they always line up with the
 * exact zone [edgeTapStepDelta] treats as tappable: [zoneSize] along the main axis at each edge,
 * centered within [overlayCrossAxisSize] on the cross axis. For [PickerOrientation.Vertical],
 * the tappable band sits against the end (right) edge of the cross axis, matching the picker's
 * default marker/badge placement, so the indicator is centered within
 * `crossAxisSize - overlayCrossAxisSize .. crossAxisSize` rather than from zero.
 */
internal fun edgeTapIndicatorPositions(
    orientation: PickerOrientation,
    mainAxisSize: Float,
    crossAxisSize: Float,
    zoneSize: Float,
    overlayCrossAxisSize: Float
): EdgeTapIndicatorPositions {
    val centerAlongZone = zoneSize / 2f
    return when (orientation) {
        PickerOrientation.Horizontal -> {
            val crossCenter = overlayCrossAxisSize / 2f
            EdgeTapIndicatorPositions(
                start = Offset(centerAlongZone, crossCenter),
                end = Offset(mainAxisSize - centerAlongZone, crossCenter)
            )
        }
        PickerOrientation.Vertical -> {
            val crossCenter = crossAxisSize - overlayCrossAxisSize / 2f
            EdgeTapIndicatorPositions(
                start = Offset(crossCenter, centerAlongZone),
                end = Offset(crossCenter, mainAxisSize - centerAlongZone)
            )
        }
    }
}
