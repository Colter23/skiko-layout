import org.jetbrains.skia.*
import org.jetbrains.skia.paragraph.ParagraphBuilder
import org.jetbrains.skia.paragraph.ParagraphStyle
import org.jetbrains.skia.paragraph.TextStyle
import top.colter.skiko.*
import top.colter.skiko.layout.Image
import top.colter.skiko.layout.RichText
import top.colter.skiko.layout.Text
import top.colter.skiko.data.*
import top.colter.skiko.layout.*
import java.io.File
import java.io.FileFilter


private val previewResource = File("src/test/resources")
private val previewOutput = previewResource.resolve("output").apply {
    if (!exists()) mkdirs()
}

private fun loadPreviewResource(path: String = "", fileName: String) =
    previewResource.resolve(path).resolve(fileName)

private fun loadPreviewImage(path: String = "", fileName: String) =
    Image.makeFromEncoded(loadPreviewResource(path, fileName).readBytes())

private fun loadPreviewAllImages(path: String): Map<String, Image> {
    val imageMap = mutableMapOf<String, Image>()
    val dir = previewResource.resolve(path)
    val imgExt = listOf("png", "jpg", "jpeg", "webp")
    if (dir.exists() && dir.isDirectory) {
        dir.listFiles(FileFilter {
            it.extension.lowercase() in imgExt
        })?.forEach {
            imageMap[it.nameWithoutExtension] = Image.makeFromEncoded(it.readBytes())
        }
    }
    return imageMap
}

private fun initPreview() {
    Dp.factor = 1f

    Fonts.default.loadTextTypeface(loadPreviewResource("font", "LXGWWenKai-Bold.ttf").absolutePath)
//    Fonts.default.loadEmojiTypeface(loadPreviewResource("font", "Seguiemj.ttf").absolutePath)
    Fonts.default.loadEmojiTypeface(loadPreviewResource("font", "NotoColorEmoji.ttf").absolutePath)
}

fun main() {
    initPreview()
//    generateParagraph()
//    generateViewLayout1()
//    generateEdgeRatio()
//    generateVisualOverflow()
//    generateMixTypeset()
//    generateGrid()
//    generateShadow()
//    generateBackgroundImage()
//    generateImageAlphaPreview()
    generateTextEffectPreview()
//    generateCanvas()
//    generateText1()
}

private fun generateParagraph() {
    val textStyle = TextStyle().setColor(Color.WHITE).setFontSize(30f).setFontFamily(Fonts.default.textTypeface!!.familyName)
    val style = ParagraphStyle()
    val builder = ParagraphBuilder(style, Fonts.default.fonts)
        .pushStyle(textStyle.setFontFamily(Fonts.default.emojiTypeface!!.familyName))
        .addText("❤️😍")
        .pushStyle(textStyle.setColor(Color.GREEN).setFontSize(20f).setFontFamily(Fonts.default.textTypeface!!.familyName))
        .addText("Hello")
        .pushStyle(textStyle.setColor(Color.RED).setFontSize(30f))
        .addText(" World")
        .pushStyle(textStyle.setColor(Color.YELLOW).setFontSize(40f))
        .addText(" Test")
    val paragraph = builder.build()

    val surface = Surface.makeRasterN32Premul(500, 500)
    val canvas = surface.canvas
    paragraph.layout(400f).paint(canvas, 50f, 50f)
    File("${previewOutput.absolutePath}/test1.png").writeBytes(surface.makeImageSnapshot().encodeToData()!!.bytes)
    println("generated test1.png")
}

