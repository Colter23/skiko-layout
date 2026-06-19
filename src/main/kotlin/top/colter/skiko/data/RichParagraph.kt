package top.colter.skiko.data

import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Image
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.paragraph.Alignment
import org.jetbrains.skia.paragraph.BaselineMode
import org.jetbrains.skia.paragraph.Paragraph
import org.jetbrains.skia.paragraph.ParagraphBuilder
import org.jetbrains.skia.paragraph.ParagraphStyle
import org.jetbrains.skia.paragraph.PlaceholderAlignment
import org.jetbrains.skia.paragraph.PlaceholderStyle
import org.jetbrains.skia.paragraph.TextStyle
import top.colter.skiko.FontRegistry
import top.colter.skiko.Fonts
import top.colter.skiko.copyStyle
import top.colter.skiko.lineBoxClipOutset
import top.colter.skiko.px

/**
 * 富文本原始行。
 */
public data class RichLine(
    public val height: Float,
    public val width: Float,
    public val nodes: List<RichText>
) {
    public constructor(nodes: List<RichText>) : this(0f, 0f, nodes)
    public constructor(height: Float, nodes: List<RichText>) : this(height, 0f, nodes)
}

/**
 * 富文本段落。
 *
 * 通过 [RichParagraphBuilder] 构建。
 */
public class RichParagraph(
    public val defaultStyle: TextStyle,
    public val lines: List<RichLine>
) {
    public val height: Float get() = lines.fold(0f) { acc, line -> acc + line.height }
    public val width: Float get() = lines.maxOfOrNull { it.width } ?: 0f
}

public data class RichParagraphLineMetric(
    public val startIndex: Int,
    public val endIndex: Int,
    public val width: Float,
    public val left: Float,
    public val right: Float,
    public val height: Float,
    public val baseline: Float,
    public val isHardBreak: Boolean,
)

public data class RichParagraphPlaceholder(
    public val img: Image,
    public val rect: Rect,
)

public class RichParagraphLayout(
    public val paragraph: Paragraph?,
    public val width: Float,
    public val height: Float,
    public val placeholders: List<RichParagraphPlaceholder>,
    public val lineMetrics: List<RichParagraphLineMetric>,
    public val didExceedMaxLines: Boolean,
    public val lineBoxClipOutset: Float = 0f,
) {
    public val lineCount: Int get() = lineMetrics.size

    public companion object {
        public fun empty(): RichParagraphLayout =
            RichParagraphLayout(
                paragraph = null,
                width = 0f,
                height = 0f,
                placeholders = emptyList(),
                lineMetrics = emptyList(),
                didExceedMaxLines = false,
                lineBoxClipOutset = 0f,
            )
    }
}

/**
 * 根据宽度计算富文本真实布局。
 */
public fun RichParagraph.layout(
    width: Float,
    maxLinesCount: Int = Int.MAX_VALUE,
    fontRegistry: FontRegistry = Fonts.default,
    alignment: Alignment = Alignment.START,
): RichParagraphLayout {
    if (maxLinesCount <= 0 || lines.isEmpty()) return RichParagraphLayout.empty()

    val maxWidth = width.coerceAtLeast(1f)
    val builtParagraph = buildParagraph(
        width = maxWidth,
        maxLinesCount = maxLinesCount,
        fontRegistry = fontRegistry,
        alignment = alignment,
    )
    val paragraph = builtParagraph.paragraph
    val lineMetrics = paragraph.lineMetrics.map {
        RichParagraphLineMetric(
            startIndex = it.startIndex,
            endIndex = it.endIndex,
            width = it.width.toFloat(),
            left = it.left.toFloat(),
            right = it.right.toFloat(),
            height = it.height.toFloat(),
            baseline = it.baseline.toFloat(),
            isHardBreak = it.isHardBreak,
        )
    }
    val placeholders = paragraph.rectsForPlaceholders
        .zip(builtParagraph.emojiNodes)
        .map { (box, node) ->
            RichParagraphPlaceholder(
                img = node.img,
                rect = box.rect,
            )
        }
    val layoutWidth = sequenceOf(
        paragraph.longestLine,
        lineMetrics.maxOfOrNull { it.right } ?: 0f,
        placeholders.maxOfOrNull { it.rect.right } ?: 0f,
    ).maxOrNull()
        ?.coerceAtMost(maxWidth)
        ?: 0f

    return RichParagraphLayout(
        paragraph = paragraph,
        width = layoutWidth,
        height = paragraph.height,
        placeholders = placeholders,
        lineMetrics = lineMetrics,
        didExceedMaxLines = paragraph.didExceedMaxLines(),
        lineBoxClipOutset = builtParagraph.lineBoxClipOutset,
    )
}

/**
 * 构建仅用于绘制视觉加粗的 Paragraph。
 *
 * 返回的 Paragraph 使用与正常布局相同的文字和占位符序列，但只负责绘制文字描边，
 * 不参与测量、换行和图片 Emoji 绘制。
 */
public fun RichParagraph.emphasisParagraph(
    width: Float,
    maxLinesCount: Int = Int.MAX_VALUE,
    fontRegistry: FontRegistry = Fonts.default,
    alignment: Alignment = Alignment.START,
    textEmphasis: TextEmphasis?,
): Paragraph? {
    val activeTextEmphasis = textEmphasis.activeTextEmphasis() ?: return null
    if (maxLinesCount <= 0 || lines.isEmpty()) return null

    return buildParagraph(
        width = width.coerceAtLeast(1f),
        maxLinesCount = maxLinesCount,
        fontRegistry = fontRegistry,
        alignment = alignment,
        textEmphasis = activeTextEmphasis,
    ).paragraph
}

