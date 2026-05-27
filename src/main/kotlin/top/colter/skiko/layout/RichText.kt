package top.colter.skiko.layout

import org.jetbrains.skia.Canvas
import top.colter.skiko.Dp
import top.colter.skiko.FontRegistry
import top.colter.skiko.Fonts
import top.colter.skiko.Modifier
import top.colter.skiko.data.LayoutAlignment
import top.colter.skiko.data.RichParagraph
import top.colter.skiko.data.layout
import top.colter.skiko.data.print
import top.colter.skiko.data.place
import top.colter.skiko.dp
import top.colter.skiko.toDp


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
    modifier: Modifier = Modifier(),
) {
    Layout(
        layout = RichTextLayout(
            paragraph = paragraph,
            alignment = alignment,
            maxLinesCount = maxLinesCount,
            modifier = modifier,
            parentLayout = this
        ),
        content = {},
    )
}

public class RichTextLayout(
    public val paragraph: RichParagraph,
    public val alignment: LayoutAlignment,
    public val maxLinesCount: Int,
    modifier: Modifier,
    parentLayout: Layout?,
    fontRegistry: FontRegistry = parentLayout?.fontRegistry ?: Fonts.default,
) : Layout(modifier, parentLayout, fontRegistry) {

    private var cachedParagraph: RichParagraph? = null
    private var layoutWidthPx: Float = -1f

    private fun resolveParagraph(widthPx: Float): RichParagraph {
        val safeWidth = widthPx.coerceAtLeast(1f)
        val cached = cachedParagraph
        if (cached != null && layoutWidthPx == safeWidth) return cached

        val result = paragraph.layout(safeWidth, maxLinesCount, fontRegistry)
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

        val layout = resolveParagraph(maxContentWidth.px)
        if (width.isNull()) {
            width = layout.width.coerceAtMost(maxContentWidth.px.coerceAtLeast(0f)).toDp() +
                    resolvedPadding.horizontal
        }
        if (height.isNull()) height = layout.height.toDp() + resolvedPadding.vertical

        finishMeasure()
    }

    override fun place(bounds: LayoutBounds) {
        position = alignment.place(width, height, resolvedMargin, bounds)
        resolvePaintBounds()
    }

    override fun draw(canvas: Canvas) {
        drawBgBox(canvas) {
            cachedParagraph?.print(canvas, it.left, it.top, fontRegistry)
        }
    }
}