private fun generateViewLayout1() {
    View(
        file = previewOutput.resolve("layout1.png"),
        modifier = Modifier()
            .width(1000.dp)
            .background(Color.makeRGB(204, 218, 255))
    ) {
        Column(Modifier()
            .fillMaxWidth()
            .margin(horizontal = 20.dp, vertical = 30.dp)
            .padding(20.dp)
            .background(Color.WHITE.withAlpha(0.8f))
            .border(3.dp, 15.dp, Color.WHITE)
            .shadows(Shadow.ELEVATION_5)
        ) {
            Row(Modifier()
                .fillMaxWidth()
                .height(100.dp)
                .padding(10.dp)
                .background(Color.WHITE.withAlpha(0.5f))
                .border(3.dp, 15.dp, Color.WHITE)
                .shadows(Shadow.ELEVATION_2)
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
                            Text(text = "测试文字右对齐", fontSize = 22.dp, alignment = LayoutAlignment.RIGHT_TOP)
                        }
                    }
                    Box(Modifier()
                        .fillMaxWidth()
                        .fillHeight()
                        .margin(10.dp, 0.dp, 0.dp, 0.dp)
                        .background(Color.GREEN)
                    ) {
                        Box(Modifier().fillMaxWidth().fillMaxHeight()) {
                            Text(text = "测试文字左对齐", fontSize = 22.dp)
                        }
                    }
                }
                Box(Modifier().width(80.dp).fillMaxHeight()) { }
            }
            Column(
                Modifier()
                    .fillMaxWidth()
                    .margin(top = 20.dp)
                    .background(Color.WHITE.withAlpha(0.7f))
                    .border(3.dp, 15.dp, Color.WHITE)
                    .shadows(Shadow.ELEVATION_2)
            ) {
                Row(Modifier().fillMaxWidth().margin(10.dp)) {
                    Text(
                        text = "文字并排测试".repeat(8),
                        fontSize = 22.dp,
                        modifier = Modifier().fillWidth()
                    )
                    Text(
                        text = "文字超出自动隐藏".repeat(6),
                        fontSize = 22.dp,
                        modifier = Modifier().fillWidth()
                    )
                }
                Grid(maxLineCount = 3, modifier = Modifier().fillMaxWidth().margin(10.dp)) {
                    Image(
                        image = loadPreviewImage("image", "bg1.jpg"),
                        modifier = Modifier().background(Color.RED.withAlpha(0.6f)).border(2.dp, 10.dp).shadows(Shadow.ELEVATION_1)
                    )
                    Image(
                        image = loadPreviewImage("image", "bg1.jpg"),
                        modifier = Modifier().background(Color.RED.withAlpha(0.6f)).border(2.dp, 10.dp).shadows(Shadow.ELEVATION_1)
                    )
                    Image(
                        image = loadPreviewImage("image", "bg1.jpg"),
                        modifier = Modifier().background(Color.RED.withAlpha(0.6f)).border(2.dp, 10.dp).shadows(Shadow.ELEVATION_1)
                    )
                }
            }
            richParagraphPreview(
                Modifier()
                    .fillMaxWidth()
                    .padding(20.dp)
                    .margin(top = 20.dp)
                    .background(Color.WHITE.withAlpha(0.7f))
                    .border(3.dp, 15.dp, Color.WHITE)
                    .shadows(Shadow.ELEVATION_2)
            )
        }
    }
    println("generated layout1.png")
}

private fun generateEdgeRatio() {
    View(
        file = previewOutput.resolve("edge_ratio.png"),
        modifier = Modifier()
            .width(1000.dp)
            .height(600.dp)
            .background(Color.makeRGB(233, 241, 255))
    ) {
        Box(
            Modifier()
                .fillMaxWidth()
                .fillMaxHeight()
                .paddingRatio(horizontal = 0.08f, vertical = 0.1f)
                .background(Color.WHITE.withAlpha(0.72f))
                .border(3.dp, 18.dp, Color.YELLOW)
        ) {
            Box(
                Modifier()
                    .width(220.dp)
                    .height(140.dp)
                    .marginRatio(horizontal = 0.06f, vertical = 0.08f)
                    .background(Color.RED.withAlpha(0.7f))
                    .border(5.dp, 14.dp, Color.BLUE)
            )
            Box(
                Modifier()
                    .width(220.dp)
                    .height(140.dp)
                    .marginRatio(horizontal = 0.18f, vertical = 0.24f)
                    .background(Color.BLUE.withAlpha(0.65f))
                    .border(5.dp, 14.dp, Color.GREEN),
                alignment = LayoutAlignment.RIGHT_BOTTOM
            )
        }
    }
    println("generated edge_ratio.png")
}

