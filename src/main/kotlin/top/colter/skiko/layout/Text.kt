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
 * 由于文本不是一个完整的盒子，如果达不到想要的效果，可以试着在外面套一层 [Box]
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
 */
public fun Layout.Text(
    text: String,
    textStyle: TextStyle,
    maxLinesCount: Int = 1,
    alignment: LayoutAlignment = LayoutAlignment.TOP_LEFT,
    modifier: Modifier = Modifier(),
) {
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

    private val paragraph = ParagraphBuilder(paragraphStyle, FontUtils.fonts).addText(text).build()
    private var layoutParagraph: Paragraph? = null


    override fun measure(deep: Boolean) {
        // 第一遍计算宽高
        preMeasure()

        // 计算宽度
        val maxWidth = if (modifier.width.isNotNull()) modifier.contentWidth.px
        else if (modifier.maxWidth.isNotNull()) modifier.maxWidth.px
        else if (!modifier.fillWidth) parentLayout!!.modifier.contentWidth.px - modifier.margin.horizontal.px
        else 0f

        // 进行布局 确定高度
        if (maxWidth != 0f) {
            layoutParagraph = paragraph.layout(maxWidth)
            if (width.isNull()) width = layoutParagraph!!.maxIntrinsicWidth.toDp()
            if (height.isNull()) height = layoutParagraph!!.height.toDp()
        }

    }

    override fun place(bounds: LayoutBounds) {
        // 确定当前元素位置
        position = LayoutAlignment.CENTER_LEFT.place(width, height, modifier, bounds)
    }

    override fun draw(canvas: Canvas) {
        // 绘制文本
        layoutParagraph?.paint(canvas, position.x.px, position.y.px)
    }

}