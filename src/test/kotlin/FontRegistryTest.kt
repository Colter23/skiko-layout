import org.jetbrains.skia.Color
import org.jetbrains.skia.Surface
import org.jetbrains.skia.paragraph.TextStyle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import top.colter.skiko.FontRegistry
import top.colter.skiko.Modifier
import top.colter.skiko.data.LayoutAlignment
import top.colter.skiko.data.RichParagraphBuilder
import top.colter.skiko.data.layout
import top.colter.skiko.dp
import top.colter.skiko.layout.BoxLayout
import top.colter.skiko.layout.LayoutBounds
import top.colter.skiko.layout.RichText
import top.colter.skiko.layout.Text
import top.colter.skiko.layout.TextLayout
import top.colter.skiko.px
import top.colter.skiko.width
import java.io.File

internal class FontRegistryTest {
    private val testResource = File("src/test/resources")

    @Test
    fun `generic family resolves to registry text typeface`() {
        val registry = testRegistry()
        val textTypeface = registry.textTypeface!!

        val style = TextStyle().setFontSize(24f)
        style.fontFamilies = arrayOf("sans-serif")

        val resolved = registry.resolveTextStyle(style)

        assertEquals(textTypeface.familyName, resolved.typeface?.familyName)
        assertEquals(FontRegistry.TEXT_FAMILY, resolved.fontFamilies.single())
    }

    @Test
    fun `explicit family is not replaced by registry text typeface`() {
        val registry = testRegistry()
        val emojiTypeface = registry.emojiTypeface!!

        val style = TextStyle().setFontSize(24f)
        style.fontFamilies = arrayOf(emojiTypeface.familyName)

        val resolved = registry.resolveTextStyle(style)

        assertEquals(emojiTypeface.familyName, resolved.typeface?.familyName)
        assertNotEquals(FontRegistry.TEXT_FAMILY, resolved.fontFamilies.single())
    }

    @Test
    fun `text layout resolves generic family before paragraph build`() {
        val registry = testRegistry()
        val style = TextStyle().setColor(Color.BLACK).setFontSize(24f)
        style.fontFamilies = arrayOf("sans-serif")

        val root = measureRoot(registry) {
            Text(
                text = "font test",
                textStyle = style,
            )
        }

        val textLayout = root.child.single() as TextLayout
        assertEquals(registry.textTypeface?.familyName, textLayout.textStyle.typeface?.familyName)
        assertEquals(FontRegistry.TEXT_FAMILY, textLayout.textStyle.fontFamilies.single())
    }

    @Test
    fun `rich paragraph uses registry fonts for measure and draw`() {
        val registry = testRegistry()
        val style = TextStyle().setColor(Color.BLACK).setFontSize(24f)
        style.fontFamilies = arrayOf("sans-serif")
        val paragraph = RichParagraphBuilder(style)
            .addText("富文本字体测试")
            .build()

        val layout = paragraph.layout(300f, fontRegistry = registry)
        assertTrue(layout.height > 0f)

        val root = measureRoot(registry) {
            RichText(paragraph)
        }
        val surface = Surface.makeRasterN32Premul(
            root.width.px.toInt().coerceAtLeast(1),
            root.height.px.toInt().coerceAtLeast(1),
        )
        surface.canvas.clear(Color.TRANSPARENT)
        root.draw(surface.canvas)
    }

    private fun testRegistry(): FontRegistry {
        val registry = FontRegistry()
        registry.loadTextTypeface(testResource.resolve("font").resolve("LXGWWenKai-Bold.ttf").absolutePath)
        registry.loadEmojiTypeface(testResource.resolve("font").resolve("NotoColorEmoji.ttf").absolutePath)
        return registry
    }

    private fun measureRoot(
        fontRegistry: FontRegistry,
        content: BoxLayout.() -> Unit,
    ): BoxLayout {
        val root = BoxLayout(
            alignment = LayoutAlignment.DEFAULT,
            modifier = Modifier().width(500.dp),
            parentLayout = null,
            fontRegistry = fontRegistry,
        )
        root.content()
        root.measure(true)
        root.place(LayoutBounds.makeXYWH(width = root.width, height = root.height))
        return root
    }
}
