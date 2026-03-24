package com.saitotk.horizontalpicker

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import java.util.Locale
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Selects a value by horizontally scrolling ticks under a fixed center indicator.
 *
 * The picker maps one tick to one discrete value step. When scrolling stops, it snaps to the
 * nearest tick and reports a canonical stepped value through [onValueChange].
 */
@Composable
fun HorizontalPicker(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    step: Float,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(vertical = 12.dp),
    flingBehavior: FlingBehavior = PickerDefaults.SnapFlingBehavior,
    centerMarker: CenterMarkerStyle = CenterMarkerStyle(),
    valueBadge: @Composable BoxScope.(valueText: String, color: Color) -> Unit = { valueText, color ->
        DefaultValueBadge(valueText = valueText, color = color)
    },
    tick: TickStyle = TickStyle(),
    label: LabelStyle = LabelStyle(),
    haptics: HapticFeedbackType? = HapticFeedbackType.TextHandleMove,
    edgeTapZoneFraction: Float = 0f,
    enabled: Boolean = true
) {
    require(edgeTapZoneFraction == 0f || edgeTapZoneFraction in 0.1f..0.5f) {
        "edgeTapZoneFraction must be 0f (off) or in 0.1f..0.5f"
    }

    val edgeTapStepEnabled = edgeTapZoneFraction >= 0.1f

    val model = remember(valueRange, step) { createPickerModel(valueRange, step) }
    val clampedValue = remember(value, model) { model.snapToStep(value) }
    val targetIndex = remember(clampedValue, model) { model.valueToIndex(clampedValue) }
    val density = LocalDensity.current
    val stepPx = remember(density, tick.spacing) {
        with(density) { tick.spacing.toPx().coerceAtLeast(1f) }
    }
    val maxIndexFloat = remember(model) { model.lastIndex.toFloat() }

    val hapticFeedback = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var currentIndexFloat by remember(model) { mutableFloatStateOf(targetIndex.toFloat()) }
    var emittedIndex by remember(model) { mutableIntStateOf(targetIndex) }
    var hapticIndex by remember(model) { mutableIntStateOf(targetIndex) }
    var edgeTapAnchorIndex by remember(model) { mutableIntStateOf(targetIndex) }
    var isProgrammaticScroll by remember(model) { mutableStateOf(false) }

    val scrollableState = rememberScrollableState { delta ->
        val nextIndex = (currentIndexFloat - delta / stepPx).coerceIn(0f, maxIndexFloat)
        val consumed = (currentIndexFloat - nextIndex) * stepPx
        currentIndexFloat = nextIndex
        consumed
    }
    val resolvedFlingBehavior = if (flingBehavior === PickerDefaults.SnapFlingBehavior) {
        rememberPickerSnapFlingBehavior(
            scrollableState = scrollableState,
            currentIndexFloat = { currentIndexFloat },
            stepPx = { stepPx },
            maxIndex = { model.lastIndex }
        )
    } else {
        flingBehavior
    }

    val selectedIndex by remember(currentIndexFloat, model) {
        derivedStateOf {
            currentIndexFloat.roundToInt().coerceIn(0, model.lastIndex)
        }
    }

    suspend fun animateToIndex(index: Int, markProgrammatic: Boolean) {
        val clampedTarget = index.coerceIn(0, model.lastIndex).toFloat()
        if (abs(currentIndexFloat - clampedTarget) < 0.0001f) {
            currentIndexFloat = clampedTarget
            edgeTapAnchorIndex = clampedTarget.roundToInt()
            return
        }

        if (markProgrammatic) isProgrammaticScroll = true
        try {
            scrollableState.scroll(MutatePriority.PreventUserInput) {
                var previousValue = 0f
                animate(
                    initialValue = 0f,
                    targetValue = (currentIndexFloat - clampedTarget) * stepPx,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) { value, _ ->
                    val delta = value - previousValue
                    previousValue = value
                    scrollBy(delta)
                }
            }
            edgeTapAnchorIndex = clampedTarget.roundToInt()
        } finally {
            if (markProgrammatic) isProgrammaticScroll = false
        }
    }

    suspend fun stopAndSnapToIndex(index: Int) {
        val clampedTarget = index.coerceIn(0, model.lastIndex).toFloat()
        scrollableState.scroll(MutatePriority.PreventUserInput) {
            currentIndexFloat = clampedTarget
            edgeTapAnchorIndex = clampedTarget.roundToInt()
        }
    }

    LaunchedEffect(model) {
        currentIndexFloat = targetIndex.toFloat()
        emittedIndex = targetIndex
        hapticIndex = targetIndex
        edgeTapAnchorIndex = targetIndex
        isProgrammaticScroll = false
    }

    LaunchedEffect(targetIndex) {
        if (scrollableState.isScrollInProgress) return@LaunchedEffect
        if (targetIndex == emittedIndex) return@LaunchedEffect
        if (abs(currentIndexFloat - targetIndex.toFloat()) < 0.0001f) return@LaunchedEffect
        animateToIndex(targetIndex, markProgrammatic = true)
    }

    LaunchedEffect(scrollableState, enabled, haptics, isProgrammaticScroll, stepPx) {
        var previousCenteredIndexFloat: Float? = null

        snapshotFlow {
            PickerSnapshot(
                centeredIndex = currentIndexFloat.roundToInt().coerceIn(0, model.lastIndex),
                centeredIndexFloat = currentIndexFloat,
                alignedCenteredIndex = alignedCenteredIndex(
                    currentIndexFloat = currentIndexFloat,
                    maxIndex = model.lastIndex,
                    stepPx = stepPx
                ),
                isScrolling = scrollableState.isScrollInProgress,
                isProgrammaticScroll = isProgrammaticScroll
            )
        }
            .distinctUntilChanged()
            .collect { snapshot ->
            val centered = snapshot.centeredIndex ?: return@collect
            val centeredFloat = snapshot.centeredIndexFloat
            val previousFloat = previousCenteredIndexFloat
            previousCenteredIndexFloat = centeredFloat
            var crossedAnyIndex = false

            val alignedCentered = snapshot.alignedCenteredIndex
            if (!snapshot.isScrolling) {
                edgeTapAnchorIndex = alignedCentered ?: centered
            }
            if (
                haptics != null &&
                enabled &&
                !snapshot.isProgrammaticScroll
            ) {
                if (previousFloat != null && centeredFloat != null) {
                    forEachCrossedAlignedIndex(previousFloat, centeredFloat, model.lastIndex) { index ->
                        crossedAnyIndex = true
                        if (index != hapticIndex) {
                            hapticIndex = index
                            hapticFeedback.performHapticFeedback(haptics)
                        }
                    }
                }
                if (!crossedAnyIndex && alignedCentered != null && alignedCentered != hapticIndex) {
                    hapticIndex = alignedCentered
                    hapticFeedback.performHapticFeedback(haptics)
                }
            }

            if (snapshot.isProgrammaticScroll) return@collect

            // Emit when center line crosses tick centers even if exact aligned frames are skipped.
            if (previousFloat != null && centeredFloat != null) {
                forEachCrossedAlignedIndex(previousFloat, centeredFloat, model.lastIndex) { index ->
                    crossedAnyIndex = true
                    if (index != emittedIndex) {
                        emittedIndex = index
                        onValueChange(model.indexToValue(index))
                    }
                }
            }
            if (!crossedAnyIndex && alignedCentered != null && alignedCentered != emittedIndex) {
                emittedIndex = alignedCentered
                onValueChange(model.indexToValue(alignedCentered))
            }
        }
    }

    val reportedValue = remember(emittedIndex, model) {
        model.indexToValue(emittedIndex)
    }
    val actionLabelFormatter = label.formatter
    val semanticsLabel = remember(reportedValue, actionLabelFormatter) {
        actionLabelFormatter(reportedValue)
    }
    val selectedValueLabel = remember(reportedValue, step) {
        formatSelectedValueForBadge(reportedValue, step)
    }
    val centerMarkerColor = centerMarker.color.orFallback(MaterialTheme.colorScheme.primary)
    val edgeTapOverlayHeight = maxOf(
        contentPadding.calculateTopPadding() + centerMarker.stemHeight,
        centerMarker.stemHeight + if (centerMarker.showValueBadge) 20.dp else 0.dp
    )
    val edgeTapOverlayHeightPx = remember(density, edgeTapOverlayHeight) {
        with(density) { edgeTapOverlayHeight.toPx() }
    }
    fun requestEdgeTapStep(delta: Int) {
        if (!edgeTapStepEnabled || !enabled || delta == 0) return

        val next = nextEdgeTapAnchorIndex(
            anchorIndex = edgeTapAnchorIndex,
            delta = delta,
            maxIndex = model.lastIndex
        )
        if (next != edgeTapAnchorIndex) {
            edgeTapAnchorIndex = next
            scope.launch { stopAndSnapToIndex(next) }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pickerSemantics(
                enabled = enabled,
                valueLabel = semanticsLabel,
                currentIndex = selectedIndex,
                maxIndex = model.lastIndex,
                onIncrease = {
                    scope.launch { stopAndSnapToIndex((selectedIndex + 1).coerceAtMost(model.lastIndex)) }
                },
                onDecrease = {
                    scope.launch { stopAndSnapToIndex((selectedIndex - 1).coerceAtLeast(0)) }
                }
            )
            .progressSemantics(
                value = reportedValue,
                valueRange = model.start..model.endInclusive,
                steps = (model.lastIndex - 1).coerceAtLeast(0)
            )
            .pointerInput(
                edgeTapStepEnabled,
                enabled,
                edgeTapZoneFraction,
                edgeTapOverlayHeightPx,
                model.lastIndex
            ) {
                if (!edgeTapStepEnabled || !enabled || edgeTapZoneFraction <= 0f || edgeTapOverlayHeightPx <= 0f) {
                    return@pointerInput
                }

                awaitEachGesture {
                    val down = awaitFirstPointerDown()
                    val zoneWidth = size.width.toFloat() * edgeTapZoneFraction
                    val delta = edgeTapStepDelta(
                        downPosition = down.position,
                        width = size.width.toFloat(),
                        zoneWidth = zoneWidth,
                        overlayHeight = edgeTapOverlayHeightPx
                    )
                    val releasedAsTap = awaitReleaseWithoutDrag(
                        pointerId = down.id,
                        startPosition = down.position,
                        touchSlop = viewConfiguration.touchSlop
                    )

                    if (releasedAsTap && delta != 0) {
                        requestEdgeTapStep(delta)
                    }
                }
            }
            .scrollable(
                state = scrollableState,
                orientation = Orientation.Horizontal,
                enabled = enabled,
                flingBehavior = resolvedFlingBehavior
            )
    ) {
        PickerTrackCanvas(
            currentIndexFloat = currentIndexFloat,
            model = model,
            tickStyle = tick,
            labelStyle = label,
            contentPadding = contentPadding,
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeightIn(min = trackHeight(tick, label, contentPadding))
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            if (centerMarker.showValueBadge) {
                valueBadge(selectedValueLabel, centerMarkerColor)
            }
            DefaultSelectionStem(
                color = centerMarkerColor,
                width = centerMarker.stemWidth,
                height = centerMarker.stemHeight
            )
        }
    }
}

/** Int overload for [HorizontalPicker]. */
@Composable
fun HorizontalPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    step: Int,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(vertical = 12.dp),
    flingBehavior: FlingBehavior = PickerDefaults.SnapFlingBehavior,
    centerMarker: CenterMarkerStyle = CenterMarkerStyle(),
    valueBadge: @Composable BoxScope.(valueText: String, color: Color) -> Unit = { valueText, color ->
        DefaultValueBadge(valueText = valueText, color = color)
    },
    tick: TickStyle = TickStyle(),
    label: LabelStyle = LabelStyle(formatter = { it.roundToInt().toString() }),
    haptics: HapticFeedbackType? = HapticFeedbackType.TextHandleMove,
    edgeTapZoneFraction: Float = 0f,
    enabled: Boolean = true
) {
    require(step > 0) { "step must be > 0" }

    HorizontalPicker(
        value = value.toFloat(),
        onValueChange = { onValueChange(it.roundToInt()) },
        valueRange = range.first.toFloat()..range.last.toFloat(),
        step = step.toFloat(),
        modifier = modifier,
        contentPadding = contentPadding,
        flingBehavior = flingBehavior,
        centerMarker = centerMarker,
        valueBadge = valueBadge,
        tick = tick,
        label = label,
        haptics = haptics,
        edgeTapZoneFraction = edgeTapZoneFraction,
        enabled = enabled
    )
}

