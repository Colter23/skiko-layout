package top.colter.skiko.layout

import org.jetbrains.skia.Canvas
import org.jetbrains.skia.paragraph.Paragraph
import org.jetbrains.skia.paragraph.Alignment
import top.colter.skiko.Dp
import top.colter.skiko.FontRegistry
import top.colter.skiko.Fonts
import top.colter.skiko.Modifier
import top.colter.skiko.data.LayoutAlignment
import top.colter.skiko.data.RichParagraph
import top.colter.skiko.data.RichParagraphLayout
import top.colter.skiko.data.TextEmphasis
import top.colter.skiko.data.activeTextEmphasis
import top.colter.skiko.data.emphasisParagraph
import top.colter.skiko.data.layout
import top.colter.skiko.data.print
import top.colter.skiko.data.place
import top.colter.skiko.dp
import top.colter.skiko.toDp
import top.colter.skiko.data.Edge


/**
 * ## 富文本
 *
 * [paragraph] 文章 使用 [RichParagraphBuilder] 富文本构造器构建
 *
 * [maxLinesCount] 最大行数 默认 50 行
 *
 * @param paragraph 文章
 * @param maxLinesCount 最大行数
 * @param alignment 对齐
 * @param modifier 样式
 */
public fun Layout.RichText(
    paragraph: RichParagraph,
    maxLinesCount: Int = 50,
    alignment: LayoutAlignment = LayoutAlignment.DEFAULT,
    intrinsicAlignment: Alignment = Alignment.START,
    modifier: Modifier = Modifier(),
    textEmphasis: TextEmphasis? = null,
) {
    Layout(
        layout = RichTextLayout(
            paragraph = paragraph,
            alignment = alignment,
            intrinsicAlignment = intrinsicAlignment,
            maxLinesCount = maxLinesCount,
            textEmphasis = textEmphasis,
            modifier = modifier,
            parentLayout = this
        ),
        content = {},
    )
}

public class RichTextLayout(
    public val paragraph: RichParagraph,
    public val alignment: LayoutAlignment,
    public val intrinsicAlignment: Alignment,
    public val maxLinesCount: Int,
    public val textEmphasis: TextEmphasis? = null,
    modifier: Modifier,
    parentLayout: Layout?,
    fontRegistry: FontRegistry = parentLayout?.fontRegistry ?: Fonts.default,
) : Layout(modifier, parentLayout, fontRegistry) {

    private var cachedParagraph: RichParagraphLayout? = null
    private var cachedEmphasisParagraph: Paragraph? = null
    private var layoutWidthPx: Float = -1f

    private fun resolveParagraph(widthPx: Float): RichParagraphLayout {
        val safeWidth = widthPx.coerceAtLeast(1f)
        val cached = cachedParagraph
        if (cached != null && layoutWidthPx == safeWidth) return cached

        val result = paragraph.layout(safeWidth, maxLinesCount, fontRegistry, intrinsicAlignment)
        cachedEmphasisParagraph = paragraph.emphasisParagraph(
            width = safeWidth,
            maxLinesCount = maxLinesCount,
            fontRegistry = fontRegistry,
            alignment = intrinsicAlignment,
            textEmphasis = textEmphasis,
        )
        cachedParagraph = result
        layoutWidthPx = safeWidth
        return result
    }

    override fun measure(deep: Boolean) {
        preMeasure()

        if (modifier.fillWidth && width.isNull()) {
            finishMeasure()
            if (width.isNull()) return
        }

        val availableWidth = availableParentWidth()
        val maxOuterWidth = when {
            width.isNotNull() -> width
            modifier.maxWidth.isNotNull() -> modifier.maxWidth
            availableWidth.isNotNull() -> availableWidth
            else -> Dp.NULL
        }
        val maxContentWidth = if (maxOuterWidth.isNotNull())
            (maxOuterWidth - resolvedPadding.horizontal).coerceAtLeast(0.dp)
        else 10000.dp

        val paragraphWidth = (maxContentWidth.px - RICH_TEXT_VISUAL_OUTSET_HORIZONTAL).coerceAtLeast(1f)
        val layout = resolveParagraph(paragraphWidth)
        if (width.isNull()) {
            width = (layout.width + RICH_TEXT_VISUAL_OUTSET_HORIZONTAL)
                .coerceAtMost(maxContentWidth.px.coerceAtLeast(0f))
                .toDp() +
                    resolvedPadding.horizontal
        }
        if (height.isNull()) height = (layout.height + RICH_TEXT_VISUAL_OUTSET_VERTICAL).toDp() + resolvedPadding.vertical

        finishMeasure()
    }

    override fun place(bounds: LayoutBounds) {
        position = alignment.place(width, height, resolvedMargin, bounds)
        resolvePaintBounds()
    }

    override fun draw(canvas: Canvas) {
        drawBgBox(canvas, contentClipOutset = richTextContentClipOutset(cachedParagraph, textEmphasis)) {
            val layout = cachedParagraph ?: return@drawBgBox
            layout.print(
                canvas = canvas,
                x = it.left + RICH_TEXT_VISUAL_OUTSET,
                y = it.top + RICH_TEXT_VISUAL_OUTSET,
                emphasisParagraph = cachedEmphasisParagraph,
            )
        }
    }
}

internal const val RICH_TEXT_VISUAL_OUTSET: Float = 1f
internal const val RICH_TEXT_VISUAL_OUTSET_HORIZONTAL: Float = RICH_TEXT_VISUAL_OUTSET * 2f
internal const val RICH_TEXT_VISUAL_OUTSET_VERTICAL: Float = RICH_TEXT_VISUAL_OUTSET * 2f
internal fun richTextContentClipOutset(
    layout: RichParagraphLayout?,
    textEmphasis: TextEmphasis? = null,
): Edge = textEffectContentClipOutset(layout?.lineBoxClipOutset ?: 0f, textEmphasis)

internal fun textEffectContentClipOutset(
    verticalOutset: Float,
    textEmphasis: TextEmphasis? = null,
): Edge {
    val emphasisOutset = textEmphasis.activeTextEmphasis()?.width ?: 0.dp
    return Edge(
        top = verticalOutset.toDp() + emphasisOutset,
        right = emphasisOutset,
        bottom = verticalOutset.toDp() + emphasisOutset,
        left = emphasisOutset,
    )
}

internal fun verticalContentClipOutset(outset: Float): Edge =
    if (outset <= 0f) Edge() else Edge(top = outset.toDp(), bottom = outset.toDp())
