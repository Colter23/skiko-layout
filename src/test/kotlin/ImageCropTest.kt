import org.jetbrains.skia.Color
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import top.colter.skiko.Dp
import top.colter.skiko.Modifier
import top.colter.skiko.data.LayoutAlignment
import top.colter.skiko.dp
import top.colter.skiko.height
import top.colter.skiko.layout.BoxLayout
import top.colter.skiko.layout.Image
import top.colter.skiko.layout.LayoutBounds
import top.colter.skiko.px
import top.colter.skiko.width
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

internal class ImageCropTest {

    @BeforeEach
    fun resetDpFactor() {
        Dp.factor = 1f
    }

    @Test
    fun `Image 顶部裁剪会从源图顶部取景`() {
        val result = renderCrop(LayoutAlignment.TOP)
        val pixel = result.getRGB(10, 10)

        assertTrue(red(pixel) > 220 && green(pixel) < 40 && blue(pixel) < 40, "顶部裁剪应取到源图顶部红色区域")
    }

    @Test
    fun `Image 默认居中裁剪会从源图中间取景`() {
        val result = renderCrop(LayoutAlignment.CENTER)
        val pixel = result.getRGB(10, 10)

        assertTrue(green(pixel) > 120 && red(pixel) < 40 && blue(pixel) < 40, "居中裁剪应取到源图中间绿色区域")
    }

    private fun renderCrop(cropAlignment: LayoutAlignment): BufferedImage {
        val source = verticalBandsImage()
        val root = BoxLayout(
            alignment = LayoutAlignment.DEFAULT,
            modifier = Modifier().width(20.dp).height(20.dp),
            parentLayout = null,
        )
        root.Image(
            image = source,
            cropAlignment = cropAlignment,
            modifier = Modifier().width(20.dp).height(20.dp),
        )
        root.measure(true)
        root.place(LayoutBounds.makeXYWH(width = root.width, height = root.height))

        val surface = Surface.makeRasterN32Premul(root.width.px.toInt(), root.height.px.toInt())
        surface.canvas.clear(Color.TRANSPARENT)
        root.draw(surface.canvas)
        return ImageIO.read(ByteArrayInputStream(surface.makeImageSnapshot().encodeToData()!!.bytes))
    }

    private fun verticalBandsImage(): org.jetbrains.skia.Image {
        val surface = Surface.makeRasterN32Premul(20, 60)
        val paint = Paint()
        paint.color = Color.RED
        surface.canvas.drawRect(Rect.makeXYWH(0f, 0f, 20f, 20f), paint)
        paint.color = Color.GREEN
        surface.canvas.drawRect(Rect.makeXYWH(0f, 20f, 20f, 20f), paint)
        paint.color = Color.BLUE
        surface.canvas.drawRect(Rect.makeXYWH(0f, 40f, 20f, 20f), paint)
        return surface.makeImageSnapshot()
    }

    private fun red(argb: Int): Int = argb ushr 16 and 0xFF

    private fun green(argb: Int): Int = argb ushr 8 and 0xFF

    private fun blue(argb: Int): Int = argb and 0xFF
}