private fun generateVisualOverflow() {
    View(
        file = previewOutput.resolve("visual_overflow.png"),
        modifier = Modifier()
            .width(1000.dp)
            .height(560.dp)
            .padding(60.dp)
            .background(Color.makeRGB(238, 242, 247))
    ) {
        Column(Modifier().fillMaxWidth().fillMaxHeight()) {
            Box(
                Modifier()
                    .fillMaxWidth()
                    .height(130.dp)
                    .margin(bottom = 36.dp)
                    .background(Color.WHITE)
                    .border(2.dp, 18.dp, Color.makeRGB(190, 200, 220))
            ) {
                Box(
                    Modifier()
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .overflowRatioWidth(1.16f)
                        .background(Color.RED.withAlpha(0.72f))
                        .border(4.dp, 26.dp, Color.WHITE)
                )
            }

            Row(Modifier().fillMaxWidth().height(130.dp).margin(bottom = 36.dp)) {
                Box(
                    Modifier()
                        .width(280.dp)
                        .fillMaxHeight()
                        .margin(right = 40.dp)
                        .background(Color.WHITE)
                        .border(2.dp, 18.dp, Color.makeRGB(190, 200, 220))
                ) {
                    Box(
                        Modifier()
                            .width(120.dp)
                            .height(70.dp)
                            .bleedRatio(left = 0.16f, right = 0.08f, top = 0.12f)
                            .background(Color.GREEN.withAlpha(0.78f))
                            .border(4.dp, 18.dp, Color.WHITE)
                    )
                }

                Box(
                    Modifier()
                        .fillWidth()
                        .fillMaxHeight()
                        .background(Color.WHITE)
                        .border(2.dp, 18.dp, Color.makeRGB(190, 200, 220))
                ) {
                    Box(
                        Modifier()
                            .width(180.dp)
                            .height(80.dp)
                            .offsetRatio(x = -0.12f)
                            .background(Color.BLUE.withAlpha(0.72f))
                            .border(4.dp, 20.dp, Color.WHITE)
                    )
                }
            }

            Box(
                Modifier()
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(Color.WHITE)
                    .border(2.dp, 18.dp, Color.makeRGB(190, 200, 220))
            ) {
                Box(
                    Modifier()
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(18.dp)
                        .overflowRatioWidth(1.1f)
                        .background(Color.makeRGB(255, 194, 71).withAlpha(0.8f))
                        .border(4.dp, 24.dp, Color.WHITE)
                ) {
                    Box(
                        Modifier()
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .circle()
                            .background(Color.makeRGB(58, 172, 138))
                    )
                }
            }
        }
    }
    println("generated visual_overflow.png")
}

