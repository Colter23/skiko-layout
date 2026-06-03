import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Color
import org.jetbrains.skia.Paint
import org.jetbrains.skia.RRect
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkiaLayerRenderDelegate
import org.jetbrains.skiko.SkikoRenderDelegate
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import top.colter.skiko.data.Shadow
import top.colter.skiko.drawRectShadowAntiAlias
import java.awt.Dimension
import java.time.Instant
import javax.swing.JFrame
import javax.swing.SwingUtilities
import javax.swing.WindowConstants


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class Test {

//    @Test
//    fun `test paragraph`(): Unit = runBlocking {
//        val skiaLayer = SkiaLayer()
//        skiaLayer.renderDelegate = SkiaLayerRenderDelegate(skiaLayer, object : SkikoRenderDelegate {
//            val paint = Paint().apply {
//                color = Color.RED
//            }
//            override fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
//                canvas.clear(Color.CYAN)
//                val ts = nanoTime / 5_000_000
//                canvas.drawCircle( (ts % width).toFloat(), (ts % height).toFloat(), 20f, paint )
//                val t1 = Instant.now().toEpochMilli()
//                Shadow.ELEVATION_3.forEach {
//                    canvas.drawRectShadowAntiAlias(RRect.makeXYWH((ts % width).toFloat(), (ts % height).toFloat(),
//                        width.toFloat(), height.toFloat(), width.toFloat()), it)
//                }
//                val t2 = Instant.now().toEpochMilli()
//                println(t2-t1)
//            }
//        })
//        SwingUtilities.invokeLater {
//            val window = JFrame("Skiko example").apply {
//                defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
//                preferredSize = Dimension(800, 600)
//            }
//            skiaLayer.attachTo(window.contentPane)
//            skiaLayer.needRedraw()
//            window.pack()
//            window.isVisible = true
//        }
//
//        Thread.sleep(10000)
//    }

}