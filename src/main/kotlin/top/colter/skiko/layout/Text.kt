package top.colter.skiko.layout

import org.jetbrains.skia.*
import org.jetbrains.skia.paragraph.*
import top.colter.skiko.*
import top.colter.skiko.data.LayoutAlignment
import top.colter.skiko.data.place


/**
 * ## 纯文本
 *
 * 由于文本不是一个完整的盒子，如果达不到想要的效果，可以试着在外面套一层 [Box]
 *
 */
fun Layout.Text(
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
fun Layout.Text(
    text: String,
    textStyle: TextStyle,
    alignment: LayoutAlignment = LayoutAlignment.TOP_LEFT,
    maxLinesCount: Int = 1,
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

class TextLayout(
    val text: String,
    val textStyle: TextStyle,
    val alignment: LayoutAlignment,
    val maxLinesCount: Int,
    modifier: Modifier,
    parentLayout: Layout,
) : Layout(modifier, parentLayout) {

    private val paragraphStyle = ParagraphStyle().apply {
        maxLinesCount = this@TextLayout.maxLinesCount
        ellipsis = "..."
        alignment = Alignment.START // this@TextLayout.alignment.toAlignment()
        textStyle = this@TextLayout.textStyle
    }

    private val paragraph = ParagraphBuilder(paragraphStyle, FontUtils.fonts).addText(text).build()
    private var layoutParagraph: Paragraph? = null


    override fun measure(deep: Boolean) {
        preMeasure()
        val maxWidth = if (modifier.width.isNotNull()) modifier.contentWidth.px
        else if (modifier.maxWidth.isNotNull()) modifier.maxWidth.px
        else if (!modifier.fillWidth) parentLayout!!.modifier.contentWidth.px - modifier.margin.horizontal.px
        else 0f

        if (maxWidth != 0f) {
            layoutParagraph = paragraph.layout(maxWidth)
            if (width.isNull()) width = layoutParagraph!!.maxIntrinsicWidth.toDp()
            if (height.isNull()) height = layoutParagraph!!.height.toDp()
        }

    }

    override fun place(bounds: LayoutBounds) {
        position = alignment.place(width, height, modifier, bounds)
    }

    override fun draw(canvas: Canvas) {
        layoutParagraph?.paint(canvas, position.x.px, position.y.px)
    }

}