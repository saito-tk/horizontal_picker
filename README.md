# Horizontal Picker for Jetpack Compose

Jetpack Compose 向けの **横スクロール式 Picker ライブラリ**です。  
中央の固定インジケータに目盛りを合わせて値を選択します。

- Module: `:picker`（ライブラリ）
- Sample: `:sample`（動作確認アプリ）
- minSdk: `21+`

## できること

- `LazyRow` ベースの高パフォーマンスな横 picker
- 中央インジケータ固定 + スナップ挙動
- `Float` / `Int` API
- 小 / 中 / 大目盛り（`mediumEvery`, `majorEvery`）
- ラベル表示・フォーマット
- インジケータ差し替え
- haptic フィードバック
- アクセシビリティ対応（stateDescription / 増減アクション）
- RTL 対応

## できないこと（意図的な非対応）

- 無限ホイール
- 非線形スケール（対数など）
- View システム版（Compose 専用）

## インストール

Maven Central に公開する場合の想定座標（雛形）:

```kotlin
dependencies {
    implementation("io.github.your-github-id:horizontal-picker:0.1.0")
}
```

ローカルで試す場合は `:picker` を参照:

```kotlin
dependencies {
    implementation(project(":picker"))
}
```

## 最小使用例（Float）

```kotlin
var value by rememberSaveable { mutableFloatStateOf(50f) }

HorizontalPicker(
    value = value,
    onValueChange = { value = it },
    valueRange = 0f..100f,
    step = 1f
)
```

## 最小使用例（Int）

```kotlin
var age by rememberSaveable { mutableIntStateOf(30) }

HorizontalPicker(
    value = age,
    onValueChange = { age = it },
    range = 0..120,
    step = 1
)
```

## 値更新モード（重要）

`valueChangeMode` で「いつ `onValueChange` するか」を選べます。

- `ValueChangeMode.Continuous`
  - 連続性重視。高速フリングでも 1 刻みで補間更新。
- `ValueChangeMode.AlignedContinuous`（デフォルト）
  - 中央線が目盛り中心を通過したタイミングで連続更新。
  - haptic と体感を揃えやすい。
- `ValueChangeMode.OnScrollFinished`
  - スクロール停止時に確定値だけ更新。

## haptic の仕様

- 目盛り中心が中央線を通過したタイミングで発火。
- 高速スクロール時も通過した tick を補間して発火。
- プログラム的なスクロール同期中（外部 state 反映）は発火抑制。

## カスタマイズ例

### 1. 目盛りとラベル

```kotlin
HorizontalPicker(
    value = price,
    onValueChange = { price = it },
    valueRange = 0f..1000f,
    step = 5f,
    tick = TickStyle(
        spacing = 10.dp,
        majorEvery = 10,
        mediumEvery = 5,
        majorHeight = 16.dp,
        mediumHeight = 8.dp,
        minorHeight = 4.dp
    ),
    label = LabelStyle(
        showEvery = 10,
        width = 64.dp,
        formatter = { "${it.toInt()} 円" }
    )
)
```

### 2. インジケータ差し替え

```kotlin
HorizontalPicker(
    value = value,
    onValueChange = { value = it },
    valueRange = 0f..100f,
    step = 1f,
    indicator = {
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .width(2.dp)
                .height(32.dp)
                .background(MaterialTheme.colorScheme.error)
        )
    }
)
```

### 3. Integer picker を軽量運用

```kotlin
HorizontalPicker(
    value = count,
    onValueChange = { count = it },
    range = 0..600,
    step = 1,
    valueChangeMode = ValueChangeMode.OnScrollFinished,
    haptics = null
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
    contentPadding: PaddingValues = PaddingValues(vertical = 12.dp),
    flingBehavior: FlingBehavior = PickerDefaults.SnapFlingBehavior,
    indicator: @Composable BoxScope.() -> Unit = { DefaultCenterIndicator() },
    tick: TickStyle = TickStyle(),
    label: LabelStyle = LabelStyle(),
    haptics: HapticFeedbackType? = HapticFeedbackType.TextHandleMove,
    enabled: Boolean = true,
    valueChangeMode: ValueChangeMode = ValueChangeMode.AlignedContinuous
)

@Composable
fun HorizontalPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    step: Int,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(vertical = 12.dp),
    flingBehavior: FlingBehavior = PickerDefaults.SnapFlingBehavior,
    indicator: @Composable BoxScope.() -> Unit = { DefaultCenterIndicator() },
    tick: TickStyle = TickStyle(),
    label: LabelStyle = LabelStyle(formatter = { it.roundToInt().toString() }),
    haptics: HapticFeedbackType? = HapticFeedbackType.TextHandleMove,
    enabled: Boolean = true,
    valueChangeMode: ValueChangeMode = ValueChangeMode.AlignedContinuous
)
```

## 設計メモ

- 1 tick = 1 step の線形モデル
- `value <-> index` 変換をライブラリ内で完結
- 左右パディングを自動補正し、端値も中央に合わせられる
- `LazyRow` の virtualization を使うため大量 tick でも破綻しにくい

## テスト

- Unit test: `:picker:test`
  - 変換ロジック（`valueToIndex`, `indexToValue`, `snapToStep`）
- UI test: `:picker:connectedAndroidTest`
  - 値変化 / スナップの最低限確認

## サンプル起動

```bash
./gradlew :sample:installDebug
```

## 公開準備（雛形）

`picker/build.gradle.kts` に以下を用意済み:

- `maven-publish`, `signing`
- sources/javadocs Jar
- POM メタデータ（要差し替え）

