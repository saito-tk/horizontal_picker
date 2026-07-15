# Horizontal / Vertical Picker for Jetpack Compose

Jetpack Compose 向けの目盛り式 Picker ライブラリです。中央の固定マーカーに目盛りを合わせて、`Float` または `Int` の値を選択できます。横向きは `HorizontalPicker`、縦向きは `VerticalPicker` を使います。

- Compose 専用
- minSdk `21+`
- 横向き/縦向きの `Float` / `Int` API を提供
- スナップ、haptic、アクセシビリティ(TalkBack のカスタムアクション/Adjustable ジェスチャー、無効状態通知)、日本語ローカライズ済み文字列に対応
- `Canvas + scrollable` ベースの軽量描画実装
- RTL レイアウトのミラーリングは未対応です(詳細は下記「制約と注意点」参照)

## デモ

サンプルアプリでの操作感です。

![Horizontal Picker demo](docs/assets/horizontal_picker_480p.gif)

## 導入

Maven Central から追加します。

```kotlin
dependencies {
    implementation("io.github.saito-tk:horizontal-picker:0.1.1")
}
```

このリポジトリを直接 checkout して開発する場合、ライブラリ本体は `:picker` モジュールです。

メンテナー向けの公開手順は [`docs/publishing.md`](docs/publishing.md) を参照してください。

## 基本の使い方

### HorizontalPicker / Float

```kotlin
var temperature by rememberSaveable { mutableFloatStateOf(36.5f) }

HorizontalPicker(
    value = temperature,
    onValueChange = { temperature = it },
    valueRange = 35f..42f,
    step = 0.1f
)
```

### HorizontalPicker / Int

```kotlin
var count by rememberSaveable { mutableIntStateOf(0) }

HorizontalPicker(
    value = count,
    onValueChange = { count = it },
    range = 0..600,
    step = 1
)
```

### VerticalPicker

`HorizontalPicker` を `Modifier.rotate(90f)` で回転させると、見た目とスクロール入力の座標軸がずれてフリングや端タップが安定しません。縦向きで表示したい場合は `VerticalPicker` を使います。

```kotlin
var count by rememberSaveable { mutableIntStateOf(0) }

VerticalPicker(
    value = count,
    onValueChange = { count = it },
    range = 0..600,
    step = 1,
    modifier = Modifier.fillMaxHeight()
)
```

`VerticalPicker` の幅は、目盛りとラベルに必要な幅から内部で決まります。通常は sample と同じく高さだけを指定し、外側から `.width(...)` を指定する必要はありません。`Clockwise` / `CounterClockwise` のときは、回転後のラベルの高さを考慮して幅を計算します。

Activity を portrait のまま固定し、端末を横向きに持って見る用途では、ラベルとデフォルト値バッジだけを回転できます。

```kotlin
HorizontalPicker(
    value = count,
    onValueChange = { count = it },
    range = 0..600,
    step = 1,
    modifier = Modifier.fillMaxWidth(),
    contentRotation = PickerContentRotation.UpsideDown
)

VerticalPicker(
    value = count,
    onValueChange = { count = it },
    range = 0..600,
    step = 1,
    modifier = Modifier.fillMaxHeight(),
    contentRotation = PickerContentRotation.Clockwise
)
```

`contentRotation` は、端末の持ち方に合わせて文字の読み向きだけを変えるための設定です。picker 本体を回転させる設定ではありません。

- `None`: 通常表示です。
- `Clockwise`: ラベルとデフォルト値バッジを時計回りに 90 度回転します。
- `CounterClockwise`: ラベルとデフォルト値バッジを反時計回りに 90 度回転します。
- `UpsideDown`: ラベルとデフォルト値バッジを 180 度回転します。

sample では、通常の `HorizontalPicker` / `VerticalPicker` に加えて、`HorizontalPicker` の `UpsideDown` 表示、`VerticalPicker` の `Clockwise` 表示、`VerticalPicker` の `CounterClockwise` 表示を確認できます。

## sample と同じ設定

