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

    private var layoutParagraph: Paragraph? = null

    override fun measure(deep: Boolean) {
        // 第一遍计算宽高
        preMeasure()

        // 计算宽度
        val maxWidth = if (modifier.width.isNotNull()) modifier.contentWidth
        else if (modifier.maxWidth.isNotNull()) modifier.maxWidth
        else if (!modifier.fillWidth && parentLayout!!.modifier.contentWidth == 0.dp) 10000.dp
        else if (!modifier.fillWidth) parentLayout!!.modifier.contentWidth - modifier.margin.horizontal
        else 0.dp

        // 进行布局 确定宽高
        if (maxWidth != 0.dp) {
            val paragraph = ParagraphBuilder(paragraphStyle, FontUtils.fonts).addText(text).build()
            layoutParagraph = paragraph.layout(maxWidth.px)
            if (width.isNull()) width = min(layoutParagraph!!.maxIntrinsicWidth, maxWidth.px).toDp()
            if (height.isNull()) height = layoutParagraph!!.height.toDp()
        }

    }

    override fun place(bounds: LayoutBounds) {
        // 确定当前元素位置
        position = alignment.place(width, height, modifier, bounds)
    }

    override fun draw(canvas: Canvas) {
        // 绘制盒子
        drawBgBox(canvas)
        // 绘制文本
        layoutParagraph?.paint(canvas, position.x.px, position.y.px)
    }

}