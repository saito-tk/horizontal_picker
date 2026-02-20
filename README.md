# Horizontal Picker (Jetpack Compose)

A reusable Jetpack Compose horizontal picker library where users scroll ticks and the value under a fixed center indicator is selected.

## Modules

- `:picker` - reusable Compose UI library.
- `:sample` - sample Android app with multiple usage patterns.

## Features

- LazyRow-based horizontal picker.
- Fixed center indicator.
- Snap-to-nearest tick behavior.
- Float and Int APIs.
- Tick hierarchy (minor / medium / major).
- Optional labels with formatter.
- Optional haptic feedback when crossing ticks.
- Accessibility semantics with current value and custom increment/decrement actions.
- RTL-safe layout using start/end paddings.

## Non-goals

- Infinite wheel behavior.
- Arbitrary non-linear scales (linear stepped values only).
- View-based implementation (Compose only).

## Installation

```kotlin
dependencies {
    implementation("io.github.your-github-id:horizontal-picker:0.1.0")
}
```

## Basic usage

```kotlin
var speed by rememberSaveable { mutableFloatStateOf(50f) }

HorizontalPicker(
    value = speed,
    onValueChange = { speed = it },
    valueRange = 0f..200f,
    step = 1f
)
```

## Int overload

```kotlin
var age by rememberSaveable { mutableIntStateOf(30) }

HorizontalPicker(
    value = age,
    onValueChange = { age = it },
    range = 0..120,
    step = 1
)
```

## Custom indicator and label formatter

```kotlin
HorizontalPicker(
    value = price,
    onValueChange = { price = it },
    valueRange = 0f..1000f,
    step = 5f,
    indicator = {
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .width(2.dp)
                .height(48.dp)
                .background(MaterialTheme.colorScheme.error)
        )
    },
    label = LabelStyle(
        showEvery = 10,
        formatter = { "${it.toInt()}$" }
    )
)
```

## API

```kotlin
@Composable
fun HorizontalPicker(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    step: Float,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = ...,
    flingBehavior: FlingBehavior = snapping,
    indicator: @Composable BoxScope.() -> Unit = defaultCenterIndicator,
    tick: TickStyle = TickStyle(...),
    label: LabelStyle = LabelStyle(...),
    haptics: HapticFeedbackType? = ...,
    enabled: Boolean = true,
)
```

## Design notes

- Scale: one tick equals one `step`.
- Centering: picker adds automatic start/end center paddings so the first/last ticks can align with center indicator.
- Conversion: internal model maps `value <-> index` and uses canonical stepped values to avoid drift.
- Snap: default fling behavior resolves to `SnapFlingBehavior` per `LazyListState`.
- Performance: uses `LazyRow(items(count))` to virtualize large ranges (`0..10_000 step 1`), and derives centered index from layout info.

## Testing

- Unit tests (`:picker:test`): value/index conversion and snapping.
- UI tests (`:picker:connectedAndroidTest`): swipe updates value and final value snaps to step.

## Maven Central publishing template

`picker/build.gradle.kts` already includes:

- `maven-publish` + `signing` plugins.
- Release publication with sources/javadocs jars.
- Placeholder POM metadata to replace before publishing.

## Run sample

```bash
./gradlew :sample:installDebug
```
