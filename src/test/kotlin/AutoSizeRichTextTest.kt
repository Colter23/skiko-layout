import org.jetbrains.skia.paragraph.Alignment
import org.jetbrains.skia.paragraph.TextStyle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import top.colter.skiko.Dp
import top.colter.skiko.Modifier
import top.colter.skiko.data.LayoutAlignment
import top.colter.skiko.data.RichParagraph
import top.colter.skiko.data.RichParagraphBuilder
import top.colter.skiko.data.RichParagraphLayout
import top.colter.skiko.data.RichParagraphLineMetric
import top.colter.skiko.data.TextEmphasis
import top.colter.skiko.dp
import top.colter.skiko.layout.AutoSizeRichTextLayout
import top.colter.skiko.layout.BalancedRichTextFontSizeSelector
import top.colter.skiko.layout.RichTextFontSizeCandidate

internal class AutoSizeRichTextTest {

    @BeforeEach
    fun resetDpFactor() {
        Dp.factor = 1f
    }

    @Test
    fun `默认选择器会跳过超过最大行数的候选`() {
        val exceeded = RichTextFontSizeCandidate(
            fontSize = 48.dp,
            layout = fakeLayout(width = 200f, lineCount = 1, didExceedMaxLines = true),
        )
        val normal = RichTextFontSizeCandidate(
            fontSize = 36.dp,
            layout = fakeLayout(width = 180f, lineCount = 2, didExceedMaxLines = false),
        )

        val selected = BalancedRichTextFontSizeSelector.select(
            candidates = listOf(exceeded, normal),
            contentWidth = 200f,
        )

        assertEquals(normal, selected, "默认选择器应优先选择未超过最大行数的候选")
    }

    @Test
    fun `所有候选都超过最大行数时选择器仍会回退评分`() {
        val larger = RichTextFontSizeCandidate(
            fontSize = 48.dp,
            layout = fakeLayout(width = 120f, lineCount = 1, didExceedMaxLines = true),
        )
        val smaller = RichTextFontSizeCandidate(
            fontSize = 36.dp,
            layout = fakeLayout(width = 200f, lineCount = 2, didExceedMaxLines = true),
        )

        val selected = BalancedRichTextFontSizeSelector.select(
            candidates = listOf(larger, smaller),
            contentWidth = 200f,
        )

        assertEquals(larger, selected, "全部候选都超出时不应丢弃候选，应回退到原评分逻辑")
    }

    @Test
    fun `最小字号不能大于最大字号`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            autoSizeLayout(minFontSize = 40.dp, maxFontSize = 20.dp)
        }

        assertEquals("minFontSize 需要小于等于 maxFontSize", error.message)
    }

    @Test
    fun `字号步长必须大于零`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            autoSizeLayout(fontSizeStep = 0.dp)
        }

        assertEquals("fontSizeStep 需要大于 0", error.message)
    }

    @Test
    fun `text emphasis does not affect auto size measurement or paragraph candidates`() {
        var plainBuildCount = 0
        var emphasisBuildCount = 0
        val plain = autoSizeLayout(
            paragraph = {
                plainBuildCount++
                paragraph(it)
            }
        )
        val emphasis = autoSizeLayout(
            textEmphasis = TextEmphasis(1.dp),
            paragraph = {
                emphasisBuildCount++
                paragraph(it)
            }
        )

        plain.measure(deep = true)
        emphasis.measure(deep = true)

        assertEquals(plain.selectedFontSize.px, emphasis.selectedFontSize.px, 0.01f)
        assertEquals(plain.width.px, emphasis.width.px, 0.01f)
        assertEquals(plain.height.px, emphasis.height.px, 0.01f)
        assertEquals(plainBuildCount, emphasisBuildCount)
    }

    private fun autoSizeLayout(
        minFontSize: Dp = 20.dp,
        maxFontSize: Dp = 40.dp,
        fontSizeStep: Dp = 1.dp,
        textEmphasis: TextEmphasis? = null,
        paragraph: (Dp) -> RichParagraph = ::paragraph,
    ): AutoSizeRichTextLayout {
        return AutoSizeRichTextLayout(
            minFontSize = minFontSize,
            maxFontSize = maxFontSize,
            fontSizeStep = fontSizeStep,
            maxLinesCount = 2,
            alignment = LayoutAlignment.DEFAULT,
            intrinsicAlignment = Alignment.START,
            fontSizeSelector = BalancedRichTextFontSizeSelector,
            textEmphasis = textEmphasis,
            paragraph = paragraph,
            modifier = Modifier(),
            parentLayout = null,
        )
    }

    private fun paragraph(fontSize: Dp): RichParagraph {
        val style = TextStyle()
            .setFontSize(fontSize.px)
        return RichParagraphBuilder(style)
            .addText("自适应字号测试")
            .build()
    }

    private fun fakeLayout(
        width: Float,
        lineCount: Int,
        didExceedMaxLines: Boolean,
    ): RichParagraphLayout {
        return RichParagraphLayout(
            paragraph = null,
            width = width,
            height = lineCount * 32f,
            placeholders = emptyList(),
            lineMetrics = List(lineCount) { index ->
                RichParagraphLineMetric(
                    startIndex = index,
                    endIndex = index + 1,
                    width = width,
                    left = 0f,
                    right = width,
                    height = 32f,
                    baseline = 24f,
                    isHardBreak = false,
                )
            },
            didExceedMaxLines = didExceedMaxLines,
        )
    }
}