@Immutable
data class CenterMarkerStyle(
    val color: Color = Color.Unspecified,
    val stemWidth: Dp = 4.dp,
    val stemHeight: Dp = 26.dp,
    val showValueBadge: Boolean = true
)

/** Default selection stem used by [HorizontalPicker]. */
@Composable
fun BoxScope.DefaultSelectionStem(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    width: Dp = 4.dp,
    height: Dp = 26.dp
) {
    Box(
        modifier = modifier
            .align(Alignment.TopCenter)
            .width(width)
            .height(height)
            .background(color, RoundedCornerShape(percent = 50))
    )
}

@Deprecated("Use DefaultSelectionStem")
@Composable
fun BoxScope.DefaultCenterIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    DefaultSelectionStem(
        modifier = modifier,
        color = color
    )
}

/** Default value badge used by [HorizontalPicker]. */
@Composable
fun BoxScope.DefaultValueBadge(
    valueText: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Text(
        text = valueText,
        modifier = modifier
            .align(Alignment.TopCenter)
            .offset(y = (-20).dp)
            .background(
                color = color,
                shape = RoundedCornerShape(
                    topStart = 8.dp,
                    topEnd = 8.dp,
                    bottomStart = 8.dp,
                    bottomEnd = 8.dp
                )
            )
            .padding(horizontal = 8.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelMedium,
        color = Color.White
    )
}

/** Visual style for each tick mark. */
data class TickStyle(
    val spacing: Dp = 12.dp,
    val thickness: Dp = 2.dp,
    val minorHeight: Dp = 4.dp,
    val mediumHeight: Dp = 8.dp,
    val majorHeight: Dp = 16.dp,
    val minorColor: Color = Color.Unspecified,
    val mediumColor: Color = Color.Unspecified,
    val majorColor: Color = Color.Unspecified,
    val mediumEvery: Int = 5,
    val majorEvery: Int = 10
)

/** Visual style and formatter for labels shown under selected ticks. */
data class LabelStyle(
    val enabled: Boolean = true,
    val showEvery: Int = 10,
    val topPadding: Dp = 8.dp,
    val width: Dp = 48.dp,
    val textStyle: TextStyle = TextStyle.Default,
    val color: Color = Color.Unspecified,
    val formatter: (Float) -> String = { value ->
        if (abs(value - value.roundToInt()) < 0.0001f) {
            value.roundToInt().toString()
        } else {
            value.toString()
        }
    }
)

/** Defaults used by [HorizontalPicker]. */
@Stable
object PickerDefaults {
    /** Sentinel value for using snap fling behavior with [HorizontalPicker]. */
    val SnapFlingBehavior: FlingBehavior = object : FlingBehavior {
        override suspend fun ScrollScope.performFling(initialVelocity: Float): Float = initialVelocity
    }
}

private data class PickerSnapshot(
    val centeredIndex: Int?,
    val centeredIndexFloat: Float?,
    val alignedCenteredIndex: Int?,
    val isScrolling: Boolean,
    val isProgrammaticScroll: Boolean
)

private enum class TickType {
    Minor,
    Medium,
    Major
}

private fun Modifier.pickerSemantics(
    enabled: Boolean,
    valueLabel: String,
    currentIndex: Int,
    maxIndex: Int,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit
): Modifier {
    return semantics(mergeDescendants = true) {
        contentDescription = "Horizontal picker"
        stateDescription = valueLabel

        if (enabled) {
            val canIncrease = currentIndex < maxIndex
            val canDecrease = currentIndex > 0

            customActions = listOfNotNull(
                if (canIncrease) {
                    CustomAccessibilityAction(label = "Increase") {
                        onIncrease()
                        true
                    }
                } else {
                    null
                },
                if (canDecrease) {
                    CustomAccessibilityAction(label = "Decrease") {
                        onDecrease()
                        true
                    }
                } else {
                    null
                }
            )
        }
    }
}

@Composable
private fun PickerTrackCanvas(
    currentIndexFloat: Float,
    model: PickerModel,
    tickStyle: TickStyle,
    labelStyle: LabelStyle,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val colorScheme = MaterialTheme.colorScheme
    val labelHeight = 20.dp
    val labelTextStyle = MaterialTheme.typography.labelSmall.merge(labelStyle.textStyle).copy(
        color = labelStyle.color.orFallback(colorScheme.onSurfaceVariant)
    )

    Box(
        modifier = modifier.drawBehind {
            val spacingPx = tickStyle.spacing.toPx()
            val thicknessPx = tickStyle.thickness.toPx()
            val labelWidthPx = labelStyle.width.toPx().roundToInt().coerceAtLeast(1)
            val topPaddingPx = contentPadding.calculateTopPadding().toPx()
            val labelTopPaddingPx = if (labelStyle.enabled) labelStyle.topPadding.toPx() else 0f
            val labelHeightPx = if (labelStyle.enabled) labelHeight.toPx() else 0f
            val centerX = size.width / 2f
            val visibleRadius = size.width / spacingPx / 2f
            val startIndex = floor(currentIndexFloat - visibleRadius).toInt().coerceAtLeast(0)
            val endIndex = ceil(currentIndexFloat + visibleRadius).toInt().coerceAtMost(model.lastIndex)
            val labelTopY = topPaddingPx + tickStyle.majorHeight.toPx() + labelTopPaddingPx

            for (index in startIndex..endIndex) {
                val tickType = when {
                    tickStyle.majorEvery > 0 && index % tickStyle.majorEvery == 0 -> TickType.Major
                    tickStyle.mediumEvery > 0 && index % tickStyle.mediumEvery == 0 -> TickType.Medium
                    else -> TickType.Minor
                }
                val tickColor = when (tickType) {
                    TickType.Minor -> tickStyle.minorColor.orFallback(colorScheme.outlineVariant)
                    TickType.Medium -> tickStyle.mediumColor.orFallback(colorScheme.outline)
                    TickType.Major -> tickStyle.majorColor.orFallback(colorScheme.onSurface)
                }
                val tickHeightPx = when (tickType) {
                    TickType.Minor -> tickStyle.minorHeight.toPx()
                    TickType.Medium -> tickStyle.mediumHeight.toPx()
                    TickType.Major -> tickStyle.majorHeight.toPx()
                }
                val x = centerX + (index - currentIndexFloat) * spacingPx

                drawRect(
                    color = tickColor,
                    topLeft = Offset(x = x - thicknessPx / 2f, y = topPaddingPx),
                    size = Size(width = thicknessPx, height = tickHeightPx)
                )

                val showLabel = labelStyle.enabled &&
                    labelStyle.showEvery > 0 &&
                    index % labelStyle.showEvery == 0
                if (showLabel) {
                    val textLayoutResult = textMeasurer.measure(
                        text = AnnotatedString(labelStyle.formatter(model.indexToValue(index))),
                        style = labelTextStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        constraints = Constraints(maxWidth = labelWidthPx)
                    )
                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(
                            x = x - textLayoutResult.size.width / 2f,
                            y = labelTopY + (labelHeightPx - textLayoutResult.size.height) / 2f
                        )
                    )
                }
            }
        }
    )
}

