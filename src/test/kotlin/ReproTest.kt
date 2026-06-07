import org.jetbrains.skia.Color
import org.jetbrains.skia.Surface
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import top.colter.skiko.*
import top.colter.skiko.data.BoxShape
import top.colter.skiko.data.LayoutAlignment
import top.colter.skiko.data.Ratio
import top.colter.skiko.layout.*
import top.colter.skiko.layout.Image

/**
 * 针对历史 bug 的回归测试。
 *
 * #1  Dp.NULL 曾等于 0.dp，导致合法的 0 尺寸与“未设置”无法区分。
 * #10 circle()/shape() 之后调用 border(width, radius) 会被 radius 覆盖成 Rounded。
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ReproTest {

    private fun solidImage(w: Int, h: Int, color: Int) =
        Surface.makeRasterN32Premul(w, h).apply { canvas.clear(color) }.makeImageSnapshot()

    private fun measureRoot(modifier: Modifier, content: BoxLayout.() -> Unit): BoxLayout {
        Dp.factor = 1f
        val root = BoxLayout(LayoutAlignment.DEFAULT, modifier, null)
        root.content()
        root.measure(true)
        root.place(LayoutBounds.makeXYWH(width = root.width, height = root.height))
        return root
    }

    /** #1a: 0.dp 是合法值，不应被判定为 NULL（未设置）。 */
    @Test
    fun `zero dp is not null`() {
        assertFalse(0.dp.isNull(), "0.dp 不应等于 Dp.NULL")
        assertTrue(Dp.NULL.isNull(), "Dp.NULL 应判定为 NULL")
        assertEquals(0f, 0.dp.px, 0f)
    }

    /**
     * #1c: Column 分配给 fill 子元素的剩余高度为 0 时，0 应被尊重，
     * 子元素高度为 0，而不是被当作“未设置”后按内容比例膨胀溢出。
     */
    @Test
    fun `fill child respects zero remaining height`() {
        val square = solidImage(100, 100, Color.RED)

        val root = measureRoot(Modifier().width(200.dp).height(100.dp)) {
            Column(Modifier().fillMaxWidth().height(100.dp)) {
                Box(Modifier().fillMaxWidth().height(100.dp))
                Image(image = square, ratio = Ratio.SQUARE, modifier = Modifier().fillHeight())
            }
        }

        val column = root.child.single() as ColumnLayout
        val fillImage = column.child[1] as ImageLayout

        assertEquals(0f, fillImage.height.px, 0.01f,
            "fill 子元素分到 0 剩余空间，高度应为 0，实际 ${fillImage.height.px}px")
    }

    /** #10: circle() 之后 border(width, radius) 不应把 shape 覆盖成 Rounded。 */
    @Test
    fun `border radius does not override circle shape`() {
        val m = Modifier().circle().border(2.dp, 10.dp, Color.WHITE)
        assertTrue(m.shape is BoxShape.Circle,
            "circle() 之后 border 不应覆盖 shape，实际 ${m.shape}")
    }

    /** #10 反向验证：默认 Rectangle 时 border(radius) 仍应推导出圆角。 */
    @Test
    fun `border radius still derives rounded on default shape`() {
        val m = Modifier().border(2.dp, 10.dp, Color.WHITE)
        assertTrue(m.shape is BoxShape.Rounded,
            "默认形状下 border(radius) 应推导出 Rounded，实际 ${m.shape}")
    }
}
