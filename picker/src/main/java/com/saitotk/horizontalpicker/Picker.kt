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
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.requiredWidthIn
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
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
    contentRotation: PickerContentRotation = PickerContentRotation.None,
    valueBadge: @Composable BoxScope.(valueText: String, color: Color) -> Unit = { valueText, color ->
        DefaultValueBadge(valueText = valueText, color = color, contentRotation = contentRotation)
    },
    tick: TickStyle = TickStyle(),
    label: LabelStyle = LabelStyle(),
    haptics: HapticFeedbackType? = HapticFeedbackType.TextHandleMove,
    edgeTapZoneFraction: Float = 0f,
    enabled: Boolean = true
) {
    Picker(
        orientation = PickerOrientation.Horizontal,
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        step = step,
        modifier = modifier,
        contentPadding = contentPadding,
        flingBehavior = flingBehavior,
        centerMarker = centerMarker,
        contentRotation = contentRotation,
        valueBadge = valueBadge,
        tick = tick,
        label = label,
        haptics = haptics,
        edgeTapZoneFraction = edgeTapZoneFraction,
        enabled = enabled
    )
}

/**
 * Selects a value by vertically scrolling ticks under a fixed center indicator.
 *
 * Use this instead of rotating [HorizontalPicker] when the picker should be displayed at 90 degrees.
 * The gesture axis, fling velocity, drawing axis, and edge tap zones all use vertical coordinates.
 */
@Composable
fun VerticalPicker(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    step: Float,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp),
    flingBehavior: FlingBehavior = PickerDefaults.SnapFlingBehavior,
    centerMarker: CenterMarkerStyle = CenterMarkerStyle(),
    contentRotation: PickerContentRotation = PickerContentRotation.None,
    valueBadge: @Composable BoxScope.(valueText: String, color: Color) -> Unit = { valueText, color ->
        DefaultVerticalValueBadge(valueText = valueText, color = color, contentRotation = contentRotation)
    },
    tick: TickStyle = TickStyle(),
    label: LabelStyle = LabelStyle(),
    haptics: HapticFeedbackType? = HapticFeedbackType.TextHandleMove,
    edgeTapZoneFraction: Float = 0f,
    enabled: Boolean = true
) {
    Picker(
        orientation = PickerOrientation.Vertical,
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        step = step,
        modifier = modifier,
        contentPadding = contentPadding,
        flingBehavior = flingBehavior,
        centerMarker = centerMarker,
        contentRotation = contentRotation,
        valueBadge = valueBadge,
        tick = tick,
        label = label,
        haptics = haptics,
        edgeTapZoneFraction = edgeTapZoneFraction,
        enabled = enabled
    )
}

