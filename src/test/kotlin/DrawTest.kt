import org.jetbrains.skia.*
import org.jetbrains.skia.paragraph.Shadow as ParagraphShadow
import org.jetbrains.skia.paragraph.TextStyle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import top.colter.skiko.*
import top.colter.skiko.data.*
import top.colter.skiko.layout.*
import top.colter.skiko.layout.Image
import top.colter.skiko.layout.RichText
import top.colter.skiko.layout.Text
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.system.measureNanoTime


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DrawTest {

    private val testResource = File("src/test/resources")

    private fun loadTestResource(path: String = "", fileName: String) =
        testResource.resolve(path).resolve(fileName)

    private fun solidImage(width: Int, height: Int, color: Int): org.jetbrains.skia.Image =
        Surface.makeRasterN32Premul(width, height).apply {
            canvas.clear(color)
        }.makeImageSnapshot()

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

    private fun renderRoot(root: BoxLayout): BufferedImage {
        val surface = Surface.makeRasterN32Premul(
            root.width.px.toInt().coerceAtLeast(1),
            root.height.px.toInt().coerceAtLeast(1)
        )
        surface.canvas.clear(Color.TRANSPARENT)
        root.draw(surface.canvas)
        return ImageIO.read(ByteArrayInputStream(surface.makeImageSnapshot().encodeToData()!!.bytes))
    }

    private fun measureTextLayout(
        modifier: Modifier = Modifier(),
        stroke: TextStroke? = null,
        textEmphasis: TextEmphasis? = null,
        textShadows: List<TextShadow> = emptyList(),
    ): TextLayout {
        val root = measureRoot(
            Modifier().width(2000.dp).height(500.dp)
        ) {
            Text(
                text = "TEXT",
                color = Color.WHITE,
                fontSize = 54.dp,
                modifier = modifier,
                stroke = stroke,
                textEmphasis = textEmphasis,
                textShadows = textShadows
            )
        }
        return root.child.single() as TextLayout
    }

    private fun alphaOf(argb: Int): Int = argb ushr 24 and 0xFF
    private fun redOf(argb: Int): Int = argb ushr 16 and 0xFF
    private fun greenOf(argb: Int): Int = argb ushr 8 and 0xFF
    private fun blueOf(argb: Int): Int = argb and 0xFF

    private fun assertChannelInRange(value: Int, start: Int, endInclusive: Int) {
        assertTrue(value in start..endInclusive, "颜色通道 $value 不在 $start..$endInclusive 之间")
    }

    private fun countPixels(image: BufferedImage, predicate: (Int) -> Boolean): Int {
        var count = 0
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                if (predicate(image.getRGB(x, y))) count++
            }
        }
        return count
    }

    private fun isRedDominant(argb: Int): Boolean =
        alphaOf(argb) > 0 && redOf(argb) > 120 && redOf(argb) > greenOf(argb) + 40 && redOf(argb) > blueOf(argb) + 40

    private fun isWhiteDominant(argb: Int): Boolean =
        alphaOf(argb) > 0 && redOf(argb) > 180 && greenOf(argb) > 180 && blueOf(argb) > 180

    private fun isBlackDominant(argb: Int): Boolean =
        alphaOf(argb) > 0 && redOf(argb) < 60 && greenOf(argb) < 60 && blueOf(argb) < 60

    @BeforeAll
    fun init() {
        Dp.factor = 1f

        Fonts.default.loadTextTypeface(loadTestResource("font", "LXGWWenKai-Bold.ttf").absolutePath)
        Fonts.default.loadEmojiTypeface(loadTestResource("font", "NotoColorEmoji.ttf").absolutePath)
    }

    @Test
    fun `regression text padding`() {
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
    fun `regression row column text respects allocated width`() {
        val cover = Surface.makeRasterN32Premul(200, 200).apply {
            canvas.clear(Color.RED)
        }.makeImageSnapshot()

        val title = "A very long title that should wrap inside the allocated column width and never grow beyond it"
        val desc = "This description is also long enough to wrap several lines instead of forcing the column wider than the row"

        val root = measureRoot(
            Modifier().width(600.dp).height(200.dp)
        ) {
            Row(
                modifier = Modifier()
                    .height(200.dp)
                    .fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier().fillMaxHeight()
                ) {
                    Image(
                        image = cover,
                        ratio = 1f,
                        modifier = Modifier().fillMaxHeight()
                    )
                }

                Column(modifier = Modifier().fillWidth().fillMaxHeight().margin(15.dp)) {
                    Text(
                        text = title,
                        maxLinesCount = 2,
                        modifier = Modifier().fillRatioHeight(0.4f)
                    )

                    Text(
                        text = desc,
                        maxLinesCount = 4,
                        modifier = Modifier().fillRatioHeight(0.6f)
                    )
                }
            }
        }

        val row = root.child.single() as RowLayout
        val imageBox = row.child[0] as BoxLayout
        val column = row.child[1] as ColumnLayout
        val firstText = column.child[0] as TextLayout
        val secondText = column.child[1] as TextLayout

        assertEquals(200f, imageBox.width.px, 0.01f)
        assertEquals(370f, column.width.px, 0.01f)
        assertEquals(370f, column.contentWidth.px, 0.01f)
        assertTrue(firstText.width.px <= column.contentWidth.px + 0.01f)
        assertTrue(secondText.width.px <= column.contentWidth.px + 0.01f)
        assertTrue(firstText.height.px <= 80f + 0.01f)
        assertTrue(secondText.height.px <= 120f + 0.01f)
    }

    @Test
    fun `regression rich text padding and max lines`() {
        val style = TextStyle()
            .setColor(Color.BLACK)
            .setFontSize(24f)
            .setFontFamily(Fonts.default.textTypeface!!.familyName)

        val paragraph = RichParagraphBuilder(style)
            .addText("Rich line one")
            .wrap()
            .addText("Rich line two")
            .build()

        val wrappedOnce = paragraph.layout(180f, 1)
        val wrappedTwice = paragraph.layout(180f, 2)
        assertEquals(1, wrappedOnce.lineCount)
        assertEquals(2, wrappedTwice.lineCount)

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
    fun `regression rich text layout keeps measured lines inside width`() {
        val emoji = solidImage(64, 64, Color.YELLOW)
        val style = TextStyle()
            .setColor(Color.BLACK)
            .setFontSize(32f)
            .setFontFamily(Fonts.default.textTypeface!!.familyName)
        val linkStyle = TextStyle()
            .setColor(Color.BLUE)
            .setFontSize(32f)
            .setFontFamily(Fonts.default.textTypeface!!.familyName)

        val paragraph = RichParagraphBuilder(style)
            .addText("这是一段用于复现右侧裁切的中文长文本，多个字在最右侧竖直平齐时也不应该被内容裁剪整齐切掉。")
            .addEmoji("[热词系列_知识增加]", emoji, style)
            .addText(" 链接文字和普通文字混排后仍然必须使用同一套测量和绘制宽度。", linkStyle)
            .build()
        val width = 360f
        val layout = paragraph.layout(width, maxLinesCount = 20)

        assertTrue(layout.width <= width + 0.01f)
        layout.lineMetrics.forEach {
            assertTrue(it.right <= width + 0.01f, "line right ${it.right} exceeds $width")
        }
        layout.placeholders.forEach {
            assertTrue(it.rect.right <= width + 0.01f, "placeholder right ${it.rect.right} exceeds $width")
        }
    }

    @Test
    fun `rich text image emoji placeholder follows font size`() {
        val emoji = solidImage(64, 64, Color.YELLOW)
        val style = TextStyle()
            .setColor(Color.BLACK)
            .setFontSize(42f)
            .setFontFamily(Fonts.default.textTypeface!!.familyName)

        val paragraph = RichParagraphBuilder(style)
            .addEmoji("[emoji]", emoji, style)
            .build()

        val layout = paragraph.layout(200f)
        val placeholder = layout.placeholders.single().rect

        assertEquals(42f, placeholder.width, 0.01f)
        assertEquals(42f, placeholder.height, 0.01f)
    }

    @Test
    fun `文字样式复制保留绘制效果`() {
        val style = TextStyle()
            .setColor(Color.BLACK)
            .setFontSize(24f)
            .setForeground(Paint().apply {
                color = Color.RED
                mode = PaintMode.STROKE
                strokeWidth = 3f
            })
            .setBackground(Paint().apply {
                color = Color.YELLOW
            })
            .addShadow(ParagraphShadow(Color.BLUE, 1f, 2f, 3.0))

        val copy = style.copyStyle()

        assertEquals(Color.RED, copy.foreground!!.color)
        assertEquals(PaintMode.STROKE, copy.foreground!!.mode)
        assertEquals(3f, copy.foreground!!.strokeWidth, 0.01f)
        assertEquals(Color.YELLOW, copy.background!!.color)
        assertEquals(1, copy.shadows.size)
        assertEquals(Color.BLUE, copy.shadows.single().color)
    }

    @Test
    fun `文字描边绘制描边和填充`() {
        val root = measureRoot(
            Modifier().width(240.dp).height(110.dp).background(Color.BLACK)
        ) {
            Text(
                text = "TEXT",
                color = Color.WHITE,
                fontSize = 54.dp,
                stroke = TextStroke(5.dp, Color.RED)
            )
        }

        val image = renderRoot(root)

        assertTrue(countPixels(image, ::isRedDominant) > 30)
        assertTrue(countPixels(image, ::isWhiteDominant) > 30)
    }

    @Test
    fun `文字阴影绘制偏移阴影`() {
        val root = measureRoot(
            Modifier().width(260.dp).height(120.dp).background(Color.BLACK)
        ) {
            Text(
                text = "TEXT",
                color = Color.WHITE,
                fontSize = 54.dp,
                textShadows = listOf(
                    TextShadow(
                        offsetX = 10.dp,
                        offsetY = 8.dp,
                        color = Color.RED
                    )
                )
            )
        }

        val image = renderRoot(root)

        assertTrue(countPixels(image, ::isRedDominant) > 30)
        assertTrue(countPixels(image, ::isWhiteDominant) > 30)
    }

    @Test
    fun `粗描边不会盖住文字阴影`() {
        val root = measureRoot(
            Modifier().width(360.dp).height(190.dp)
        ) {
            Text(
                text = "EDGE",
                color = Color.WHITE,
                fontSize = 64.dp,
                stroke = TextStroke(28.dp, Color.BLACK),
                textShadows = listOf(
                    TextShadow(
                        offsetX = 6.dp,
                        offsetY = 6.dp,
                        blur = 6.dp,
                        color = Color.RED
                    )
                )
            )
        }

        val image = renderRoot(root)

        val redPixels = countPixels(image, ::isRedDominant)
        assertTrue(redPixels > 30, "红色阴影像素不足: $redPixels")
        assertTrue(countPixels(image, ::isBlackDominant) > 30)
        assertTrue(countPixels(image, ::isWhiteDominant) > 30)
    }

    @Test
    fun `文字效果尺寸计算匹配视觉外扩`() {
        val plain = measureTextLayout()

        val strokeOnly = measureTextLayout(stroke = TextStroke(6.dp, Color.RED))
        assertEquals(plain.width.px + 12f, strokeOnly.width.px, 0.1f)
        assertEquals(plain.height.px + 12f, strokeOnly.height.px, 0.1f)

        val shadowOnly = measureTextLayout(
            textShadows = listOf(
                TextShadow(
                    offsetX = 10.dp,
                    offsetY = 8.dp,
                    color = Color.RED
                )
            )
        )
        assertEquals(plain.width.px + 10f, shadowOnly.width.px, 0.1f)
        assertEquals(plain.height.px + 8f, shadowOnly.height.px, 0.1f)

        val strokeAndShadow = measureTextLayout(
            stroke = TextStroke(20.dp, Color.BLACK),
            textShadows = listOf(
                TextShadow(
                    blur = 3.dp,
                    color = Color.RED
                )
            )
        )
        assertEquals(plain.width.px + 58f, strokeAndShadow.width.px, 0.1f)
        assertEquals(plain.height.px + 58f, strokeAndShadow.height.px, 0.1f)

        val fixedSize = measureTextLayout(
            modifier = Modifier().width(180.dp).height(90.dp),
            stroke = TextStroke(20.dp, Color.BLACK),
            textShadows = listOf(
                TextShadow(
                    offsetX = 16.dp,
                    blur = 3.dp,
                    color = Color.RED
                )
            )
        )
        assertEquals(180f, fixedSize.width.px, 0.01f)
        assertEquals(90f, fixedSize.height.px, 0.01f)
    }

    @Test
    fun `文字效果参数校验`() {
        assertThrows(IllegalArgumentException::class.java) {
            TextStroke(width = (-1).dp, color = Color.RED)
        }
        assertThrows(IllegalArgumentException::class.java) {
            TextShadow(blur = (-1).dp, color = Color.RED)
        }
    }

    @Test
    fun `regression grid row height`() {
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
    fun `regression grid locked item remeasures nested content`() {
        val image = Surface.makeRasterN32Premul(1000, 800).makeImageSnapshot()

        val root = measureRoot(
            Modifier().width(1000.dp).height(700.dp)
        ) {
            Grid(
                maxLineCount = 2,
                space = 20.dp,
                lockRatio = true,
                modifier = Modifier().fillMaxWidth()
            ) {
                repeat(2) {
                    Box(Modifier().background(Color.RED)) {
                        Image(image = image, ratio = Ratio.SQUARE)
                    }
                }
            }
        }

        val grid = root.child.single() as GridLayout
        val firstTile = grid.child[0] as BoxLayout
        val firstImage = firstTile.child.single() as ImageLayout
        val secondTile = grid.child[1] as BoxLayout

        assertEquals(490f, firstTile.width.px, 0.01f)
        assertEquals(490f, firstTile.height.px, 0.01f)
        assertEquals(490f, firstImage.width.px, 0.01f)
        assertEquals(490f, firstImage.height.px, 0.01f)
        assertTrue((secondTile.position.x + secondTile.boxWidth).px <= (grid.position.x + grid.contentWidth).px + 0.01f)
    }

    @Test
    fun `regression image ratio with max constraints`() {
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
    fun `普通图片透明度按 alpha 混合`() {
        val blueImage = solidImage(20, 20, Color.BLUE)
        val root = measureRoot(
            Modifier().width(20.dp).height(20.dp).background(Color.RED)
        ) {
            Image(
                image = blueImage,
                modifier = Modifier().width(20.dp).height(20.dp),
                alpha = 0.5f
            )
        }

        val pixel = renderRoot(root).getRGB(10, 10)

        assertEquals(255, alphaOf(pixel))
        assertChannelInRange(redOf(pixel), 120, 135)
        assertChannelInRange(greenOf(pixel), 0, 5)
        assertChannelInRange(blueOf(pixel), 120, 135)
    }

    @Test
    fun `背景图片透明度按 imageAlpha 混合`() {
        val blueImage = solidImage(20, 20, Color.BLUE)
        val root = measureRoot(
            Modifier().width(20.dp).height(20.dp).background(Color.RED)
        ) {
            Box(
                Modifier()
                    .width(20.dp)
                    .height(20.dp)
                    .background(image = blueImage, imageAlpha = 0.5f)
            )
        }

        val pixel = renderRoot(root).getRGB(10, 10)

        assertEquals(255, alphaOf(pixel))
        assertChannelInRange(redOf(pixel), 120, 135)
        assertChannelInRange(greenOf(pixel), 0, 5)
        assertChannelInRange(blueOf(pixel), 120, 135)
    }

    @Test
    fun `背景图片先于颜色遮罩绘制`() {
        val blueImage = solidImage(20, 20, Color.BLUE)
        val root = measureRoot(
            Modifier()
                .width(20.dp)
                .height(20.dp)
                .background(
                    color = Color.BLACK.withAlpha(0.5f),
                    image = blueImage,
                    imageAlpha = 1f
                )
        ) { }

        val pixel = renderRoot(root).getRGB(10, 10)

        assertEquals(255, alphaOf(pixel))
        assertChannelInRange(redOf(pixel), 0, 5)
        assertChannelInRange(greenOf(pixel), 0, 5)
        assertChannelInRange(blueOf(pixel), 120, 135)
    }

    @Test
    fun `图片透明度参数校验`() {
        val image = solidImage(20, 20, Color.BLUE)

        assertThrows(IllegalArgumentException::class.java) {
            measureRoot(Modifier().width(20.dp).height(20.dp)) {
                Image(image = image, alpha = -0.01f)
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            measureRoot(Modifier().width(20.dp).height(20.dp)) {
                Image(image = image, alpha = 1.01f)
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            Modifier().background(image = image, imageAlpha = -0.01f)
        }
        assertThrows(IllegalArgumentException::class.java) {
            Modifier().background(image = image, imageAlpha = 1.01f)
        }
    }

    @Test
    fun `regression fill nesting`() {
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
    fun `regression padding ratio`() {
        val root = measureRoot(
            Modifier().width(400.dp).height(300.dp)
        ) {
            Box(
                Modifier()
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .paddingRatio(horizontal = 0.1f, vertical = 0.2f)
            ) {
                Box(Modifier().fillMaxWidth().fillMaxHeight())
            }
        }

        val outer = root.child.single() as BoxLayout
        val inner = outer.child.single() as BoxLayout

        assertEquals(400f, outer.width.px, 0.01f)
        assertEquals(300f, outer.height.px, 0.01f)
        assertEquals(320f, outer.contentWidth.px, 0.01f)
        assertEquals(180f, outer.contentHeight.px, 0.01f)
        assertEquals(40f, inner.position.x.px, 0.01f)
        assertEquals(60f, inner.position.y.px, 0.01f)
        assertEquals(320f, inner.width.px, 0.01f)
        assertEquals(180f, inner.height.px, 0.01f)
    }

    @Test
    fun `regression margin ratio`() {
        val root = measureRoot(
            Modifier().width(400.dp).height(300.dp)
        ) {
            Box(Modifier().fillMaxWidth().fillMaxHeight()) {
                Box(
                    Modifier()
                        .width(100.dp)
                        .height(80.dp)
                        .marginRatio(horizontal = 0.1f, vertical = 0.2f)
                )
            }
        }

        val outer = root.child.single() as BoxLayout
        val inner = outer.child.single() as BoxLayout

        assertEquals(40f, inner.position.x.px, 0.01f)
        assertEquals(60f, inner.position.y.px, 0.01f)
        assertEquals(100f, inner.width.px, 0.01f)
        assertEquals(80f, inner.height.px, 0.01f)
        assertEquals(180f, inner.boxWidth.px, 0.01f)
        assertEquals(200f, inner.boxHeight.px, 0.01f)
    }

    @Test
    fun `regression grid item modifier ratio`() {
        val root = measureRoot(
            Modifier().width(320.dp).height(200.dp)
        ) {
            Grid(
                maxLineCount = 2,
                lockRatio = false,
                modifier = Modifier().fillMaxWidth().fillMaxHeight(),
                itemModifier = Modifier()
                    .paddingRatio(horizontal = 0.1f, vertical = 0.05f)
                    .marginRatio(horizontal = 0.05f, vertical = 0.1f)
            ) {
                Box(Modifier().width(100.dp).height(100.dp))
                Box(Modifier().width(100.dp).height(100.dp))
            }
        }

        val grid = root.child.single() as GridLayout
        val first = grid.child[0]

        assertEquals(100f, first.width.px, 0.01f)
        assertEquals(100f, first.height.px, 0.01f)
        assertEquals(36f, first.contentWidth.px, 0.01f)
        assertEquals(80f, first.contentHeight.px, 0.01f)
        assertEquals(16f, first.position.x.px, 0.01f)
        assertEquals(20f, first.position.y.px, 0.01f)
        assertEquals(132f, first.boxWidth.px, 0.01f)
        assertEquals(140f, first.boxHeight.px, 0.01f)
    }

    @Test
    fun `regression overflow ratio keeps layout slot`() {
        val root = measureRoot(
            Modifier().width(500.dp).height(180.dp)
        ) {
            Row(Modifier().fillMaxWidth().height(100.dp)) {
                Box(Modifier().width(200.dp).fillMaxHeight()) {
                    Box(
                        Modifier()
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .overflowRatioWidth(1.5f)
                            .background(Color.RED)
                    )
                }
                Box(Modifier().width(50.dp).height(100.dp).background(Color.BLUE))
            }
        }

        val row = root.child.single() as RowLayout
        val overflowSlot = row.child[0] as BoxLayout
        val sibling = row.child[1] as BoxLayout

        assertEquals(200f, overflowSlot.width.px, 0.01f)
        assertEquals(200f, overflowSlot.boxWidth.px, 0.01f)
        assertEquals(200f, sibling.position.x.px, 0.01f)

        val image = renderRoot(root)
        val overflowPixel = image.getRGB(10, 50)
        assertEquals(255, alphaOf(overflowPixel))
        assertTrue(redOf(overflowPixel) > 200)

        val siblingPixel = image.getRGB(225, 50)
        assertEquals(255, alphaOf(siblingPixel))
        assertTrue(blueOf(siblingPixel) > 200)
    }

    @Test
    fun `regression bleed ratio paints outside without occupying space`() {
        val root = measureRoot(
            Modifier().width(360.dp).height(180.dp)
        ) {
            Box(Modifier().width(200.dp).height(100.dp).margin(left = 120.dp, top = 40.dp)) {
                Box(
                    Modifier()
                        .width(100.dp)
                        .height(40.dp)
                        .bleedRatio(left = 0.2f, right = 0.1f)
                        .background(Color.GREEN)
                )
            }
        }

        val parent = root.child.single() as BoxLayout
        val child = parent.child.single() as BoxLayout
        assertEquals(100f, child.width.px, 0.01f)
        assertEquals(100f, child.boxWidth.px, 0.01f)

        val image = renderRoot(root)
        val bleedPixel = image.getRGB(90, 60)
        assertEquals(255, alphaOf(bleedPixel))
        assertTrue(greenOf(bleedPixel) > 200)
        assertEquals(0, alphaOf(image.getRGB(75, 60)))
    }

    @Test
    fun `regression offset ratio moves paint only`() {
        val root = measureRoot(
            Modifier().width(360.dp).height(180.dp)
        ) {
            Box(Modifier().width(300.dp).height(100.dp).margin(left = 40.dp, top = 40.dp)) {
                Box(
                    Modifier()
                        .width(100.dp)
                        .height(40.dp)
                        .offsetRatio(x = -0.1f)
                        .background(Color.RED)
                )
            }
        }

        val parent = root.child.single() as BoxLayout
        val child = parent.child.single() as BoxLayout
        assertEquals(40f, child.position.x.px, 0.01f)
        assertEquals(100f, child.width.px, 0.01f)

        val image = renderRoot(root)
        val offsetPixel = image.getRGB(15, 60)
        assertEquals(255, alphaOf(offsetPixel))
        assertTrue(redOf(offsetPixel) > 200)
        assertEquals(0, alphaOf(image.getRGB(145, 60)))
    }

    @Test
    fun `regression overflow content bounds feed nested fill`() {
        val root = measureRoot(
            Modifier().width(500.dp).height(180.dp)
        ) {
            Box(Modifier().width(300.dp).height(100.dp).margin(left = 100.dp, top = 40.dp)) {
                Box(
                    Modifier()
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .overflowRatioWidth(1.2f)
                        .padding(10.dp)
                        .background(Color.RED)
                ) {
                    Box(Modifier().fillMaxWidth().fillMaxHeight().background(Color.BLUE))
                }
            }
        }

        val parent = root.child.single() as BoxLayout
        val overflow = parent.child.single() as BoxLayout
        val nested = overflow.child.single() as BoxLayout

        assertEquals(300f, overflow.width.px, 0.01f)
        assertEquals(340f, nested.width.px, 0.01f)
        assertEquals(80f, nested.position.x.px, 0.01f)
    }

    @Test
    fun `regression bleed does not expand auto parent`() {
        val root = measureRoot(
            Modifier().width(300.dp).height(120.dp)
        ) {
            Box {
                Box(
                    Modifier()
                        .width(100.dp)
                        .height(50.dp)
                        .bleed(50.dp)
                        .background(Color.RED)
                )
            }
        }

        val autoParent = root.child.single() as BoxLayout
        val child = autoParent.child.single() as BoxLayout
        assertEquals(100f, autoParent.width.px, 0.01f)
        assertEquals(50f, autoParent.height.px, 0.01f)
        assertEquals(100f, child.boxWidth.px, 0.01f)

        val image = renderRoot(root)
        val bleedPixel = image.getRGB(125, 25)
        assertEquals(255, alphaOf(bleedPixel))
        assertTrue(redOf(bleedPixel) > 200)
    }

    @Test
    fun `regression grid item modifier keeps visual overflow`() {
        val root = measureRoot(
            Modifier().width(160.dp).height(100.dp)
        ) {
            Grid(
                maxLineCount = 1,
                lockRatio = false,
                itemModifier = Modifier().bleed(10.dp)
            ) {
                Box(Modifier().width(40.dp).height(40.dp).background(Color.RED))
            }
        }

        val grid = root.child.single() as GridLayout
        val item = grid.child.single() as BoxLayout
        assertEquals(40f, grid.width.px, 0.01f)
        assertEquals(40f, item.boxWidth.px, 0.01f)

        val image = renderRoot(root)
        val bleedPixel = image.getRGB(45, 20)
        assertEquals(255, alphaOf(bleedPixel))
        assertTrue(redOf(bleedPixel) > 200)
    }

    @Test
    fun `regression min max constraints`() {
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
    fun `regression aspect ratio and circle`() {
        val heightDrivenRoot = measureRoot(
            Modifier().width(300.dp).height(200.dp)
        ) {
            Box(
                Modifier()
                    .height(100.dp)
                    .aspectRatio(2f)
            )
        }

        val heightDriven = heightDrivenRoot.child.single() as BoxLayout
        assertEquals(200f, heightDriven.width.px, 0.01f)
        assertEquals(100f, heightDriven.height.px, 0.01f)

        val circleRoot = measureRoot(
            Modifier().width(160.dp).height(160.dp)
        ) {
            Box(
                Modifier()
                    .fillMaxWidth()
                    .circle()
                    .background(Color.RED)
            )
        }

        val circle = circleRoot.child.single() as BoxLayout
        assertEquals(160f, circle.width.px, 0.01f)
        assertEquals(160f, circle.height.px, 0.01f)

        val circleImage = renderRoot(circleRoot)
        assertEquals(0, alphaOf(circleImage.getRGB(0, 0)))
        assertEquals(255, alphaOf(circleImage.getRGB(80, 80)))
    }

    @Test
    fun `regression relative radius and legacy border`() {
        val radiusModifier = Modifier()
            .width(120.dp)
            .height(80.dp)
            .radiusRatio(0.5f)
            .background(Color.BLUE)

        val root = measureRoot(
            Modifier().width(160.dp).height(120.dp)
        ) {
            Box(radiusModifier)
        }

        val box = root.child.single() as BoxLayout
        assertEquals(120f, box.width.px, 0.01f)
        assertEquals(80f, box.height.px, 0.01f)

        val image = renderRoot(root)
        assertEquals(0, alphaOf(image.getRGB(0, 0)))
        assertEquals(255, alphaOf(image.getRGB(60, 40)))

        val legacyBorder = Modifier().border(2.dp, 10.dp, Color.WHITE)
        assertTrue(legacyBorder.shape is BoxShape.Rounded)
        val circleSafe = Modifier().circle().border(2.dp, color = Color.WHITE)
        assertTrue(circleSafe.shape is BoxShape.Circle)
    }

    @Test
    fun `text emphasis does not change measured text layout`() {
        val plain = measureTextLayout()
        val emphasis = measureTextLayout(textEmphasis = TextEmphasis(2.dp, Color.RED))

        assertEquals(plain.width.px, emphasis.width.px, 0.01f)
        assertEquals(plain.height.px, emphasis.height.px, 0.01f)
    }

    @Test
    fun `rich text emphasis does not change measured layout`() {
        val style = TextStyle()
            .setColor(Color.BLACK)
            .setFontSize(32f)
            .setFontFamily(Fonts.default.textTypeface!!.familyName)
        val linkStyle = TextStyle()
            .setColor(Color.BLUE)
            .setFontSize(32f)
            .setFontFamily(Fonts.default.textTypeface!!.familyName)
        val paragraph = RichParagraphBuilder(style)
            .addText("Rich title ")
            .addText("with link", linkStyle)
            .build()

        val plainRoot = measureRoot(Modifier().width(320.dp).height(160.dp)) {
            RichText(paragraph, maxLinesCount = 2, modifier = Modifier().fillMaxWidth())
        }
        val emphasisRoot = measureRoot(Modifier().width(320.dp).height(160.dp)) {
            RichText(
                paragraph = paragraph,
                maxLinesCount = 2,
                modifier = Modifier().fillMaxWidth(),
                textEmphasis = TextEmphasis(1.dp),
            )
        }

        val plain = plainRoot.child.single() as RichTextLayout
        val emphasis = emphasisRoot.child.single() as RichTextLayout

        assertEquals(plain.width.px, emphasis.width.px, 0.01f)
        assertEquals(plain.height.px, emphasis.height.px, 0.01f)
    }

    @Test
    fun `rich text emphasis draws stroke and fill without duplicating image emoji`() {
        val emoji = solidImage(32, 32, Color.YELLOW)
        val style = TextStyle()
            .setColor(Color.WHITE)
            .setFontSize(42f)
            .setFontFamily(Fonts.default.textTypeface!!.familyName)
        val paragraph = RichParagraphBuilder(style)
            .addText("TEXT")
            .addEmoji("emoji", emoji, style)
            .build()

        val plainRoot = measureRoot(Modifier().width(280.dp).height(120.dp).background(Color.BLACK)) {
            RichText(paragraph = paragraph)
        }
        val emphasisRoot = measureRoot(Modifier().width(280.dp).height(120.dp).background(Color.BLACK)) {
            RichText(
                paragraph = paragraph,
                textEmphasis = TextEmphasis(4.dp, Color.RED),
            )
        }

        val plainImage = renderRoot(plainRoot)
        val emphasisImage = renderRoot(emphasisRoot)
        val yellowPredicate: (Int) -> Boolean = {
            alphaOf(it) > 0 && redOf(it) > 180 && greenOf(it) > 160 && blueOf(it) < 80
        }
        val plainYellowPixels = countPixels(plainImage, yellowPredicate)
        val emphasisYellowPixels = countPixels(emphasisImage, yellowPredicate)

        assertTrue(countPixels(emphasisImage, ::isRedDominant) > 30)
        assertTrue(countPixels(emphasisImage, ::isWhiteDominant) > 30)
        assertEquals(plainYellowPixels, emphasisYellowPixels)
    }

    @Test
    fun `performance smoke`() {
        val emojiSurface = Surface.makeRasterN32Premul(24, 24)
        emojiSurface.canvas.clear(Color.YELLOW)
        val emojiImage = emojiSurface.makeImageSnapshot()

        val richStyle = TextStyle()
            .setColor(Color.BLACK)
            .setFontSize(18f)
            .setFontFamily(Fonts.default.textTypeface!!.familyName)

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
