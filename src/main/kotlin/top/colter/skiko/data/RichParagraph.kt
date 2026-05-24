package top.colter.skiko.data

import org.jetbrains.skia.*
import org.jetbrains.skia.paragraph.Paragraph
import org.jetbrains.skia.paragraph.ParagraphBuilder
import org.jetbrains.skia.paragraph.ParagraphStyle
import org.jetbrains.skia.paragraph.TextStyle
import top.colter.skiko.FontUtils
import java.util.IdentityHashMap
import kotlin.math.max


/**
 * 富文本行
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
 * 富文本文章
 *
 * 通过 [RichParagraphBuilder] 构建
 */
public class RichParagraph(
    public val defaultStyle: TextStyle,
    public val lines: List<RichLine>
) {
    public val height: Float get() = lines.fold(0f) { acc, line -> acc + line.height }
    public val width: Float get() = lines.maxOfOrNull { it.width } ?: 0f
}


/**
 * 根据宽度计算换行
 */
public fun RichParagraph.layout(width: Float, maxLinesCount: Int = Int.MAX_VALUE): RichParagraph {
    if (maxLinesCount <= 0) return RichParagraph(defaultStyle, emptyList())

    val maxWidth = width.coerceAtLeast(1f)
    val result = mutableListOf<RichLine>()
    val fontCache = IdentityHashMap<TextStyle, Font>()
    val lineCache = IdentityHashMap<TextStyle, MutableMap<String, TextLine>>()

    fun styleOf(style: TextStyle?): TextStyle = style ?: defaultStyle
    fun fontOf(style: TextStyle): Font = fontCache.getOrPut(style) { style.toFont(defaultStyle) }
    fun textLine(style: TextStyle, value: String): TextLine {
        val cache = lineCache.getOrPut(style) { HashMap() }
        return cache.getOrPut(value) { TextLine.make(value, fontOf(style)) }
    }
    fun blankHeight(style: TextStyle): Float = textLine(style, "|").height

    var currentNodes = mutableListOf<RichText>()
    var currentWidth = 0f
    var currentHeight = 0f
    var truncated = false

    fun flushLine(force: Boolean, fallbackHeight: Float): Boolean {
        if (!force && currentNodes.isEmpty()) return true
        if (result.size >= maxLinesCount) return false

        val lineHeight = if (currentHeight > 0f) currentHeight else fallbackHeight
        result.add(RichLine(lineHeight, currentWidth, currentNodes))
        currentNodes = mutableListOf()
        currentWidth = 0f
        currentHeight = 0f
        return true
    }

    fun appendAdvance(width: Float, height: Float, spacing: Float) {
        if (currentWidth > 0f) currentWidth += spacing
        currentWidth += width
        currentHeight = max(currentHeight, height)
    }

    loop@ for (sourceLine in lines) {
        if (sourceLine.nodes.isEmpty()) {
            truncated = !flushLine(true, blankHeight(defaultStyle))
            if (truncated) break
            continue
        }

        for (node in sourceLine.nodes) {
            when (node) {
                is RichText.Text -> {
                    val style = styleOf(node.style)
                    val buffer = StringBuilder()

                    fun flushTextBuffer() {
                        if (buffer.isNotEmpty()) {
                            currentNodes.add(RichText.Text(buffer.toString(), node.style))
                            buffer.clear()
                        }
                    }

                    for (point in node.value.codePoints()) {
                        val value = String(Character.toChars(point))
                        if (value == "\n") {
                            flushTextBuffer()
                            currentHeight = max(currentHeight, blankHeight(style))
                            truncated = !flushLine(true, blankHeight(style))
                            if (truncated) break@loop
                            continue
                        }

                        val line = textLine(style, value)
                        val spacing = style.letterSpacing
                        val advance = line.width + if (currentWidth > 0f) spacing else 0f
                        if (currentWidth > 0f && currentWidth + advance > maxWidth) {
                            flushTextBuffer()
                            truncated = !flushLine(true, line.height)
                            if (truncated) break@loop
                        }

                        buffer.append(value)
                        appendAdvance(line.width, line.height, spacing)
                    }
                    flushTextBuffer()
                }

                is RichText.Emoji -> {
                    val style = styleOf(node.style)
                    val line = node.textLine(style, defaultStyle)
                    val spacing = style.letterSpacing
                    val advance = line.width + if (currentWidth > 0f) spacing else 0f
                    if (currentWidth > 0f && currentWidth + advance > maxWidth) {
                        truncated = !flushLine(true, line.height)
                        if (truncated) break@loop
                    }

                    currentNodes.add(node)
                    appendAdvance(line.width, line.height, spacing)
                }
            }
        }

        if (currentNodes.isNotEmpty()) {
            truncated = !flushLine(true, blankHeight(defaultStyle))
            if (truncated) break
        }
    }

    return RichParagraph(defaultStyle, result)
}