private fun trackHeight(
    tickStyle: TickStyle,
    labelStyle: LabelStyle,
    contentPadding: PaddingValues
): Dp {
    val labelSpace = if (labelStyle.enabled) labelStyle.topPadding + 20.dp else 0.dp
    return contentPadding.calculateTopPadding() + tickStyle.majorHeight + labelSpace + contentPadding.calculateBottomPadding()
}

private fun alignedCenteredIndex(
    currentIndexFloat: Float,
    maxIndex: Int,
    stepPx: Float,
    alignmentTolerancePx: Float = 1f
): Int? {
    val centeredIndex = currentIndexFloat.roundToInt().coerceIn(0, maxIndex)
    return if (abs(currentIndexFloat - centeredIndex) * stepPx <= alignmentTolerancePx) {
        centeredIndex
    } else {
        null
    }
}

@Composable
private fun rememberPickerSnapFlingBehavior(
    scrollableState: androidx.compose.foundation.gestures.ScrollableState,
    currentIndexFloat: () -> Float,
    stepPx: () -> Float,
    maxIndex: () -> Int
): FlingBehavior {
    return remember(scrollableState) {
        object : FlingBehavior {
            override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                val spacingPx = stepPx().coerceAtLeast(1f)
                val velocityInSteps = abs(initialVelocity) / spacingPx
                val projectedSteps = when {
                    velocityInSteps < 10f -> 0f
                    else -> ((velocityInSteps - 10f) / 2.8f).toDouble().pow(1.05).toFloat() * 0.3f
                }
                val direction = when {
                    initialVelocity > 0f -> -1f
                    initialVelocity < 0f -> 1f
                    else -> 0f
                }
                val targetIndex = (
                    currentIndexFloat() + (direction * projectedSteps)
                ).roundToInt().coerceIn(0, maxIndex())
                var previousSnapValue = 0f
                animate(
                    initialValue = 0f,
                    targetValue = (currentIndexFloat() - targetIndex) * spacingPx,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = if (velocityInSteps >= 80f) {
                            Spring.StiffnessMediumLow
                        } else {
                            Spring.StiffnessMedium
                        }
                    )
                ) { value, _ ->
                    val delta = value - previousSnapValue
                    previousSnapValue = value
                    scrollBy(delta)
                }
                return 0f
            }
        }
    }
}