private fun RichParagraph.buildParagraph(
    width: Float,
    maxLinesCount: Int,
    fontRegistry: FontRegistry,
    alignment: Alignment,
    textEmphasis: TextEmphasis? = null,
): BuiltRichParagraph {
    val emojiNodes = mutableListOf<RichText.Emoji>()
    var lineBoxClipOutset = 0f
    val paragraphStyle = ParagraphStyle().apply {
        this.maxLinesCount = maxLinesCount
        this.alignment = alignment
        textStyle = fontRegistry.resolveTextStyle(defaultStyle).also {
            lineBoxClipOutset = maxOf(lineBoxClipOutset, it.lineBoxClipOutset(defaultStyle))
        }.withTextEmphasis(textEmphasis)
    }
    val builder = ParagraphBuilder(paragraphStyle, fontRegistry.fonts)

    lines.forEachIndexed { lineIndex, line ->
        line.nodes.forEach { node ->
            when (node) {
                is RichText.Text -> {
                    val style = node.style ?: defaultStyle
                    val resolvedStyle = fontRegistry.resolveTextStyle(style, defaultStyle)
                    lineBoxClipOutset = maxOf(lineBoxClipOutset, resolvedStyle.lineBoxClipOutset(defaultStyle))
                    builder.pushStyle(resolvedStyle.withTextEmphasis(textEmphasis))
                    builder.addText(node.value)
                    builder.popStyle()
                }

                is RichText.Emoji -> {
                    val style = node.style ?: defaultStyle
                    val placeholderSize = style.emojiPlaceholderSize(defaultStyle)
                    val scaledPlaceholderSize = (placeholderSize * node.scale).coerceAtLeast(1f)
                    val resolvedStyle = fontRegistry.resolveTextStyle(style, defaultStyle)
                    lineBoxClipOutset = maxOf(lineBoxClipOutset, resolvedStyle.lineBoxClipOutset(defaultStyle))
                    builder.pushStyle(resolvedStyle.withTextEmphasis(textEmphasis))
                    builder.addPlaceholder(
                        PlaceholderStyle(
                            scaledPlaceholderSize,
                            scaledPlaceholderSize,
                            PlaceholderAlignment.MIDDLE,
                            BaselineMode.ALPHABETIC,
                            0f,
                        )
                    )
                    builder.popStyle()
                    emojiNodes.add(node)
                }
            }
        }
        if (lineIndex < lines.lastIndex) builder.addText("\n")
    }

    return BuiltRichParagraph(
        paragraph = builder.build().layout(width.coerceAtLeast(1f)),
        emojiNodes = emojiNodes,
        lineBoxClipOutset = lineBoxClipOutset,
    )
}

/**
 * 绘制富文本真实布局。
 */
public fun RichParagraphLayout.print(
    canvas: Canvas,
    x: Float,
    y: Float,
    emphasisParagraph: Paragraph? = null,
) {
    emphasisParagraph?.paint(canvas, x, y)
    paragraph?.paint(canvas, x, y)

    placeholders.forEach { placeholder ->
        val srcRect = Rect.makeXYWH(
            0f,
            0f,
            placeholder.img.width.toFloat(),
            placeholder.img.height.toFloat(),
        )
        val rect = placeholder.rect
        val imageAspect = placeholder.img.width.toFloat() / placeholder.img.height.toFloat()
        val rectAspect = rect.width / rect.height
        val (drawWidth, drawHeight) = if (imageAspect >= rectAspect) {
            val height = rect.width / imageAspect
            rect.width to height
        } else {
            val width = rect.height * imageAspect
            width to rect.height
        }
        val drawLeft = x + rect.left + (rect.width - drawWidth) / 2f
        val drawTop = y + rect.top + (rect.height - drawHeight) / 2f
        val targetRect = Rect.makeLTRB(
            drawLeft,
            drawTop,
            drawLeft + drawWidth,
            drawTop + drawHeight,
        )
        canvas.drawImageRect(
            placeholder.img,
            srcRect,
            targetRect,
            SamplingMode.MITCHELL,
            null,
            true,
        )
    }
}

internal fun TextEmphasis?.activeTextEmphasis(): TextEmphasis? =
    this?.takeIf { it.width.px > 0f }

private fun TextStyle.withTextEmphasis(textEmphasis: TextEmphasis?): TextStyle {
    val activeTextEmphasis = textEmphasis.activeTextEmphasis() ?: return this
    val strokePaint = Paint().apply {
        color = activeTextEmphasis.color ?: foreground?.color ?: this@withTextEmphasis.color
        mode = PaintMode.STROKE
        strokeWidth = activeTextEmphasis.width.px
        isAntiAlias = true
    }
    return copyStyle().apply {
        clearShadows()
        setForeground(strokePaint)
    }
}

private data class BuiltRichParagraph(
    val paragraph: Paragraph,
    val emojiNodes: List<RichText.Emoji>,
    val lineBoxClipOutset: Float,
)

private const val DEFAULT_FONT_SIZE: Float = 14f

private fun TextStyle.emojiPlaceholderSize(fallbackStyle: TextStyle): Float {
    return fontSize
        .takeIf { it > 0f }
        ?: fallbackStyle.fontSize.takeIf { it > 0f }
        ?: DEFAULT_FONT_SIZE
}