private fun Layout.richParagraphPreview(modifier: Modifier) {
    val emojiMap = loadPreviewAllImages("emoji")

    var currEmoji = "[tv_doge]"
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

private fun generateMixTypeset() {
    View(
        file = previewOutput.resolve("typeset1.png"),
        modifier = Modifier()
            .width(1000.dp)
            .padding(30.dp)
            .background(Color.makeRGB(255, 205, 204))
    ) {
        richParagraphPreview(
            Modifier()
                .fillMaxWidth()
                .padding(20.dp)
                .background(Color.WHITE.withAlpha(0.7f))
                .border(3.dp, 15.dp, Color.WHITE)
                .shadows(Shadow.ELEVATION_4)
        )
    }
    println("generated typeset1.png")
}

private fun generateGrid() {
    val imgMap = loadPreviewAllImages("image")
    val imgList = imgMap.values

    View(
        file = previewOutput.resolve("grid1.png"),
        modifier = Modifier()
            .height(1000.dp)
            .padding(30.dp)
            .background(Color.makeRGB(255, 205, 204))
    ) {
        Row(Modifier().fillMaxHeight()) {
            val modifier = Modifier()
                .fillMaxHeight()
                .padding(20.dp)
                .margin(horizontal = 20.dp)
                .background(Color.WHITE.withAlpha(0.7f))
                .border(3.dp, 15.dp, Color.WHITE)
                .shadow(6.px, 6.px, 25.px, 0.px, Color.makeARGB(70, 0, 0, 0))

            Grid(maxLineCount = 3, modifier = modifier) {
                val boxModifier = Modifier().background(Color.RED.withAlpha(0.6f)).border(2.dp, 10.dp).shadows(Shadow.ELEVATION_1)
                for (i in 1..9) Box(boxModifier)
            }

            Grid(maxLineCount = 3, modifier = modifier) {
                val imgModifier = Modifier().background(Color.RED.withAlpha(0.6f)).border(2.dp, 10.dp).shadows(Shadow.ELEVATION_1)
                for (i in 1..9) Image(imgList.random(), modifier = imgModifier)
            }
        }
    }
    println("generated grid1.png")
}

private fun generateShadow() {
    View(
        file = previewOutput.resolve("shadow.png"),
        modifier = Modifier()
            .width(1000.dp)
            .padding(60.dp)
            .background(Color.makeRGB(255, 205, 204))
    ) {
        Grid(
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
    println("generated shadow.png")
}

private fun generateBackgroundImage() {
    val bg1 = loadPreviewImage("image", "bg1.jpg")

    View(
        file = previewOutput.resolve("background.png"),
        modifier = Modifier()
            .width(1000.dp)
            .padding(20.dp)
            .background(Color.makeRGB(255, 205, 204))
    ) {
        Box(
            modifier = Modifier()
                .fillMaxWidth()
                .height(500.dp)
//                    .background(color = Color.BLACK.withAlpha(0.3f), image = bg1)
                .background(gradient = top.colter.skiko.data.Gradient(LayoutAlignment.LEFT_TOP, LayoutAlignment.RIGHT_BOTTOM, listOf(Color.BLACK.withAlpha(1f), Color.BLACK.withAlpha(0f))), image = bg1)
                .border(2.dp, 10.dp)
        ) { }
    }
    println("generated background.png")
}

private fun generateImageAlphaPreview() {
    val bg1 = loadPreviewImage("image", "bg1.jpg")

    fun Layout.previewLabel(text: String) {
        Text(
            text = text,
            color = Color.WHITE,
            fontSize = 22.dp,
            modifier = Modifier()
                .margin(left = 14.dp, bottom = 14.dp)
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .background(Color.BLACK.withAlpha(0.55f))
                .border(1.dp, 6.dp, Color.WHITE.withAlpha(0.55f)),
            alignment = LayoutAlignment.LEFT_BOTTOM
        )
    }

    View(
        file = previewOutput.resolve("image_alpha.png"),
        modifier = Modifier()
            .width(1000.dp)
            .padding(30.dp)
            .background(Color.makeRGB(236, 241, 248))
    ) {
        Column(Modifier().fillMaxWidth()) {
            Text(
                text = "图片透明度与背景图预览",
                color = Color.BLACK,
                fontSize = 34.dp,
                modifier = Modifier().margin(bottom = 20.dp)
            )
            Row(Modifier().fillMaxWidth().height(280.dp)) {
                Box(
                    Modifier()
                        .fillWidth()
                        .fillMaxHeight()
                        .margin(right = 18.dp)
                        .background(Color.RED)
                        .border(2.dp, 16.dp, Color.WHITE)
                        .shadows(Shadow.ELEVATION_2)
                ) {
                    Image(
                        image = bg1,
                        modifier = Modifier().fillMaxWidth().fillMaxHeight(),
                        alpha = 0.45f
                    )
                    previewLabel("Image alpha = 0.45")
                }

                Box(
                    Modifier()
                        .fillWidth()
                        .fillMaxHeight()
                        .margin(right = 18.dp)
                        .background(Color.RED)
                        .border(2.dp, 16.dp, Color.WHITE)
                        .shadows(Shadow.ELEVATION_2)
                ) {
                    Box(
                        Modifier()
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .background(image = bg1, imageAlpha = 0.45f)
                    )
                    previewLabel("背景图 imageAlpha = 0.45")
                }

                Box(
                    Modifier()
                        .fillWidth()
                        .fillMaxHeight()
                        .background(
                            color = Color.BLACK.withAlpha(0.45f),
                            image = bg1,
                            imageAlpha = 1f
                        )
                        .border(2.dp, 16.dp, Color.WHITE)
                        .shadows(Shadow.ELEVATION_2)
                ) {
                    previewLabel("背景图 + 半透明遮罩")
                }
            }
        }
    }
    println("generated image_alpha.png")
}

private fun generateTextEffectPreview() {
    val titleColor = Color.makeRGB(39, 48, 67)
    val cardBorder = Color.makeRGB(205, 214, 228)

    fun Layout.effectCard(title: String, content: BoxLayout.() -> Unit) {
        Box(
            Modifier()
                .fillWidth()
                .fillMaxHeight()
                .margin(right = 18.dp)
                .padding(16.dp)
                .background(Color.WHITE.withAlpha(0.82f))
                .border(2.dp, 16.dp, cardBorder)
                .shadows(Shadow.ELEVATION_1)
        ) {
            Text(
                text = title,
                color = titleColor,
                fontSize = 18.dp,
                modifier = Modifier().margin(bottom = 10.dp)
            )
            Box(
                Modifier()
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(top = 34.dp),
                content = content
            )
        }
    }

    val richBaseStyle = TextStyle()
        .setColor(Color.makeRGB(39, 48, 67))
        .setFontSize(28f)
        .setFontFamily(Fonts.default.textTypeface!!.familyName)
    val richShadowStyle = richBaseStyle.copyStyle()
        .setColor(Color.makeRGB(218, 64, 87))
        .addShadow(
            org.jetbrains.skia.paragraph.Shadow(
                Color.makeRGB(53, 96, 210).withAlpha(0.55f),
                5f,
                5f,
                2.0
            )
        )
    val richParagraph = RichParagraphBuilder(richBaseStyle)
        .addText("富文本 ")
        .addText("阴影样式", richShadowStyle)
        .addText(" 保留")
        .build()

    View(
        file = previewOutput.resolve("text_effects.png"),
        modifier = Modifier()
            .width(1100.dp)
            .padding(30.dp)
            .background(Color.makeRGB(236, 241, 248))
    ) {
        Column(Modifier().fillMaxWidth()) {
            Text(
                text = "文字描边与文字阴影预览",
                color = titleColor,
                fontSize = 36.dp,
                modifier = Modifier().margin(bottom = 22.dp)
            )

            Row(Modifier().fillMaxWidth().height(180.dp).margin(bottom = 18.dp)) {
                effectCard("纯描边 + 填充") {
                    Text(
                        text = "OUTLINE",
                        color = Color.WHITE,
                        fontSize = 52.dp,
                        stroke = TextStroke(5.dp, Color.makeRGB(38, 67, 132)),
                        alignment = LayoutAlignment.CENTER
                    )
                }

                effectCard("硬阴影") {
                    Text(
                        text = "SHADOW",
                        color = Color.BLACK,
                        fontSize = 50.dp,
                        textShadows = listOf(
                            TextShadow(9.dp, 8.dp, 0.dp, Color.makeRGB(218, 64, 87))
                        ),
                        alignment = LayoutAlignment.CENTER
                    )
                }

                effectCard("柔和阴影") {
                    Text(
                        text = "柔和阴影",
                        color = Color.makeRGB(39, 48, 67),
                        fontSize = 46.dp,
                        textShadows = listOf(
                            TextShadow(6.dp, 8.dp, 4.dp, Color.makeRGB(70, 102, 163).withAlpha(0.38f))
                        ),
                        alignment = LayoutAlignment.CENTER
                    )
                }
            }

            Row(Modifier().fillMaxWidth().height(180.dp).margin(bottom = 18.dp)) {
                effectCard("描边 + 柔阴影") {
                    Text(
                        text = "描边阴影",
                        color = Color.makeRGB(255, 242, 166),
                        fontSize = 46.dp,
                        stroke = TextStroke(4.dp, Color.makeRGB(39, 48, 67)),
                        textShadows = listOf(
                            TextShadow(9.dp, 9.dp, 3.dp, Color.BLACK.withAlpha(0.35f))
                        ),
                        alignment = LayoutAlignment.CENTER
                    )
                }

                effectCard("多层彩色阴影") {
                    Text(
                        text = "多层阴影",
                        color = Color.WHITE,
                        fontSize = 46.dp,
                        stroke = TextStroke(2.dp, Color.makeRGB(39, 48, 67)),
                        textShadows = listOf(
                            TextShadow((-5).dp, 5.dp, 0.dp, Color.makeRGB(46, 196, 182).withAlpha(0.8f)),
                            TextShadow(6.dp, 7.dp, 0.dp, Color.makeRGB(255, 107, 107).withAlpha(0.8f)),
                            TextShadow(0.dp, 12.dp, 5.dp, Color.BLACK.withAlpha(0.32f))
                        ),
                        alignment = LayoutAlignment.CENTER
                    )
                }

                effectCard("小字号细描边") {
                    Text(
                        text = "小字号 24dp",
                        color = Color.makeRGB(39, 48, 67),
                        fontSize = 24.dp,
                        stroke = TextStroke(1.dp, Color.WHITE),
                        textShadows = listOf(
                            TextShadow(3.dp, 3.dp, 1.dp, Color.BLACK.withAlpha(0.25f))
                        ),
                        alignment = LayoutAlignment.CENTER
                    )
                }
            }

            Row(Modifier().fillMaxWidth().height(190.dp)) {
                effectCard("固定宽度换行") {
                    Text(
                        text = "描边和阴影在自动换行时也应保持完整显示",
                        color = Color.makeRGB(39, 48, 67),
                        fontSize = 31.dp,
                        maxLinesCount = 2,
                        stroke = TextStroke(2.dp, Color.WHITE),
                        textShadows = listOf(
                            TextShadow(5.dp, 5.dp, 2.dp, Color.BLACK.withAlpha(0.22f))
                        ),
                        modifier = Modifier().fillMaxWidth(),
                        intrinsicAlignment = LayoutAlignment.CENTER,
                        alignment = LayoutAlignment.CENTER
                    )
                }

                effectCard("富文本 TextStyle 阴影") {
                    RichText(
                        paragraph = richParagraph,
                        maxLinesCount = 2,
                        alignment = LayoutAlignment.CENTER
                    )
                }

                effectCard("大描边边界外扩") {
                    Text(
                        text = "EDGE",
                        color = Color.makeRGB(255, 255, 255),
                        fontSize = 54.dp,
                        stroke = TextStroke(8.dp, Color.makeRGB(218, 64, 87)),
                        textShadows = listOf(
                            TextShadow(0.dp, 10.dp, 4.dp, Color.BLACK.withAlpha(0.28f))
                        ),
                        alignment = LayoutAlignment.CENTER
                    )
                }
            }
        }
    }
    println("generated text_effects.png")
}

private fun generateCanvas() {
    View(
        file = previewOutput.resolve("canvas.png"),
        modifier = Modifier()
            .width(1000.dp)
            .padding(20.dp)
            .background(Color.makeRGB(255, 205, 204))
    ) {
        Canvas(
            modifier = Modifier().fillMaxWidth().height(500.dp).background(color = Color.WHITE)
        ) { rect ->
            val paint = Paint().apply {
                color = Color.RED
                mode = PaintMode.STROKE
                strokeWidth = 2f
            }

            drawLine(rect.left, rect.top, rect.right, rect.bottom, paint)
            drawLine(rect.right, rect.top, rect.left, rect.bottom, paint)
            drawCircle(rect.centerX(), rect.centerY(), 100f, paint)
        }
    }
    println("generated canvas.png")
}

private fun generateText1() {
    View(
        file = previewOutput.resolve("text1.png"),
        modifier = Modifier()
            .width(500.dp)
            .padding(20.dp)
            .background(Color.WHITE)
    ) {
        Column {
            Text(
                text = "不指定宽度测试",
                color = Color.BLACK,
                fontSize = 30.dp,
                modifier = Modifier()
                    .border(2.dp, 0.dp, Color.RED)
                    .margin(bottom = 20.dp)
            )
            Text(
                text = "指定宽度测试",
                color = Color.BLACK,
                fontSize = 30.dp,
                modifier = Modifier()
                    .border(2.dp, 0.dp, Color.GREEN)
                    .width(300.dp)
            )
        }
    }
    println("generated text1.png")
}