private fun Color.orFallback(fallback: Color): Color {
    return if (this == Color.Unspecified) fallback else this
}

private inline fun forEachCrossedAlignedIndex(
    from: Float,
    to: Float,
    maxIndex: Int,
    block: (Int) -> Unit
) {
    if (from == to) return

    if (to > from) {
        val start = kotlin.math.floor(from).toInt() + 1
        val end = kotlin.math.floor(to).toInt()
        if (end >= start) {
            for (index in start..end) {
                block(index.coerceIn(0, maxIndex))
            }
        }
    } else {
        val start = kotlin.math.ceil(from).toInt() - 1
        val end = kotlin.math.ceil(to).toInt()
        if (start >= end) {
            for (index in start downTo end) {
                block(index.coerceIn(0, maxIndex))
            }
        }
    }
}

internal fun nextEdgeTapAnchorIndex(anchorIndex: Int, delta: Int, maxIndex: Int): Int {
    if (maxIndex < 0 || delta == 0) return anchorIndex.coerceAtLeast(0)
    return (anchorIndex + delta).coerceIn(0, maxIndex)
}

private suspend fun androidx.compose.ui.input.pointer.AwaitPointerEventScope.awaitFirstPointerDown():
    androidx.compose.ui.input.pointer.PointerInputChange {
    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Final)
        val down = event.changes.firstOrNull { it.pressed && !it.previousPressed }
        if (down != null) {
            return down
        }
    }
}

