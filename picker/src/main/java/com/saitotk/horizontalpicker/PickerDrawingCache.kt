package com.saitotk.horizontalpicker

/** Viewport-bounded storage reused across scroll frames; no per-tick drawing objects. */
internal class PickerTickBuffer(
    private val model: PickerModel,
    private val mainAxisSize: Float,
    private val spacingPx: Float
) {
    var positions = FloatArray(0)
        private set
    var values = FloatArray(0)
        private set
    var firstIndex = 0
        private set
    var count = 0
        private set
    private var cachedIndex = Float.NaN

    fun update(currentIndex: Float) {
        if (cachedIndex == currentIndex) return
        val visible = visibleTickIndices(currentIndex, mainAxisSize, spacingPx, model.lastIndex)
        count = (visible.last - visible.first + 1).coerceAtLeast(0)
        firstIndex = visible.first
        if (positions.size < count) {
            // Reserve the largest visible window, including partially visible edge ticks.
            val capacity = minOf(model.tickCount, kotlin.math.ceil(mainAxisSize / spacingPx).toInt() + 3)
            positions = FloatArray(capacity)
            values = FloatArray(capacity)
        }
        for (slot in 0 until count) {
            val index = firstIndex + slot
            positions[slot] = mainAxisSize / 2f + (index - currentIndex) * spacingPx
            values[slot] = model.indexToValue(index)
        }
        cachedIndex = currentIndex
    }
}
