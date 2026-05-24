import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.*
import org.jetbrains.skia.paragraph.ParagraphBuilder
import org.jetbrains.skia.paragraph.ParagraphStyle
import org.jetbrains.skia.paragraph.TextStyle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.system.measureNanoTime
import top.colter.skiko.*
import top.colter.skiko.layout.Image
import top.colter.skiko.layout.RichText
import top.colter.skiko.layout.Text
import top.colter.skiko.data.*
import top.colter.skiko.layout.*
import java.io.File
import java.io.FileFilter


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DrawTest {

    private val testResource = File("src/test/resources")
    private val testOutput = testResource.resolve("output").apply {
        if(!exists()) this.mkdirs()
    }

    private fun loadTestResource(path: String = "", fileName: String) =
        testResource.resolve(path).resolve(fileName)
    private fun loadTestImage(path: String = "", fileName: String) =
        Image.makeFromEncoded(loadTestResource(path, fileName).readBytes())

    private fun loadAllTestImage(path: String): Map<String, Image> {
        val imageMap = mutableMapOf<String, Image>()
        val dir = testResource.resolve(path)
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

    private fun measureRoot(
        modifier: Modifier,
        content: BoxLayout.() -> Unit
    ): BoxLayout {
        val root = BoxLayout(
            alignment = LayoutAlignment.DEFAULT,
            modifier = modifier,
            parentLayout = null
        )
        root.content()
        root.measure(true)
        root.place(
            LayoutBounds.makeXYWH(
                width = root.width,
                height = root.height
            )
        )
        return root
    }

    @BeforeAll
    fun init() {
        Dp.factor = 1f

        FontUtils.loadTypeface(loadTestResource("font", "LXGWWenKai-Bold.ttf").absolutePath)
//        FontUtils.loadEmojiTypeface(loadTestResource("font", "Seguiemj.ttf").absolutePath)
        FontUtils.loadEmojiTypeface(loadTestResource("font", "NotoColorEmoji.ttf").absolutePath)
    }

    @Test
    fun `test paragraph`(): Unit = runBlocking {
        val textStyle = TextStyle().setColor(Color.WHITE).setFontSize(30f).setFontFamily(FontUtils.defaultFont!!.familyName)
        val style = ParagraphStyle()
        val builder = ParagraphBuilder(style, FontUtils.fonts)
            .pushStyle(textStyle.setFontFamily(FontUtils.emojiFont!!.familyName))
            .addText("❤️😍")
            .pushStyle(textStyle.setColor(Color.GREEN).setFontSize(20f).setFontFamily(FontUtils.defaultFont!!.familyName))
            .addText("Hello")
            .pushStyle(textStyle.setColor(Color.RED).setFontSize(30f))
            .addText(" World")
            .pushStyle(textStyle.setColor(Color.YELLOW).setFontSize(40f))
            .addText(" Test")
        val paragraph = builder.build()

        val surface = Surface.makeRasterN32Premul(500, 500)
        val canvas = surface.canvas
        paragraph.layout(400f).paint(canvas, 50f, 50f)
        File("${testOutput.absolutePath}/test1.png").writeBytes(surface.makeImageSnapshot().encodeToData()!!.bytes)
    }

    @Test
    fun `test view layout 1`(): Unit = runBlocking {
        View(
            file = testOutput.resolve("layout1.png"),
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
                    Box(Modifier().width(80.dp).fillMaxHeight()) {  }
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
                            text = "文字并排测试文字并排测试文字并排测试文字并排测试文字并排测试文字并排测试文字并排测试文字并排测试",
                            fontSize = 22.dp,
                            modifier = Modifier().fillWidth()
                        )
                        Text(
                            text = "文字超出自动隐藏文字超出自动隐藏文字超出自动隐藏文字超出自动隐藏文字超出自动隐藏文字超出自动隐藏",
                            fontSize = 22.dp,
                            modifier = Modifier().fillWidth()
                        )
                    }
                    Grid (maxLineCount = 3, modifier = Modifier().fillMaxWidth().margin(10.dp)) {
                        Image(
                            image = loadTestImage("image", "bg1.jpg"),
                            modifier = Modifier().background(Color.RED.withAlpha(0.6f)).border(2.dp, 10.dp).shadows(Shadow.ELEVATION_1)
                        )
                        Image(
                            image = loadTestImage("image", "bg1.jpg"),
                            modifier = Modifier().background(Color.RED.withAlpha(0.6f)).border(2.dp, 10.dp).shadows(Shadow.ELEVATION_1)
                        )
                        Image(
                            image = loadTestImage("image", "bg1.jpg"),
                            modifier = Modifier().background(Color.RED.withAlpha(0.6f)).border(2.dp, 10.dp).shadows(Shadow.ELEVATION_1)
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
                    .shadows(Shadow.ELEVATION_2)
                )
            }
        }
    }

    private fun Layout.RichParagraphTest(modifier: Modifier) {
        val emojiMap = loadAllTestImage("emoji")

        var currEmoji = "[tv_doge]"
        fun randomEmoji(): String {
            currEmoji = emojiMap.keys.random()
            return currEmoji
        }

        val style = TextStyle().setColor(Color.BLACK).setFontSize(30.px).setFontFamily(FontUtils.defaultFont!!.familyName)
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
            .addText("😍❤️🤣😁🙌", style.setFontSize(40.px).setFontFamily(FontUtils.emojiFont!!.familyName))
            .build()

        Box(modifier) {
            RichText(paragraph)
        }
    }

    @Test
    fun `test mix typeset`(): Unit = runBlocking {
        View(
            file = testOutput.resolve("typeset1.png"),
            modifier = Modifier()
                .width(1000.dp)
                .padding(30.dp)
                .background(Color.makeRGB(255, 205, 204))
        ) {
            RichParagraphTest(
                Modifier()
                .fillMaxWidth()
                .padding(20.dp)
                .background(Color.WHITE.withAlpha(0.7f))
                .border(3.dp, 15.dp, Color.WHITE)
                .shadows(Shadow.ELEVATION_4)
            )
        }
    }

    @Test
    fun `test grid`(): Unit = runBlocking {

        val imgMap = loadAllTestImage("image")
        val imgList = imgMap.values

        View(
            file = testOutput.resolve("grid1.png"),
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

                Grid (maxLineCount = 3, modifier = modifier) {
                    val boxModifier = Modifier().background(Color.RED.withAlpha(0.6f)).border(2.dp, 10.dp).shadows(Shadow.ELEVATION_1)
                    for (i in 1..9) Box(boxModifier)
                }

                Grid(maxLineCount = 3, modifier = modifier) {
                    val imgModifier = Modifier().background(Color.RED.withAlpha(0.6f)).border(2.dp, 10.dp).shadows(Shadow.ELEVATION_1)
                    for (i in 1..9) Image(imgList.random(), modifier = imgModifier)
                }

            }
        }
    }

    @Test
    fun `test shadow`(): Unit = runBlocking {
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
    }

    @Test
    fun `test background image`(): Unit = runBlocking {

        val bg1 = loadTestImage("image", "bg1.jpg")

        View(
            file = testOutput.resolve("background.png"),
            modifier = Modifier()
                .width(1000.dp)
                .padding(20.dp)
                .background(Color.makeRGB(255, 205, 204))
        ) {
            Box (
                modifier = Modifier()
                    .fillMaxWidth()
                    .height(500.dp)
//                    .background(color = Color.BLACK.withAlpha(0.3f), image = bg1)
                    .background(gradient = Gradient(LayoutAlignment.LEFT_TOP, LayoutAlignment.RIGHT_BOTTOM, listOf(Color.BLACK.withAlpha(1f), Color.BLACK.withAlpha(0f))), image = bg1)
                    .border(2.dp, 10.dp)
            ) { }
        }
    }

    @Test
    fun `test canvas`(): Unit = runBlocking {
        View(
            file = testOutput.resolve("canvas.png"),
            modifier = Modifier()
                .width(1000.dp)
                .padding(20.dp)
                .background(Color.makeRGB(255, 205, 204))
        ) {
            Canvas (
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
    }

    @Test
    fun `test text1`(): Unit = runBlocking {

        View(
            file = testOutput.resolve("text1.png"),
            modifier = Modifier()
                .width(500.dp)
                .padding(20.dp)
                .background(Color.WHITE)
        ) {
            Column{
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
    }

    @Test
    fun `regression text padding`(): Unit = runBlocking {
        val root = measureRoot(
            Modifier().width(500.dp).height(220.dp)
        ) {
            Column(Modifier().fillMaxWidth()) {
                Text(
                    text = "padding regression",
                    fontSize = 24.dp,
                    modifier = Modifier().margin(bottom = 8.dp)
                )
                Text(
                    text = "padding regression",
                    fontSize = 24.dp,
                    modifier = Modifier().padding(12.dp)
                )
            }
        }

        val column = root.child.single() as ColumnLayout
        val plain = column.child[0]
        val padded = column.child[1]

        assertEquals(24f, padded.width.px - plain.width.px, 0.01f)
        assertEquals(24f, padded.height.px - plain.height.px, 0.01f)
    }

    @Test
    fun `regression rich text padding and max lines`(): Unit = runBlocking {
        val style = TextStyle()
            .setColor(Color.BLACK)
            .setFontSize(24f)
            .setFontFamily(FontUtils.defaultFont!!.familyName)

        val paragraph = RichParagraphBuilder(style)
            .addText("Rich line one")
            .wrap()
            .addText("Rich line two")
            .build()

        val wrappedOnce = paragraph.layout(180f, 1)
        val wrappedTwice = paragraph.layout(180f, 2)
        assertEquals(1, wrappedOnce.lines.size)
        assertEquals(2, wrappedTwice.lines.size)

        val root = measureRoot(
            Modifier().width(1000.dp).height(260.dp)
        ) {
            Column(Modifier().fillMaxWidth()) {
                RichText(paragraph, maxLinesCount = 2, modifier = Modifier().margin(bottom = 8.dp))
                RichText(paragraph, maxLinesCount = 2, modifier = Modifier().padding(16.dp))
            }
        }

        val column = root.child.single() as ColumnLayout
        val plain = column.child[0]
        val padded = column.child[1]

        assertEquals(32f, padded.width.px - plain.width.px, 0.01f)
        assertEquals(32f, padded.height.px - plain.height.px, 0.01f)
    }

    @Test
    fun `regression grid row height`(): Unit = runBlocking {
        val root = measureRoot(
            Modifier().width(300.dp).height(240.dp)
        ) {
            Grid(maxLineCount = 2, space = 10.dp, modifier = Modifier().fillMaxWidth()) {
                Box(Modifier().width(40.dp).height(20.dp))
                Box(Modifier().width(40.dp).height(60.dp))
                Box(Modifier().width(40.dp).height(25.dp))
                Box(Modifier().width(40.dp).height(40.dp))
            }
        }

        val grid = root.child.single() as GridLayout
        val firstRowHeight = Dp.max(grid.child[0].boxHeight, grid.child[1].boxHeight)
        assertEquals(
            firstRowHeight.px + 10f,
            (grid.child[2].position.y - grid.child[0].position.y).px,
            0.01f
        )
    }

    @Test
    fun `regression image ratio with max constraints`(): Unit = runBlocking {
        val image = Surface.makeRasterN32Premul(400, 200).makeImageSnapshot()

        val root = measureRoot(
            Modifier().width(300.dp).height(240.dp)
        ) {
            Image(
                image = image,
                modifier = Modifier().maxWidth(100.dp).maxHeight(40.dp)
            )
        }

        val imageLayout = root.child.single() as ImageLayout
        assertEquals(80f, imageLayout.width.px, 0.01f)
        assertEquals(40f, imageLayout.height.px, 0.01f)
    }

    @Test
    fun `regression fill nesting`(): Unit = runBlocking {
        val root = measureRoot(
            Modifier().width(400.dp).height(300.dp).padding(20.dp)
        ) {
            Box(Modifier().fillMaxWidth().fillMaxHeight()) {
                Box(Modifier().fillRatioWidth(0.5f).fillRatioHeight(0.5f))
            }
        }

        val outer = root.child.single() as BoxLayout
        val inner = outer.child.single() as BoxLayout

        assertEquals(360f, outer.width.px, 0.01f)
        assertEquals(260f, outer.height.px, 0.01f)
        assertEquals(180f, inner.width.px, 0.01f)
        assertEquals(130f, inner.height.px, 0.01f)
    }

    @Test
    fun `regression min max constraints`(): Unit = runBlocking {
        val root = measureRoot(
            Modifier().width(320.dp).height(260.dp)
        ) {
            Column(Modifier().fillMaxWidth()) {
                Box(
                    Modifier()
                        .width(80.dp)
                        .height(40.dp)
                        .minWidth(120.dp)
                        .minHeight(60.dp)
                )
                Box(
                    Modifier()
                        .width(220.dp)
                        .height(180.dp)
                        .maxWidth(150.dp)
                        .maxHeight(100.dp)
                )
            }
        }

        val column = root.child.single() as ColumnLayout
        val minClamped = column.child[0]
        val maxClamped = column.child[1]

        assertEquals(120f, minClamped.width.px, 0.01f)
        assertEquals(60f, minClamped.height.px, 0.01f)
        assertEquals(150f, maxClamped.width.px, 0.01f)
        assertEquals(100f, maxClamped.height.px, 0.01f)
    }

    @Test
    fun `performance smoke`(): Unit = runBlocking {
        val emojiSurface = Surface.makeRasterN32Premul(24, 24)
        emojiSurface.canvas.clear(Color.YELLOW)
        val emojiImage = emojiSurface.makeImageSnapshot()

        val richStyle = TextStyle()
            .setColor(Color.BLACK)
            .setFontSize(18f)
            .setFontFamily(FontUtils.defaultFont!!.familyName)

        val richParagraph = RichParagraphBuilder(richStyle)
            .addText("Rich smoke ".repeat(40))
            .addEmoji("smoke", emojiImage, richStyle)
            .wrap()
            .addText("Second line ".repeat(30))
            .build()

        val richRoot = measureRoot(
            Modifier().width(1200.dp).height(800.dp).padding(20.dp)
        ) {
            RichText(
                richParagraph,
                maxLinesCount = 20,
                modifier = Modifier().fillMaxWidth().padding(12.dp).background(Color.WHITE.withAlpha(0.4f))
            )
        }

        val gridRoot = measureRoot(
            Modifier().width(1200.dp).height(800.dp).padding(20.dp)
        ) {
            Grid(maxLineCount = 10, space = 4.dp, modifier = Modifier().fillMaxWidth()) {
                repeat(120) {
                    Box(
                        Modifier()
                            .width(18.dp)
                            .height(18.dp)
                            .background(Color.WHITE.withAlpha(0.7f))
                            .border(1.dp, 2.dp)
                    )
                }
            }
        }

        val bgRoot = measureRoot(
            Modifier().width(1200.dp).height(800.dp).padding(20.dp)
        ) {
            Column(Modifier().fillMaxWidth()) {
                repeat(80) {
                    Box(
                        Modifier()
                            .fillMaxWidth()
                            .height(10.dp)
                            .background(Color.WHITE.withAlpha(0.65f))
                            .border(1.dp, 2.dp)
                    )
                }
            }
        }

        val richSurface = Surface.makeRasterN32Premul(1400, 1000)
        val gridSurface = Surface.makeRasterN32Premul(1400, 1000)
        val bgSurface = Surface.makeRasterN32Premul(1400, 1000)

        val richTime = measureNanoTime {
            repeat(3) {
                richSurface.canvas.clear(Color.TRANSPARENT)
                richRoot.measure(true)
                richRoot.place(LayoutBounds.makeXYWH(width = richRoot.width, height = richRoot.height))
                richRoot.draw(richSurface.canvas)
            }
        }
        val gridTime = measureNanoTime {
            repeat(3) {
                gridSurface.canvas.clear(Color.TRANSPARENT)
                gridRoot.measure(true)
                gridRoot.place(LayoutBounds.makeXYWH(width = gridRoot.width, height = gridRoot.height))
                gridRoot.draw(gridSurface.canvas)
            }
        }
        val bgTime = measureNanoTime {
            repeat(3) {
                bgSurface.canvas.clear(Color.TRANSPARENT)
                bgRoot.measure(true)
                bgRoot.place(LayoutBounds.makeXYWH(width = bgRoot.width, height = bgRoot.height))
                bgRoot.draw(bgSurface.canvas)
            }
        }

        println(
            "performance smoke ms: rich=${richTime / 1_000_000.0}, " +
                    "grid=${gridTime / 1_000_000.0}, bg=${bgTime / 1_000_000.0}"
        )
        assertTrue(richTime > 0 && gridTime > 0 && bgTime > 0)
    }


}
