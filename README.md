# Skiko Layout

使用 [skiko](https://github.com/JetBrains/skiko) 静态布局绘图

可以非常方便的布局，不需要计算各个元素的大小和位置，类 jetpack compose 的形式

目前还是测试版，会有很多bug，所有api均可能发生变动

示例代码可在 [DrawPreview](src/test/kotlin/DrawPreview.kt) 和 [DrawTest](src/test/kotlin/DrawTest.kt) 中找到

有问题都可加群学习交流 QQ群：734922374

v0.0.2升级v0.0.3请看 [v0.0.3](https://github.com/Colter23/skiko-layout/releases/tag/v0.0.3)

## 使用
### Gradle
```kotlin
implementation("top.colter.skiko:skiko-layout:0.0.9")
```
### Maven
```xml
<dependency>
    <groupId>top.colter.skiko</groupId>
    <artifactId>skiko-layout</artifactId>
    <version>0.0.9</version>
</dependency>
```
## 单位
dp 默认与 px 1:1 转换

可通过 `Dp.factor` 调整整体转换比例

## 元素

### 布局元素
视图 `View` 元素最外层 最好指定宽或高    
列 `Column` 内部元素会自动向下排列    
行 `Row` 内部元素会自动向右排列    
盒子 `Box` 内部元素绝对定位    
宫格 `Grid` 宫格    

### 内容元素
图片 `Image`，支持透明度 `alpha`、整图模糊 `blur` 与渐变模糊 `gradientBlur`
文本 `Text` 纯文本元素，支持文字描边与文字阴影    
富文本 `RichText` 支持自定义样式与emoji图片    
画板 `Canvas` 支持自行绘制图形(v0.0.3)   

## 样式
`Modifier`    
宽度 `width`    
高度 `height`    
最小宽度 `minWidth`    
最小高度 `minHeight`    
最大宽度 `maxWidth`    
最大高度 `maxHeight`    
填充剩余宽度 `fillWidth`    
填充剩余高度 `fillHeight`    
继承父元素宽度 `fillMaxWidth`    
继承父元素高度 `fillMaxHeight`    
按比例继承父元素宽度 `fillRatioWidth`    
按比例继承父元素高度 `fillRatioHeight`    
内边距 `padding`    
外边距 `margin`    
比例内边距 `paddingRatio`    
比例外边距 `marginRatio`    
视觉宽度比例 `overflowRatioWidth`    
视觉高度比例 `overflowRatioHeight`    
视觉外扩 `bleed` / `bleedRatio` (v0.0.4)     
视觉偏移 `offset` / `offsetRatio` (v0.0.4)    
背景 `background`，支持背景图 `image`、背景图透明度 `imageAlpha`、背景图整图模糊 `imageBlur` 与背景图渐变模糊 `imageGradientBlur` (v0.0.4)
边框 `border`    
阴影 `shadows`   
圆角 `radius` (v0.0.4)    
比例圆角 `radiusRatio` (v0.0.4)    
圆形 `circle` (v0.0.4)    
形状 `shape` (v0.0.4)    

## 布局

<img src="docs/layout1.png" width="400" alt="样式1">  

```kotlin
// View视图，最外层。最好指定宽或高
View(
    file = testOutput.resolve("layout1.png"),
    // 样式
    modifier = Modifier()
        .width(1000.dp)
        .background(Color.makeRGB(255, 205, 204))
) {
    // Column 列布局
    Column(Modifier()
        .fillMaxWidth() // 继承父元素宽度
        .margin(horizontal = 20.dp, vertical = 30.dp)
        .padding(20.dp)
        .background(Color.WHITE)
        .border(3.dp, 15.dp, Color.WHITE.withAlpha(0.8f))
        .shadows(Shadow.ELEVATION_5)
    ) {
        Row(Modifier()
            .fillMaxWidth()
            .height(100.dp)
            .padding(10.dp)
            .background(Color.WHITE.withAlpha(0.5f))
            .border(3.dp, 15.dp, Color.WHITE)
            .shadows(Shadow.ELEVATION_3)
        ) {
            Box(Modifier().width(80.dp).fillMaxHeight()) {
                Box(Modifier()
                    .width(80.dp)
                    .height(80.dp)
                    .background(Color.CYAN.withAlpha(0.6f))
                    .border(3.dp, 40.dp)
                )
                Box(modifier = Modifier()
                    .width(20.dp)
                    .height(20.dp)
                    .background(Color.YELLOW)
                    .border(1.dp, 10.dp),
                    alignment = LayoutAlignment.RIGHT_BOTTOM
                )
            }
            Column(Modifier().fillWidth().fillMaxHeight().padding(10.dp)) {
                Box(Modifier().fillMaxWidth().fillHeight().background(Color.RED)) {
                    Box(Modifier().fillMaxWidth().fillMaxHeight()) {
                        Text(text = "测试测试测试", alignment = LayoutAlignment.RIGHT_TOP)
                    }
                }
                Box(Modifier()
                    .fillMaxWidth()
                    .fillHeight()
                    .margin(10.dp, 0.dp, 0.dp, 0.dp)
                    .background(Color.GREEN)
                ) {
                    Box(Modifier().fillMaxWidth().fillMaxHeight()) {
                        Text(text = "TEST TEST TEST")
                    }
                }
            }
            Box(Modifier().width(80.dp).fillMaxHeight()) {  }
        }
        Column(
            Modifier()
                .fillMaxWidth()
                .margin(top = 20.dp)
                .background(Color.WHITE.withAlpha(0.7f))
                .border(3.dp, 15.dp, Color.WHITE)
                .shadows(Shadow.ELEVATION_3)
        ) {
            Row(Modifier().fillMaxWidth().margin(10.dp)) {
                Text(
                    text = "啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊",
                    modifier = Modifier().fillWidth()
                )
                Text(
                    text = "哦哦哦哦哦哦哦哦哦哦哦哦哦哦哦哦哦哦哦哦哦哦哦哦哦哦哦哦哦哦哦哦哦哦哦哦哦哦哦哦哦哦哦哦哦",
                    modifier = Modifier().fillWidth()
                )
            }
            Box(Modifier().fillMaxWidth().margin(10.dp)) {
                Image(
                    image = loadTestImage("image", "bg1.jpg"),
                    ratio = 16/10f,
                    modifier = Modifier().border(3.dp, 15.dp, Color.RED)
                )
            }
        }
        RichParagraphTest(
            Modifier()
                .fillMaxWidth()
                .padding(20.dp)
                .margin(top = 20.dp)
                .background(Color.WHITE.withAlpha(0.7f))
                .border(3.dp, 15.dp, Color.WHITE)
                .shadows(Shadow.ELEVATION_3)
        )
    }
}
```

`paddingRatio` / `marginRatio` 以父级内容区为基准，适合容器固定时做等比例留白。
建议配合固定宽高或 `fillMax*` 使用，父级尺寸未确定时比例边距不会有稳定基准。

`overflowRatioWidth` / `overflowRatioHeight`、`bleedRatio`、`offsetRatio` 是视觉外溢语义。
它们只影响背景、边框、裁剪和子元素可用区域，不改变父元素自动测量、`Row` / `Grid` 占位和兄弟元素位置。
需要“超出父元素但不撑开父元素”时，优先使用这些 API，而不是负 `marginRatio` 或大于 1 的 `fillRatio*`。

```kotlin
Box(Modifier().width(300.dp)) {
    Box(
        Modifier()
            .fillMaxWidth()
            .overflowRatioWidth(1.2f)
            .background(Color.RED)
    )
}
```

`bleedRatio(left = 0.1f)` 表示向左额外绘制父级内容宽度的 10%，`offsetRatio(x = -0.1f)` 表示视觉上向左移动父级内容宽度的 10%。

## 图片与背景图

`Image` 支持通过 `alpha` 控制图片本身透明度，只影响图片像素，不影响背景、边框和阴影。

```kotlin
Image(
    image = avatar,
    alpha = 0.5f,
    modifier = Modifier()
        .width(160.dp)
        .height(160.dp)
        .background(Color.RED)
)
```

背景图通过 `Modifier.background(image = ...)` 绘制，`imageAlpha` 控制背景图透明度。背景图会先绘制，随后绘制渐变或纯色，因此可以用半透明颜色作为遮罩。

```kotlin
Box(
    Modifier()
        .fillMaxWidth()
        .height(320.dp)
        .background(
            image = bg,
            imageAlpha = 0.65f,
            color = Color.BLACK.withAlpha(0.35f)
        )
)
```

整张图片统一模糊使用 `blur` / `imageBlur`。它只做一次 cover 裁剪和一次 Skia 模糊，不会走渐变模糊的多档 sigma 合成。`blur` 和 `gradientBlur`、`imageBlur` 和 `imageGradientBlur` 不能同时设置。

```kotlin
Image(
    image = bg,
    blur = 16.dp
)

Box(
    Modifier()
        .fillMaxWidth()
        .height(320.dp)
        .background(
            image = bg,
            imageBlur = 16.dp,
            color = Color.BLACK.withAlpha(0.28f)
        )
)
```

图片渐变模糊通过 `GradientBlur` 控制，常用场景可以直接使用 `GradientBlur.edge(...)`，表示两侧模糊、中间清晰。`angle = 0f` 表示从左到右，`90f` 表示从上到下。

```kotlin
Image(
    image = bg,
    gradientBlur = GradientBlur.edge(
        maxBlur = 24.dp,
        angle = 0f,
        clearStart = 0.35f,
        clearEnd = 0.65f
    )
)

Box(
    Modifier()
        .fillMaxWidth()
        .height(320.dp)
        .background(
            image = bg,
            imageGradientBlur = GradientBlur.edge(24.dp, angle = 90f),
            color = Color.BLACK.withAlpha(0.28f)
        )
)
```

如果图片会反复使用，并且尺寸、裁剪和模糊效果基本不变，建议先预生成，再按普通图片绘制，运行时性能最好。

```kotlin
val uniformBlurred = bg.blurred(
    width = 1000,
    height = 500,
    blur = 16.dp
)

bg.writeBlurred(
    file = File("bg_blurred.png"),
    width = 1000,
    height = 500,
    blur = 16.dp
)

val blurred = bg.gradientBlurred(
    width = 1000,
    height = 500,
    blur = GradientBlur.edge(24.dp)
)

bg.writeGradientBlurred(
    file = File("bg_blurred.png"),
    width = 1000,
    height = 500,
    blur = GradientBlur.edge(24.dp)
)
```

## 测试与预览

- [DrawPreview](src/test/kotlin/DrawPreview.kt) 用来直接生成图片，运行 `main()` 就会写出预览图
- [DrawTest](src/test/kotlin/DrawTest.kt) 只做自动化回归和性能烟雾测试

## 宫格 阴影

内置了 Material Design 的阴影规范

<img src="docs/shadow.png" width="400" alt="宫格">  

```kotlin
View(
    file = testOutput.resolve("shadow.png"),
    modifier = Modifier()
        .width(1000.dp)
        .padding(60.dp)
        .background(Color.makeRGB(255, 205, 204))
) {
    Grid (
        maxLineCount = 4, 
        space = 40.dp, 
        modifier = Modifier().fillMaxWidth()
    ) {
        Shadow.elevations.forEachIndexed { index, shadows ->
            Box(Modifier()
                .background(Color.WHITE.withAlpha(0.6f))
                .border(2.dp, 20.dp)
                .shadows(shadows)
            ) {
                Text("${index + 1}", fontSize = 40.dp, alignment = LayoutAlignment.CENTER)
            }
        }
    }
}
```

## 文字

### 文字描边与阴影

`Text` 支持 `stroke` 和 `textShadows`。描边和阴影会自动预留视觉外扩，减少边缘被裁剪。

```kotlin
Text(
    text = "描边阴影",
    color = Color.WHITE,
    fontSize = 48.dp,
    stroke = TextStroke(
        width = 4.dp,
        color = Color.BLACK
    ),
    textShadows = listOf(
        TextShadow(
            offsetX = 6.dp,
            offsetY = 6.dp,
            blur = 3.dp,
            color = Color.BLACK.withAlpha(0.45f)
        )
    )
)
```

`TextShadow.blur` 会作为 Skia Paragraph 的 `blurSigma` 使用。富文本 `RichText` 可直接在 `TextStyle` 上使用 Skia 原生阴影，样式复制时会保留 `foreground`、`background` 和 `shadows`。

```kotlin
val style = TextStyle()
    .setColor(Color.RED)
    .setFontSize(32f)
    .addShadow(
        org.jetbrains.skia.paragraph.Shadow(
            Color.BLACK.withAlpha(0.4f),
            4f,
            4f,
            2.0
        )
    )
```

### 自适应字号富文本

`AutoSizeRichText` 会在最小字号和最大字号之间按步长生成候选，并对每个候选重新排版。
默认选择器会优先避开超过 `maxLinesCount` 的候选；当文本可以单行显示时使用较大字号，
多行时会综合行宽填充度和字号大小，减少右侧大块留白。

适合动态正文、标题等“字数少时放大、字数多时缩小”的场景。为了保证视觉结果稳定，
默认实现会全量测量候选字号，不做粗排/细排启发式搜索；如果更关注性能，可以适当调大
`fontSizeStep`。

```kotlin
AutoSizeRichText(
    minFontSize = 30.dp,
    maxFontSize = 48.dp,
    fontSizeStep = 0.5.dp,
    maxLinesCount = 8,
    modifier = Modifier().fillMaxWidth(),
) { fontSize ->
    val style = TextStyle()
        .setColor(Color.BLACK)
        .setFontSize(fontSize.px)
    RichParagraphBuilder(style)
        .addText("这是一段会根据内容和宽度自动选择字号的中文动态正文。")
        .build()
}
```

### 富文本构建器
```kotlin
fun Layout.RichParagraphTest(modifier: Modifier) {
    val emojiMap = loadAllTestImage("emoji")

    var currEmoji = "[阿库娅_不关我事]"
    fun randomEmoji(): String {
        currEmoji = emojiMap.keys.random()
        return currEmoji
    }

    val style = TextStyle().setColor(Color.BLACK).setFontSize(30.px).setFontFamily(Fonts.default.textTypeface!!.familyName)
    val paragraph = RichParagraphBuilder(style)
        .addText("文字混排测试")
        .addText("自定义文字样式", style.setColor(Color.RED).setFontSize(40.px))
        .addText("文字混排测试，测试自动换行。文字混排测试，测试自动换行。")
        .wrap()
        .addText("测试主动换行")
        .wrap()
        .addText("测试emoji混排")
        .addEmoji(randomEmoji(), emojiMap[currEmoji]!!)
        .addEmoji(randomEmoji(), emojiMap[currEmoji]!!)
        .addEmoji(randomEmoji(), emojiMap[currEmoji]!!)
        .addEmoji(randomEmoji(), emojiMap[currEmoji]!!)
        .addText("测试emoji混排")
        .wrap()
        .addText("测试emoji自定义样式")
        .addEmoji(randomEmoji(), emojiMap[currEmoji]!!, style.setFontSize(50.px))
        .addEmoji(randomEmoji(), emojiMap[currEmoji]!!, style.setFontSize(70.px))
        .addEmoji(randomEmoji(), emojiMap[currEmoji]!!, style.setFontSize(50.px))
        .addText("测试emoji自定义样式")
        .wrap()
        .addText("字体emoji")
        .addText("😍❤️🤣😁🙌", style.setFontSize(40.px).setFontFamily(Fonts.default.emojiTypeface!!.familyName))
        .build()

    Box(modifier) {
        RichText(paragraph)
    }
}
```

## TODO
- [x] 背景图片
- [x] 背景渐变色
- [x] 图片透明度
- [x] 图片整图模糊
- [x] 图片渐变模糊
- [x] 文字描边与文字阴影
- [ ] 主题
- [ ] ~~提取图片主题色, 使用 [Material Color Utilities](https://github.com/material-foundation/material-color-utilities)~~ (效果不佳，暂时放弃)
- [ ] more...
