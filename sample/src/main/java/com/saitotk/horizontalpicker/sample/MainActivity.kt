package com.saitotk.horizontalpicker.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.saitotk.horizontalpicker.CenterMarkerStyle
import com.saitotk.horizontalpicker.DefaultValueBadge
import com.saitotk.horizontalpicker.DefaultVerticalValueBadge
import com.saitotk.horizontalpicker.EdgeTapIndicatorStyle
import com.saitotk.horizontalpicker.HorizontalPicker
import com.saitotk.horizontalpicker.LabelStyle
import com.saitotk.horizontalpicker.PickerContentRotation
import com.saitotk.horizontalpicker.TickStyle
import com.saitotk.horizontalpicker.VerticalPicker
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SampleScreen()
                }
            }
        }
    }
}

@Composable
private fun SampleScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceContainerLow
                    )
                )
            ),
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        item { BitrateSection() }
        item { CountdownSection() }
        item { RotationVariantsSection() }
        item { EdgeTapIndicatorSection() }
        item { DisabledStateSection() }
        item { ThemedPriceSection() }
    }
}

/**
 * Float picker synced with an external text field, badge hidden (`showValueBadge = false`).
 * Pattern taken from a real app's bitrate setting: the picker and the text field are two views
 * onto the same state, and only one of them needs to show the exact value.
 */
@Composable
private fun BitrateSection() {
    var bitrateMbps by rememberSaveable { mutableFloatStateOf(8f) }
    var textValue by rememberSaveable { mutableStateOf(formatBitrate(8f)) }

    SectionTitle("1) Bitrate (external text field, no badge)")
    Text(
        text = "Selected: ${formatBitrate(bitrateMbps)} Mbps",
        modifier = Modifier.padding(horizontal = 20.dp),
        style = MaterialTheme.typography.titleMedium
    )
    OutlinedTextField(
        value = textValue,
        onValueChange = { input ->
            textValue = input
            val parsed = input.toFloatOrNull() ?: return@OutlinedTextField
            if (parsed in MIN_BITRATE_MBPS..MAX_BITRATE_MBPS) {
                bitrateMbps = snapBitrate(parsed)
            }
        },
        label = { Text("Mbps") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    )
    HorizontalPicker(
        value = bitrateMbps,
        onValueChange = { value ->
            bitrateMbps = value
            textValue = formatBitrate(value)
        },
        valueRange = MIN_BITRATE_MBPS..MAX_BITRATE_MBPS,
        step = 0.5f,
        modifier = Modifier.fillMaxWidth(),
        centerMarker = CenterMarkerStyle(
            color = MaterialTheme.colorScheme.error,
            stemHeight = 24.dp,
            showValueBadge = false
        ),
        tick = TickStyle(
            spacing = 8.dp,
            thickness = 2.dp,
            minorHeight = 6.dp,
            mediumHeight = 10.dp,
            majorHeight = 14.dp,
            mediumEvery = 5,
            majorEvery = 10
        ),
        label = LabelStyle(
            showEvery = 20,
            width = 48.dp,
            textStyle = MaterialTheme.typography.labelSmall,
            formatter = { formatBitrate(it) }
        ),
        edgeTapZoneFraction = 0.3f
    )
}

/**
 * Int [VerticalPicker] with a custom [DefaultVerticalValueBadge] that appends a unit suffix.
 * Pattern taken from a real app's countdown-seconds control.
 */
@Composable
private fun CountdownSection() {
    var seconds by rememberSaveable { mutableIntStateOf(30) }

    SectionTitle("2) Countdown seconds (custom badge, VerticalPicker)")
    Text(
        text = "Selected: ${seconds}秒",
        modifier = Modifier.padding(horizontal = 20.dp),
        style = MaterialTheme.typography.titleMedium
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        VerticalPicker(
            value = seconds,
            onValueChange = { seconds = it },
            range = 0..120,
            step = 1,
            modifier = Modifier.fillMaxHeight(),
            centerMarker = CenterMarkerStyle(
                color = MaterialTheme.colorScheme.tertiary,
                stemHeight = 28.dp
            ),
            contentRotation = PickerContentRotation.Clockwise,
            valueBadge = { valueText, color ->
                DefaultVerticalValueBadge(
                    valueText = "${valueText}秒",
                    color = color,
                    contentRotation = PickerContentRotation.Clockwise
                )
            },
            tick = TickStyle(majorEvery = 10, mediumEvery = 5),
            label = LabelStyle(showEvery = 10),
            edgeTapZoneFraction = 0.3f
        )
    }
}

/** [PickerContentRotation.UpsideDown] and [PickerContentRotation.CounterClockwise]. */
@Composable
private fun RotationVariantsSection() {
    var upsideDownValue by rememberSaveable { mutableIntStateOf(0) }
    var counterClockwiseValue by rememberSaveable { mutableIntStateOf(0) }

    SectionTitle("3) Rotation variants")
    Text(
        text = "HorizontalPicker upside down — Selected: $upsideDownValue",
        modifier = Modifier.padding(horizontal = 20.dp),
        style = MaterialTheme.typography.titleMedium
    )
    HorizontalPicker(
        value = upsideDownValue,
        onValueChange = { upsideDownValue = it },
        range = 0..600,
        step = 1,
        modifier = Modifier.fillMaxWidth(),
        tick = TickStyle(majorEvery = 10, mediumEvery = 5),
        label = LabelStyle(showEvery = 10),
        centerMarker = CenterMarkerStyle(color = MaterialTheme.colorScheme.error),
        contentRotation = PickerContentRotation.UpsideDown,
        edgeTapZoneFraction = 0.3f
    )
    Text(
        text = "VerticalPicker counter clockwise — Selected: $counterClockwiseValue",
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        style = MaterialTheme.typography.titleMedium
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        VerticalPicker(
            value = counterClockwiseValue,
            onValueChange = { counterClockwiseValue = it },
            range = 0..600,
            step = 1,
            modifier = Modifier.fillMaxHeight(),
            tick = TickStyle(majorEvery = 10, mediumEvery = 5),
            label = LabelStyle(showEvery = 10),
            centerMarker = CenterMarkerStyle(color = MaterialTheme.colorScheme.error),
            contentRotation = PickerContentRotation.CounterClockwise,
            edgeTapZoneFraction = 0.3f
        )
    }
}

/** `edgeTapIndicator` makes the tap-to-step-by-one zones at the edges visible. */
@Composable
private fun EdgeTapIndicatorSection() {
    var value by rememberSaveable { mutableIntStateOf(50) }

    SectionTitle("4) Edge-tap zone with visual indicator")
    Text(
        text = "Selected: $value",
        modifier = Modifier.padding(horizontal = 20.dp),
        style = MaterialTheme.typography.titleMedium
    )
    Text(
        text = "画面端をタップすると 1 step ずつ移動します。矢印はそのタップ領域を示しています。",
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        style = MaterialTheme.typography.bodyMedium
    )
    HorizontalPicker(
        value = value,
        onValueChange = { value = it },
        range = 0..100,
        step = 1,
        modifier = Modifier.fillMaxWidth(),
        tick = TickStyle(majorEvery = 10, mediumEvery = 5),
        label = LabelStyle(showEvery = 10),
        edgeTapZoneFraction = 0.3f,
        edgeTapIndicator = EdgeTapIndicatorStyle(
            visible = true,
            color = MaterialTheme.colorScheme.primary,
            size = 12.dp,
            strokeWidth = 2.5.dp
        )
    )
}

/** `enabled = false` dims the picker and reports it as disabled to accessibility services. */
@Composable
private fun DisabledStateSection() {
    var value by rememberSaveable { mutableIntStateOf(20) }
    var enabled by rememberSaveable { mutableStateOf(true) }

    SectionTitle("5) Disabled state")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "有効", style = MaterialTheme.typography.titleMedium)
        Switch(checked = enabled, onCheckedChange = { enabled = it })
    }
    HorizontalPicker(
        value = value,
        onValueChange = { value = it },
        range = 0..100,
        step = 1,
        modifier = Modifier.fillMaxWidth(),
        tick = TickStyle(majorEvery = 10, mediumEvery = 5),
        label = LabelStyle(showEvery = 10),
        enabled = enabled
    )
}

