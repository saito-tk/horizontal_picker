package com.saitotk.horizontalpicker

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Highlights ticks from the range start through [value], independently of the selected value.
 *
 * Read observable Compose state inside [value], e.g. `value = { elapsed.floatValue }`.
 * The provider is called only while drawing ticks; changing that state does not require picker
 * recomposition or layout. Keep it cheap and free of side effects. It uses the same units as the
 * picker (also for Int overloads: `value = { elapsed.intValue.toFloat() }`).
 *
 * Only ticks whose values are <= progress are highlighted; progress is not rounded to the nearest
 * step. Below the range none are highlighted, above the range all are. Non-finite progress
 * disables highlighting. Progress never changes selection, callbacks, haptics or scrolling.
 * Labels, the selected-value badge and accessibility selection remain unchanged.
 *
 * [Color.Unspecified] brightens each tick's normal color toward white by 35%, preserving alpha.
 * Set [color] explicitly to suit your theme. Pass `progress = null` to disable this feature.
 */
@Immutable
data class PickerProgress(
    val value: () -> Float,
    val color: Color = Color.Unspecified
)

internal fun isTickReached(tickValue: Float, progressValue: Float): Boolean =
    progressValue.isFinite() && tickValue <= progressValue
