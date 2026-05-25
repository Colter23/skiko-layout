package top.colter.skiko.layout

import org.jetbrains.skia.*
import org.jetbrains.skia.paragraph.*
import top.colter.skiko.*
import top.colter.skiko.data.LayoutAlignment
import top.colter.skiko.data.place
import top.colter.skiko.data.toAlignment
import kotlin.math.min


/**
 * ## 纯文本
 *
 * @param text 文本内容
 * @param color 文本颜色
 * @param fontSize 字体大小
 * @param fontFamily 字体名
 * @param fontStyle 字体样式
 * @param maxLinesCount 最大行数
 * @param alignment 外部对齐
 * @param intrinsicAlignment 内部对齐
 * @param modifier 样式
 */
public fun Layout.Text(
    text: String,
    color: Int = Color.BLACK,
    fontSize: Dp = 18.dp,
    fontFamily: String = "Source Han Sans",
    fontStyle: FontStyle = FontStyle.NORMAL,
    maxLinesCount: Int = 1,
    alignment: LayoutAlignment = LayoutAlignment.DEFAULT,
    intrinsicAlignment: LayoutAlignment = LayoutAlignment.DEFAULT,
    modifier: Modifier = Modifier()
) {
    Text(
        text = text,
        textStyle = TextStyle().apply {
            this.color = color
            this.fontSize = fontSize.px
            this.fontFamilies = arrayOf(fontFamily)
            this.fontStyle = fontStyle
        },
        alignment = alignment,
        intrinsicAlignment = intrinsicAlignment,
        maxLinesCount = maxLinesCount,
        modifier = modifier,
    )
}
/**
 * ## 纯文本
 *
 * @param text 文本内容
 * @param textStyle 文本样式
 * @param maxLinesCount 最大行数
 * @param alignment 外部对齐
 * @param intrinsicAlignment 内部对齐
 * @param modifier 样式
 */
public fun Layout.Text(
    text: String,
    textStyle: TextStyle,
    maxLinesCount: Int = 1,
    alignment: LayoutAlignment = LayoutAlignment.DEFAULT,
    intrinsicAlignment: LayoutAlignment = LayoutAlignment.DEFAULT,
    modifier: Modifier = Modifier()
) {
    if (textStyle.typeface != null) {
        val fonts = FontUtils.fonts.findTypefaces(arrayOf(textStyle.typeface!!.familyName), FontStyle.NORMAL)
        if (fonts.isEmpty()) {
            FontUtils.loadTypeface(textStyle.typeface!!)
        }
    }

    Layout(
        layout = TextLayout(
            text = text,
            textStyle = textStyle,
            alignment = alignment,
            intrinsicAlignment = intrinsicAlignment,
            maxLinesCount = maxLinesCount,
            modifier = modifier,
            parentLayout = this,
        ),
        content = {},
    )
}

public class TextLayout(
    public val text: String,
    public val textStyle: TextStyle,
    public val alignment: LayoutAlignment,
    public val intrinsicAlignment: LayoutAlignment,
    public val maxLinesCount: Int,
    modifier: Modifier,
    parentLayout: Layout
) : Layout(modifier, parentLayout) {

    private val paragraphStyle = ParagraphStyle().apply {
        maxLinesCount = this@TextLayout.maxLinesCount
        ellipsis = "..."
        alignment = this@TextLayout.intrinsicAlignment.toAlignment()
        textStyle = this@TextLayout.textStyle
    }

    private var cachedParagraph: Paragraph? = null
    private var layoutWidthPx: Float = -1f

    private fun resolveParagraph(widthPx: Float): Paragraph {
        val safeWidth = widthPx.coerceAtLeast(1f)
        val cached = cachedParagraph
        if (cached != null && layoutWidthPx == safeWidth) return cached

        val paragraph = ParagraphBuilder(paragraphStyle, FontUtils.fonts)
            .addText(text)
            .build()
            .layout(safeWidth)
        cachedParagraph = paragraph
        layoutWidthPx = safeWidth
        return paragraph
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

        val paragraph = resolveParagraph(maxContentWidth.px)
        if (width.isNull()) {
            width = min(paragraph.maxIntrinsicWidth, maxContentWidth.px.coerceAtLeast(0f)).toDp() +
                    resolvedPadding.horizontal
        }
        if (height.isNull()) height = paragraph.height.toDp() + resolvedPadding.vertical

        finishMeasure()
    }

    override fun place(bounds: LayoutBounds) {
        position = alignment.place(width, height, resolvedMargin, bounds)
    }

    override fun draw(canvas: Canvas) {
        drawBgBox(canvas) {
            cachedParagraph?.paint(canvas, it.left, it.top)
        }
    }
}
