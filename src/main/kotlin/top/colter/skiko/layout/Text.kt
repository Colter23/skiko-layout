package top.colter.skiko.layout

import org.jetbrains.skia.*
import org.jetbrains.skia.paragraph.*
import top.colter.skiko.*
import top.colter.skiko.data.LayoutAlignment
import top.colter.skiko.data.TextEmphasis
import top.colter.skiko.data.TextShadow
import top.colter.skiko.data.TextStroke
import top.colter.skiko.data.activeTextEmphasis
import top.colter.skiko.data.place
import top.colter.skiko.data.toAlignment
import kotlin.math.max
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
    fontFamily: String = "",
    fontStyle: FontStyle = FontStyle.NORMAL,
    maxLinesCount: Int = 1,
    alignment: LayoutAlignment = LayoutAlignment.DEFAULT,
    intrinsicAlignment: LayoutAlignment = LayoutAlignment.DEFAULT,
    modifier: Modifier = Modifier(),
    stroke: TextStroke? = null,
    textEmphasis: TextEmphasis? = null,
    textShadows: List<TextShadow> = emptyList(),
) {
    Text(
        text = text,
        textStyle = TextStyle().apply {
            this.color = color
            this.fontSize = fontSize.px
            if (fontFamily.isNotBlank()) this.fontFamilies = arrayOf(fontFamily)
            this.fontStyle = fontStyle
        },
        alignment = alignment,
        intrinsicAlignment = intrinsicAlignment,
        maxLinesCount = maxLinesCount,
        modifier = modifier,
        stroke = stroke,
        textEmphasis = textEmphasis,
        textShadows = textShadows,
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
    modifier: Modifier = Modifier(),
    stroke: TextStroke? = null,
    textEmphasis: TextEmphasis? = null,
    textShadows: List<TextShadow> = emptyList(),
) {
    val resolvedTextStyle = fontRegistry.resolveTextStyle(textStyle).withTextShadows(textShadows)

    Layout(
        layout = TextLayout(
            text = text,
            textStyle = resolvedTextStyle,
            alignment = alignment,
            intrinsicAlignment = intrinsicAlignment,
            maxLinesCount = maxLinesCount,
            modifier = modifier,
            parentLayout = this,
            stroke = stroke,
            textEmphasis = textEmphasis,
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
    parentLayout: Layout?,
    fontRegistry: FontRegistry = parentLayout?.fontRegistry ?: Fonts.default,
    public val stroke: TextStroke? = null,
    public val textEmphasis: TextEmphasis? = null,
) : Layout(modifier, parentLayout, fontRegistry) {

    private val visualOutset: TextVisualOutset = textVisualOutset(stroke, textStyle.shadows)
    private val lineBoxClipOutset: Float = textStyle.lineBoxClipOutset()

    private var cachedFillParagraph: Paragraph? = null
    private var cachedStrokeParagraph: Paragraph? = null
    private var cachedEmphasisParagraph: Paragraph? = null
    private var cachedShadowParagraphs: List<TextShadowParagraph> = emptyList()
    private var cachedFillNoShadowParagraph: Paragraph? = null
    private var layoutWidthPx: Float = -1f

    private fun paragraphStyle(style: TextStyle): ParagraphStyle = ParagraphStyle().apply {
        maxLinesCount = this@TextLayout.maxLinesCount
        ellipsis = "..."
        alignment = this@TextLayout.intrinsicAlignment.toAlignment()
        textStyle = style
    }

    private fun buildParagraph(style: TextStyle, widthPx: Float): Paragraph =
        ParagraphBuilder(paragraphStyle(style), fontRegistry.fonts)
            .addText(text)
            .build()
            .layout(widthPx)

    private fun strokeTextStyle(stroke: TextStroke): TextStyle {
        val strokePaint = Paint().apply {
            color = stroke.color
            mode = PaintMode.STROKE
            strokeWidth = stroke.width.px
            isAntiAlias = true
        }
        return textStyle.copyStyle().apply {
            clearShadows()
            setForeground(strokePaint)
        }
    }

    private fun emphasisTextStyle(textEmphasis: TextEmphasis): TextStyle {
        val strokePaint = Paint().apply {
            color = textEmphasis.color ?: textStyle.foreground?.color ?: textStyle.color
            mode = PaintMode.STROKE
            strokeWidth = textEmphasis.width.px
            isAntiAlias = true
        }
        return textStyle.copyStyle().apply {
            clearShadows()
            setForeground(strokePaint)
        }
    }

    private fun fillShadowTextStyle(color: Int): TextStyle =
        textStyle.copyStyle().apply {
            clearShadows()
            setForeground(Paint().apply {
                this.color = color
                isAntiAlias = true
            })
        }

    private fun strokeShadowTextStyle(stroke: TextStroke, color: Int): TextStyle {
        val strokePaint = Paint().apply {
            this.color = color
            mode = PaintMode.STROKE
            strokeWidth = stroke.width.px
            isAntiAlias = true
        }
        return textStyle.copyStyle().apply {
            clearShadows()
            setForeground(strokePaint)
        }
    }

    private fun fillTextStyleWithoutShadows(): TextStyle =
        textStyle.copyStyle().apply { clearShadows() }

    private fun resolveParagraph(widthPx: Float): Paragraph {
        val safeWidth = widthPx.coerceAtLeast(1f)
        val cached = cachedFillParagraph
        if (cached != null && layoutWidthPx == safeWidth) return cached

        val paragraph = buildParagraph(textStyle, safeWidth)
        val hasStrokeAndShadows = stroke != null && textStyle.shadows.isNotEmpty()
        cachedFillParagraph = paragraph
        cachedStrokeParagraph = stroke?.let { buildParagraph(strokeTextStyle(it), safeWidth) }
        cachedEmphasisParagraph = textEmphasis.activeTextEmphasis()?.let { buildParagraph(emphasisTextStyle(it), safeWidth) }
        cachedShadowParagraphs = if (hasStrokeAndShadows) {
            val effectStroke = stroke
            textStyle.shadows.map {
                TextShadowParagraph(
                    shadow = it,
                    fill = buildParagraph(fillShadowTextStyle(it.color), safeWidth),
                    stroke = if (effectStroke.width.px > 0f)
                        buildParagraph(strokeShadowTextStyle(effectStroke, it.color), safeWidth)
                    else null,
                )
            }
        } else {
            emptyList()
        }
        cachedFillNoShadowParagraph = if (hasStrokeAndShadows) {
            buildParagraph(fillTextStyleWithoutShadows(), safeWidth)
        } else {
            null
        }
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

        val paragraphWidth = (maxContentWidth.px - visualOutset.horizontal).coerceAtLeast(1f)
        val paragraph = resolveParagraph(paragraphWidth)
        if (width.isNull()) {
            width = min(
                paragraph.maxIntrinsicWidth + visualOutset.horizontal,
                maxContentWidth.px.coerceAtLeast(0f)
            ).toDp() +
                    resolvedPadding.horizontal
        }
        if (height.isNull()) height = (paragraph.height + visualOutset.vertical).toDp() + resolvedPadding.vertical

        finishMeasure()
    }

    override fun place(bounds: LayoutBounds) {
        position = alignment.place(width, height, resolvedMargin, bounds)
        resolvePaintBounds()
    }

    override fun draw(canvas: Canvas) {
        drawBgBox(canvas, contentClipOutset = textEffectContentClipOutset(lineBoxClipOutset, textEmphasis)) {
            val fillParagraph = cachedFillParagraph ?: return@drawBgBox
            val x = it.left + visualOutset.left
            val y = it.top + visualOutset.top

            if (stroke == null) {
                cachedEmphasisParagraph?.paint(canvas, x, y)
                fillParagraph.paint(canvas, x, y)
                return@drawBgBox
            }

            cachedShadowParagraphs.forEach { shadowParagraph ->
                shadowParagraph.paint(canvas, it, x, y)
            }
            cachedStrokeParagraph?.paint(canvas, x, y)
            cachedEmphasisParagraph?.paint(canvas, x, y)
            (cachedFillNoShadowParagraph ?: fillParagraph).paint(canvas, x, y)
        }
    }
}

private fun TextStyle.withTextShadows(shadows: List<TextShadow>): TextStyle {
    if (shadows.isEmpty()) return this
    return copyStyle().apply {
        shadows.forEach {
            addShadow(
                org.jetbrains.skia.paragraph.Shadow(
                    it.color,
                    it.offsetX.px,
                    it.offsetY.px,
                    it.blur.px.toDouble()
                )
            )
        }
    }
}

private data class TextVisualOutset(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val horizontal: Float get() = left + right
    val vertical: Float get() = top + bottom
}

private data class TextShadowParagraph(
    val shadow: org.jetbrains.skia.paragraph.Shadow,
    val fill: Paragraph,
    val stroke: Paragraph?,
) {
    fun paint(canvas: Canvas, bounds: RRect, x: Float, y: Float) {
        val shadowX = x + shadow.offsetX
        val shadowY = y + shadow.offsetY
        val blur = shadow.blurSigma.toFloat()

        if (blur <= 0f) {
            fill.paint(canvas, shadowX, shadowY)
            stroke?.paint(canvas, shadowX, shadowY)
            return
        }

        val filter = ImageFilter.makeBlur(blur, blur, FilterTileMode.CLAMP, null, null)
        val paint = Paint().apply { imageFilter = filter }
        try {
            canvas.saveLayer(Rect.makeLTRB(bounds.left, bounds.top, bounds.right, bounds.bottom), paint)
            fill.paint(canvas, shadowX, shadowY)
            stroke?.paint(canvas, shadowX, shadowY)
            canvas.restore()
        } finally {
            paint.close()
            filter.close()
        }
    }
}

private fun textVisualOutset(
    stroke: TextStroke?,
    shadows: Array<org.jetbrains.skia.paragraph.Shadow>,
): TextVisualOutset {
    val strokeOutset = stroke?.width?.px ?: 0f
    var left = strokeOutset
    var top = strokeOutset
    var right = strokeOutset
    var bottom = strokeOutset

    shadows.forEach {
        val blurOutset = it.blurSigma.toFloat() * 3f
        // 阴影层包含描边轮廓，预留范围需要覆盖“描边 + 模糊”的最终外扩。
        val shadowOutset = strokeOutset + blurOutset
        left = max(left, (shadowOutset - it.offsetX).coerceAtLeast(0f))
        top = max(top, (shadowOutset - it.offsetY).coerceAtLeast(0f))
        right = max(right, (shadowOutset + it.offsetX).coerceAtLeast(0f))
        bottom = max(bottom, (shadowOutset + it.offsetY).coerceAtLeast(0f))
    }

    return TextVisualOutset(left, top, right, bottom)
}
