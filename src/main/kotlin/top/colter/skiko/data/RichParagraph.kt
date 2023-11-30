package top.colter.skiko.data

import org.jetbrains.skia.*
import org.jetbrains.skia.paragraph.ParagraphBuilder
import org.jetbrains.skia.paragraph.ParagraphStyle
import org.jetbrains.skia.paragraph.TextStyle
import top.colter.skiko.FontUtils
import top.colter.skiko.sumOf
import kotlin.math.max


/**
 * 富文本行
 */
public class RichLine(
    public val height: Float,
    public val nodes: List<RichText>
) {
    public constructor(nodes: List<RichText>) : this(0f, nodes)
}


/**
 * 富文本文章
 */
public class RichParagraph(
    public val defaultStyle: TextStyle,
    public val lines: List<RichLine>
) {
    public val height: Float get() = lines.sumOf { it.height }
}


/**
 * 根据宽度计算换行
 */
public fun RichParagraph.layout(width: Float): RichParagraph {
    val tempParagraph = mutableListOf<RichLine>()
    val defaultFont =
        Font(FontUtils.matchFamily(defaultStyle.fontFamilies.first()).getTypeface(0), defaultStyle.fontSize)

    for (line in lines) {
        var tempLine = mutableListOf<RichText>()
        var currWidth = 0f
        var maxHeight = 0f

        // 换行
        val wrap: (RichText?) -> Unit = {
            it?.let { tempLine.add(it) }
            if (tempLine.isNotEmpty()) {
                tempParagraph.add(RichLine(maxHeight, tempLine))
                tempLine = mutableListOf()
                currWidth = 0f
                maxHeight = 0f
            }
        }
        val wrapText: (StringBuilder, TextStyle?) -> Unit = { sb, s ->
            wrap(RichText.Text(sb.toString(), s))
            sb.clear()
        }

        for (node in line.nodes) {
            when (node) {
                is RichText.Text -> {
                    val font = if (node.style == null) defaultFont
                    else Font(
                        FontUtils.matchFamily(node.style.fontFamilies.first()).getTypeface(0),
                        node.style.fontSize
                    )
                    val tempText = StringBuilder()
                    // 根据码点分割字符串
                    for (point in node.value.codePoints()) {
                        val c = String(intArrayOf(point), 0, intArrayOf(point).size)
                        if (c == "\n") {
                            wrapText(tempText, node.style)
                        } else {
                            val charLine = TextLine.make(c, font)
                            if (currWidth + charLine.width > width) {
                                wrapText(tempText, node.style)
                            }
                            currWidth += charLine.width + (node.style?.letterSpacing ?: defaultStyle.letterSpacing)
                            maxHeight = max(maxHeight, charLine.height)
                            tempText.append(c)
                        }
                    }
                    if (tempText.toString() == node.value) tempLine.add(node)
                    else tempLine.add(RichText.Text(tempText.toString(), node.style))
                }

                is RichText.Emoji -> {
                    val font = if (node.style == null) defaultFont
                    else Font(
                        FontUtils.matchFamily(node.style.fontFamilies.first()).getTypeface(0),
                        node.style.fontSize
                    )
                    // 用来测量对应字体下emoji的大小
                    val emojiText = TextLine.make("🙂", font)
                    if (currWidth + emojiText.width > width) wrap(null)
                    currWidth += emojiText.width
                    maxHeight = max(maxHeight, emojiText.height)
                    tempLine.add(node)
                }
            }

        }

        wrap(null)
    }

    return RichParagraph(defaultStyle, tempParagraph)
}


/**
 * 绘制
 */
public fun RichParagraph.print(canvas: Canvas, x: Float, y: Float) {
    val defaultFont = Font(defaultStyle.typeface, defaultStyle.fontSize)
    val paragraphStyle = ParagraphStyle()
    var currY = y
    for (line in lines) {
        var currX = x
        for (node in line.nodes) {
            when (node) {
                is RichText.Text -> {
                    val style = node.style ?: defaultStyle
                    val paragraph = ParagraphBuilder(paragraphStyle, FontUtils.fonts)
                        .pushStyle(style)
                        .addText(node.value)
                        .build()
                        .layout(10000f)
                    val cy = currY + if (paragraph.height < line.height) line.height - paragraph.height else 0f
                    currX += paragraph.paint(canvas, currX, cy).maxIntrinsicWidth
                    // 使用TextLine绘制
//                    val font = if (node.style == null) defaultFont
//                    else Font(FontUtils.matchFamily(node.style.fontFamilies.first()).getTypeface(0), node.style.fontSize)
//                    val lineNode = TextLine.make(node.value, font)
//                    val cy = currY + if (lineNode.height < line.height) line.height - lineNode.height else 0f
//                    canvas.drawTextLine(lineNode, currX, cy + lineNode.height, Paint().apply {
//                        color = style.color
//                    })
//                    currX += lineNode.width
                }

                is RichText.Emoji -> {
                    val font = if (node.style == null) defaultFont
                    else Font(
                        FontUtils.matchFamily(node.style.fontFamilies.first()).getTypeface(0),
                        node.style.fontSize
                    )
                    // img 为空时
//                    if (node.img == null) {
//                        val emojiLine = TextLine.make(node.value, font)
//                        val cy = currY + if (emojiLine.height < line.height) line.height - emojiLine.height else 0f
//                        canvas.drawTextLine(emojiLine, currX, cy + emojiLine.height * 0.8f, Paint())
//                        currX += emojiLine.width
//                    }else {
                    val emojiLine = TextLine.make("🙂", font)
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