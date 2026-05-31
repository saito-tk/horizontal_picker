# Horizontal Picker for Jetpack Compose

Jetpack Compose 向けの横スクロール式 Picker ライブラリです。中央の固定マーカーに目盛りを合わせて、`Float` または `Int` の値を選択できます。

- Compose 専用
- minSdk `21+`
- 横向き/縦向きの `Float` / `Int` API を提供
- スナップ、haptic、アクセシビリティ、RTL に対応
- `Canvas + scrollable` ベースの軽量描画実装

## デモ

サンプルアプリでの操作感です。

![Horizontal Picker demo](docs/assets/horizontal_picker_480p.gif)

## 導入

このリポジトリではライブラリ本体は `:picker` モジュールです。

```kotlin
dependencies {
    implementation(project(":picker"))
}
```

## 基本の使い方

### Float

```kotlin
var temperature by rememberSaveable { mutableFloatStateOf(36.5f) }

HorizontalPicker(
    value = temperature,                 // 現在選択中の値
    onValueChange = { temperature = it },  // 値変更時の反映先
    valueRange = 35f..42f,               // 選択可能な範囲
    step = 0.1f                          // 1 tick あたりの増減幅
)
```

### Int

```kotlin
var age by rememberSaveable { mutableIntStateOf(30) }

HorizontalPicker(
    value = age,                 // 現在選択中の値
    onValueChange = { age = it },  // 値変更時の反映先
    range = 0..120,              // 選択可能な範囲
    step = 1                     // 1 tick あたりの増減幅
)
```

### 縦向き表示

`HorizontalPicker` を `Modifier.rotate(90f)` で回転させると、見た目とスクロール入力の座標軸がずれてフリングが安定しません。縦向きで表示したい場合は `VerticalPicker` を使ってください。

```kotlin
VerticalPicker(
    value = weight,
    onValueChange = { weight = it },
    valueRange = 0f..100f,
    step = 0.5f,
    modifier = Modifier
        .fillMaxHeight()
        .width(120.dp)
)
```

Activity を portrait のまま固定し、端末を横向きに持って見る用途では、ラベルとデフォルト値バッジだけを回転できます。

```kotlin
VerticalPicker(
    value = weight,
    onValueChange = { weight = it },
    valueRange = 0f..100f,
    step = 0.5f,
    contentRotation = PickerContentRotation.Clockwise
)
```

## 挙動

- 1 tick = 1 step の線形 picker です。
- `value` はレンジ内に clamp され、最も近い step に snap されます。
- `onValueChange` は、中央線が目盛り中心を通過したタイミングで呼ばれます。
- 高速フリング時も、通過した step を補間して値更新と haptic を行います。
- 外部 state の反映で発生するプログラム的なスクロール中は haptic を抑制します。
- デフォルトでは速度に応じた移動量を持つスナップ付き fling を使います。
- 描画は `Canvas` ベースで、可視範囲の tick と label だけを毎フレーム描画します。

## カスタマイズ

以下の例では、`value` / `onValueChange` / `valueRange` または `range` / `step` の共通説明は省略し、カスタマイズ対象の引数にだけコメントを付けています。

### 目盛りとラベル

```kotlin
HorizontalPicker(
    value = price,
    onValueChange = { price = it },
    valueRange = 0f..1000f,
    step = 5f,
    tick = TickStyle(
        spacing = 10.dp,      // 各目盛りの横間隔
        majorEvery = 10,      // 10 tick ごとに大目盛り
        mediumEvery = 5,      // 5 tick ごとに中目盛り
        majorHeight = 16.dp,  // 大目盛りの高さ
        mediumHeight = 8.dp,  // 中目盛りの高さ
        minorHeight = 4.dp    // 小目盛りの高さ
    ),
    label = LabelStyle(
        showEvery = 10,                     // 10 tick ごとにラベル表示
        width = 64.dp,                      // ラベルの描画幅
        formatter = { "${it.toInt()} 円" }  // ラベル文字列の変換
    )
)
```

### センターマーカーと値バッジ

```kotlin
HorizontalPicker(
    value = amount,
    onValueChange = { amount = it },
    valueRange = 0f..100f,
    step = 1f,
    centerMarker = CenterMarkerStyle(
        color = MaterialTheme.colorScheme.error,  // 中央マーカーの色
        stemWidth = 2.dp,                         // 中央マーカーの幅
        stemHeight = 32.dp,                       // 中央マーカーの高さ
        showValueBadge = true                     // デフォルト値バッジを表示
    ),
    valueBadge = { valueText, color ->
        DefaultValueBadge(
            valueText = "$valueText kg",  // 表示文字列を差し替え
            color = color                 // マーカー色をそのまま利用
        )
    }
)
```