/** Fully themed tick/label/badge colors and a custom value badge with a currency suffix. */
@Composable
private fun ThemedPriceSection() {
    var price by rememberSaveable { mutableFloatStateOf(500f) }

    SectionTitle("6) Custom theming (price picker)")
    Text(
        text = "Selected: ¥${price.roundToInt()}",
        modifier = Modifier.padding(horizontal = 20.dp),
        style = MaterialTheme.typography.titleMedium
    )
    HorizontalPicker(
        value = price,
        onValueChange = { price = it },
        valueRange = 0f..2000f,
        step = 10f,
        modifier = Modifier.fillMaxWidth(),
        centerMarker = CenterMarkerStyle(
            color = MaterialTheme.colorScheme.secondary,
            stemWidth = 2.dp,
            stemHeight = 32.dp
        ),
        tick = TickStyle(
            spacing = 10.dp,
            majorEvery = 10,
            mediumEvery = 5,
            majorColor = MaterialTheme.colorScheme.primary,
            mediumColor = MaterialTheme.colorScheme.tertiary,
            minorColor = MaterialTheme.colorScheme.outlineVariant
        ),
        label = LabelStyle(
            showEvery = 10,
            width = 64.dp,
            formatter = { "${it.roundToInt()} 円" }
        ),
        valueBadge = { valueText, color ->
            DefaultValueBadge(valueText = "¥$valueText", color = color)
        }
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = 20.dp),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF1D3557)
    )
}

private const val MIN_BITRATE_MBPS = 1f
private const val MAX_BITRATE_MBPS = 50f

private fun snapBitrate(value: Float): Float {
    return (value / 0.5f).roundToInt() * 0.5f
}

private fun formatBitrate(value: Float): String {
    return String.format(Locale.US, "%.1f", value)
}
