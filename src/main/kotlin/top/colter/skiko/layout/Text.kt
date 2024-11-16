package top.colter.skiko.layout

import org.jetbrains.skia.*
import org.jetbrains.skia.paragraph.*
import top.colter.skiko.*
import top.colter.skiko.data.LayoutAlignment
import top.colter.skiko.data.place
import top.colter.skiko.data.toAlignment


/**
 * ## 纯文本
 *
 */
public fun Layout.Text(
    text: String,
    color: Int = Color.BLACK,
    fontSize: Dp = 18.dp,
    fontFamily: String = "Source Han Sans",
    fontStyle: FontStyle = FontStyle.NORMAL,
    maxLinesCount: Int = 1,
    alignment: LayoutAlignment = LayoutAlignment.TOP_LEFT,
    modifier: Modifier = Modifier(),
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
        maxLinesCount = maxLinesCount,
        modifier = modifier,
    )
}

/**
 * ## 纯文本
 *
 */
public fun Layout.Text(
    text: String,
    textStyle: TextStyle,
    maxLinesCount: Int = 1,
    alignment: LayoutAlignment = LayoutAlignment.TOP_LEFT,
    modifier: Modifier = Modifier(),
) {
    // 检查字体是否在字体集中
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
    public val maxLinesCount: Int,
    modifier: Modifier,
    parentLayout: Layout,
) : Layout(modifier, parentLayout) {

    private val paragraphStyle = ParagraphStyle().apply {
        maxLinesCount = this@TextLayout.maxLinesCount
        ellipsis = "..."
        alignment = this@TextLayout.alignment.toAlignment()
        textStyle = this@TextLayout.textStyle
    }

    private var layoutParagraph: Paragraph? = null
    private var layoutTextLine: TextLine? = null

    override fun measure(deep: Boolean) {
        // 第一遍计算宽高
        preMeasure()

        // 计算宽度
        val maxWidth = if (modifier.width.isNotNull()) modifier.contentWidth.px
        else if (modifier.maxWidth.isNotNull()) modifier.maxWidth.px
        else if (!modifier.fillWidth) parentLayout!!.modifier.contentWidth.px - modifier.margin.horizontal.px
        else 0f

        // 进行布局 如果可以确定宽度, 就用 Paragraph, 确认不了宽度就用 TextLine
        if (maxWidth != 0f) {
            val paragraph = ParagraphBuilder(paragraphStyle, FontUtils.fonts).addText(text).build()
            layoutParagraph = paragraph.layout(maxWidth)
            if (width.isNull()) width = layoutParagraph!!.maxIntrinsicWidth.toDp()
            if (height.isNull()) height = layoutParagraph!!.height.toDp()
        }else {
            val font = Font(FontUtils.matchFamily(textStyle.fontFamilies.first()).matchStyle(textStyle.fontStyle))
            font.size = textStyle.fontSize
            layoutTextLine = TextLine.make(text, font)
            if (width.isNull()) width = layoutTextLine!!.width.toDp()
            if (height.isNull()) height = layoutTextLine!!.height.toDp()
        }

    }

    override fun place(bounds: LayoutBounds) {
        // 确定当前元素位置
        position = LayoutAlignment.CENTER_LEFT.place(width, height, modifier, bounds)
    }

    override fun draw(canvas: Canvas) {
        // 绘制盒子
        drawBgBox(canvas)
        // 绘制文本
        layoutParagraph?.paint(canvas, position.x.px, position.y.px)
        if (layoutParagraph == null) {
            layoutTextLine?.let {
                val textY = position.y.px + it.capHeight + ((it.height - it.capHeight) / 2)
                canvas.drawTextLine(it, position.x.px, textY, Paint().apply {
                    color = textStyle.color
                })
            }
        }
    }

}