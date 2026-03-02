package com.saitotk.horizontalpicker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import java.util.Locale
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.abs
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
    enabled: Boolean = true,
    valueChangeMode: ValueChangeMode = ValueChangeMode.AlignedContinuous
) {
    val model = remember(valueRange, step) { createPickerModel(valueRange, step) }
    val clampedValue = remember(value, model) { model.snapToStep(value) }
    val targetIndex = remember(clampedValue, model) { model.valueToIndex(clampedValue) }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = targetIndex)
    val resolvedFlingBehavior = if (flingBehavior === PickerDefaults.SnapFlingBehavior) {
        rememberSnapFlingBehavior(lazyListState = listState)
    } else {
        flingBehavior
    }

    val hapticFeedback = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var emittedIndex by remember(model) { mutableIntStateOf(targetIndex) }
    var hapticIndex by remember(model) { mutableIntStateOf(targetIndex) }
    var isProgrammaticScroll by remember(model) { mutableStateOf(false) }

    val selectedIndex by remember(listState, targetIndex) {
        derivedStateOf {
            findCenteredIndex(listState.layoutInfo) ?: targetIndex
        }
    }

    LaunchedEffect(targetIndex) {
        val currentCentered = findCenteredIndex(listState.layoutInfo)
        if (currentCentered == null) {
            isProgrammaticScroll = true
            listState.scrollToItem(targetIndex)
            emittedIndex = targetIndex
            hapticIndex = targetIndex
            isProgrammaticScroll = false
            return@LaunchedEffect
        }

        if (!listState.isScrollInProgress && currentCentered != targetIndex) {
            isProgrammaticScroll = true
            try {
                listState.animateScrollToItem(targetIndex)
            } finally {
                isProgrammaticScroll = false
            }
        }
    }

    LaunchedEffect(listState, enabled, valueChangeMode, haptics, isProgrammaticScroll) {
        var previousCenteredIndexFloat: Float? = null

        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            PickerSnapshot(
                centeredIndex = findCenteredIndex(layoutInfo),
                centeredIndexFloat = findCenteredIndexFloat(layoutInfo),
                alignedCenteredIndex = findAlignedCenteredIndex(layoutInfo),
                isScrolling = listState.isScrollInProgress,
                isProgrammaticScroll = isProgrammaticScroll
            )
        }
            .distinctUntilChanged()
            .collect { snapshot ->
            val centered = snapshot.centeredIndex ?: return@collect
            val centeredFloat = snapshot.centeredIndexFloat
            val previousFloat = previousCenteredIndexFloat
            previousCenteredIndexFloat = centeredFloat
            val crossedIndices = if (previousFloat != null && centeredFloat != null) {
                crossedAlignedIndices(previousFloat, centeredFloat, model.lastIndex)
            } else {
                emptyList()
            }

            val alignedCentered = snapshot.alignedCenteredIndex
            if (
                haptics != null &&
                enabled &&
                !snapshot.isProgrammaticScroll
            ) {
                if (crossedIndices.isNotEmpty()) {
                    for (index in crossedIndices) {
                        if (index != hapticIndex) {
                            hapticIndex = index
                            hapticFeedback.performHapticFeedback(haptics)
                        }
                    }
                } else if (alignedCentered != null && alignedCentered != hapticIndex) {
                    hapticIndex = alignedCentered
                    hapticFeedback.performHapticFeedback(haptics)
                }
            }

            if (snapshot.isProgrammaticScroll) return@collect

            when (valueChangeMode) {
                ValueChangeMode.Continuous -> {
                    // Keep 1-step continuity even when frames are skipped during fast fling.
                    for (index in steppedIndices(emittedIndex, centered)) {
                        emittedIndex = index
                        onValueChange(model.indexToValue(index))
                    }
                }
                ValueChangeMode.AlignedContinuous -> {
                    // Emit when center line crosses tick centers even if exact aligned frames are skipped.
                    for (index in crossedIndices) {
                        if (index != emittedIndex) {
                            emittedIndex = index
                            onValueChange(model.indexToValue(index))
                        }
                    }
                    if (crossedIndices.isEmpty() && alignedCentered != null && alignedCentered != emittedIndex) {
                        emittedIndex = alignedCentered
                        onValueChange(model.indexToValue(alignedCentered))
                    }
                }
                ValueChangeMode.OnScrollFinished -> {
                    if (!snapshot.isScrolling) {
                        val finalIndex = alignedCentered ?: centered
                        if (finalIndex != emittedIndex) {
                            emittedIndex = finalIndex
                            onValueChange(model.indexToValue(finalIndex))
                        }
                    }
                }
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

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .pickerSemantics(
                enabled = enabled,
                valueLabel = semanticsLabel,
                currentIndex = selectedIndex,
                maxIndex = model.lastIndex,
                onIncrease = {
                    scope.launch { listState.animateScrollToItem((selectedIndex + 1).coerceAtMost(model.lastIndex)) }
                },
                onDecrease = {
                    scope.launch { listState.animateScrollToItem((selectedIndex - 1).coerceAtLeast(0)) }
                }
            )
            .progressSemantics(
                value = reportedValue,
                valueRange = model.start..model.endInclusive,
                steps = (model.lastIndex - 1).coerceAtLeast(0)
            )
    ) {
        val density = LocalDensity.current
        val centerPadding = remember(constraints.maxWidth, tick.spacing, density) {
            with(density) {
                ((constraints.maxWidth.toDp() / 2) - (tick.spacing / 2)).coerceAtLeast(0.dp)
            }
        }
        val layoutDirection = LocalLayoutDirection.current
        val resolvedPadding = remember(contentPadding, centerPadding, layoutDirection) {
            resolveContentPadding(contentPadding, centerPadding, layoutDirection)
        }

        LazyRow(
            state = listState,
            userScrollEnabled = enabled,
            contentPadding = resolvedPadding,
            flingBehavior = resolvedFlingBehavior,
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeightIn(min = tick.majorHeight + if (label.enabled) label.topPadding + 20.dp else 0.dp)
        ) {
            items(model.tickCount, key = { it }) { index ->
                val tickValue = model.indexToValue(index)
                PickerTick(
                    index = index,
                    value = tickValue,
                    tickStyle = tick,
                    labelStyle = label
                )
            }
        }

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
    enabled: Boolean = true,
    valueChangeMode: ValueChangeMode = ValueChangeMode.AlignedContinuous
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
        enabled = enabled,
        valueChangeMode = valueChangeMode
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

/** Controls how and when [onValueChange] is called while scrolling. */
enum class ValueChangeMode {
    /**
     * Emits continuously while scrolling based on nearest centered tick.
     * Best for smooth 1-step continuity during fast flings.
     */
    Continuous,

    /**
     * Emits only when a tick is actually aligned with the center indicator.
     * Can feel less continuous at high velocity.
     */
    AlignedContinuous,

    OnScrollFinished
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
private fun PickerTick(
    index: Int,
    value: Float,
    tickStyle: TickStyle,
    labelStyle: LabelStyle
) {
    val tickType = remember(index, tickStyle.mediumEvery, tickStyle.majorEvery) {
        when {
            tickStyle.majorEvery > 0 && index % tickStyle.majorEvery == 0 -> TickType.Major
            tickStyle.mediumEvery > 0 && index % tickStyle.mediumEvery == 0 -> TickType.Medium
            else -> TickType.Minor
        }
    }

    val colorScheme = MaterialTheme.colorScheme
    val tickColor = when (tickType) {
        TickType.Minor -> tickStyle.minorColor.orFallback(colorScheme.outlineVariant)
        TickType.Medium -> tickStyle.mediumColor.orFallback(colorScheme.outline)
        TickType.Major -> tickStyle.majorColor.orFallback(colorScheme.onSurface)
    }

    val tickHeight = when (tickType) {
        TickType.Minor -> tickStyle.minorHeight
        TickType.Medium -> tickStyle.mediumHeight
        TickType.Major -> tickStyle.majorHeight
    }

    val showLabel = labelStyle.enabled && labelStyle.showEvery > 0 && index % labelStyle.showEvery == 0

    Column(
        modifier = Modifier.width(tickStyle.spacing),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(
            modifier = Modifier
                .width(tickStyle.thickness)
                .height(tickHeight)
        ) {
            drawRect(color = tickColor)
        }

        if (showLabel) {
            val textStyle = MaterialTheme.typography.labelSmall.merge(labelStyle.textStyle)
            Text(
                text = labelStyle.formatter(value),
                modifier = Modifier
                    .height(20.dp)
                    .requiredWidth(labelStyle.width),
                textAlign = TextAlign.Center,
                maxLines = 1,
                style = textStyle.copy(
                    color = labelStyle.color.orFallback(colorScheme.onSurfaceVariant)
                )
            )
        } else {
            Box(modifier = Modifier.height(labelStyle.topPadding + 20.dp))
        }
    }
}

private fun resolveContentPadding(
    base: PaddingValues,
    centerPadding: Dp,
    layoutDirection: LayoutDirection
): PaddingValues {
    val start = base.calculateStartPadding(layoutDirection) + centerPadding
    val end = base.calculateEndPadding(layoutDirection) + centerPadding

    return PaddingValues(
        start = start,
        top = base.calculateTopPadding(),
        end = end,
        bottom = base.calculateBottomPadding()
    )
}

private fun findCenteredIndex(layoutInfo: LazyListLayoutInfo): Int? {
    val visibleItems = layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty()) return null

    val center = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
    return visibleItems.minByOrNull { item ->
        abs((item.offset + item.size / 2f) - center)
    }?.index
}

private fun findCenteredIndexFloat(layoutInfo: LazyListLayoutInfo): Float? {
    val visibleItems = layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty()) return null

    val center = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
    val centeredItem = visibleItems.minByOrNull { item ->
        abs((item.offset + item.size / 2f) - center)
    } ?: return null

    if (centeredItem.size == 0) return centeredItem.index.toFloat()

    val centeredItemCenter = centeredItem.offset + centeredItem.size / 2f
    val offsetInItems = (center - centeredItemCenter) / centeredItem.size.toFloat()
    return centeredItem.index + offsetInItems
}

private fun findAlignedCenteredIndex(
    layoutInfo: LazyListLayoutInfo,
    alignmentTolerancePx: Float = 1f
): Int? {
    val visibleItems = layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty()) return null

    val center = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
    val centeredItem = visibleItems.minByOrNull { item ->
        abs((item.offset + item.size / 2f) - center)
    } ?: return null

    val distanceToCenter = abs((centeredItem.offset + centeredItem.size / 2f) - center)
    return if (distanceToCenter <= alignmentTolerancePx) centeredItem.index else null
}

private fun Color.orFallback(fallback: Color): Color {
    return if (this == Color.Unspecified) fallback else this
}

private fun steppedIndices(fromExclusive: Int, toInclusive: Int): IntProgression {
    if (toInclusive == fromExclusive) {
        return IntProgression.fromClosedRange(0, -1, 1)
    }
    val step = if (toInclusive > fromExclusive) 1 else -1
    return IntProgression.fromClosedRange(fromExclusive + step, toInclusive, step)
}

private fun crossedAlignedIndices(from: Float, to: Float, maxIndex: Int): List<Int> {
    if (from == to) return emptyList()

    return if (to > from) {
        val start = kotlin.math.floor(from).toInt() + 1
        val end = kotlin.math.floor(to).toInt()
        if (end < start) {
            emptyList()
        } else {
            (start..end).map { it.coerceIn(0, maxIndex) }
        }
    } else {
        val start = kotlin.math.ceil(from).toInt() - 1
        val end = kotlin.math.ceil(to).toInt()
        if (start < end) {
            emptyList()
        } else {
            (start downTo end).map { it.coerceIn(0, maxIndex) }
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