sample の `VerticalPicker` は、`HorizontalPicker` と同じ範囲・step・目盛り・ラベル・センターマーカー・端タップ設定を使っています。

```kotlin
VerticalPicker(
    value = verticalValue,
    onValueChange = { verticalValue = it },
    range = 0..600,
    step = 1,
    modifier = Modifier.fillMaxHeight(),
    tick = TickStyle(majorEvery = 10, mediumEvery = 5),
    label = LabelStyle(showEvery = 10),
    centerMarker = CenterMarkerStyle(
        color = MaterialTheme.colorScheme.error
    ),
    contentRotation = PickerContentRotation.Clockwise,
    edgeTapZoneFraction = 0.3f
)
```

## 挙動

- 1 tick = 1 step の線形 picker です。
- `value` はレンジ内に clamp され、最も近い step に snap されます。
- `onValueChange` は、中央線が目盛り中心を通過したタイミングで呼ばれます。高速フリング時も、通過した step をすべて補間して値更新します。
- haptic は通過した step ごとに鳴りますが、1 フレームで複数 step を跨ぐ高速フリング中は 1 フレームにつき最大 1 回に間引かれます(値の更新自体は間引かれません)。
- 外部 state の反映で発生するプログラム的なスクロール中は haptic を抑制します。
- デフォルトでは速度に応じた移動量を持つスナップ付き fling を使います。
- 描画は `Canvas` ベースで、可視範囲の tick と label だけを毎フレーム描画します。
- `enabled = false` のとき、tick・ラベル・値バッジ・センターマーカーを半透明(alpha 0.38)で減光し、アクセシビリティサービスにも無効状態として通知します(TalkBack はこの picker を操作不可として読み上げます)。
- TalkBack など画面読み上げサービスの上下/左右スワイプ(Adjustable の標準ジェスチャー)でも値を変更できます。カスタムアクションの「増やす」「減らす」と併用できます。
- ラベルとデフォルト値バッジに使う領域は、システムのフォントサイズ設定(フォントスケール)に応じて拡大します。標準設定では従来と全く同じレイアウトです。
- コンテンツ説明・アクセシビリティアクション名は `strings.xml` 経由で提供され、`values-ja` で日本語に対応しています。
- tick/label の描画と `edgeTapZoneFraction` のタップ判定は絶対座標で処理されるため、RTL レイアウトでもミラーリングされません。

## HorizontalPicker と VerticalPicker の表示仕様

`HorizontalPicker` は横方向をスクロール軸として扱い、目盛りは上揃えで下向きに描画します。ラベルは目盛りの下に表示され、デフォルトのセンターマーカーと値バッジは上中央に配置されます。

`VerticalPicker` は縦方向をスクロール軸として扱い、目盛りは右揃えで左向きに描画します。目盛りの数字ラベルは目盛りの左側に表示され、デフォルトのセンターマーカーと現在値バッジは picker の右側に配置されます。現在値バッジは中央線の右側にあり、バッジ左端が中央線右端に少し重なる仕様です。