### 端タップで 1 step 移動

```kotlin
HorizontalPicker(
    value = count,
    onValueChange = { count = it },
    range = 0..600,
    step = 1,
    edgeTapZoneFraction = 0.3f  // 左右それぞれ 30% を端タップ領域にする
)
```

`edgeTapZoneFraction = 0f` のときは無効です。`0.1f..0.5f` を指定すると、マーカー上部の左右端をタップしたときに 1 step ずつ移動できます。

`edgeTapZoneFraction` は、コンポーネント全体の幅に対して左右それぞれ何割を端タップ領域にするかを表します。
`VerticalPicker` では高さに対する上下端の割合として扱われます。

- `0.1f` なら左 10% と右 10%
- `0.3f` なら左 30% と右 30%
- 判定されるのはマーカー上部のオーバーレイ領域内だけです
- ドラッグに入った場合は端タップとして扱われません
- 現状、端タップの長押しオートリピートはありません

## 主な引数

- `tick: TickStyle`
  目盛りの間隔、高さ、太さ、色、`mediumEvery`、`majorEvery` を指定します。`mediumEvery <= 0` または `majorEvery <= 0` にすると、その種別の目盛りは出ません。
- `label: LabelStyle`
  ラベルの有無、表示間隔、幅、文字色、フォーマッタを指定します。`enabled = false` または `showEvery <= 0` でラベルを非表示にできます。
- `centerMarker: CenterMarkerStyle`
  中央マーカーの色、幅、高さ、値バッジ表示を指定します。
- `valueBadge`
  選択中の値バッジを差し替えます。引数は整形済み文字列とマーカー色です。
- `contentRotation`
  `VerticalPicker` のラベルとデフォルト値バッジの向きを指定します。Activity を portrait 固定のまま端末横向きで見る場合は `Clockwise` または `CounterClockwise` を指定します。
- `haptics`
  `null` を渡すと haptic を無効化できます。
- `flingBehavior`
  デフォルトは速度に応じた移動量を持つスナップ挙動です。独自の `FlingBehavior` を渡せます。
- `edgeTapZoneFraction`
  `0f` で無効です。`0.1f..0.5f` を指定すると、`HorizontalPicker` は上部の左右端、`VerticalPicker` は右側の上下端をタップしたときに 1 step 移動できます。
- `enabled`
  `false` でスクロールと端タップを無効化します。

## 制約と注意点

- `Float` API の `step` は `> 0f`、`Int` API の `step` は `> 0` が必要です。
- `Float` API は `valueRange.start <= valueRange.endInclusive`、`Int` API は `range.first <= range.last` が必要です。
- 選択可能な値は `start + n * step` です。
- `valueRange` と `step` が割り切れない場合、上限ぴったりの値は選択できないことがあります。
  例: `0f..10f` と `step = 3f` の選択値は `0, 3, 6, 9` です。
- `edgeTapZoneFraction` は `0f` または `0.1f..0.5f` で指定します。
- デフォルトの値バッジ表示は `step` に応じて小数桁数を自動決定し、最大 6 桁まで表示します。
- tick 数が極端に多い構成は拒否されます。`Too many ticks. Reduce range size or increase step.` が出た場合は、レンジを狭めるか `step` を大きくしてください。

## 公開 API

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
    centerMarker: CenterMarkerStyle = CenterMarkerStyle(),
    valueBadge: @Composable BoxScope.(valueText: String, color: Color) -> Unit = { valueText, color ->
        DefaultValueBadge(valueText = valueText, color = color)
    },
    tick: TickStyle = TickStyle(),
    label: LabelStyle = LabelStyle(),
    haptics: HapticFeedbackType? = HapticFeedbackType.TextHandleMove,
    edgeTapZoneFraction: Float = 0f,
    enabled: Boolean = true
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
    centerMarker: CenterMarkerStyle = CenterMarkerStyle(),
    valueBadge: @Composable BoxScope.(valueText: String, color: Color) -> Unit = { valueText, color ->
        DefaultValueBadge(valueText = valueText, color = color)
    },
    tick: TickStyle = TickStyle(),
    label: LabelStyle = LabelStyle(formatter = { it.roundToInt().toString() }),
    haptics: HapticFeedbackType? = HapticFeedbackType.TextHandleMove,
    edgeTapZoneFraction: Float = 0f,
    enabled: Boolean = true
)

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
)

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
)

enum class PickerContentRotation {
    None,
    Clockwise,
    CounterClockwise
}
```