private suspend fun androidx.compose.ui.input.pointer.AwaitPointerEventScope.awaitReleaseWithoutDrag(
    pointerId: androidx.compose.ui.input.pointer.PointerId,
    startPosition: Offset,
    touchSlop: Float
): Boolean {
    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Final)
        val change = event.changes.firstOrNull { it.id == pointerId } ?: return false
        if ((change.position - startPosition).getDistance() > touchSlop) {
            return false
        }
        if (!change.pressed) {
            return true
        }
    }
}

internal fun edgeTapStepDelta(
    downPosition: Offset,
    width: Float,
    zoneWidth: Float,
    overlayHeight: Float
): Int {
    if (zoneWidth <= 0f || downPosition.y > overlayHeight) return 0
    return when {
        downPosition.x <= zoneWidth -> -1
        downPosition.x >= width - zoneWidth -> 1
        else -> 0
    }
}

private fun formatSelectedValueForBadge(value: Float, step: Float): String {
    val decimals = stepDecimalPlaces(step)
    return if (decimals == 0) {
        value.roundToInt().toString()
    } else {
        String.format(Locale.US, "%.${decimals}f", value)
    }
}

private fun stepDecimalPlaces(step: Float): Int {
    val scale = try {
        BigDecimal.valueOf(step.toDouble()).stripTrailingZeros().scale()
    } catch (_: NumberFormatException) {
        0
    }
    return scale.coerceAtLeast(0).coerceAtMost(6)
}
