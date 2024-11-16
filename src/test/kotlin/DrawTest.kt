import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.*
import org.jetbrains.skia.paragraph.ParagraphBuilder
import org.jetbrains.skia.paragraph.ParagraphStyle
import org.jetbrains.skia.paragraph.TextStyle
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
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
                            alignment = LayoutAlignment.BOTTOM_RIGHT
                        )
                    }
                    Column(Modifier().fillWidth().fillMaxHeight().padding(10.dp)) {
                        Box(Modifier().fillMaxWidth().fillHeight().background(Color.RED)) {
                            Box(Modifier().fillMaxWidth().fillMaxHeight()) {
                                Text(text = "测试文字右对齐", fontSize = 22.dp, alignment = LayoutAlignment.TOP_RIGHT)
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
                    .background(gradient = Gradient(LayoutAlignment.TOP_LEFT, LayoutAlignment.BOTTOM_RIGHT, listOf(Color.BLACK.withAlpha(1f), Color.BLACK.withAlpha(0f))), image = bg1)
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


}