`contentRotation` はラベルとデフォルト値バッジにだけ適用されます。ジェスチャー軸、fling、端タップ判定、目盛り描画は各 picker の向きどおりに処理されます。

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
        spacing = 10.dp,      // 各目盛りの間隔
        thickness = 2.dp,     // 目盛り線の太さ
        majorEvery = 10,      // 10 tick ごとに大目盛り
        mediumEvery = 5,      // 5 tick ごとに中目盛り
        majorHeight = 16.dp,  // 大目盛りの長さ
        mediumHeight = 8.dp,  // 中目盛りの長さ
        minorHeight = 4.dp    // 小目盛りの長さ
    ),
    label = LabelStyle(
        showEvery = 10,
        width = 64.dp,
        formatter = { "${it.toInt()} 円" }
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
        color = MaterialTheme.colorScheme.error,
        stemWidth = 2.dp,
        stemHeight = 32.dp,
        showValueBadge = true
    ),
    valueBadge = { valueText, color ->
        DefaultValueBadge(
            valueText = "$valueText kg",
            color = color
        )
    }
)
```

`VerticalPicker` で値バッジを差し替える場合は、必要に応じて `DefaultVerticalValueBadge` を使います。

```kotlin
VerticalPicker(
    value = count,
    onValueChange = { count = it },
    range = 0..600,
    step = 1,
    contentRotation = PickerContentRotation.Clockwise,
    valueBadge = { valueText, color ->
        DefaultVerticalValueBadge(
            valueText = valueText,
            color = color,
            contentRotation = PickerContentRotation.Clockwise
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
    edgeTapZoneFraction = 0.3f
)
```

`edgeTapZoneFraction = 0f` のときは無効です。`0.1f..0.5f` を指定すると、端タップで 1 step ずつ移動できます。

- `HorizontalPicker` は上部オーバーレイの左右端を判定します。
- `VerticalPicker` は右側オーバーレイの上下端を判定します。
- `edgeTapZoneFraction` は、横向きでは幅、縦向きでは高さに対する端領域の割合です。
- `0.3f` なら左右または上下それぞれ 30% が端タップ領域です。
- ドラッグに入った場合は端タップとして扱われません。
- 現状、端タップの長押しオートリピートはありません。

端タップ領域はデフォルトでは見た目に何も表示されないため、存在に気づかれにくいという難点があります。`edgeTapIndicator` にスタイルを渡すと、判定領域と同じ位置・同じ範囲にシェブロン(`‹` `›` / `^` `v`)を表示できます。

```kotlin
HorizontalPicker(
    value = count,
    onValueChange = { count = it },
    range = 0..600,
    step = 1,
    edgeTapZoneFraction = 0.3f,
    edgeTapIndicator = EdgeTapIndicatorStyle(
        visible = true,
        color = MaterialTheme.colorScheme.outline,
        size = 10.dp,
        strokeWidth = 2.dp
    )
)
```

`edgeTapIndicator` を指定しない場合(デフォルトの `EdgeTapIndicatorStyle()`)は `visible = false` のため何も描画されず、既存コードの見た目・挙動は変わりません。

## 主な引数

- `tick: TickStyle`
  目盛りの間隔、太さ、長さ、色、`mediumEvery`、`majorEvery` を指定します。`mediumEvery <= 0` または `majorEvery <= 0` にすると、その種別の目盛りは出ません。
- `label: LabelStyle`
  ラベルの有無、表示間隔、余白、幅、文字色、フォーマッタを指定します。`enabled = false` または `showEvery <= 0` でラベルを非表示にできます。
- `centerMarker: CenterMarkerStyle`
  中央マーカーの色、幅、高さ、値バッジ表示を指定します。
- `valueBadge`
  選択中の値バッジを差し替えます。引数は整形済み文字列とマーカー色です。
- `contentRotation`
  ラベルとデフォルト値バッジの向きを指定します。Activity を portrait 固定のまま端末横向きで見る場合は `Clockwise`、`CounterClockwise`、`UpsideDown` を指定します。
- `haptics`
  `null` を渡すと haptic を無効化できます。
- `flingBehavior`
  デフォルトは速度に応じた移動量を持つスナップ挙動です。独自の `FlingBehavior` を渡せます。
- `edgeTapZoneFraction`
  `0f` で無効です。`0.1f..0.5f` を指定すると、`HorizontalPicker` は上部の左右端、`VerticalPicker` は右側の上下端をタップしたときに 1 step 移動できます。
- `edgeTapIndicator: EdgeTapIndicatorStyle`
  端タップ領域にシェブロンを表示するかどうか、色、大きさ、線幅を指定します。デフォルトは `visible = false`(非表示)です。
- `enabled`
  `false` でスクロールと端タップを無効化し、見た目を半透明(alpha 0.38)にして、アクセシビリティサービスへ無効状態を通知します。

## 制約と注意点

- `Float` API の `step` は `> 0f`、`Int` API の `step` は `> 0` が必要です。
- `Float` API は `valueRange.start <= valueRange.endInclusive`、`Int` API は `range.first <= range.last` が必要です。
- `Int` API の `range` は `-16,777,216..16,777,216` の範囲で指定してください。内部のスクロール座標が `Float` のため、この範囲を超える整数は正確に表せず、ライブラリは `IllegalArgumentException` を送出します。
- 選択可能な値は `start + n * step` です。
- `valueRange` と `step` が割り切れない場合、上限ぴったりの値は選択できないことがあります。
  例: `0f..10f` と `step = 3f` の選択値は `0, 3, 6, 9` です。
- この場合、TalkBack などに公開する最大値も、指定レンジの上限ではなく実際に選択できる最終値になります。
- `edgeTapZoneFraction` は `0f` または `0.1f..0.5f` で指定します。
- デフォルトの値バッジ表示はレンジの開始値と `step` に応じて小数桁数を自動決定し、最大 6 桁まで表示します。
- tick 数が極端に多い構成は拒否されます。`Too many ticks. Reduce range size or increase step.` が出た場合は、レンジを狭めるか `step` を大きくしてください。
- tick/label の描画と `edgeTapZoneFraction` のタップ判定は絶対座標です。RTL レイアウトでもミラーリングされないため、RTL 対応が必要な画面では利用側でレイアウト方向を考慮してください。

## 公開 API

主な公開APIは `picker/src/main/java/com/saitotk/horizontalpicker/Picker.kt` にあります。ライブラリ名と package は `horizontalpicker` のままですが、利用側は用途に応じて `HorizontalPicker` または `VerticalPicker` を呼び分けます。

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
    contentRotation: PickerContentRotation = PickerContentRotation.None,
    valueBadge: @Composable BoxScope.(valueText: String, color: Color) -> Unit = { valueText, color ->
        DefaultValueBadge(valueText = valueText, color = color, contentRotation = contentRotation)
    },
    tick: TickStyle = TickStyle(),
    label: LabelStyle = LabelStyle(),
    haptics: HapticFeedbackType? = HapticFeedbackType.TextHandleMove,
    edgeTapZoneFraction: Float = 0f,
    edgeTapIndicator: EdgeTapIndicatorStyle = EdgeTapIndicatorStyle(),
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
    contentRotation: PickerContentRotation = PickerContentRotation.None,
    valueBadge: @Composable BoxScope.(valueText: String, color: Color) -> Unit = { valueText, color ->
        DefaultValueBadge(valueText = valueText, color = color, contentRotation = contentRotation)
    },
    tick: TickStyle = TickStyle(),
    label: LabelStyle = LabelStyle(formatter = { it.roundToInt().toString() }),
    haptics: HapticFeedbackType? = HapticFeedbackType.TextHandleMove,
    edgeTapZoneFraction: Float = 0f,
    edgeTapIndicator: EdgeTapIndicatorStyle = EdgeTapIndicatorStyle(),
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
    edgeTapIndicator: EdgeTapIndicatorStyle = EdgeTapIndicatorStyle(),
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
    edgeTapIndicator: EdgeTapIndicatorStyle = EdgeTapIndicatorStyle(),
    enabled: Boolean = true
)

data class CenterMarkerStyle(
    val color: Color = Color.Unspecified,
    val stemWidth: Dp = 4.dp,
    val stemHeight: Dp = 26.dp,
    val showValueBadge: Boolean = true
)

data class EdgeTapIndicatorStyle(
    val visible: Boolean = false,
    val color: Color = Color.Unspecified,
    val size: Dp = 10.dp,
    val strokeWidth: Dp = 2.dp
)

enum class PickerContentRotation(val degrees: Float) {
    None(0f),
    Clockwise(90f),
    CounterClockwise(-90f),
    UpsideDown(180f)
}

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

data class LabelStyle(
    val enabled: Boolean = true,
    val showEvery: Int = 10,
    val topPadding: Dp = 8.dp,
    val width: Dp = 48.dp,
    val textStyle: TextStyle = TextStyle.Default,
    val color: Color = Color.Unspecified,
    val formatter: (Float) -> String = { ... }
)
```