/**
 * 绘制
 */
public fun RichParagraph.print(canvas: Canvas, x: Float, y: Float) {
    val paragraphStyle = ParagraphStyle()
    var currY = y

    for (line in lines) {
        var currX = x
        for (node in line.nodes) {
            when (node) {
                is RichText.Text -> {
                    val style = node.style ?: defaultStyle
                    val paragraph = node.paragraph(style, paragraphStyle)
                    val cy = currY + if (paragraph.height < line.height) line.height - paragraph.height else 0f
                    paragraph.paint(canvas, currX, cy)
                    currX += paragraph.maxIntrinsicWidth
                }

                is RichText.Emoji -> {
                    val style = node.style ?: defaultStyle
                    val emojiLine = node.textLine(style, defaultStyle)
                    val cy = currY + if (emojiLine.height < line.height) line.height - emojiLine.height else 0f
                    val srcRect = Rect.makeXYWH(0f, 0f, node.img.width.toFloat(), node.img.height.toFloat())
                    val tarRect = Rect.makeXYWH(currX, cy, emojiLine.width, emojiLine.height)
                    canvas.drawImageRect(
                        node.img,
                        srcRect,
                        tarRect,
                        FilterMipmap(FilterMode.LINEAR, MipmapMode.NEAREST),
                        null,
                        true
                    )
                    currX += emojiLine.width
                }
            }
        }
        currY += line.height
    }
}

private const val DEFAULT_FONT_SIZE: Float = 14f
private const val EMOJI_MEASURE_TEXT: String = "\uD83D\uDE42"

private fun TextStyle.toFont(fallbackStyle: TextStyle? = null): Font {
    val size = fontSize.takeIf { it > 0f }
        ?: fallbackStyle?.fontSize?.takeIf { it > 0f }
        ?: DEFAULT_FONT_SIZE
    val typeface = typeface
        ?: resolveTypeface()
        ?: fallbackStyle?.typeface
        ?: fallbackStyle?.resolveTypeface()
        ?: FontUtils.defaultFont
    return Font(typeface, size)
}

private fun TextStyle.resolveTypeface(): Typeface? {
    val familyName = fontFamilies.firstOrNull() ?: return null
    val set = FontUtils.matchFamily(familyName)
    return if (set.count() > 0) set.getTypeface(0) else null
}

private fun RichText.Text.paragraph(style: TextStyle, paragraphStyle: ParagraphStyle): Paragraph {
    val cached = drawCacheParagraph
    if (cached != null && drawCacheStyle === style) return cached

    val paragraph = ParagraphBuilder(paragraphStyle, FontUtils.fonts)
        .pushStyle(style)
        .addText(value)
        .build()
        .layout(10000f)
    drawCacheStyle = style
    drawCacheParagraph = paragraph
    return paragraph
}

private fun RichText.Emoji.textLine(style: TextStyle, fallbackStyle: TextStyle): TextLine {
    val cached = drawCacheLine
    if (cached != null && drawCacheStyle === style) return cached

    val line = TextLine.make(EMOJI_MEASURE_TEXT, style.toFont(fallbackStyle))
    drawCacheStyle = style
    drawCacheLine = line
    return line
}
