package top.colter.skiko.layout

import org.jetbrains.skia.*
import top.colter.skiko.Modifier
import top.colter.skiko.data.*
import top.colter.skiko.toDp


/**
 * ## 富文本
 */
fun Layout.RichText(
    paragraph: RichParagraph,
    maxLinesCount: Int = 50,
    alignment: LayoutAlignment = LayoutAlignment.TOP_LEFT,
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

class RichTextLayout(
    val paragraph: RichParagraph,
    val alignment: LayoutAlignment,
    val maxLinesCount: Int,
    modifier: Modifier,
    parentLayout: Layout
) : Layout(modifier, parentLayout) {

    private var layoutParagraph: RichParagraph? = null

    override fun measure(deep: Boolean) {
        preMeasure()

        val maxWidth = if (modifier.width.isNotNull()) modifier.contentWidth.px
        else if (modifier.maxWidth.isNotNull()) modifier.maxWidth.px
        else if (!modifier.fillWidth) parentLayout!!.modifier.contentWidth.px - modifier.margin.horizontal.px
        else 0f

        if (layoutParagraph == null && maxWidth != 0f) {
            layoutParagraph = paragraph.layout(maxWidth)
            if (height.isNull()) height = layoutParagraph!!.height.toDp()
        }

    }

    override fun place(bounds: LayoutBounds) {
        position = alignment.place(width, height, modifier, bounds)
    }

    override fun draw(canvas: Canvas) {
        layoutParagraph?.print(canvas, position.x.px, position.y.px)
    }

}
