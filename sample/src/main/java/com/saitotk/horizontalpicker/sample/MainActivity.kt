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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saitotk.horizontalpicker.CenterMarkerStyle
import com.saitotk.horizontalpicker.HorizontalPicker
import com.saitotk.horizontalpicker.LabelStyle
import com.saitotk.horizontalpicker.PickerContentRotation
import com.saitotk.horizontalpicker.TickStyle
import com.saitotk.horizontalpicker.VerticalPicker
import java.util.Locale

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
    var integerValue by rememberSaveable { mutableIntStateOf(0) }
    var floatValue by rememberSaveable { mutableFloatStateOf(37.5f) }
    var customValue by rememberSaveable { mutableFloatStateOf(500f) }
    var verticalValue by rememberSaveable { mutableIntStateOf(0) }

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
        item {
            SectionTitle("1) Integer picker")
            Text(
                text = "Selected: $integerValue",
                modifier = Modifier.padding(horizontal = 20.dp),
                style = MaterialTheme.typography.titleMedium
            )
            HorizontalPicker(
                value = integerValue,
                onValueChange = { integerValue = it },
                range = 0..600,
                step = 1,
                modifier = Modifier.fillMaxWidth(),
                tick = TickStyle(majorEvery = 10, mediumEvery = 5),
                label = LabelStyle(showEvery = 10),
                centerMarker = CenterMarkerStyle(
                    color = MaterialTheme.colorScheme.error,
                ),
                edgeTapZoneFraction = 0.3f   // 上部の左右 30% を端タップ領域にする
            )
        }

        item {
            SectionTitle("2) Vertical picker")
            Text(
                text = "Selected: $verticalValue",
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
                    value = verticalValue,
                    onValueChange = { verticalValue = it },
                    range = 0..600,
                    step = 1,
                    modifier = Modifier
                        .fillMaxHeight(),
                    tick = TickStyle(majorEvery = 10, mediumEvery = 5),
                    label = LabelStyle(showEvery = 10),
                    centerMarker = CenterMarkerStyle(
                        color = MaterialTheme.colorScheme.error,
                    ),
                    contentRotation = PickerContentRotation.Clockwise,
                    edgeTapZoneFraction = 0.3f
                )
            }
        }

//        SectionTitle("2) Float picker with labels")
//        Text(
//            text = "Selected: ${String.format(Locale.US, "%.1f", floatValue)}",
//            modifier = Modifier.padding(horizontal = 20.dp),
//            style = MaterialTheme.typography.titleMedium
//        )
//        HorizontalPicker(
//            value = floatValue,
//            onValueChange = { floatValue = it },
//            valueRange = 0f..100f,
//            step = 0.5f,
//            modifier = Modifier.fillMaxWidth(),
//            contentPadding = PaddingValues(vertical = 8.dp),
//            tick = TickStyle(
//                spacing = 10.dp,
//                majorEvery = 20,
//                mediumEvery = 10,
//                majorHeight = 36.dp,
//                mediumHeight = 26.dp,
//                minorHeight = 16.dp
//            ),
//            label = LabelStyle(
//                showEvery = 20,
//                formatter = { value -> String.format(Locale.US, "%.0f", value) }
//            ),
//            haptics = HapticFeedbackType.TextHandleMove
//        )
//
//        SectionTitle("3) Custom indicator + wide range")
//        Text(
//            text = "Selected: ${customValue.toInt()}",
//            modifier = Modifier.padding(horizontal = 20.dp),
//            style = MaterialTheme.typography.titleMedium
//        )
//        HorizontalPicker(
//            value = customValue,
//            onValueChange = { customValue = it },
//            valueRange = 0f..10_000f,
//            step = 1f,
//            modifier = Modifier.fillMaxWidth(),
//            tick = TickStyle(
//                spacing = 8.dp,
//                majorEvery = 100,
//                mediumEvery = 50,
//                majorColor = MaterialTheme.colorScheme.primary,
//                mediumColor = MaterialTheme.colorScheme.tertiary,
//                minorColor = MaterialTheme.colorScheme.outlineVariant
//            ),
//            label = LabelStyle(
//                showEvery = 100,
//                formatter = { it.toInt().toString() }
//            ),
//            centerMarker = CenterMarkerStyle(
//                color = MaterialTheme.colorScheme.error,
//                stemHeight = 56.dp
//            )
//        )
    }
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
