import org.jetbrains.skia.Color
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import top.colter.skiko.*
import top.colter.skiko.layout.BoxLayout
import top.colter.skiko.layout.Image
import top.colter.skiko.layout.LayoutBounds
import top.colter.skiko.layout.ImageLayout
import top.colter.skiko.data.GradientBlur
import top.colter.skiko.data.GradientBlurInterpolation
import top.colter.skiko.data.GradientBlurStop
import top.colter.skiko.data.LayoutAlignment
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GradientBlurTest {

    @BeforeAll
    fun init() {
        Dp.factor = 1f
    }

    private fun solidImage(width: Int, height: Int, color: Int): org.jetbrains.skia.Image =
        Surface.makeRasterN32Premul(width, height).apply {
            canvas.clear(color)
        }.makeImageSnapshot()

    private fun verticalStripeImage(width: Int, height: Int, stripeWidth: Int = 2): org.jetbrains.skia.Image {
        val surface = Surface.makeRasterN32Premul(width, height)
        val paint = Paint()
        for (x in 0 until width step stripeWidth) {
            paint.color = if ((x / stripeWidth) % 2 == 0) Color.WHITE else Color.BLACK
            surface.canvas.drawRect(
                Rect.makeXYWH(x.toFloat(), 0f, stripeWidth.toFloat(), height.toFloat()),
                paint
            )
        }
        return surface.makeImageSnapshot()
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

    private fun renderRoot(root: BoxLayout): BufferedImage {
        val surface = Surface.makeRasterN32Premul(
            root.width.px.toInt().coerceAtLeast(1),
            root.height.px.toInt().coerceAtLeast(1)
        )
        surface.canvas.clear(Color.TRANSPARENT)
        root.draw(surface.canvas)
        return imageToBuffered(surface.makeImageSnapshot())
    }

    private fun imageToBuffered(image: org.jetbrains.skia.Image): BufferedImage =
        ImageIO.read(ByteArrayInputStream(image.encodeToData()!!.bytes))

    private fun blueOf(argb: Int): Int = argb and 0xFF

    private fun horizontalContrast(image: BufferedImage, xStart: Int, xEnd: Int, yStart: Int, yEnd: Int): Double {
        var total = 0.0
        var count = 0
        for (y in yStart until yEnd) {
            for (x in xStart until xEnd) {
                total += kotlin.math.abs(luma(image.getRGB(x, y)) - luma(image.getRGB(x + 1, y)))
                count++
            }
        }
        return total / count.coerceAtLeast(1)
    }

    private fun luma(argb: Int): Int {
        val r = argb ushr 16 and 0xFF
        val g = argb ushr 8 and 0xFF
        val b = argb and 0xFF
        return (r * 0.299f + g * 0.587f + b * 0.114f).toInt()
    }

    @Test
    fun `预生成 edge 渐变模糊让两侧更模糊中间保持清晰`() {
        val source = verticalStripeImage(160, 80)
        val result = imageToBuffered(
            source.gradientBlurred(
                width = 160,
                height = 80,
                blur = GradientBlur.edge(
                    maxBlur = 10.dp,
                    clearStart = 0.42f,
                    clearEnd = 0.58f,
                    steps = 5,
                    stripWidth = 2.dp,
                )
            )
        )

        val left = horizontalContrast(result, 8, 36, 30, 50)
        val center = horizontalContrast(result, 72, 88, 30, 50)

        assertTrue(center > left * 2.0, "中间清晰区域的条纹对比度应明显高于左侧模糊区域")
    }

    @Test
    fun `多级 stop 可以让中间更模糊两侧更清晰`() {
        val source = verticalStripeImage(160, 80)
        val result = imageToBuffered(
            source.gradientBlurred(
                width = 160,
                height = 80,
                blur = GradientBlur(
                    stops = listOf(
                        GradientBlurStop(0f, 0.dp),
                        GradientBlurStop(0.5f, 12.dp),
                        GradientBlurStop(1f, 0.dp),
                    ),
                    steps = 6,
                    stripWidth = 2.dp,
                    interpolation = GradientBlurInterpolation.LINEAR,
                )
            )
        )

        val left = horizontalContrast(result, 4, 24, 30, 50)
        val center = horizontalContrast(result, 72, 88, 30, 50)

        assertTrue(left > center * 2.0, "多级 stop 的中心高模糊区域应显著降低条纹对比度")
    }

    @Test
    fun `渐变模糊角度会改变模糊分布方向`() {
        val source = verticalStripeImage(160, 80)
        val horizontal = imageToBuffered(
            source.gradientBlurred(160, 80, GradientBlur.edge(10.dp, angle = 0f, clearStart = 0.42f, clearEnd = 0.58f))
        )
        val vertical = imageToBuffered(
            source.gradientBlurred(160, 80, GradientBlur.edge(10.dp, angle = 90f, clearStart = 0.42f, clearEnd = 0.58f))
        )

        val horizontalLeft = horizontalContrast(horizontal, 8, 36, 34, 46)
        val verticalLeft = horizontalContrast(vertical, 8, 36, 34, 46)

        assertTrue(verticalLeft > horizontalLeft * 1.5, "90 度渐变在垂直中线附近应保留左侧条纹清晰度")
    }

    @Test
    fun `普通 Image 支持运行时渐变模糊`() {
        val source = verticalStripeImage(160, 80)
        val root = measureRoot(Modifier().width(160.dp).height(80.dp)) {
            Image(
                image = source,
                modifier = Modifier().width(160.dp).height(80.dp),
                gradientBlur = GradientBlur.edge(10.dp, clearStart = 0.42f, clearEnd = 0.58f)
            )
        }

        val result = renderRoot(root)
        val layout = root.child.single() as ImageLayout
        val left = horizontalContrast(result, 8, 36, 30, 50)
        val center = horizontalContrast(result, 72, 88, 30, 50)

        assertEquals(160f, layout.width.px, 0.01f)
        assertTrue(center > left * 2.0, "Image 运行时渐变模糊应保留中间清晰区域")
    }

    @Test
    fun `背景图支持运行时渐变模糊`() {
        val source = verticalStripeImage(160, 80)
        val root = measureRoot(
            Modifier().width(160.dp).height(80.dp).background(
                image = source,
                imageGradientBlur = GradientBlur.edge(10.dp, clearStart = 0.42f, clearEnd = 0.58f)
            )
        ) { }

        val result = renderRoot(root)
        val left = horizontalContrast(result, 8, 36, 30, 50)
        val center = horizontalContrast(result, 72, 88, 30, 50)

        assertTrue(center > left * 2.0, "背景图运行时渐变模糊应保留中间清晰区域")
    }

    @Test
    fun `背景图渐变模糊仍先于颜色遮罩绘制`() {
        val root = measureRoot(
            Modifier()
                .width(20.dp)
                .height(20.dp)
                .background(
                    color = Color.BLACK.withAlpha(0.5f),
                    image = solidImage(20, 20, Color.BLUE),
                    imageGradientBlur = GradientBlur.edge(4.dp)
                )
        ) { }

        val pixel = renderRoot(root).getRGB(10, 10)

        assertTrue(blueOf(pixel) in 120..135)
    }

    @Test
    fun `渐变模糊可以预生成并写入文件`() {
        val source = verticalStripeImage(40, 20)
        val file = File("build/test-output/gradient-blur/pre-rendered.png")

        source.writeGradientBlurred(file, 40, 20, GradientBlur.edge(4.dp))

        assertTrue(file.exists() && file.length() > 0)
    }

    @Test
    fun `渐变模糊参数校验`() {
        assertThrows(IllegalArgumentException::class.java) {
            GradientBlur(stops = listOf(GradientBlurStop(0f, 0.dp)))
        }
        assertThrows(IllegalArgumentException::class.java) {
            GradientBlur(stops = listOf(GradientBlurStop(0.1f, 0.dp), GradientBlurStop(1f, 0.dp)))
        }
        assertThrows(IllegalArgumentException::class.java) {
            GradientBlur(stops = listOf(GradientBlurStop(0f, 0.dp), GradientBlurStop(1f, (-1).dp)))
        }
        assertThrows(IllegalArgumentException::class.java) {
            GradientBlur(
                stops = listOf(GradientBlurStop(0f, 0.dp), GradientBlurStop(1f, 0.dp)),
                steps = 1,
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            GradientBlur(
                stops = listOf(GradientBlurStop(0f, 0.dp), GradientBlurStop(1f, 0.dp)),
                stripWidth = 0.dp,
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            GradientBlur.edge(maxBlur = 4.dp, clearStart = 0f, clearEnd = 0.6f)
        }
        assertThrows(IllegalArgumentException::class.java) {
            GradientBlur.edge(maxBlur = 4.dp, clearStart = 0.6f, clearEnd = 1f)
        }
        assertThrows(IllegalArgumentException::class.java) {
            GradientBlur.edge(maxBlur = 4.dp, clearStart = 0.7f, clearEnd = 0.6f)
        }
    }
}
