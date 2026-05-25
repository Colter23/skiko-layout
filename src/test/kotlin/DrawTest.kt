import org.jetbrains.skia.*
import org.jetbrains.skia.paragraph.TextStyle
import org.junit.jupiter.api.Assertions.assertEquals
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

    private fun alphaOf(argb: Int): Int = argb ushr 24 and 0xFF

    @BeforeAll
    fun init() {
        Dp.factor = 1f

        FontUtils.loadTypeface(loadTestResource("font", "LXGWWenKai-Bold.ttf").absolutePath)
        FontUtils.loadEmojiTypeface(loadTestResource("font", "NotoColorEmoji.ttf").absolutePath)
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
    fun `regression rich text padding and max lines`() {
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
    fun `performance smoke`() {
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