@Composable
private fun Picker(
    orientation: PickerOrientation,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    step: Float,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    flingBehavior: FlingBehavior,
    centerMarker: CenterMarkerStyle,
    contentRotation: PickerContentRotation,
    valueBadge: @Composable BoxScope.(valueText: String, color: Color) -> Unit,
    tick: TickStyle,
    label: LabelStyle,
    haptics: HapticFeedbackType?,
    edgeTapZoneFraction: Float,
    enabled: Boolean
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

    val selectedIndex by remember(model) {
        derivedStateOf {
            currentIndexFloat.roundToInt().coerceIn(0, model.lastIndex)
        }
    }

    fun commitSettledIndex(index: Int, emitValueChange: Boolean, triggerHaptic: Boolean) {
        val clampedIndex = index.coerceIn(0, model.lastIndex)
        currentIndexFloat = clampedIndex.toFloat()
        edgeTapAnchorIndex = clampedIndex

        if (triggerHaptic && haptics != null && enabled && clampedIndex != hapticIndex) {
            hapticIndex = clampedIndex
            hapticFeedback.performHapticFeedback(haptics)
        }

        if (emitValueChange && clampedIndex != emittedIndex) {
            emittedIndex = clampedIndex
            onValueChange(model.indexToValue(clampedIndex))
        }
    }

    suspend fun animateToIndex(index: Int, markProgrammatic: Boolean) {
        val clampedTarget = index.coerceIn(0, model.lastIndex).toFloat()
        if (abs(currentIndexFloat - clampedTarget) < 0.0001f) {
            commitSettledIndex(
                index = clampedTarget.roundToInt(),
                emitValueChange = !markProgrammatic,
                triggerHaptic = !markProgrammatic
            )
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
        val clampedTarget = index.coerceIn(0, model.lastIndex)
        scrollableState.scroll(MutatePriority.PreventUserInput) {
            currentIndexFloat = clampedTarget.toFloat()
        }
        commitSettledIndex(
            index = clampedTarget,
            emitValueChange = true,
            triggerHaptic = true
        )
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

    LaunchedEffect(scrollableState, enabled, haptics, stepPx) {
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
    val layoutDirection = LocalLayoutDirection.current
    val edgeTapOverlayCrossAxisSize = when (orientation) {
        PickerOrientation.Horizontal -> maxOf(
            contentPadding.calculateTopPadding() + centerMarker.stemHeight,
            centerMarker.stemHeight + if (centerMarker.showValueBadge) 20.dp else 0.dp
        )
        PickerOrientation.Vertical -> maxOf(
            contentPadding.calculateEndPadding(layoutDirection) + centerMarker.stemHeight,
            centerMarker.stemHeight + if (centerMarker.showValueBadge) 20.dp else 0.dp
        )
    }
    val edgeTapOverlayCrossAxisSizePx = remember(density, edgeTapOverlayCrossAxisSize) {
        with(density) { edgeTapOverlayCrossAxisSize.toPx() }
    }
    fun requestEdgeTapStep(delta: Int) {
        if (!edgeTapStepEnabled || !enabled || delta == 0) return

        val anchorIndex = effectiveEdgeTapAnchorIndex(
            settledAnchorIndex = edgeTapAnchorIndex,
            currentIndexFloat = currentIndexFloat,
            isScrolling = scrollableState.isScrollInProgress,
            maxIndex = model.lastIndex
        )
        val next = nextEdgeTapAnchorIndex(
            anchorIndex = anchorIndex,
            delta = delta,
            maxIndex = model.lastIndex
        )
        if (next != anchorIndex) {
            edgeTapAnchorIndex = next
            scope.launch { stopAndSnapToIndex(next) }
        }
    }

    val axisFillModifier = when (orientation) {
        PickerOrientation.Horizontal -> Modifier.fillMaxWidth()
        PickerOrientation.Vertical -> Modifier.fillMaxHeight()
    }
    val crossAxisSizeModifier = when (orientation) {
        PickerOrientation.Horizontal -> Modifier
        PickerOrientation.Vertical -> Modifier.requiredWidth(
            verticalPickerWidth(
                tickStyle = tick,
                labelStyle = label,
                contentPadding = contentPadding,
                layoutDirection = layoutDirection
            )
        )
    }
    val trackModifier = when (orientation) {
        PickerOrientation.Horizontal -> Modifier
            .fillMaxWidth()
            .requiredHeightIn(min = trackHeight(tick, label, contentPadding))
        PickerOrientation.Vertical -> Modifier
            .fillMaxHeight()
            .requiredWidth(
                verticalPickerWidth(
                    tickStyle = tick,
                    labelStyle = label,
                    contentPadding = contentPadding,
                    layoutDirection = layoutDirection
                )
            )
    }

    Box(
        modifier = modifier
            .then(axisFillModifier)
            .then(crossAxisSizeModifier)
            .pickerSemantics(
                enabled = enabled,
                contentDescription = orientation.contentDescription,
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
                edgeTapOverlayCrossAxisSizePx,
                orientation,
                model.lastIndex
            ) {
                if (!edgeTapStepEnabled || !enabled || edgeTapZoneFraction <= 0f || edgeTapOverlayCrossAxisSizePx <= 0f) {
                    return@pointerInput
                }

                awaitEachGesture {
                    val down = awaitFirstPointerDown()
                    val mainAxisSize = orientation.mainAxisSize(size.width.toFloat(), size.height.toFloat())
                    val crossAxisSize = orientation.crossAxisSize(size.width.toFloat(), size.height.toFloat())
                    val zoneSize = mainAxisSize * edgeTapZoneFraction
                    val delta = edgeTapStepDelta(
                        downPosition = down.position,
                        orientation = orientation,
                        mainAxisSize = mainAxisSize,
                        crossAxisSize = crossAxisSize,
                        zoneSize = zoneSize,
                        overlayCrossAxisSize = edgeTapOverlayCrossAxisSizePx
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
                orientation = orientation.scrollableOrientation,
                enabled = enabled,
                flingBehavior = resolvedFlingBehavior
            )
    ) {
        PickerTrackCanvas(
            currentIndexFloat = { currentIndexFloat },
            model = model,
            tickStyle = tick,
            labelStyle = label,
            contentPadding = contentPadding,
            orientation = orientation,
            contentRotation = contentRotation,
            modifier = trackModifier
        )

        val markerOverlayModifier = when (orientation) {
            PickerOrientation.Horizontal -> Modifier.fillMaxWidth()
            PickerOrientation.Vertical -> Modifier
                .fillMaxHeight()
                .requiredWidth(centerMarker.stemHeight)
                .align(Alignment.CenterEnd)
        }

        Box(modifier = markerOverlayModifier) {
            if (centerMarker.showValueBadge) {
                valueBadge(selectedValueLabel, centerMarkerColor)
            }
            when (orientation) {
                PickerOrientation.Horizontal -> DefaultSelectionStem(
                    color = centerMarkerColor,
                    width = centerMarker.stemWidth,
                    height = centerMarker.stemHeight
                )
                PickerOrientation.Vertical -> DefaultVerticalSelectionStem(
                    color = centerMarkerColor,
                    width = centerMarker.stemHeight,
                    height = centerMarker.stemWidth
                )
            }
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
    contentRotation: PickerContentRotation = PickerContentRotation.None,
    valueBadge: @Composable BoxScope.(valueText: String, color: Color) -> Unit = { valueText, color ->
        DefaultValueBadge(valueText = valueText, color = color, contentRotation = contentRotation)
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
        contentRotation = contentRotation,
        valueBadge = valueBadge,
        tick = tick,
        label = label,
        haptics = haptics,
        edgeTapZoneFraction = edgeTapZoneFraction,
        enabled = enabled
    )
}

/** Int overload for [VerticalPicker]. */
@Composable
fun VerticalPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    step: Int,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp),
    flingBehavior: FlingBehavior = PickerDefaults.SnapFlingBehavior,
    centerMarker: CenterMarkerStyle = CenterMarkerStyle(),
    contentRotation: PickerContentRotation = PickerContentRotation.None,
    valueBadge: @Composable BoxScope.(valueText: String, color: Color) -> Unit = { valueText, color ->
        DefaultVerticalValueBadge(valueText = valueText, color = color, contentRotation = contentRotation)
    },
    tick: TickStyle = TickStyle(),
    label: LabelStyle = LabelStyle(formatter = { it.roundToInt().toString() }),
    haptics: HapticFeedbackType? = HapticFeedbackType.TextHandleMove,
    edgeTapZoneFraction: Float = 0f,
    enabled: Boolean = true
) {
    require(step > 0) { "step must be > 0" }

    VerticalPicker(
        value = value.toFloat(),
        onValueChange = { onValueChange(it.roundToInt()) },
        valueRange = range.first.toFloat()..range.last.toFloat(),
        step = step.toFloat(),
        modifier = modifier,
        contentPadding = contentPadding,
        flingBehavior = flingBehavior,
        centerMarker = centerMarker,
        contentRotation = contentRotation,
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

/** Rotation applied to labels and the default vertical value badge. */
enum class PickerContentRotation(val degrees: Float) {
    None(0f),
    Clockwise(90f),
    CounterClockwise(-90f),
    UpsideDown(180f)
}

private val VerticalCenterMarkerLabelOverlap = 2.dp

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

/** Default selection stem used by [VerticalPicker]. */
@Composable
fun BoxScope.DefaultVerticalSelectionStem(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    width: Dp = 26.dp,
    height: Dp = 4.dp
) {
    Box(
        modifier = modifier
            .align(Alignment.CenterStart)
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
    modifier: Modifier = Modifier,
    contentRotation: PickerContentRotation = PickerContentRotation.None
) {
    Layout(
        content = {
            Text(
                text = valueText,
                modifier = Modifier
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
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        },
        modifier = modifier
            .align(Alignment.TopCenter)
            .offset(y = (-20).dp)
    ) { measurables, constraints ->
        val placeable = measurables.single().measure(
            Constraints(
                minWidth = 0,
                maxWidth = Constraints.Infinity,
                minHeight = 0,
                maxHeight = constraints.maxHeight
            )
        )
        layout(width = 0, height = 0) {
            placeable.placeRelativeWithLayer(
                x = (-placeable.width / 2f).roundToInt(),
                y = 0
            ) {
                rotationZ = contentRotation.degrees
            }
        }
    }
}

/** Default value badge used by [VerticalPicker]. */
@Composable
fun BoxScope.DefaultVerticalValueBadge(
    valueText: String,
    color: Color,
    modifier: Modifier = Modifier,
    contentRotation: PickerContentRotation = PickerContentRotation.None
) {
    Layout(
        content = {
            Text(
                text = valueText,
                modifier = Modifier
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
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        },
        modifier = modifier
            .align(Alignment.CenterEnd)
            .offset(x = -VerticalCenterMarkerLabelOverlap)
    ) { measurables, constraints ->
        val placeable = measurables.single().measure(
            Constraints(
                minWidth = 0,
                maxWidth = Constraints.Infinity,
                minHeight = 0,
                maxHeight = constraints.maxHeight
            )
        )
        layout(width = 0, height = 0) {
            val x = if (contentRotation == PickerContentRotation.None) {
                0
            } else {
                ((placeable.height - placeable.width) / 2f).roundToInt()
            }
            val y = (-placeable.height / 2f).roundToInt()
            placeable.placeRelativeWithLayer(x = x, y = y) {
                rotationZ = contentRotation.degrees
            }
        }
    }
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

internal enum class PickerOrientation(
    val scrollableOrientation: Orientation,
    val contentDescription: String
) {
    Horizontal(Orientation.Horizontal, "Horizontal picker"),
    Vertical(Orientation.Vertical, "Vertical picker");

    fun mainAxisSize(width: Float, height: Float): Float {
        return when (this) {
            Horizontal -> width
            Vertical -> height
        }
    }

    fun crossAxisSize(width: Float, height: Float): Float {
        return when (this) {
            Horizontal -> height
            Vertical -> width
        }
    }
}

private fun Modifier.pickerSemantics(
    enabled: Boolean,
    contentDescription: String,
    valueLabel: String,
    currentIndex: Int,
    maxIndex: Int,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit
): Modifier {
    return semantics(mergeDescendants = true) {
        this.contentDescription = contentDescription
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
    currentIndexFloat: () -> Float,
    model: PickerModel,
    tickStyle: TickStyle,
    labelStyle: LabelStyle,
    contentPadding: PaddingValues,
    orientation: PickerOrientation,
    contentRotation: PickerContentRotation,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer(cacheSize = 16)
    val colorScheme = MaterialTheme.colorScheme
    val layoutDirection = LocalLayoutDirection.current
    val labelHeight = 20.dp
    val labelTextStyle = MaterialTheme.typography.labelSmall.merge(labelStyle.textStyle).copy(
        color = labelStyle.color.orFallback(colorScheme.onSurfaceVariant)
    )

    Box(
        modifier = modifier.drawBehind {
            val currentIndex = currentIndexFloat()
            val spacingPx = tickStyle.spacing.toPx()
            val thicknessPx = tickStyle.thickness.toPx()
            val labelWidthPx = labelStyle.width.toPx().roundToInt().coerceAtLeast(1)
            val topPaddingPx = contentPadding.calculateTopPadding().toPx()
            val startPaddingPx = contentPadding.calculateStartPadding(layoutDirection).toPx()
            val labelTopPaddingPx = if (labelStyle.enabled) labelStyle.topPadding.toPx() else 0f
            val labelHeightPx = if (labelStyle.enabled) labelHeight.toPx() else 0f
            val mainAxisSize = orientation.mainAxisSize(size.width, size.height)
            val centerOnMainAxis = mainAxisSize / 2f
            val visibleRadius = mainAxisSize / spacingPx / 2f
            val startIndex = floor(currentIndex - visibleRadius).toInt().coerceAtLeast(0)
            val endIndex = ceil(currentIndex + visibleRadius).toInt().coerceAtMost(model.lastIndex)
            val labelTopY = topPaddingPx + tickStyle.majorHeight.toPx() + labelTopPaddingPx
            val verticalLabelCrossAxisSizePx = if (contentRotation == PickerContentRotation.None) {
                labelStyle.width.toPx()
            } else {
                labelHeightPx
            }
            val verticalTickStartX = startPaddingPx + verticalLabelCrossAxisSizePx + labelTopPaddingPx
            val verticalTickEndX = verticalTickStartX + tickStyle.majorHeight.toPx()
            val verticalLabelStartX = startPaddingPx
            val minorTickColor = tickStyle.minorColor.orFallback(colorScheme.outlineVariant)
            val mediumTickColor = tickStyle.mediumColor.orFallback(colorScheme.outline)
            val majorTickColor = tickStyle.majorColor.orFallback(colorScheme.onSurface)
            val minorTickHeightPx = tickStyle.minorHeight.toPx()
            val mediumTickHeightPx = tickStyle.mediumHeight.toPx()
            val majorTickHeightPx = tickStyle.majorHeight.toPx()

            for (index in startIndex..endIndex) {
                val tickType = when {
                    tickStyle.majorEvery > 0 && index % tickStyle.majorEvery == 0 -> TickType.Major
                    tickStyle.mediumEvery > 0 && index % tickStyle.mediumEvery == 0 -> TickType.Medium
                    else -> TickType.Minor
                }
                val tickColor = when (tickType) {
                    TickType.Minor -> minorTickColor
                    TickType.Medium -> mediumTickColor
                    TickType.Major -> majorTickColor
                }
                val tickHeightPx = when (tickType) {
                    TickType.Minor -> minorTickHeightPx
                    TickType.Medium -> mediumTickHeightPx
                    TickType.Major -> majorTickHeightPx
                }
                val positionOnMainAxis = centerOnMainAxis + (index - currentIndex) * spacingPx

                when (orientation) {
                    PickerOrientation.Horizontal -> drawRect(
                        color = tickColor,
                        topLeft = Offset(
                            x = positionOnMainAxis - thicknessPx / 2f,
                            y = topPaddingPx
                        ),
                        size = Size(width = thicknessPx, height = tickHeightPx)
                    )
                    PickerOrientation.Vertical -> drawRect(
                        color = tickColor,
                        topLeft = Offset(
                            x = verticalTickEndX - tickHeightPx,
                            y = positionOnMainAxis - thicknessPx / 2f
                        ),
                        size = Size(width = tickHeightPx, height = thicknessPx)
                    )
                }

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
                    when (orientation) {
                        PickerOrientation.Horizontal -> {
                            val textCenter = Offset(
                                x = positionOnMainAxis,
                                y = labelTopY + labelHeightPx / 2f
                            )
                            val textTopLeft = Offset(
                                x = textCenter.x - textLayoutResult.size.width / 2f,
                                y = textCenter.y - textLayoutResult.size.height / 2f
                            )
                            if (contentRotation == PickerContentRotation.None) {
                                drawText(
                                    textLayoutResult = textLayoutResult,
                                    topLeft = textTopLeft
                                )
                            } else {
                                rotate(degrees = contentRotation.degrees, pivot = textCenter) {
                                    drawText(
                                        textLayoutResult = textLayoutResult,
                                        topLeft = textTopLeft
                                    )
                                }
                            }
                        }
                        PickerOrientation.Vertical -> {
                            val textCenter = if (contentRotation == PickerContentRotation.None) {
                                Offset(
                                    x = verticalLabelStartX + textLayoutResult.size.width / 2f,
                                    y = positionOnMainAxis
                                )
                            } else {
                                Offset(
                                    x = verticalLabelStartX + labelHeightPx / 2f,
                                    y = positionOnMainAxis
                                )
                            }
                            val textTopLeft = Offset(
                                x = textCenter.x - textLayoutResult.size.width / 2f,
                                y = textCenter.y - textLayoutResult.size.height / 2f
                            )
                            if (contentRotation == PickerContentRotation.None) {
                                drawText(
                                    textLayoutResult = textLayoutResult,
                                    topLeft = textTopLeft
                                )
                            } else {
                                rotate(degrees = contentRotation.degrees, pivot = textCenter) {
                                    drawText(
                                        textLayoutResult = textLayoutResult,
                                        topLeft = textTopLeft
                                    )
                                }
                            }
                        }
                    }
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

private fun verticalPickerWidth(
    tickStyle: TickStyle,
    labelStyle: LabelStyle,
    contentPadding: PaddingValues,
    layoutDirection: androidx.compose.ui.unit.LayoutDirection
): Dp {
    val labelSpace = if (labelStyle.enabled) labelStyle.topPadding + 20.dp else 0.dp
    return contentPadding.calculateStartPadding(layoutDirection) +
        tickStyle.majorHeight +
        labelSpace +
        contentPadding.calculateEndPadding(layoutDirection)
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

internal fun effectiveEdgeTapAnchorIndex(
    settledAnchorIndex: Int,
    currentIndexFloat: Float,
    isScrolling: Boolean,
    maxIndex: Int
): Int {
    if (maxIndex < 0) return 0
    return if (isScrolling) {
        currentIndexFloat.roundToInt().coerceIn(0, maxIndex)
    } else {
        settledAnchorIndex.coerceIn(0, maxIndex)
    }
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
    orientation: PickerOrientation,
    mainAxisSize: Float,
    crossAxisSize: Float,
    zoneSize: Float,
    overlayCrossAxisSize: Float
): Int {
    if (zoneSize <= 0f) return 0
    return when (orientation) {
        PickerOrientation.Horizontal -> {
            if (downPosition.y > overlayCrossAxisSize) return 0
            when {
                downPosition.x <= zoneSize -> -1
                downPosition.x >= mainAxisSize - zoneSize -> 1
                else -> 0
            }
        }
        PickerOrientation.Vertical -> {
            if (downPosition.x < crossAxisSize - overlayCrossAxisSize) return 0
            when {
                downPosition.y <= zoneSize -> -1
                downPosition.y >= mainAxisSize - zoneSize -> 1
                else -> 0
            }
        }
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
