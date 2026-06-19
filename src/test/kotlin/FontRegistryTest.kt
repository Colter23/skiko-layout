import org.jetbrains.skia.Color
import org.jetbrains.skia.Font
import org.jetbrains.skia.Surface
import org.jetbrains.skia.paragraph.TextStyle
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import top.colter.skiko.FontRegistry
import top.colter.skiko.FontTypographySettings
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
        assertArrayEquals(arrayOf(FontRegistry.TEXT_FAMILY, FontRegistry.EMOJI_FAMILY), resolved.fontFamilies)
    }

    @Test
    fun `explicit text family keeps primary family and appends emoji fallback`() {
        val registry = testRegistry()
        val textTypeface = registry.textTypeface!!

        val style = TextStyle().setFontSize(24f)
        style.fontFamilies = arrayOf(textTypeface.familyName)

        val resolved = registry.resolveTextStyle(style)

        assertEquals(textTypeface.familyName, resolved.typeface?.familyName)
        assertArrayEquals(arrayOf(textTypeface.familyName, FontRegistry.EMOJI_FAMILY), resolved.fontFamilies)
    }

    @Test
    fun `explicit emoji family is not duplicated as fallback`() {
        val registry = testRegistry()
        val emojiTypeface = registry.emojiTypeface!!

        val style = TextStyle().setFontSize(24f)
        style.fontFamilies = arrayOf(emojiTypeface.familyName)

        val resolved = registry.resolveTextStyle(style)

        assertEquals(emojiTypeface.familyName, resolved.typeface?.familyName)
        assertArrayEquals(arrayOf(emojiTypeface.familyName), resolved.fontFamilies)
    }

    @Test
    fun `registry without emoji typeface keeps original resolved families`() {
        val registry = FontRegistry()
        registry.loadTextTypeface(testResource.resolve("font").resolve("LXGWWenKai-Bold.ttf").absolutePath)

        val style = TextStyle().setFontSize(24f)
        style.fontFamilies = arrayOf("sans-serif")

        val resolved = registry.resolveTextStyle(style)

        assertEquals(registry.textTypeface?.familyName, resolved.typeface?.familyName)
        assertArrayEquals(arrayOf(FontRegistry.TEXT_FAMILY), resolved.fontFamilies)
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
        assertArrayEquals(arrayOf(FontRegistry.TEXT_FAMILY, FontRegistry.EMOJI_FAMILY), textLayout.textStyle.fontFamilies)
    }

    @Test
    fun `rich paragraph uses registry fonts for measure and draw`() {
        val registry = testRegistry()
        val style = TextStyle().setColor(Color.BLACK).setFontSize(24f)
        style.fontFamilies = arrayOf("sans-serif")
        val paragraph = RichParagraphBuilder(style)
            .addText("富文本字体测试 😍❤️")
            .build()

        val resolved = registry.resolveTextStyle(style)
        assertArrayEquals(arrayOf(FontRegistry.TEXT_FAMILY, FontRegistry.EMOJI_FAMILY), resolved.fontFamilies)

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

    @Test
    fun `typography normalization is disabled by default`() {
        val registry = testRegistry()
        val style = TextStyle().setFontSize(24f)

        val resolved = registry.resolveTextStyle(style)

        assertNull(resolved.height)
    }

    @Test
    fun `typography normalization sets bounded line height`() {
        val registry = testRegistry()
        registry.typographySettings = FontTypographySettings.NORMALIZED
        val style = TextStyle().setFontSize(24f)

        val resolved = registry.resolveTextStyle(style)

        assertTrue(resolved.height!! in 1.16f..1.58f)
    }

    @Test
    fun `line height scale changes normalized result`() {
        val style = TextStyle().setFontSize(24f)

        val smallerRegistry = testRegistry().apply {
            typographySettings = FontTypographySettings.NORMALIZED.copy(lineHeightScale = 0.9f)
        }
        val defaultRegistry = testRegistry().apply {
            typographySettings = FontTypographySettings.NORMALIZED
        }
        val largerRegistry = testRegistry().apply {
            typographySettings = FontTypographySettings.NORMALIZED.copy(lineHeightScale = 1.1f)
        }

        val smaller = smallerRegistry.resolveTextStyle(style).height!!
        val default = defaultRegistry.resolveTextStyle(style).height!!
        val larger = largerRegistry.resolveTextStyle(style).height!!

        assertTrue(smaller < default)
        assertTrue(default < larger)
    }

    @Test
    fun `explicit line height is preserved when typography normalization is enabled`() {
        val registry = testRegistry()
        registry.typographySettings = FontTypographySettings.NORMALIZED
        val style = TextStyle()
            .setFontSize(24f)
            .setHeight(1.8f)

        val resolved = registry.resolveTextStyle(style)

        assertEquals(1.8f, resolved.height)
    }

    @Test
    fun `letter spacing em is applied after style resolution`() {
        val registry = testRegistry()
        registry.typographySettings = FontTypographySettings(letterSpacingEm = 0.02f)
        val style = TextStyle()
            .setFontSize(25f)
            .setLetterSpacing(1f)

        val resolved = registry.resolveTextStyle(style)

        assertEquals(1.5f, resolved.letterSpacing, 0.001f)
    }

    @Test
    fun `text layout keeps measured height stable when normalized line height is smaller than font metrics`() {
        val registry = testRegistry()
        registry.typographySettings = FontTypographySettings.NORMALIZED.copy(lineHeightScale = 0.9f)
        val style = TextStyle().setColor(Color.BLACK).setFontSize(34f)
        val resolved = registry.resolveTextStyle(style)
        val naturalHeight = Font(resolved.typeface, resolved.fontSize).metrics.height
        val lineBoxHeight = resolved.height!! * resolved.fontSize

        val root = measureRoot(registry) {
            Text(
                text = "荷叶糯米节安乐快康g.ip",
                textStyle = style,
            )
        }

        val textLayout = root.child.single() as TextLayout
        assertTrue(lineBoxHeight < naturalHeight)
        assertTrue(textLayout.height.px <= lineBoxHeight + 1f)
    }

    @Test
    fun `rich paragraph exposes line box clip safety for normalized text`() {
        val registry = testRegistry()
        registry.typographySettings = FontTypographySettings.NORMALIZED.copy(lineHeightScale = 0.9f)
        val style = TextStyle().setColor(Color.BLACK).setFontSize(42f)
        val paragraph = RichParagraphBuilder(style)
            .addText("Dynamic title g.ip")
            .build()

        val layout = paragraph.layout(400f, fontRegistry = registry)

        assertTrue(layout.lineBoxClipOutset > 0f)
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
